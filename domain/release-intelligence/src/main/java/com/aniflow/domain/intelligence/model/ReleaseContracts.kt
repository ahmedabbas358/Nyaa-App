package com.aniflow.domain.intelligence.model

import com.aniflow.domain.identity.ProviderId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.model.aggregate.release.ReleaseSource
import com.aniflow.domain.valueobject.ByteSize
import com.aniflow.domain.valueobject.EpisodeRange
import com.aniflow.domain.valueobject.InfoHash
import java.time.Instant

/**
 * Step 19 — Input Contract (Section 3).
 * Clean, decoupled domain input representing a raw release from any provider.
 * Does not depend on Nyaa HTML, Room DAO, or UI.
 */
data class RawReleaseInput(
    val providerId: ProviderId,
    val providerReleaseId: String,
    val title: String,
    val uploader: String? = null,
    val size: ByteSize? = null,
    val seeds: Int? = null,
    val leeches: Int? = null,
    val downloads: Int? = null,
    val publishedAt: Instant? = null,
    val sources: List<ReleaseSource> = emptyList()
)

/**
 * Step 19 — Anime Identity (Section 5).
 * Represents resolved anime identity candidates, aliases, and hints without assuming canonical == raw title.
 */
data class AnimeIdentity(
    val canonicalTitle: String?,
    val aliases: List<String> = emptyList(),
    val year: Int? = null,
    val seasonHint: SeasonHint? = null
) {
    val primaryTitle: String get() = canonicalTitle ?: ""
    val hasYear: Boolean get() = year != null
}

/**
 * Structural hint for season identification (Section 5, 13).
 */
data class SeasonHint(
    val seasonNumber: Int?,
    val partNumber: Int? = null,
    val isCour: Boolean = false,
    val rawText: String? = null
)

/**
 * Safe reference for season identification distinguishing Season vs Part (Section 13, 76, 98).
 */
data class SeasonReference(
    val seasonNumber: Int?,
    val partNumber: Int? = null,
    val isCour: Boolean = false,
    val isSpecial: Boolean = false,
    val rawText: String? = null
) {
    val effectiveSeason: Int get() = seasonNumber ?: 1
}

/**
 * Episode coverage model (Section 14, 43, 54).
 * Single release can cover multiple episodes without creating multiple release records.
 */
sealed interface EpisodeCoverage : Iterable<Int> {
    data class Single(val episode: Int) : EpisodeCoverage
    data class Range(val from: Int, val to: Int) : EpisodeCoverage
    data class Set(val episodes: kotlin.collections.Set<Int>) : EpisodeCoverage
    data class Season(val seasonNumber: Int) : EpisodeCoverage
    data object Series : EpisodeCoverage
    data object Unknown : EpisodeCoverage

    override fun iterator(): Iterator<Int> = allEpisodes.iterator()
    val size: Int get() = allEpisodes.size
    fun isEmpty(): Boolean = allEpisodes.isEmpty()
    fun isNotEmpty(): Boolean = allEpisodes.isNotEmpty()

    fun covers(episodeNumber: Int): Boolean = when (this) {
        is Single -> episode == episodeNumber
        is Range -> episodeNumber in from..to
        is Set -> episodes.contains(episodeNumber)
        is Season -> true
        is Series -> true
        is Unknown -> false
    }

    val allEpisodes: List<Int> get() = when (this) {
        is Single -> listOf(episode)
        is Range -> (from..to).toList()
        is Set -> episodes.toList().sorted()
        is Season -> emptyList()
        is Series -> emptyList()
        is Unknown -> emptyList()
    }

    val isSingleEpisode: Boolean get() = this is Single
    val isBatch: Boolean get() = this is Range || this is Set || this is Season || this is Series
}

/**
 * Fractional and dual-numbered episode representation (Section 41, 77, 78, 79).
 */
data class EpisodeNumberValue(
    val major: Int,
    val minor: Float? = null // e.g. 12.5
) {
    val displayName: String get() = if (minor != null) "$major.$minor" else major.toString()
}

/**
 * Episode reference structure (Section 41).
 */
data class EpisodeReference(
    val number: EpisodeNumberValue?,
    val absoluteNumber: Int? = null,
    val range: EpisodeRange? = null,
    val special: Boolean = false
)

/**
 * Batch classification (Section 15).
 */
enum class BatchType {
    SingleEpisode,
    EpisodeRange,
    SeasonBatch,
    SeriesBatch,
    Complete,
    Unknown
}

/**
 * Separate Uploader (from Provider) and Release Group (from title parsing) (Section 26, 27).
 */
data class UploaderReference(
    val name: String,
    val isVerified: Boolean = false
) {
    override fun toString(): String = name
    override fun equals(other: Any?): Boolean = when (other) {
        is UploaderReference -> name.equals(other.name, ignoreCase = true)
        is String -> name.equals(other, ignoreCase = true)
        else -> false
    }
    override fun hashCode(): Int = name.lowercase().hashCode()
}

