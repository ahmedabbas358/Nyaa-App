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
import com.aniflow.provider.core.health.ProviderHealth
import com.aniflow.provider.core.health.ProviderHealthMetrics
import com.aniflow.provider.core.model.ProviderCapabilities
import com.aniflow.provider.core.model.ProviderDescriptor
import com.aniflow.provider.core.model.ProviderRelease
import com.aniflow.provider.core.model.ProviderSearchPage
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
        baseUrl = UrlValue.HttpsUrl("https://nyaa.si"),
        capabilities = ProviderCapabilities.NYAA,
        version = "1.0"
    )

    override suspend fun search(request: ProviderSearchRequest): AniFlowResult<ProviderSearchPage> {
        return AniFlowResult.Success(
            ProviderSearchPage(
                items = cannedReleases,
                page = request.page,
                hasNextPage = false
            )
        )
    }

    override suspend fun getRelease(providerReleaseId: String): AniFlowResult<ProviderRelease> {
        val found = cannedReleases.firstOrNull { it.providerReleaseId == providerReleaseId }
            ?: cannedReleases.firstOrNull()
        return if (found != null) {
            AniFlowResult.Success(found)
        } else {
            AniFlowResult.Error(ErrorType.ProviderUnavailable(providerId), "Release not found: $providerReleaseId")
        }
    }

    override suspend fun searchByUploader(uploader: String, request: ProviderSearchRequest): AniFlowResult<ProviderSearchPage> {
        return search(request)
    }

    override suspend fun healthCheck(): ProviderHealth {
        return ProviderHealth.Healthy(ProviderHealthMetrics())
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
    private val fileStore = mutableMapOf<com.aniflow.domain.identity.DownloadFileId, DownloadFile>()

    override suspend fun getTaskById(id: DownloadTaskId): DownloadTask? = taskStore[id]
    override suspend fun getAllTasks(): List<DownloadTask> = taskStore.values.toList()
    override fun observeTasks(): Flow<List<DownloadTask>> = flowOf(taskStore.values.toList())
    override fun observeTasksByState(states: Set<DownloadState>): Flow<List<DownloadTask>> =
        flowOf(taskStore.values.filter { it.state in states })
    override suspend fun saveTask(task: DownloadTask) { taskStore[task.id] = task }
    override suspend fun deleteTask(id: DownloadTaskId) { taskStore.remove(id) }
    override suspend fun getFilesForTask(taskId: DownloadTaskId): List<DownloadFile> =
        fileStore.values.filter { it.taskId == taskId }
    override suspend fun saveFile(file: DownloadFile) { fileStore[file.id] = file }
    override suspend fun updateTaskState(id: DownloadTaskId, state: DownloadState, errorMessage: String?) {
        taskStore[id]?.let { taskStore[id] = it.copy(state = state, errorMessage = errorMessage) }
    }
    override suspend fun updateTaskProgress(id: DownloadTaskId, downloadedBytes: Long, totalBytes: Long?, speed: Long, eta: Long) {
        taskStore[id]?.let { taskStore[id] = it.copy(downloadedBytes = downloadedBytes, totalBytes = totalBytes ?: it.totalBytes, speedBytesPerSecond = speed, etaSeconds = eta) }
    }
    override suspend fun getQueuedTasks(limit: Int): List<DownloadTask> =
        taskStore.values.filter { it.state == DownloadState.Queued }.take(limit)
}

class InMemoryLibraryRepository : LibraryRepository {
    private val itemStore = mutableMapOf<LibraryItemId, LibraryItem>()
    private val fileStore = mutableMapOf<LibraryFileId, LibraryFile>()

    override suspend fun getItemById(id: LibraryItemId): LibraryItem? = itemStore[id]
    override suspend fun getAllItems(): List<LibraryItem> = itemStore.values.toList()
    override fun observeItems(): Flow<List<LibraryItem>> = flowOf(itemStore.values.toList())
    override suspend fun getFilesForItem(itemId: LibraryItemId): List<LibraryFile> =
        fileStore.values.filter { it.itemId == itemId }
    override suspend fun saveItem(item: LibraryItem) { itemStore[item.id] = item }
    override suspend fun saveFile(file: LibraryFile) { fileStore[file.id] = file }
    override suspend fun deleteItem(id: LibraryItemId) { itemStore.remove(id) }
}
