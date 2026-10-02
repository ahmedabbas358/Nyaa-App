package com.aniflow.domain.usecase

import com.aniflow.domain.anime.Anime
import com.aniflow.domain.anime.Episode
import com.aniflow.domain.anime.ReleaseEpisode
import com.aniflow.domain.anime.ReviewQueueItem
import com.aniflow.domain.anime.ReviewStatus
import com.aniflow.domain.anime.Season
import com.aniflow.domain.anime.UserMapping
import com.aniflow.domain.coverage.AnimeCoverage
import com.aniflow.domain.coverage.AnimeCoverageCalculationService
import com.aniflow.domain.coverage.EpisodeAvailabilityState
import com.aniflow.domain.coverage.EpisodeCoverage
import com.aniflow.domain.coverage.SeasonCoverage
import com.aniflow.domain.identity.AnimeId
import com.aniflow.domain.identity.EpisodeId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.identity.SeasonId
import com.aniflow.domain.model.aggregate.release.Release
import com.aniflow.domain.anime.AnimeExperienceRepository
import com.aniflow.domain.repository.ReleaseRepository
import com.aniflow.domain.repository.ReviewQueueRepository
import com.aniflow.domain.repository.UserMappingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Step 20 — Query Use Cases for Anime Experience (Section 67).
 */

class GetAnimeUseCase(
    private val repository: AnimeExperienceRepository
) {
    suspend operator fun invoke(id: AnimeId): Anime? = repository.getAnime(id)
    fun observe(id: AnimeId): Flow<Anime?> = repository.observeAnime(id)
}

class GetAnimeSeasonsUseCase(
    private val repository: AnimeExperienceRepository
) {
    suspend operator fun invoke(animeId: AnimeId): List<Season> = repository.getSeasons(animeId)
    fun observe(animeId: AnimeId): Flow<List<Season>> = repository.observeSeasons(animeId)
}

class GetSeasonEpisodesUseCase(
    private val repository: AnimeExperienceRepository
) {
    suspend operator fun invoke(seasonId: SeasonId): List<Episode> = repository.getEpisodes(seasonId)
    fun observe(seasonId: SeasonId): Flow<List<Episode>> = repository.observeEpisodes(seasonId)
}

class GetEpisodeUseCase(
    private val repository: AnimeExperienceRepository
) {
    suspend operator fun invoke(id: EpisodeId): Episode? = repository.getEpisode(id)
    fun observe(id: EpisodeId): Flow<Episode?> = repository.observeEpisode(id)
}

/**
 * Data container for candidate releases associated with an episode (Section 39, 79, 80).
 */
data class EpisodeCandidateItem(
    val release: Release,
    val relation: com.aniflow.domain.anime.ReleaseEpisodeRelation,
    val isBatch: Boolean,
    val batchLabel: String,
    val confidence: Float
)

class GetEpisodeCandidatesUseCase(
    private val repository: AnimeExperienceRepository,
    private val releaseRepository: ReleaseRepository
) {
    suspend operator fun invoke(episodeId: EpisodeId): List<EpisodeCandidateItem> {
        val mappings = repository.getReleasesForEpisode(episodeId)
        val items = mutableListOf<EpisodeCandidateItem>()

        for (map in mappings) {
            val rel = releaseRepository.getById(map.releaseId) ?: continue
            val isBatch = rel.isBatch || (rel.episodeRange != null && !rel.episodeRange.isSingleEpisode)
            val batchLabel = when {
                rel.releaseType == com.aniflow.domain.model.aggregate.release.ReleaseType.CompleteSeries -> "Complete Series"
                rel.releaseType == com.aniflow.domain.model.aggregate.release.ReleaseType.Season -> "Season Batch"
                isBatch -> "Episode Batch"
                else -> "Single Episode"
            }

            items.add(
                EpisodeCandidateItem(
                    release = rel,
                    relation = map.relation,
                    isBatch = isBatch,
                    batchLabel = batchLabel,
                    confidence = map.confidence
                )
            )
        }

        // Sort: Primary first, then high seeders, then highest resolution
        return items.sortedWith(
            compareByDescending<EpisodeCandidateItem> { it.relation == com.aniflow.domain.anime.ReleaseEpisodeRelation.Primary }
                .thenByDescending { it.release.availability?.seeders ?: 0 }
        )
    }
}

class GetEpisodeCoverageUseCase(
    private val repository: AnimeExperienceRepository,
    private val coverageService: AnimeCoverageCalculationService = AnimeCoverageCalculationService()
) {
    suspend operator fun invoke(episode: Episode): EpisodeCoverage {
        val mappings = repository.getReleasesForEpisode(episode.id)
        return coverageService.calculateEpisodeCoverage(
            episode = episode,
            mappedReleases = mappings
        )
    }
}