data class ReleaseGroupReference(
    val name: String,
    val isBracketCandidate: Boolean = true,
    val confidence: Float = 1.0f
) {
    override fun toString(): String = name
    override fun equals(other: Any?): Boolean = when (other) {
        is ReleaseGroupReference -> name.equals(other.name, ignoreCase = true)
        is String -> name.equals(other, ignoreCase = true)
        else -> false
    }
    override fun hashCode(): Int = name.lowercase().hashCode()
}

/**
 * Warning emitted during parsing without discarding the release (Section 4, 63, 70).
 */
data class ParsingWarning(
    val code: String,
    val message: String,
    val matchedText: String? = null
)

/**
 * Semantic confidence thresholds (Section 33, 34).
 */
object ConfidenceThresholds {
    const val CONFIRMED = 0.95f
    const val HIGH = 0.80f
    const val MEDIUM = 0.60f
}

enum class ConfidenceLevel {
    Confirmed, // >= 0.95
    High,      // 0.80 .. 0.94
    Medium,    // 0.60 .. 0.79
    Low        // < 0.60
}

/**
 * Multi-dimensional confidence model (Section 33, 34, 85, 86).
 * Decision-critical confidences separated from purely technical values.
 */
data class ReleaseConfidence(
    val overall: Float,
    val level: ConfidenceLevel,
    val identityConfidence: Float,
    val coverageConfidence: Float,
    val technicalConfidence: Float
) {
    val isHighConfidence: Boolean get() = overall >= ConfidenceThresholds.HIGH
    val needsManualReview: Boolean get() = overall < ConfidenceThresholds.MEDIUM || level == ConfidenceLevel.Low
    val titleConfidence: Float get() = identityConfidence
    val episodeConfidence: Float get() = coverageConfidence
    val score: Float get() = overall

    constructor(
        overall: Float = 1.0f,
        identity: Float = 1.0f,
        coverage: Float = 1.0f,
        technical: Float = 1.0f
    ) : this(
        overall = overall,
        level = when {
            overall >= ConfidenceThresholds.CONFIRMED -> ConfidenceLevel.Confirmed
            overall >= ConfidenceThresholds.HIGH -> ConfidenceLevel.High
            overall >= ConfidenceThresholds.MEDIUM -> ConfidenceLevel.Medium
            else -> ConfidenceLevel.Low
        },
        identityConfidence = identity,
        coverageConfidence = coverage,
        technicalConfidence = technical
    )

    companion object {
        val Confirmed = fromScores(1.0f, 1.0f, 1.0f, 1.0f)
        val High = fromScores(0.9f, 0.9f, 0.9f, 0.9f)
        val Medium = fromScores(0.7f, 0.7f, 0.7f, 0.7f)
        val Low = fromScores(0.4f, 0.4f, 0.4f, 0.4f)

        fun fromScores(
            overall: Float,
            identity: Float,
            coverage: Float,
            technical: Float
        ): ReleaseConfidence {
            val lvl = when {
                overall >= ConfidenceThresholds.CONFIRMED -> ConfidenceLevel.Confirmed
                overall >= ConfidenceThresholds.HIGH -> ConfidenceLevel.High
                overall >= ConfidenceThresholds.MEDIUM -> ConfidenceLevel.Medium
                else -> ConfidenceLevel.Low
            }
            return ReleaseConfidence(
                overall = overall.coerceIn(0f, 1f),
                level = lvl,
                identityConfidence = identity.coerceIn(0f, 1f),
                coverageConfidence = coverage.coerceIn(0f, 1f),
                technicalConfidence = technical.coerceIn(0f, 1f)
            )
        }
    }
}

/**
 * Parser execution state (Section 35, 87).
 */
enum class ParserState {
    Parsed,
    PartiallyParsed,
    Ambiguous,
    Rejected
}

/**
 * Explicit metadata conflict when multiple detectors suggest incompatible values (Section 31, 83).
 */
data class MetadataConflict(
    val field: String,
    val candidateA: String,
    val candidateB: String,
    val evidenceA: TokenEvidence,
    val evidenceB: TokenEvidence,
    val status: ConflictStatus = ConflictStatus.Ambiguous,
    val resolvedValue: String? = null
)

enum class ConflictStatus {
    Ambiguous,
    ResolvedByPrecedence,
    ResolvedByUserOverride
}

/**
 * Media type categorization (Section 29, 44).
 */
enum class MediaType {
    AnimeEpisode,
    Movie,
    OVA,
    OAD,
    Special,
    ONA,
    Unknown
}

/**
 * Technical identity profile for uniqueness and deduplication (Section 44, 45, 46).
 */
data class TechnicalIdentity(
    val resolution: String?,
    val codec: String?,
    val source: String?,
    val audioChannels: String?,
    val bitDepth: Int?
)

/**
 * Release Identity model (Section 44).
 */
data class ReleaseIdentity(
    val anime: AnimeIdentity,
    val season: SeasonReference?,
    val coverage: EpisodeCoverage,
    val mediaType: MediaType,
    val technicalIdentity: TechnicalIdentity
)
