package com.aniflow.domain.selection.batch

import com.aniflow.domain.intelligence.model.NormalizedRelease
import com.aniflow.domain.intelligence.model.ReleaseCandidate
import com.aniflow.domain.selection.model.BatchSuggestion
import com.aniflow.domain.selection.model.EpisodeCandidateSet
import com.aniflow.domain.selection.model.ExistingMediaState

/**
 * Optimizes selection between individual episode downloads vs batch torrents (Section 63, 64, 65, 66).
 */
class CoverageOptimizationEngine {

    fun analyzeBatchCoverage(
        batchRelease: NormalizedRelease,
        episodes: List<EpisodeCandidateSet>
    ): BatchSuggestion? {
        val range = batchRelease.episodeRange ?: return null
        val coveredEps = (range.start..range.end).toList()

        val missingEps = mutableListOf<Int>()
        val existingEps = mutableListOf<Int>()

        episodes.forEach { epSet ->
            val num = epSet.episode.number.value
            if (coveredEps.contains(num)) {
                if (epSet.existingState == ExistingMediaState.Downloaded || epSet.existingState == ExistingMediaState.Downloading) {
                    existingEps += num
                } else {
                    missingEps += num
                }
            }
        }

        if (missingEps.isEmpty()) return null

        val batchSizeBytes = batchRelease.rawMetadata["sizeBytes"]?.toLongOrNull()

        // Calculate approximate individual total size
        var individualSizeBytes: Long = 0L
        episodes.filter { missingEps.contains(it.episode.number.value) }.forEach { epSet ->
            val bestCandidate = epSet.candidates.firstOrNull()
            val size = bestCandidate?.release?.rawMetadata["sizeBytes"]?.toLongOrNull() ?: 0L
            individualSizeBytes += size
        }

        val explanation = buildString {
            append("Batch covers ${coveredEps.size} episodes (${range.start}-${range.end}). ")
            append("${missingEps.size} missing episodes needed. ")
            if (existingEps.isNotEmpty()) {
                append("${existingEps.size} already downloaded (Partial batch). ")
            }
            if (batchSizeBytes != null && individualSizeBytes > 0) {
                append("Batch size: ${batchSizeBytes / (1024 * 1024)} MB vs Individual: ${individualSizeBytes / (1024 * 1024)} MB.")
            }
        }

        return BatchSuggestion(
            batchRelease = batchRelease,
            coveredEpisodeNumbers = coveredEps,
            missingEpisodeNumbers = missingEps,
            existingEpisodeNumbers = existingEps,
            totalBatchSizeBytes = batchSizeBytes,
            totalIndividualSizeBytes = if (individualSizeBytes > 0) individualSizeBytes else null,
            explanation = explanation
        )
    }
}
