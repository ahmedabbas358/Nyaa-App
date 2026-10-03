package com.aniflow.testing

import com.aniflow.core.common.result.AniFlowResult
import com.aniflow.core.common.result.ErrorType
import com.aniflow.domain.identity.AnimeId
import com.aniflow.domain.identity.DownloadTaskId
import com.aniflow.domain.identity.LibraryFileId
import com.aniflow.domain.identity.LibraryItemId
import com.aniflow.domain.identity.ProviderId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.model.aggregate.download.DownloadFile
import com.aniflow.domain.model.aggregate.download.DownloadTask
import com.aniflow.domain.model.aggregate.library.LibraryFile
import com.aniflow.domain.model.aggregate.library.LibraryItem
import com.aniflow.domain.model.aggregate.release.Release
import com.aniflow.domain.repository.DownloadRepository
import com.aniflow.domain.repository.LibraryRepository
import com.aniflow.domain.repository.ReleaseRepository
import com.aniflow.domain.state.DownloadState
import com.aniflow.domain.valueobject.InfoHash
import com.aniflow.domain.valueobject.PageResult
import com.aniflow.domain.valueobject.SearchQuery
import com.aniflow.domain.valueobject.UrlValue
import com.aniflow.provider.core.ReleaseProvider
import com.aniflow.provider.core.model.ProviderDescriptor
import com.aniflow.provider.core.model.ProviderPageResult
import com.aniflow.provider.core.model.ProviderRelease
import com.aniflow.provider.core.model.ProviderSearchRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeReleaseProvider(
    val providerId: String,
    val cannedReleases: List<ProviderRelease>
) : ReleaseProvider {
    override val descriptor = ProviderDescriptor(
        id = ProviderId(providerId),
        name = "Fake Nyaa",
        version = "1.0",
        capabilities = emptySet()
    )

    override suspend fun search(request: ProviderSearchRequest): AniFlowResult<ProviderPageResult<ProviderRelease>> {
        return AniFlowResult.Success(
            ProviderPageResult(
                items = cannedReleases,
                page = request.page,
                hasNextPage = false
            )
        )
    }

    override suspend fun getDetails(detailsUrl: UrlValue): AniFlowResult<ProviderRelease> {
        return cannedReleases.firstOrNull()?.let { AniFlowResult.Success(it) }
            ?: AniFlowResult.Error(ErrorType.NotFoundError("Not found"), "Not found")
    }

    override suspend fun getRecent(limit: Int): AniFlowResult<List<ProviderRelease>> {
        return AniFlowResult.Success(cannedReleases.take(limit))
    }
}

class InMemoryReleaseRepository : ReleaseRepository {
    private val store = mutableMapOf<String, Release>()

    override suspend fun getById(id: ReleaseId): Release? = store[id.value]
    override suspend fun getByProviderId(providerId: ProviderId, providerReleaseId: String): Release? =
        store.values.firstOrNull { it.provider.providerId == providerId && it.providerReleaseId == providerReleaseId }
    override suspend fun getByInfoHash(infoHash: InfoHash): Release? = null
    override fun search(query: SearchQuery): Flow<AniFlowResult<PageResult<Release>>> = flowOf(
        AniFlowResult.Success(PageResult(store.values.toList(), 1))
    )
    override suspend fun save(release: Release) { store[release.id.value] = release }
    override suspend fun saveAll(releases: List<Release>) { releases.forEach { save(it) } }
    override fun observeById(id: ReleaseId): Flow<Release?> = flowOf(store[id.value])
}

class InMemoryDownloadRepository : DownloadRepository {
    private val taskStore = mutableMapOf<DownloadTaskId, DownloadTask>()

    override suspend fun getTaskById(id: DownloadTaskId): DownloadTask? = taskStore[id]
    override suspend fun getAllTasks(): List<DownloadTask> = taskStore.values.toList()
    override fun observeAllTasks(): Flow<List<DownloadTask>> = flowOf(taskStore.values.toList())
    override fun observeActiveTasks(): Flow<List<DownloadTask>> = flowOf(taskStore.values.filter { it.state == DownloadState.Downloading })
    override suspend fun saveTask(task: DownloadTask) { taskStore[task.id] = task }
    override suspend fun deleteTask(id: DownloadTaskId) { taskStore.remove(id) }
    override suspend fun getFilesByTaskId(taskId: DownloadTaskId): List<DownloadFile> = emptyList()
    override suspend fun saveFile(file: DownloadFile) {}
}

class InMemoryLibraryRepository : LibraryRepository {
    private val itemStore = mutableMapOf<LibraryItemId, LibraryItem>()
    private val fileStore = mutableMapOf<LibraryFileId, LibraryFile>()

    override suspend fun getItemById(id: LibraryItemId): LibraryItem? = itemStore[id]
    override suspend fun getItemByAnimeId(animeId: AnimeId): LibraryItem? = null
    override fun observeAllItems(): Flow<List<LibraryItem>> = flowOf(itemStore.values.toList())
    override suspend fun getAllItems(): List<LibraryItem> = itemStore.values.toList()
    override suspend fun getFilesByItemId(itemId: LibraryItemId): List<LibraryFile> =
        fileStore.values.filter { it.itemId == itemId }
    override suspend fun saveItem(item: LibraryItem) { itemStore[item.id] = item }
    override suspend fun saveFile(file: LibraryFile) { fileStore[file.id] = file }
    override suspend fun deleteItem(id: LibraryItemId) { itemStore.remove(id) }
}
