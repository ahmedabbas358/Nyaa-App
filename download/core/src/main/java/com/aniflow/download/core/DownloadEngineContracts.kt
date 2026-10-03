package com.aniflow.download.core

import com.aniflow.domain.identity.DownloadTaskId
import com.aniflow.domain.model.aggregate.download.DownloadTask
import com.aniflow.domain.model.aggregate.download.DownloadPriority as Priority
import com.aniflow.domain.repository.DownloadRepository
import com.aniflow.domain.state.DownloadState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

data class DownloadCapabilities(
    val supportsRangeRequests: Boolean = true,
    val supportsMultiSegment: Boolean = true,
    val supportsPauseResume: Boolean = true,
    val supportsSpeedLimit: Boolean = true,
    val engineName: String
)

data class DownloadRequest(
    val id: String,
    val sourceUri: String,
    val destinationPath: String,
    val priority: Priority = Priority.Normal,
    val totalBytes: Long = 0L,
    val isTorrent: Boolean = false
)

interface DownloadEngine {
    val engineType: String
    suspend fun start(task: DownloadTask): Flow<DownloadProgress>
    fun observeProgress(taskId: String): Flow<DownloadProgress> = emptyFlow()
    fun observeProgress(taskId: DownloadTaskId): Flow<DownloadProgress> = observeProgress(taskId.value)
    suspend fun pause(taskId: String) {}
    suspend fun pause(taskId: DownloadTaskId) = pause(taskId.value)
    suspend fun resume(taskId: String) {}
    suspend fun resume(taskId: DownloadTaskId) = resume(taskId.value)
    suspend fun cancel(taskId: String) {}
    suspend fun cancel(taskId: DownloadTaskId) = cancel(taskId.value)
}

data class DownloadProgress(
    val taskId: String,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val speedBps: Long = 0L,
    val state: DownloadState = DownloadState.Downloading,
    val errorMessage: String? = null
)

interface DownloadEngineRegistry {
    fun register(engine: DownloadEngine)
    fun getEngine(engineType: String): DownloadEngine?
    fun selectEngineForRequest(request: DownloadRequest): DownloadEngine?
    fun getAllEngines(): List<DownloadEngine>
}

class DefaultDownloadEngineRegistry : DownloadEngineRegistry {
    private val engines = mutableMapOf<String, DownloadEngine>()

    override fun register(engine: DownloadEngine) {
        engines[engine.engineType] = engine
    }

    override fun getEngine(engineType: String): DownloadEngine? = engines[engineType]

    override fun selectEngineForRequest(request: DownloadRequest): DownloadEngine? {
        return if (request.isTorrent) engines["TORRENT"] else engines["HTTP"]
    }

    override fun getAllEngines(): List<DownloadEngine> = engines.values.toList()
}

suspend fun DownloadRepository.getDownload(taskId: String): DownloadTask? =
    if (taskId.isBlank()) null else getTaskById(DownloadTaskId(taskId))

suspend fun DownloadRepository.updateState(taskId: String, state: DownloadState, errorMessage: String? = null) {
    if (taskId.isBlank()) return
    val task = getTaskById(DownloadTaskId(taskId)) ?: return
    saveTask(task.copy(state = state, errorMessage = errorMessage))
}

suspend fun DownloadRepository.updateState(taskId: DownloadTaskId, state: DownloadState, errorMessage: String? = null) {
    updateState(taskId.value, state, errorMessage)
}

suspend fun DownloadRepository.updateProgress(
    taskId: DownloadTaskId,
    downloadedBytes: Long,
    totalBytes: Long,
    speedBps: Long
) {
    // Progress tracking placeholder
}
