package com.aniflow.domain.repository

import com.aniflow.core.common.result.AniFlowResult
import com.aniflow.domain.identity.AnimeId
import com.aniflow.domain.identity.AnimeIdentity
import com.aniflow.domain.identity.CollectionId
import com.aniflow.domain.identity.CollectionItemId
import com.aniflow.domain.identity.DownloadFileId
import com.aniflow.domain.identity.DownloadProfileId
import com.aniflow.domain.identity.DownloadTaskId
import com.aniflow.domain.identity.EpisodeId
import com.aniflow.domain.identity.LibraryItemId
import com.aniflow.domain.identity.ProviderId
import com.aniflow.domain.identity.ReleaseGroupId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.identity.RuleId
import com.aniflow.domain.identity.SavedSearchId
import com.aniflow.domain.identity.SeasonId
import com.aniflow.domain.identity.UploaderId
import com.aniflow.domain.model.aggregate.download.DownloadFile
import com.aniflow.domain.model.aggregate.download.DownloadTask
import com.aniflow.domain.model.aggregate.library.LibraryFile
import com.aniflow.domain.model.aggregate.library.LibraryItem
import com.aniflow.domain.model.aggregate.media.Anime
import com.aniflow.domain.model.aggregate.media.Episode
import com.aniflow.domain.model.aggregate.media.Season
import com.aniflow.domain.model.aggregate.organization.Collection
import com.aniflow.domain.model.aggregate.organization.CollectionItem
import com.aniflow.domain.model.aggregate.organization.DownloadProfile
import com.aniflow.domain.model.aggregate.organization.Rule
import com.aniflow.domain.model.aggregate.organization.SavedSearch
import com.aniflow.domain.model.aggregate.release.Release
import com.aniflow.domain.model.aggregate.release.ReleaseEpisode
import com.aniflow.domain.model.aggregate.release.ReleaseGroup
import com.aniflow.domain.model.aggregate.release.ReleaseGroupPreference
import com.aniflow.domain.model.aggregate.release.Uploader
import com.aniflow.domain.model.aggregate.release.UploaderPreference
import com.aniflow.domain.state.DownloadState
import com.aniflow.domain.valueobject.InfoHash
import com.aniflow.domain.valueobject.PageResult
import com.aniflow.domain.valueobject.SearchQuery
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contracts adhering strictly to Clean Architecture (Section 120, 121, 122).
 * Independent of Room DAOs, databases, and network implementations.
 */

interface ReleaseRepository {
    suspend fun getById(id: ReleaseId): Release?
    suspend fun getByProviderId(providerId: ProviderId, providerReleaseId: String): Release?
    suspend fun getByInfoHash(infoHash: InfoHash): Release?
    fun search(query: SearchQuery): Flow<AniFlowResult<PageResult<Release>>>
    suspend fun save(release: Release)
    suspend fun saveAll(releases: List<Release>)
    fun observeById(id: ReleaseId): Flow<Release?>
}

interface AnimeRepository {
    suspend fun getById(id: AnimeId): Anime?
    fun observeById(id: AnimeId): Flow<Anime?>
    suspend fun findByIdentity(identity: AnimeIdentity): Anime?
    fun observeAll(): Flow<List<Anime>>
    suspend fun save(anime: Anime)
    suspend fun delete(id: AnimeId)
}

interface SeasonRepository {
    suspend fun getById(id: SeasonId): Season?
    fun observeById(id: SeasonId): Flow<Season?>
    suspend fun getByAnimeId(animeId: AnimeId): List<Season>
    fun observeByAnimeId(animeId: AnimeId): Flow<List<Season>>
    suspend fun save(season: Season)
    suspend fun delete(id: SeasonId)
}

interface EpisodeRepository {
    suspend fun getById(id: EpisodeId): Episode?
    fun observeById(id: EpisodeId): Flow<Episode?>
    suspend fun getBySeasonId(seasonId: SeasonId): List<Episode>
    fun observeBySeasonId(seasonId: SeasonId): Flow<List<Episode>>
    suspend fun getByAnimeId(animeId: AnimeId): List<Episode>
    suspend fun getReleasesForEpisode(episodeId: EpisodeId): List<ReleaseEpisode>
    suspend fun save(episode: Episode)
    suspend fun linkRelease(releaseEpisode: ReleaseEpisode)
    suspend fun delete(id: EpisodeId)
}