class GetSeasonCoverageUseCase(
    private val repository: AnimeExperienceRepository,
    private val coverageService: AnimeCoverageCalculationService = AnimeCoverageCalculationService()
) {
    suspend operator fun invoke(season: Season): SeasonCoverage {
        val episodes = repository.getEpisodes(season.id)
        val episodeCoverages = episodes.associate { ep ->
            val mappings = repository.getReleasesForEpisode(ep.id)
            ep.id to coverageService.calculateEpisodeCoverage(ep, mappings)
        }
        return coverageService.calculateSeasonCoverage(season, episodes, episodeCoverages)
    }
}

class GetAnimeCoverageUseCase(
    private val repository: AnimeExperienceRepository,
    private val coverageService: AnimeCoverageCalculationService = AnimeCoverageCalculationService()
) {
    suspend operator fun invoke(anime: Anime): AnimeCoverage {
        val seasons = repository.getSeasons(anime.id)
        val seasonCoverages = seasons.associate { season ->
            val episodes = repository.getEpisodes(season.id)
            val episodeCoverages = episodes.associate { ep ->
                val mappings = repository.getReleasesForEpisode(ep.id)
                ep.id to coverageService.calculateEpisodeCoverage(ep, mappings)
            }
            season.id to coverageService.calculateSeasonCoverage(season, episodes, episodeCoverages)
        }
        return coverageService.calculateAnimeCoverage(anime, seasons, seasonCoverages)
    }
}

data class MissingEpisodeItem(
    val episode: Episode,
    val seasonNumber: Int?,
    val state: EpisodeAvailabilityState,
    val candidateCount: Int
)

class GetMissingEpisodesUseCase(
    private val repository: AnimeExperienceRepository,
    private val coverageService: AnimeCoverageCalculationService = AnimeCoverageCalculationService()
) {
    suspend operator fun invoke(animeId: AnimeId): List<MissingEpisodeItem> {
        val seasons = repository.getSeasons(animeId)
        val missing = mutableListOf<MissingEpisodeItem>()

        for (season in seasons) {
            val episodes = repository.getEpisodes(season.id)
            for (ep in episodes) {
                val mappings = repository.getReleasesForEpisode(ep.id)
                val cov = coverageService.calculateEpisodeCoverage(ep, mappings)
                if (cov.isMissing) {
                    missing.add(
                        MissingEpisodeItem(
                            episode = ep,
                            seasonNumber = season.number,
                            state = cov.state,
                            candidateCount = cov.releaseCount
                        )
                    )
                }
            }
        }

        return missing.sortedWith(
            compareBy<MissingEpisodeItem> { it.seasonNumber ?: Int.MAX_VALUE }
                .thenBy { it.episode.number?.major ?: Int.MAX_VALUE }
        )
    }
}

class GetReviewItemsUseCase(
    private val reviewRepository: ReviewQueueRepository
) {
    fun observePending(): Flow<List<ReviewQueueItem>> = reviewRepository.observePending()
    suspend fun updateStatus(id: String, status: ReviewStatus) = reviewRepository.updateStatus(id, status)
}

/**
 * Side-by-side release comparison model (Section 41).
 */
data class ReleaseComparisonAttribute(
    val name: String,
    val values: List<String>
)

data class ReleaseComparisonResult(
    val releases: List<Release>,
    val attributes: List<ReleaseComparisonAttribute>
)

class CompareReleasesUseCase(
    private val releaseRepository: ReleaseRepository
) {
    suspend operator fun invoke(releaseIds: List<ReleaseId>): ReleaseComparisonResult {
        require(releaseIds.size in 2..5) { "Comparison requires between 2 and 5 releases" }
        val releases = releaseIds.mapNotNull { releaseRepository.getById(it) }

        val attributes = listOf(
            ReleaseComparisonAttribute("Resolution", releases.map { it.technicalMetadata?.resolution?.displayName ?: "Unknown" }),
            ReleaseComparisonAttribute("Video Codec", releases.map { it.technicalMetadata?.videoCodec?.displayName ?: "Unknown" }),
            ReleaseComparisonAttribute("Source", releases.map { it.technicalMetadata?.source?.displayName ?: "Unknown" }),
            ReleaseComparisonAttribute("Size", releases.map { it.availability?.size?.displayString ?: "Unknown" }),
            ReleaseComparisonAttribute("Seeders", releases.map { it.availability?.seeders?.toString() ?: "0" }),
            ReleaseComparisonAttribute("Release Group", releases.map { it.groupRef?.name ?: "No Group" }),
            ReleaseComparisonAttribute("Uploader", releases.map { it.uploaderRef?.name ?: "Anonymous" }),
            ReleaseComparisonAttribute("Audio", releases.map { it.technicalMetadata?.audioTracks?.joinToString { a -> a.codec?.displayName ?: "Audio" } ?: "Not detected" }),
            ReleaseComparisonAttribute("Subtitles", releases.map { it.technicalMetadata?.subtitles?.joinToString { s -> s.language?.code ?: "Sub" } ?: "Not detected" })
        )

        return ReleaseComparisonResult(releases, attributes)
    }
}

class SaveUserMappingUseCase(
    private val userMappingRepository: UserMappingRepository
) {
    suspend operator fun invoke(mapping: UserMapping) {
        userMappingRepository.save(mapping)
    }
}
