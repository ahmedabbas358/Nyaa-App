package com.aniflow.data.repository

import com.aniflow.core.common.result.AniFlowResult
import com.aniflow.core.database.dao.AnimeDao
import com.aniflow.core.database.dao.CollectionDao
import com.aniflow.core.database.dao.DownloadFileDao
import com.aniflow.core.database.dao.DownloadTaskDao
import com.aniflow.core.database.dao.EpisodeDao
import com.aniflow.core.database.dao.LibraryDao
import com.aniflow.core.database.dao.ProfileDao
import com.aniflow.core.database.dao.ReleaseDao
import com.aniflow.core.database.dao.ReleaseEpisodeDao
import com.aniflow.core.database.dao.ReleaseGroupDao
import com.aniflow.core.database.dao.RuleDao
import com.aniflow.core.database.dao.SavedSearchDao
import com.aniflow.core.database.dao.SeasonDao
import com.aniflow.core.database.dao.UploaderDao
import com.aniflow.core.database.entity.CollectionItemEntity
import com.aniflow.core.database.entity.DownloadProfileEntity
import com.aniflow.core.database.entity.ReleaseEpisodeEntity
import com.aniflow.core.database.entity.RuleEntity
import com.aniflow.core.database.entity.SavedSearchEntity
import com.aniflow.core.database.mapper.EntityMappers.toDomain
import com.aniflow.core.database.mapper.EntityMappers.toEntity
import com.aniflow.domain.identity.AnimeId
import com.aniflow.domain.identity.AnimeIdentity
import com.aniflow.domain.identity.CollectionId
import com.aniflow.domain.identity.CollectionItemId
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
import com.aniflow.domain.model.aggregate.organization.CollectionItemType
import com.aniflow.domain.model.aggregate.organization.DownloadProfile
import com.aniflow.domain.model.aggregate.organization.Rule
import com.aniflow.domain.model.aggregate.organization.SavedSearch
import com.aniflow.domain.model.aggregate.release.ProviderRef
import com.aniflow.domain.model.aggregate.release.Release
import com.aniflow.domain.model.aggregate.release.ReleaseEpisode
import com.aniflow.domain.model.aggregate.release.ReleaseEpisodeRelationType
import com.aniflow.domain.model.aggregate.release.ReleaseGroup
import com.aniflow.domain.model.aggregate.release.ReleaseGroupPreference
import com.aniflow.domain.model.aggregate.release.Uploader
import com.aniflow.domain.model.aggregate.release.UploaderPreference
import com.aniflow.domain.repository.AnimeRepository
import com.aniflow.domain.repository.CollectionRepository
import com.aniflow.domain.repository.DownloadRepository
import com.aniflow.domain.repository.EpisodeRepository
import com.aniflow.domain.repository.LibraryRepository
import com.aniflow.domain.repository.ProfileRepository
import com.aniflow.domain.repository.ReleaseGroupRepository
import com.aniflow.domain.repository.ReleaseRepository
import com.aniflow.domain.repository.RuleRepository
import com.aniflow.domain.repository.SavedSearchRepository
import com.aniflow.domain.repository.SeasonRepository
import com.aniflow.domain.repository.UploaderRepository
import com.aniflow.domain.state.DownloadState
import com.aniflow.domain.valueobject.InfoHash
import com.aniflow.domain.valueobject.PageResult
import com.aniflow.domain.valueobject.SearchQuery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.Instant

class ReleaseRepositoryImpl(
    private val releaseDao: ReleaseDao
) : ReleaseRepository {

    override suspend fun getById(id: ReleaseId): Release? {
        return releaseDao.getById(id.value)?.toDomain()
    }

    override suspend fun getByProviderId(providerId: ProviderId, providerReleaseId: String): Release? {
        return releaseDao.getByProviderId(providerId.value, providerReleaseId)?.toDomain()
    }

    override suspend fun getByInfoHash(infoHash: InfoHash): Release? {
        return releaseDao.getByTorrentHash(infoHash.hexString)?.toDomain()
    }

    override fun search(query: SearchQuery): Flow<AniFlowResult<PageResult<Release>>> = flow {
        emit(AniFlowResult.Loading(0.1f))
        try {
            val offset = (query.pagination.page - 1) * query.pagination.pageSize
            val results = if (query.text.isBlank()) {
                releaseDao.queryProjectionsPaged(limit = query.pagination.pageSize, offset = offset)
            } else {
                releaseDao.searchFts(query.text, limit = query.pagination.pageSize, offset = offset)
                    .map { it.toDomain() }
            }

            val domainList = results.mapNotNull {
                if (it is Release) it else releaseDao.getById((it as com.aniflow.core.database.relation.ReleaseListProjection).id)?.toDomain()
            }

            emit(
                AniFlowResult.Success(
                    PageResult(
                        items = domainList,
                        page = query.pagination.page,
                        hasNextPage = domainList.size >= query.pagination.pageSize
                    )
                )
            )
        } catch (e: Exception) {
            emit(AniFlowResult.Error(com.aniflow.core.common.result.ErrorType.DatabaseError(e.message ?: "Search failed"), e.message ?: "Search query failed", e))
        }
    }

    override suspend fun save(release: Release) {
        releaseDao.insert(release.toEntity())
    }

    override suspend fun saveAll(releases: List<Release>) {
        releaseDao.insertAll(releases.map { it.toEntity() })
    }

    override fun observeById(id: ReleaseId): Flow<Release?> {
        return releaseDao.observeById(id.value).map { it?.toDomain() }
    }
}

