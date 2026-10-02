package com.aniflow.domain.intelligence.resolver

import com.aniflow.domain.intelligence.model.ConfidenceLevel
import com.aniflow.domain.intelligence.model.ConfidenceThresholds
import com.aniflow.domain.intelligence.model.ConflictStatus
import com.aniflow.domain.intelligence.model.MetadataConflict
import com.aniflow.domain.intelligence.model.ParsingWarning
import com.aniflow.domain.intelligence.model.ReleaseConfidence
import com.aniflow.domain.intelligence.model.TokenEvidence

/**
 * Resolves contradictory detections deterministically without silent guessing (Section 31, 83).
 */
class ConflictResolver {

    fun resolveSeason(
        seasons: List<Pair<Int, TokenEvidence>>
    ): Pair<Int?, List<MetadataConflict>> {
        if (seasons.isEmpty()) return null to emptyList()
        val distinct = seasons.map { it.first }.distinct()

        if (distinct.size == 1) {
            return seasons.first().first to emptyList()
        }

        // Conflict: Pick highest confidence evidence but explicitly record the conflict
        val sorted = seasons.sortedByDescending { it.second.confidence }
        val winner = sorted.first()
        val runnerUp = sorted[1]

        val conflict = MetadataConflict(
            field = "Season",
            candidateA = winner.first.toString(),
            candidateB = runnerUp.first.toString(),
            evidenceA = winner.second,
            evidenceB = runnerUp.second,
            status = ConflictStatus.ResolvedByPrecedence,
            resolvedValue = winner.first.toString()
        )

        return winner.first to listOf(conflict)
    }

    fun resolveCodecs(
        codecs: List<Pair<String, TokenEvidence>>
    ): Pair<String?, List<MetadataConflict>> {
        if (codecs.isEmpty()) return null to emptyList()
        val distinct = codecs.map { it.first }.distinct()

        if (distinct.size == 1) {
            return codecs.first().first to emptyList()
        }

        val conflict = MetadataConflict(
            field = "VideoCodec",
            candidateA = codecs[0].first,
            candidateB = codecs[1].first,
            evidenceA = codecs[0].second,
            evidenceB = codecs[1].second,
            status = ConflictStatus.Ambiguous,
            resolvedValue = null // Unresolved conflict, ambiguous
        )

        return null to listOf(conflict)
    }
}

/**
 * Computes decision-critical multi-dimensional and overall confidence ratings (Section 33, 34, 85, 86).
 */
class ConfidenceEngine {

    fun calculate(
        episodeConfidence: Float,
        seasonConfidence: Float,
        titleConfidence: Float,
        technicalConfidence: Float,
        sourceConfidence: Float,
        conflicts: List<MetadataConflict>,
        warnings: List<ParsingWarning>
    ): ReleaseConfidence {
        // Weighted composite confidence
        // Title Identity: 35%, Episode/Coverage: 30%, Season: 15%, Technical: 12%, Source: 8%
        val rawWeighted = (titleConfidence * 0.35f) +
                (episodeConfidence * 0.30f) +
                (seasonConfidence * 0.15f) +
                (technicalConfidence * 0.12f) +
                (sourceConfidence * 0.08f)

        // Penalty for conflicts and warnings
        val conflictPenalty = conflicts.size * 0.20f
        val warningPenalty = warnings.size * 0.05f

        val finalScore = (rawWeighted - conflictPenalty - warningPenalty).coerceIn(0f, 1f)

        // Decision-critical constraint (Section 85, 86):
        // If Title confidence or Episode confidence is low, overall score cannot be Confirmed or High!
        val cappedScore = if (titleConfidence < ConfidenceThresholds.MEDIUM || episodeConfidence < ConfidenceThresholds.MEDIUM) {
            finalScore.coerceAtMost(0.59f)
        } else {
            finalScore
        }

        val level = when {
            cappedScore >= ConfidenceThresholds.CONFIRMED -> ConfidenceLevel.Confirmed
            cappedScore >= ConfidenceThresholds.HIGH -> ConfidenceLevel.High
            cappedScore >= ConfidenceThresholds.MEDIUM -> ConfidenceLevel.Medium
            else -> ConfidenceLevel.Low
        }

        return ReleaseConfidence(
            overall = cappedScore,
            level = level,
            identityConfidence = titleConfidence,
            coverageConfidence = episodeConfidence,
            technicalConfidence = technicalConfidence
        )
    }

    // Double overload for backward compatibility
    fun calculate(
        episodeConfidence: Double,
        seasonConfidence: Double,
        titleConfidence: Double,
        technicalConfidence: Double,
        sourceConfidence: Double,
        conflicts: List<MetadataConflict>,
        warnings: List<ParsingWarning>
    ): com.aniflow.domain.intelligence.model.CompositeConfidence {
        val relConf = calculate(
            episodeConfidence.toFloat(),
            seasonConfidence.toFloat(),
            titleConfidence.toFloat(),
            technicalConfidence.toFloat(),
            sourceConfidence.toFloat(),
            conflicts,
            warnings
        )
        return com.aniflow.domain.intelligence.model.CompositeConfidence(
            episodeConfidence = episodeConfidence,
            seasonConfidence = seasonConfidence,
            titleConfidence = titleConfidence,
            technicalConfidence = technicalConfidence,
            sourceConfidence = sourceConfidence,
            overall = relConf.overall.toDouble()
        )
    }
}
