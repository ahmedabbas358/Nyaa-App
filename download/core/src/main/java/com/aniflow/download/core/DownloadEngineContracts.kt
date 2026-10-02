package com.aniflow.download.core

import com.aniflow.domain.entity.DownloadTask
import com.aniflow.domain.enums.DownloadState
import com.aniflow.domain.enums.Priority
import com.aniflow.domain.service.DownloadEngine
import com.aniflow.domain.service.DownloadProgress
import kotlinx.coroutines.flow.Flow
import java.time.Instant

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