class AnimeRepositoryImpl(
    private val animeDao: AnimeDao
) : AnimeRepository {

    override suspend fun getById(id: AnimeId): Anime? = animeDao.getById(id.value)?.toDomain()

    override fun observeById(id: AnimeId): Flow<Anime?> =
        animeDao.observeAll().map { list -> list.find { it.id == id.value }?.toDomain() }

    override suspend fun findByIdentity(identity: AnimeIdentity): Anime? =
        animeDao.findByNormalizedTitle(identity.normalizedTitle)?.toDomain()

    override fun observeAll(): Flow<List<Anime>> =
        animeDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun save(anime: Anime) = animeDao.insert(anime.toEntity())

    override suspend fun delete(id: AnimeId) = animeDao.delete(id.value)
}

class SeasonRepositoryImpl(
    private val seasonDao: SeasonDao
) : SeasonRepository {

    override suspend fun getById(id: SeasonId): Season? = seasonDao.getById(id.value)?.toDomain()

    override fun observeById(id: SeasonId): Flow<Season?> = flow { emit(getById(id)) }

    override suspend fun getByAnimeId(animeId: AnimeId): List<Season> =
        seasonDao.getByAnimeId(animeId.value).map { it.toDomain() }

    override fun observeByAnimeId(animeId: AnimeId): Flow<List<Season>> =
        seasonDao.observeByAnimeId(animeId.value).map { list -> list.map { it.toDomain() } }

    override suspend fun save(season: Season) = seasonDao.insert(season.toEntity())

    override suspend fun delete(id: SeasonId) = seasonDao.delete(id.value)
}

class EpisodeRepositoryImpl(
    private val episodeDao: EpisodeDao,
    private val releaseEpisodeDao: ReleaseEpisodeDao
) : EpisodeRepository {

    override suspend fun getById(id: EpisodeId): Episode? = episodeDao.getById(id.value)?.toDomain()

    override fun observeById(id: EpisodeId): Flow<Episode?> = flow { emit(getById(id)) }

    override suspend fun getBySeasonId(seasonId: SeasonId): List<Episode> =
        episodeDao.getBySeasonId(seasonId.value).map { it.toDomain() }

    override fun observeBySeasonId(seasonId: SeasonId): Flow<List<Episode>> =
        episodeDao.observeBySeasonId(seasonId.value).map { list -> list.map { it.toDomain() } }

    override suspend fun getByAnimeId(animeId: AnimeId): List<Episode> =
        episodeDao.getByAnimeId(animeId.value).map { it.toDomain() }

    override suspend fun getReleasesForEpisode(episodeId: EpisodeId): List<ReleaseEpisode> {
        return releaseEpisodeDao.getReleasesForEpisode(episodeId.value).map {
            ReleaseEpisode(
                releaseId = ReleaseId(it.id),
                episodeId = episodeId,
                relationType = ReleaseEpisodeRelationType.Primary
            )
        }
    }

    override suspend fun save(episode: Episode) = episodeDao.insert(episode.toEntity())

    override suspend fun linkRelease(releaseEpisode: ReleaseEpisode) {
        releaseEpisodeDao.insert(
            ReleaseEpisodeEntity(
                releaseId = releaseEpisode.releaseId.value,
                episodeId = releaseEpisode.episodeId.value,
                relationType = releaseEpisode.relationType.name
            )
        )
    }

    override suspend fun delete(id: EpisodeId) = episodeDao.delete(id.value)
}

class UploaderRepositoryImpl(
    private val uploaderDao: UploaderDao
) : UploaderRepository {

    private val preferences = mutableMapOf<String, UploaderPreference>()

    override suspend fun getById(id: UploaderId): Uploader? {
        val entity = uploaderDao.getById(id.value) ?: return null
        return entity.toDomain(ProviderRef(ProviderId(entity.providerId), entity.providerId))
    }

    override suspend fun getPreference(uploaderId: UploaderId): UploaderPreference? =
        preferences[uploaderId.value]

    override suspend fun savePreference(preference: UploaderPreference) {
        preferences[preference.uploaderId.value] = preference
    }

    override fun observePreferences(): Flow<List<UploaderPreference>> = flow {
        emit(preferences.values.toList())
    }
}

