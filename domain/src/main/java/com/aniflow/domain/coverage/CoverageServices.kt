package com.aniflow.domain.coverage

import com.aniflow.domain.anime.Anime
import com.aniflow.domain.anime.Episode
import com.aniflow.domain.anime.ReleaseEpisode
import com.aniflow.domain.anime.Season
import com.aniflow.domain.identity.AnimeId
import com.aniflow.domain.identity.EpisodeId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.identity.SeasonId

/**
 * Step 20 — Coverage Calculation Engine (Section 18, 19, 20, 24, 72).
 * Pure deterministic domain service aggregating release mappings, library status, and expectation policies.
 */
class AnimeCoverageCalculationService {

    fun calculateEpisodeCoverage(
        episode: Episode,
        mappedReleases: List<ReleaseEpisode>,
        localFileCount: Int = 0,
        isDownloading: Boolean = false,
        isSearched: Boolean = true,
        hasEligibleRelease: Boolean = mappedReleases.isNotEmpty()
    ): EpisodeCoverage {
        val releaseCount = mappedReleases.size
        val selectedReleaseId = mappedReleases.firstOrNull()?.releaseId

        val state = when {
            localFileCount > 0 -> EpisodeAvailabilityState.Downloaded
            isDownloading -> EpisodeAvailabilityState.Downloading
            releaseCount > 0 && hasEligibleRelease -> EpisodeAvailabilityState.Available
            releaseCount > 0 && !hasEligibleRelease -> EpisodeAvailabilityState.NoEligibleRelease
            !isSearched -> EpisodeAvailabilityState.NotSearched
            else -> EpisodeAvailabilityState.NoReleaseFound
        }

        val confidence = when {
            mappedReleases.any { it.relation == com.aniflow.domain.anime.ReleaseEpisodeRelation.UserMapped } -> CoverageConfidence.UserMapped
            mappedReleases.any { it.relation == com.aniflow.domain.anime.ReleaseEpisodeRelation.Primary } -> CoverageConfidence.High
            mappedReleases.any { it.relation == com.aniflow.domain.anime.ReleaseEpisodeRelation.Contained } -> CoverageConfidence.High
            mappedReleases.any { it.relation == com.aniflow.domain.anime.ReleaseEpisodeRelation.Inferred } -> CoverageConfidence.Inferred
            else -> CoverageConfidence.Medium
        }

        return EpisodeCoverage(
            episodeId = episode.id,
            state = state,
            releaseCount = releaseCount,
            selectedReleaseId = selectedReleaseId,
            localFileCount = localFileCount,
            upgradeAvailable = false,
            confidence = confidence
        )
    }

    fun calculateSeasonCoverage(
        season: Season,
        episodes: List<Episode>,
        episodeCoverages: Map<EpisodeId, EpisodeCoverage>
    ): SeasonCoverage {
        val knownCount = episodes.size
        val availableCount = episodes.count { ep ->
            episodeCoverages[ep.id]?.isCovered == true
        }
        val downloadingCount = episodes.count { ep ->
            episodeCoverages[ep.id]?.state == EpisodeAvailabilityState.Downloading
        }
        val reviewCount = episodes.count { ep ->
            episodeCoverages[ep.id]?.state == EpisodeAvailabilityState.ReviewRequired
        }

        return SeasonCoverage(
            seasonId = season.id,
            expectedCount = season.expectedEpisodeCount,
            knownCount = knownCount,
            availableCount = availableCount,
            downloadingCount = downloadingCount,
            reviewCount = reviewCount
        )
    }

    fun calculateAnimeCoverage(
        anime: Anime,
        seasons: List<Season>,
        seasonCoverages: Map<SeasonId, SeasonCoverage>
    ): AnimeCoverage {
        val totalExpected = if (seasons.all { it.expectedEpisodeCount != null }) {
            seasons.sumOf { it.expectedEpisodeCount ?: 0 }
        } else {
            null
        }

        val totalAvailable = seasonCoverages.values.sumOf { it.availableCount }
        val totalDownloading = seasonCoverages.values.sumOf { it.downloadingCount }
        val totalReview = seasonCoverages.values.sumOf { it.reviewCount }

        return AnimeCoverage(
            animeId = anime.id,
            seasons = seasonCoverages.values.toList(),
            totalExpected = totalExpected,
            totalAvailable = totalAvailable,
            totalDownloading = totalDownloading,
            totalReview = totalReview
        )
    }
}
