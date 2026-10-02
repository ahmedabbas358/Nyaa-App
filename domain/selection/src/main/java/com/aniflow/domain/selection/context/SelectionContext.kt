package com.aniflow.domain.selection.context

import com.aniflow.domain.coverage.EpisodeCoverage
import com.aniflow.domain.identity.AnimeId
import com.aniflow.domain.identity.CollectionId
import com.aniflow.domain.identity.EpisodeId
import com.aniflow.domain.identity.ProviderId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.identity.SeasonId
import com.aniflow.domain.intelligence.model.ReleaseConfidence
import com.aniflow.domain.model.aggregate.organization.DownloadProfile
import com.aniflow.domain.model.aggregate.release.TechnicalMetadata
import com.aniflow.domain.selection.model.ExistingDownloadState
import com.aniflow.domain.selection.model.ExistingMediaState
import com.aniflow.domain.selection.model.NetworkContext
import com.aniflow.domain.selection.model.ProviderContext
import com.aniflow.domain.selection.model.ResolvedPreferences
import com.aniflow.domain.selection.model.ResolvedRule
import com.aniflow.domain.selection.model.StorageContext
import com.aniflow.domain.valueobject.ByteSize
import com.aniflow.domain.valueobject.EpisodeNumber

/**
 * Step 21 — Selection Target (Section 4).
 * Identifies the exact scope or media entity being targeted for selection.
 */
sealed interface SelectionTarget {
    data class EpisodeTarget(
        val episodeId: EpisodeId,
        val animeId: AnimeId,
        val seasonId: SeasonId? = null,
        val episodeNumber: EpisodeNumber? = null,
        val absoluteNumber: Int? = null
    ) : SelectionTarget

    data class EpisodeRangeTarget(
        val start: EpisodeNumber,
        val end: EpisodeNumber,
        val animeId: AnimeId,
        val seasonId: SeasonId? = null
    ) : SelectionTarget

    data class SeasonTarget(
        val seasonId: SeasonId,
        val animeId: AnimeId,
        val seasonNumber: Int? = null
    ) : SelectionTarget

    data class AnimeTarget(
        val animeId: AnimeId
    ) : SelectionTarget

    data class BatchTarget(
        val targetEpisodes: Set<Int>,
        val animeId: AnimeId,
        val seasonId: SeasonId? = null
    ) : SelectionTarget

    data class CollectionTarget(
        val collectionId: CollectionId
    ) : SelectionTarget

    data class ManualCandidateSetTarget(
        val targetName: String
    ) : SelectionTarget
}

/**
 * Step 21 — Selection Candidate (Section 5).
 * Normalized domain model decoupled from parser internals.
 */
data class SelectionCandidate(
    val releaseId: ReleaseId,
    val rawTitle: String,
    val coverage: EpisodeCoverage? = null,
    val technical: TechnicalMetadata,
    val uploader: String? = null,
    val releaseGroup: String? = null,
    val sourceSummary: String? = null,
    val size: ByteSize? = null,
    val seeders: Int? = null,
    val confidence: ReleaseConfidence = ReleaseConfidence.High,
    val provider: ProviderId = ProviderId("nyaa")
)

/**
 * Step 21 — Selection Context (Section 3).
 * Complete, immutable context container passed into the Selection Engine.
 */
data class SelectionContext(
    val target: SelectionTarget,
    val candidates: List<SelectionCandidate>,
    val profile: DownloadProfile? = null,
    val preferences: ResolvedPreferences,
    val rules: List<ResolvedRule> = emptyList(),
    val existingMedia: ExistingMediaState = ExistingMediaState.Missing,
    val existingDownloads: ExistingDownloadState = ExistingDownloadState.None,
    val storage: StorageContext = StorageContext.Unlimited,
    val network: NetworkContext = NetworkContext.Unconstrained,
    val providerContext: ProviderContext = ProviderContext.Default
)