class ReleaseGroupRepositoryImpl(
    private val groupDao: ReleaseGroupDao
) : ReleaseGroupRepository {

    private val preferences = mutableMapOf<String, ReleaseGroupPreference>()

    override suspend fun getById(id: ReleaseGroupId): ReleaseGroup? {
        val entity = groupDao.getById(id.value) ?: return null
        return entity.toDomain(ProviderRef(ProviderId(entity.providerId), entity.providerId))
    }

    override suspend fun getPreference(groupId: ReleaseGroupId): ReleaseGroupPreference? =
        preferences[groupId.value]

    override suspend fun savePreference(preference: ReleaseGroupPreference) {
        preferences[preference.groupId.value] = preference
    }

    override fun observePreferences(): Flow<List<ReleaseGroupPreference>> = flow {
        emit(preferences.values.toList())
    }
}

class CollectionRepositoryImpl(
    private val collectionDao: CollectionDao
) : CollectionRepository {

    override suspend fun getById(id: CollectionId): Collection? =
        collectionDao.getById(id.value)?.toDomain()

    override fun observeAll(): Flow<List<Collection>> =
        collectionDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getItems(collectionId: CollectionId): List<CollectionItem> {
        return collectionDao.getItems(collectionId.value).map {
            CollectionItem(
                id = CollectionItemId("${it.collectionId}_${it.itemType}_${it.itemId}"),
                collectionId = collectionId,
                itemType = try { CollectionItemType.valueOf(it.itemType) } catch (_: Exception) { CollectionItemType.Release },
                itemId = it.itemId,
                order = it.sortOrder,
                addedAt = Instant.ofEpochMilli(it.addedAt)
            )
        }
    }

    override fun observeItems(collectionId: CollectionId): Flow<List<CollectionItem>> =
        collectionDao.observeItems(collectionId.value).map { list ->
            list.map {
                CollectionItem(
                    id = CollectionItemId("${it.collectionId}_${it.itemType}_${it.itemId}"),
                    collectionId = collectionId,
                    itemType = try { CollectionItemType.valueOf(it.itemType) } catch (_: Exception) { CollectionItemType.Release },
                    itemId = it.itemId,
                    order = it.sortOrder,
                    addedAt = Instant.ofEpochMilli(it.addedAt)
                )
            }
        }

    override suspend fun save(collection: Collection) = collectionDao.insert(collection.toEntity())

    override suspend fun addItem(item: CollectionItem) {
        collectionDao.insertItem(
            CollectionItemEntity(
                collectionId = item.collectionId.value,
                itemType = item.itemType.name,
                itemId = item.itemId,
                sortOrder = item.order,
                addedAt = item.addedAt.toEpochMilli()
            )
        )
    }

    override suspend fun removeItem(itemId: CollectionItemId) {
        val parts = itemId.value.split("_")
        if (parts.size >= 3) {
            collectionDao.removeItem(parts[0], parts[1], parts[2])
        }
    }

    override suspend fun delete(id: CollectionId) = collectionDao.delete(id.value)
}

class DownloadRepositoryImpl(
    private val taskDao: DownloadTaskDao,
    private val fileDao: DownloadFileDao
) : DownloadRepository {

    override suspend fun getTaskById(id: DownloadTaskId): DownloadTask? =
        taskDao.getById(id.value)?.toDomain()

    override suspend fun getAllTasks(): List<DownloadTask> =
        taskDao.getAllTasks().map { it.toDomain() }

    override fun observeTasks(): Flow<List<DownloadTask>> =
        taskDao.observeTasks().map { list -> list.map { it.toDomain() } }

    override fun observeTasksByState(states: Set<DownloadState>): Flow<List<DownloadTask>> =
        taskDao.observeTasksByStates(states.map { it.name }).map { list -> list.map { it.toDomain() } }

    override suspend fun saveTask(task: DownloadTask) = taskDao.insert(task.toEntity())

    override suspend fun getFilesForTask(taskId: DownloadTaskId): List<DownloadFile> =
        fileDao.getByTaskId(taskId.value).map { it.toDomain() }

    override suspend fun saveFile(file: DownloadFile) = fileDao.insert(file.toEntity())

    override suspend fun deleteTask(id: DownloadTaskId) = taskDao.delete(id.value)

    override suspend fun updateTaskState(id: DownloadTaskId, state: DownloadState, errorMessage: String?) {
        taskDao.updateStateWithReason(id.value, state.name, errorMessage)
    }

    override suspend fun updateTaskProgress(id: DownloadTaskId, downloadedBytes: Long, totalBytes: Long?, speed: Long, eta: Long) {
        taskDao.updateProgressWithTotal(id.value, downloadedBytes, totalBytes ?: 0L, speed, eta)
    }

    override suspend fun getQueuedTasks(limit: Int): List<DownloadTask> {
        return taskDao.getQueuedTasksForExecution(limit).map { it.toDomain() }
    }
}

