package com.aniflow.data.repository

import com.aniflow.domain.anime.Anime
import com.aniflow.domain.anime.AnimeExperienceRepository
import com.aniflow.domain.anime.Episode
import com.aniflow.domain.anime.ReleaseEpisode
import com.aniflow.domain.anime.ReviewQueueItem
import com.aniflow.domain.anime.ReviewStatus
import com.aniflow.domain.anime.Season
import com.aniflow.domain.anime.UserMapping
import com.aniflow.domain.identity.AnimeId
import com.aniflow.domain.identity.EpisodeId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.identity.SeasonId
import com.aniflow.domain.repository.ReviewQueueRepository
import com.aniflow.domain.repository.UserMappingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap

/**
 * Step 20 — Anime Experience Repository Implementation.
 * Optimized for batch querying and projections without N+1 query overhead (Section 68, 69, 96).
 */
class AnimeExperienceRepositoryImpl : AnimeExperienceRepository {

    private val animeStore = ConcurrentHashMap<String, Anime>()
    private val seasonStore = ConcurrentHashMap<String, Season>()
    private val episodeStore = ConcurrentHashMap<String, Episode>()
    private val releaseEpisodeStore = ConcurrentHashMap<String, MutableList<ReleaseEpisode>>()

    private val animeFlow = MutableStateFlow<Map<String, Anime>>(emptyMap())
    private val seasonFlow = MutableStateFlow<Map<String, Season>>(emptyMap())
    private val episodeFlow = MutableStateFlow<Map<String, Episode>>(emptyMap())

    private fun updateAnimeFlow() {
        animeFlow.value = HashMap(animeStore)
    }

    private fun updateSeasonFlow() {
        seasonFlow.value = HashMap(seasonStore)
    }

    private fun updateEpisodeFlow() {
        episodeFlow.value = HashMap(episodeStore)
    }

    override suspend fun getAnime(id: AnimeId): Anime? = animeStore[id.value]

    override fun observeAnime(id: AnimeId): Flow<Anime?> =
        animeFlow.map { it[id.value] }

    override suspend fun findAnimeByTitle(title: String): Anime? {
        val clean = title.trim().lowercase()
        return animeStore.values.firstOrNull { anime ->
            anime.canonicalTitle.lowercase() == clean ||
                anime.titles.any { it.value.lowercase() == clean || it.normalizedValue.lowercase() == clean }
        }
    }

    override fun observeAllAnime(): Flow<List<Anime>> =
        animeFlow.map { it.values.toList() }

    override suspend fun saveAnime(anime: Anime) {
        animeStore[anime.id.value] = anime
        updateAnimeFlow()
    }

    override suspend fun deleteAnime(id: AnimeId) {
        animeStore.remove(id.value)
        updateAnimeFlow()
    }

    override suspend fun getSeasons(animeId: AnimeId): List<Season> =
        seasonStore.values.filter { it.animeId == animeId }
            .sortedWith(compareBy<Season> { it.number ?: Int.MAX_VALUE })

    override fun observeSeasons(animeId: AnimeId): Flow<List<Season>> =
        seasonFlow.map { map ->
            map.values.filter { it.animeId == animeId }
                .sortedWith(compareBy<Season> { it.number ?: Int.MAX_VALUE })
        }

    override suspend fun getSeason(id: SeasonId): Season? = seasonStore[id.value]

    override fun observeSeason(id: SeasonId): Flow<Season?> =
        seasonFlow.map { it[id.value] }

    override suspend fun saveSeason(season: Season) {
        seasonStore[season.id.value] = season
        updateSeasonFlow()
    }

    override suspend fun deleteSeason(id: SeasonId) {
        seasonStore.remove(id.value)
        updateSeasonFlow()
    }

    override suspend fun getEpisodes(seasonId: SeasonId): List<Episode> =
        episodeStore.values.filter { it.seasonId == seasonId }
            .sortedWith(
                compareBy<Episode> { it.number?.major ?: Int.MAX_VALUE }
                    .thenBy { it.absoluteNumber ?: Int.MAX_VALUE }
            )

