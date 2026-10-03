package com.aniflow.domain.intelligence.model

import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.model.aggregate.release.ReleaseType
import com.aniflow.domain.state.ParseState
import com.aniflow.domain.valueobject.EpisodeRange
import com.aniflow.domain.valueobject.MediaSource
import com.aniflow.domain.valueobject.ReleaseTechnicalMetadata

/**
 * Granular field-level confidence ratings for backward compatibility (Section 57, 58).
 */
data class CompositeConfidence(
    val episodeConfidence: Double = 0.0,
    val seasonConfidence: Double = 0.0,
    val titleConfidence: Double = 0.0,
    val technicalConfidence: Double = 0.0,
    val sourceConfidence: Double = 0.0,
    val overall: Double = 0.0
) {
    val isHighConfidence: Boolean get() = overall >= 0.85
    val needsManualReview: Boolean get() = overall < 0.65

    fun toReleaseConfidence(): ReleaseConfidence = ReleaseConfidence.fromScores(
        overall = overall.toFloat(),
        identity = titleConfidence.toFloat(),
        coverage = episodeConfidence.toFloat(),
        technical = technicalConfidence.toFloat()
    )
}

/**
 * Step 19 — Output Contract (Section 4).
 * The normalized domain release object produced by the deterministic intelligence engine.
 */
data class NormalizedRelease(
    val releaseId: ReleaseId,
    val animeIdentity: AnimeIdentity?,
    val season: SeasonReference?,
    val episodes: EpisodeCoverage,
    val technical: TechnicalMetadata,
    val uploader: UploaderReference?,
    val releaseGroup: ReleaseGroupReference?,
    val batchType: BatchType,
    val confidence: ReleaseConfidence,
    val warnings: List<ParsingWarning> = emptyList(),
    // Observability & backwards compatibility fields
    val rawTitle: String = "",
    val normalizedTitle: String = "",
    val conflicts: List<MetadataConflict> = emptyList(),
    val state: ParserState = ParserState.Parsed,
    val rawMetadata: Map<String, String> = emptyMap(),
    val parserVersion: Int = 1,
    val legacyReleaseType: ReleaseType = ReleaseType.Unknown,
    val stats: ReleaseStats = ReleaseStats(),
    val links: ReleaseLinks = ReleaseLinks(),
    val fileInfo: FileInfo = FileInfo(),
    val releaseSource: ReleaseSource? = null,
    val episodeMatch: EpisodeMatch? = null
) {
    constructor(
        releaseId: ReleaseId,
        providerReleaseId: String? = null,
        source: ReleaseSource? = null,
        technical: TechnicalMetadata = TechnicalMetadata(resolution = null, codec = null),
        episodeMatch: EpisodeMatch? = null,
        stats: ReleaseStats = ReleaseStats(),
        fileInfo: FileInfo = FileInfo(),
        links: ReleaseLinks = ReleaseLinks(),
        isBatch: Boolean = false,
        animeCandidate: String? = source?.title,
        confidence: ReleaseConfidence = ReleaseConfidence.fromScores(0.95f, 0.95f, 0.95f, 0.95f)
    ) : this(
        releaseId = releaseId,
        animeIdentity = animeCandidate?.let { AnimeIdentity(it) },
        season = SeasonReference(1),
        episodes = if (isBatch) EpisodeCoverage.Range(1, 12) else EpisodeCoverage.Single(episodeMatch?.detectedEpisode?.toInt() ?: 1),
        technical = technical,
        uploader = null,
        releaseGroup = null,
        batchType = if (isBatch) BatchType.SeasonBatch else BatchType.SingleEpisode,
        confidence = confidence,
        rawTitle = source?.title ?: "",
        normalizedTitle = source?.title ?: "",
        rawMetadata = if (providerReleaseId != null) mapOf("providerReleaseId" to providerReleaseId, "provider" to (source?.providerName ?: "Nyaa")) else emptyMap(),
        stats = stats,
        links = links,
        fileInfo = fileInfo,
        releaseSource = source,
        episodeMatch = episodeMatch
    )

    // Backward compatibility getters
    val id: ReleaseId get() = releaseId
    val sourceInfo: ReleaseSource? get() = releaseSource
    val providerReleaseId: String? get() = rawMetadata["providerReleaseId"] ?: rawMetadata["releaseId"]
    val animeCandidate: String? get() = animeIdentity?.canonicalTitle
    val seasonCandidate: Int? get() = season?.seasonNumber ?: 1
    val groupCandidate: String? get() = releaseGroup?.name
    val uploaderName: String? get() = uploader?.name
    val technicalMetadata: ReleaseTechnicalMetadata
        get() = ReleaseTechnicalMetadata(
            resolution = technical.resolution,
            videoCodec = technical.codec,
            audioTracks = technical.audioTracks,
            subtitles = technical.subtitleTracks,
            source = technical.mediaSource,
            bitDepth = technical.bitDepth?.let { com.aniflow.domain.valueobject.BitDepth.fromInt(it) },
            hdr = technical.hdr?.toHdrType()
        )
    val source: MediaSource? get() = technical.mediaSource
    val isBatch: Boolean
        get() = batchType != BatchType.SingleEpisode && batchType != BatchType.Unknown || episodes.isBatch
    val episodeList: List<Int> get() = episodes.allEpisodes
    val episodeRange: EpisodeRange? get() = when (episodes) {
        is EpisodeCoverage.Range -> EpisodeRange(
            start = com.aniflow.domain.valueobject.EpisodeNumber(episodes.from),
            end = com.aniflow.domain.valueobject.EpisodeNumber(episodes.to)
        )
        is EpisodeCoverage.Single -> EpisodeRange(
            start = com.aniflow.domain.valueobject.EpisodeNumber(episodes.episode),
            end = com.aniflow.domain.valueobject.EpisodeNumber(episodes.episode)
        )
        else -> null
    }

    // Direct compatibility property for legacy code that inspected `episodes` as List<Int>
    val discreteEpisodes: List<Int> get() = episodes.allEpisodes

    val releaseType: ReleaseType get() = when {
        legacyReleaseType != ReleaseType.Unknown -> legacyReleaseType
        batchType == BatchType.Complete || batchType == BatchType.SeriesBatch -> ReleaseType.CompleteSeries
        batchType == BatchType.SeasonBatch -> ReleaseType.Season
        batchType == BatchType.EpisodeRange -> ReleaseType.Batch
        episodes is EpisodeCoverage.Range -> ReleaseType.Batch
        episodes is EpisodeCoverage.Single -> ReleaseType.SingleEpisode
        else -> ReleaseType.Unknown
    }

    // Bridge for code expecting CompositeConfidence
    val compositeConfidence: CompositeConfidence
        get() = CompositeConfidence(
            episodeConfidence = confidence.coverageConfidence.toDouble(),
            seasonConfidence = 1.0,
            titleConfidence = confidence.identityConfidence.toDouble(),
            technicalConfidence = confidence.technicalConfidence.toDouble(),
            overall = confidence.overall.toDouble()
        )
}