class LibraryRepositoryImpl(
    private val libraryDao: LibraryDao
) : LibraryRepository {

    override suspend fun getItemById(id: LibraryItemId): LibraryItem? =
        libraryDao.getItemById(id.value)?.toDomain()

    override suspend fun getAllItems(): List<LibraryItem> =
        libraryDao.getAllItems().map { it.toDomain() }

    override fun observeItems(): Flow<List<LibraryItem>> =
        libraryDao.observeItems().map { list -> list.map { it.toDomain() } }

    override suspend fun getFilesForItem(itemId: LibraryItemId): List<LibraryFile> =
        libraryDao.getFilesForItem(itemId.value).map { it.toDomain() }

    override suspend fun saveItem(item: LibraryItem) = libraryDao.insertItem(item.toEntity())

    override suspend fun saveFile(file: LibraryFile) = libraryDao.insertFile(file.toEntity())

    override suspend fun deleteItem(id: LibraryItemId) = libraryDao.deleteItem(id.value)
}

class ProfileRepositoryImpl(
    private val profileDao: ProfileDao
) : com.aniflow.domain.repository.ProfileRepository {

    override suspend fun getById(id: DownloadProfileId): DownloadProfile? {
        val entity = profileDao.getById(id.value) ?: return null
        return DownloadProfile(id = id, name = entity.name, isDefault = entity.isDefault)
    }

    override suspend fun getDefaultProfile(): DownloadProfile {
        val def = profileDao.getDefault()
        return if (def != null) {
            DownloadProfile(id = DownloadProfileId(def.id), name = def.name, isDefault = true)
        } else {
            DownloadProfile(id = DownloadProfileId("default_profile"), name = "Default", isDefault = true)
        }
    }

    override fun observeAll(): Flow<List<DownloadProfile>> =
        profileDao.observeAll().map { list ->
            list.map { DownloadProfile(id = DownloadProfileId(it.id), name = it.name, isDefault = it.isDefault) }
        }

    override suspend fun save(profile: DownloadProfile) {
        profileDao.insert(
            DownloadProfileEntity(
                id = profile.id.value,
                name = profile.name,
                configJson = "{}",
                isDefault = profile.isDefault
            )
        )
    }
}

class RuleRepositoryImpl(
    private val ruleDao: RuleDao
) : RuleRepository {

    override suspend fun getById(id: RuleId): Rule? = null

    override suspend fun getAll(): List<Rule> = emptyList()

    override fun observeAll(): Flow<List<Rule>> = ruleDao.observeAll().map { emptyList() }

    override suspend fun save(rule: Rule) {
        ruleDao.insert(
            RuleEntity(
                id = rule.id.value,
                name = rule.name,
                priority = rule.priority,
                enabled = rule.enabled,
                conditionsJson = "[]",
                actionsJson = "[]"
            )
        )
    }

    override suspend fun delete(id: RuleId) = ruleDao.delete(id.value)
}

class SavedSearchRepositoryImpl(
    private val searchDao: SavedSearchDao
) : SavedSearchRepository {

    override suspend fun getById(id: SavedSearchId): SavedSearch? {
        val s = searchDao.getById(id.value) ?: return null
        return SavedSearch(
            id = id,
            name = s.name,
            query = SearchQuery.of(s.queryText),
            isPinned = s.isPinned,
            createdAt = Instant.ofEpochMilli(s.createdAt),
            updatedAt = Instant.ofEpochMilli(s.updatedAt)
        )
    }

    override fun observeAll(): Flow<List<SavedSearch>> =
        searchDao.observeAll().map { list ->
            list.map {
                SavedSearch(
                    id = SavedSearchId(it.id),
                    name = it.name,
                    query = SearchQuery.of(it.queryText),
                    isPinned = it.isPinned,
                    createdAt = Instant.ofEpochMilli(it.createdAt),
                    updatedAt = Instant.ofEpochMilli(it.updatedAt)
                )
            }
        }

    override suspend fun save(search: SavedSearch) {
        searchDao.insert(
            SavedSearchEntity(
                id = search.id.value,
                name = search.name,
                queryText = search.query.text,
                queryConfigJson = "{}",
                isPinned = search.isPinned,
                createdAt = search.createdAt.toEpochMilli(),
                updatedAt = search.updatedAt.toEpochMilli(),
                lastRunAt = null
            )
        )
    }

    override suspend fun delete(id: SavedSearchId) = searchDao.delete(id.value)
}