    override fun observeEpisodes(seasonId: SeasonId): Flow<List<Episode>> =
        episodeFlow.map { map ->
            map.values.filter { it.seasonId == seasonId }
                .sortedWith(
                    compareBy<Episode> { it.number?.major ?: Int.MAX_VALUE }
                        .thenBy { it.absoluteNumber ?: Int.MAX_VALUE }
                )
        }

    override suspend fun getEpisode(id: EpisodeId): Episode? = episodeStore[id.value]

    override fun observeEpisode(id: EpisodeId): Flow<Episode?> =
        episodeFlow.map { it[id.value] }

    override suspend fun getEpisodesByAnimeId(animeId: AnimeId): List<Episode> =
        episodeStore.values.filter { it.animeId == animeId }
            .sortedWith(
                compareBy<Episode> { it.number?.major ?: Int.MAX_VALUE }
                    .thenBy { it.absoluteNumber ?: Int.MAX_VALUE }
            )

    override suspend fun saveEpisode(episode: Episode) {
        episodeStore[episode.id.value] = episode
        updateEpisodeFlow()
    }

    override suspend fun deleteEpisode(id: EpisodeId) {
        episodeStore.remove(id.value)
        updateEpisodeFlow()
    }

    override suspend fun getReleasesForEpisode(episodeId: EpisodeId): List<ReleaseEpisode> =
        releaseEpisodeStore[episodeId.value]?.toList() ?: emptyList()

    override suspend fun linkRelease(mapping: ReleaseEpisode) {
        val list = releaseEpisodeStore.computeIfAbsent(mapping.episodeId.value) { mutableListOf() }
        synchronized(list) {
            list.removeAll { it.releaseId == mapping.releaseId }
            list.add(mapping)
        }
    }

    override suspend fun unlinkRelease(releaseId: ReleaseId, episodeId: EpisodeId) {
        val list = releaseEpisodeStore[episodeId.value] ?: return
        synchronized(list) {
            list.removeAll { it.releaseId == releaseId }
        }
    }
}

/**
 * Step 20 — User Mapping Repository Implementation (Section 64, 66, 100).
 */
class UserMappingRepositoryImpl : UserMappingRepository {
    private val mappings = ConcurrentHashMap<String, UserMapping>()
    private val stateFlow = MutableStateFlow<List<UserMapping>>(emptyList())

    override suspend fun getById(id: String): UserMapping? = mappings[id]

    override suspend fun findBySignature(signature: String): UserMapping? =
        mappings.values.firstOrNull { it.titleSignature == signature }

    override fun observeAll(): Flow<List<UserMapping>> = stateFlow.asStateFlow()

    override suspend fun save(mapping: UserMapping) {
        mappings[mapping.id] = mapping
        stateFlow.value = mappings.values.toList()
    }

    override suspend fun delete(id: String) {
        mappings.remove(id)
        stateFlow.value = mappings.values.toList()
    }
}

/**
 * Step 20 — Review Queue Repository Implementation (Section 64, 65).
 */
class ReviewQueueRepositoryImpl : ReviewQueueRepository {
    private val items = ConcurrentHashMap<String, ReviewQueueItem>()
    private val pendingFlow = MutableStateFlow<List<ReviewQueueItem>>(emptyList())

    override suspend fun getById(id: String): ReviewQueueItem? = items[id]

    override fun observePending(): Flow<List<ReviewQueueItem>> = pendingFlow.asStateFlow()

    override suspend fun save(item: ReviewQueueItem) {
        items[item.id] = item
        updatePendingFlow()
    }

    override suspend fun updateStatus(id: String, status: ReviewStatus) {
        val item = items[id] ?: return
        items[id] = item.copy(status = status)
        updatePendingFlow()
    }

    private fun updatePendingFlow() {
        pendingFlow.value = items.values.filter { it.status == ReviewStatus.Pending }
    }
}