interface UserMappingRepository {
    suspend fun getById(id: String): com.aniflow.domain.anime.UserMapping?
    suspend fun findBySignature(signature: String): com.aniflow.domain.anime.UserMapping?
    fun observeAll(): Flow<List<com.aniflow.domain.anime.UserMapping>>
    suspend fun save(mapping: com.aniflow.domain.anime.UserMapping)
    suspend fun delete(id: String)
}

interface ReviewQueueRepository {
    suspend fun getById(id: String): com.aniflow.domain.anime.ReviewQueueItem?
    fun observePending(): Flow<List<com.aniflow.domain.anime.ReviewQueueItem>>
    suspend fun save(item: com.aniflow.domain.anime.ReviewQueueItem)
    suspend fun updateStatus(id: String, status: com.aniflow.domain.anime.ReviewStatus)
}

interface UploaderRepository {
    suspend fun getById(id: UploaderId): Uploader?
    suspend fun getPreference(uploaderId: UploaderId): UploaderPreference?
    suspend fun savePreference(preference: UploaderPreference)
    fun observePreferences(): Flow<List<UploaderPreference>>
}

interface ReleaseGroupRepository {
    suspend fun getById(id: ReleaseGroupId): ReleaseGroup?
    suspend fun getPreference(groupId: ReleaseGroupId): ReleaseGroupPreference?
    suspend fun savePreference(preference: ReleaseGroupPreference)
    fun observePreferences(): Flow<List<ReleaseGroupPreference>>
}

interface CollectionRepository {
    suspend fun getById(id: CollectionId): Collection?
    fun observeAll(): Flow<List<Collection>>
    suspend fun getItems(collectionId: CollectionId): List<CollectionItem>
    fun observeItems(collectionId: CollectionId): Flow<List<CollectionItem>>
    suspend fun save(collection: Collection)
    suspend fun addItem(item: CollectionItem)
    suspend fun removeItem(itemId: CollectionItemId)
    suspend fun delete(id: CollectionId)
}

interface DownloadRepository {
    suspend fun getTaskById(id: DownloadTaskId): DownloadTask?
    fun observeTasks(): Flow<List<DownloadTask>>
    fun observeTasksByState(states: Set<DownloadState>): Flow<List<DownloadTask>>
    suspend fun saveTask(task: DownloadTask)
    suspend fun getFilesForTask(taskId: DownloadTaskId): List<DownloadFile>
    suspend fun saveFile(file: DownloadFile)
    suspend fun deleteTask(id: DownloadTaskId)
}

interface LibraryRepository {
    suspend fun getItemById(id: LibraryItemId): LibraryItem?
    fun observeItems(): Flow<List<LibraryItem>>
    suspend fun getFilesForItem(itemId: LibraryItemId): List<LibraryFile>
    suspend fun saveItem(item: LibraryItem)
    suspend fun saveFile(file: LibraryFile)
    suspend fun deleteItem(id: LibraryItemId)
}

interface ProfileRepository {
    suspend fun getById(id: DownloadProfileId): DownloadProfile?
    suspend fun getDefaultProfile(): DownloadProfile
    fun observeAll(): Flow<List<DownloadProfile>>
    suspend fun save(profile: DownloadProfile)
}

interface RuleRepository {
    suspend fun getById(id: RuleId): Rule?
    suspend fun getAll(): List<Rule>
    fun observeAll(): Flow<List<Rule>>
    suspend fun save(rule: Rule)
    suspend fun delete(id: RuleId)
}

interface SavedSearchRepository {
    suspend fun getById(id: SavedSearchId): SavedSearch?
    fun observeAll(): Flow<List<SavedSearch>>
    suspend fun save(search: SavedSearch)
    suspend fun delete(id: SavedSearchId)
}
