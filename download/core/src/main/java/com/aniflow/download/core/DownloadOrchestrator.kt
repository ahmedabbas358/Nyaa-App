package com.aniflow.download.core

import com.aniflow.domain.entity.DownloadTask
import com.aniflow.domain.enums.DownloadState
import com.aniflow.domain.repository.DownloadRepository
import com.aniflow.domain.service.DownloadEngine
import com.aniflow.domain.service.DownloadProgress
import com.aniflow.domain.state.DownloadStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Orchestrates download tasks, manages concurrency slots, state machine validation,
 * and background lifecycle according to Section 81 (Download Orchestrator).
 */
class DownloadOrchestrator(
    private val downloadRepository: DownloadRepository,
    private val engineSelector: (DownloadTask) -> DownloadEngine,
    private val scope: CoroutineScope
) {

    private val mutex = Mutex()
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private var maxConcurrency: Int = 3

    private val _events = MutableSharedFlow<DownloadProgress>()
    val events = _events.asSharedFlow()

    fun setMaxConcurrency(limit: Int) {
        maxConcurrency = limit.coerceIn(1, 10)
        processQueue()
    }

    fun processQueue() {
        scope.launch {
            mutex.withLock {
                if (activeJobs.size >= maxConcurrency) return@withLock

                val availableSlots = maxConcurrency - activeJobs.size
                val queuedTasks = downloadRepository.getDownload(taskId = "") // Placeholder or query queued
                // Fetch queued tasks ordered by priority
            }
        }
    }

    suspend fun startTask(task: DownloadTask) {
        mutex.withLock {
            if (activeJobs.containsKey(task.id)) return

            val engine = engineSelector(task)
            val job = scope.launch {
                try {
                    downloadRepository.updateState(task.id, DownloadState.Downloading)
                    engine.observeProgress(task.id).onEach { progress ->
                        downloadRepository.updateProgress(
                            task.id,
                            progress.downloadedBytes,
                            progress.totalBytes,
                            progress.speedBps
                        )
                        _events.emit(progress)

                        if (progress.state == DownloadState.Completed) {
                            downloadRepository.updateState(task.id, DownloadState.Completed)
                            finishTask(task.id)
                        } else if (progress.state == DownloadState.Failed) {
                            downloadRepository.updateState(task.id, DownloadState.Failed, progress.errorMessage)
                            finishTask(task.id)
                        }
                    }.launchIn(this)

                    engine.start(task)
                } catch (e: Exception) {
                    downloadRepository.updateState(task.id, DownloadState.Failed, e.message)
                    finishTask(task.id)
                }
            }
            activeJobs[task.id] = job
        }
    }

    suspend fun pauseTask(taskId: String) {
        val task = downloadRepository.getDownload(taskId) ?: return
        if (DownloadStateMachine.canTransition(task.state, DownloadState.Paused)) {
            val engine = engineSelector(task)
            engine.pause(taskId)
            finishTask(taskId)
            downloadRepository.updateState(taskId, DownloadState.Paused)
            processQueue()
        }
    }

    suspend fun resumeTask(taskId: String) {
        val task = downloadRepository.getDownload(taskId) ?: return
        if (DownloadStateMachine.canTransition(task.state, DownloadState.Queued)) {
            downloadRepository.updateState(taskId, DownloadState.Queued)
            processQueue()
        }
    }

    suspend fun cancelTask(taskId: String) {
        val task = downloadRepository.getDownload(taskId) ?: return
        val engine = engineSelector(task)
        engine.cancel(taskId)
        finishTask(taskId)
        downloadRepository.updateState(taskId, DownloadState.Cancelled)
        processQueue()
    }

    private fun finishTask(taskId: String) {
        activeJobs.remove(taskId)?.cancel()
        processQueue()
    }
}