/**
 * Step 19 — Normalization Result (Section 36).
 * Encapsulates the normalized release alongside parser state, confidence, warnings, and conflicts.
 */
data class NormalizationResult(
    val release: NormalizedRelease,
    val parserState: ParserState,
    val confidence: ReleaseConfidence,
    val warnings: List<ParsingWarning>,
    val conflicts: List<MetadataConflict>
)

/**
 * Candidate release associated with a specific episode (Section 49, 107).
 */
data class ReleaseCandidate(
    val episodeNumber: Int?,
    val release: NormalizedRelease,
    val confidence: Double
)

/**
 * Group of release candidates competing for or covering an episode (Section 48, 49).
 */
data class EpisodeReleaseGroup(
    val episodeNumber: Int,
    val candidates: List<NormalizedRelease>,
    val selectedRelease: NormalizedRelease? = null,
    val coverageStatus: EpisodeCoverageStatus = EpisodeCoverageStatus.Available
)

enum class EpisodeCoverageStatus {
    Available,
    Missing,
    Queued,
    Downloading,
    Downloaded,
    Failed
}

/**
 * Flagged ambiguity item requiring user attention or override (Section 40, 64).
 */
data class ReviewItem(
    val releaseId: ReleaseId,
    val rawTitle: String,
    val issue: ReviewIssue,
    val candidateValues: List<String>
)

enum class ReviewIssue {
    AmbiguousAnime,
    AmbiguousSeason,
    AmbiguousEpisode,
    ConflictingMetadata,
    UnknownMediaType,
    PossibleBatch
}
