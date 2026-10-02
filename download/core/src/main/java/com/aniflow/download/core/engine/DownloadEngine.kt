package com.aniflow.download.core.engine

import com.aniflow.download.core.model.DownloadEngineType
import com.aniflow.download.core.model.DownloadError
import com.aniflow.download.core.model.DownloadProgress
import com.aniflow.download.core.model.DownloadSource
import com.aniflow.download.core.model.DownloadTask
import com.aniflow.download.core.model.DownloadTaskId
import com.aniflow.download.core.model.DownloadTaskState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

sealed interface DownloadCompletionResult {
    data class Success(val finalBytes: Long) : DownloadCompletionResult
    data class Failed(val error: DownloadError) : DownloadCompletionResult
}

/**
 * Handle representing an active download execution stream (Section 37).
 */
interface DownloadHandle {
    val taskId: DownloadTaskId
    val state: Flow<DownloadTaskState>
    val progress: Flow<DownloadProgress>
    val speedBps: Flow<Long>

    suspend fun pause()
    suspend fun resume()
    suspend fun cancel()
    suspend fun awaitCompletion(): DownloadCompletionResult
}

data class EngineProgress(
    val taskId: DownloadTaskId,
    val downloadedBytes: Long,
    val totalBytes: Long?,
    val speedBytesPerSecond: Long,
    val etaSeconds: Long?,
    val activeConnections: Int = 1,
    val state: DownloadTaskState = DownloadTaskState.Downloading,
    val errorMessage: String? = null
)

data class EngineCapabilities(
    val supportsResume: Boolean = true,
    val supportsPause: Boolean = true,
    val supportsFileSelection: Boolean = true,
    val supportsSegments: Boolean = true,
    val supportsChecksum: Boolean = true,
    val supportsSeeding: Boolean = false
)

/**
 * Download Engine Contract adhering to Step 22 Sections 36, 134, 135.
 */
interface DownloadEngine {
    val engineType: DownloadEngineType
    val capabilities: EngineCapabilities

    fun supports(source: DownloadSource): Boolean

    suspend fun start(task: DownloadTask): DownloadHandle
    suspend fun pause(taskId: DownloadTaskId)
    suspend fun resume(taskId: DownloadTaskId)
    suspend fun cancel(taskId: DownloadTaskId)
    suspend fun remove(taskId: DownloadTaskId)
    suspend fun recheck(taskId: DownloadTaskId)
    fun observe(taskId: DownloadTaskId): Flow<EngineProgress>
}

enum class EngineHealth {
    Available,
    Unavailable,
    Initializing,
    Error
}

/**
 * Engine Registry adhering to Step 22 Section 134.
 */
interface DownloadEngineRegistry {
    fun register(engine: DownloadEngine)
    fun resolve(source: DownloadSource): DownloadEngine?
    fun get(type: DownloadEngineType): DownloadEngine?
    fun setHealth(type: DownloadEngineType, health: EngineHealth)
    fun getHealth(type: DownloadEngineType): EngineHealth
    fun isAvailable(type: DownloadEngineType): Boolean
    fun getCapabilities(type: DownloadEngineType): EngineCapabilities?
}

class DefaultDownloadEngineRegistry : DownloadEngineRegistry {
    private val engines = mutableMapOf<DownloadEngineType, DownloadEngine>()
    private val healthMap = mutableMapOf<DownloadEngineType, EngineHealth>()

    override fun register(engine: DownloadEngine) {
        engines[engine.engineType] = engine
        healthMap[engine.engineType] = EngineHealth.Available
    }

    override fun resolve(source: DownloadSource): DownloadEngine? {
        return when (source) {
            is DownloadSource.Http, is DownloadSource.HttpSource -> engines[DownloadEngineType.Http]
            is DownloadSource.Magnet, is DownloadSource.MagnetSource,
            is DownloadSource.TorrentFile, is DownloadSource.TorrentFileSource -> engines[DownloadEngineType.Torrent]
            is DownloadSource.DirectFileSource -> engines[DownloadEngineType.DirectFile] ?: engines[DownloadEngineType.Http]
        }
    }

    override fun get(type: DownloadEngineType): DownloadEngine? = engines[type]

    override fun setHealth(type: DownloadEngineType, health: EngineHealth) {
        healthMap[type] = health
    }

    override fun getHealth(type: DownloadEngineType): EngineHealth =
        healthMap[type] ?: EngineHealth.Unavailable

    override fun isAvailable(type: DownloadEngineType): Boolean =
        getHealth(type) == EngineHealth.Available

    override fun getCapabilities(type: DownloadEngineType): EngineCapabilities? =
        engines[type]?.capabilities
}
