package com.aniflow.download.service

import android.content.Context
import com.aniflow.download.core.model.DownloadTask
import com.aniflow.download.core.model.DownloadTaskId
import com.aniflow.download.core.model.DownloadTaskState
import com.aniflow.download.core.queue.DownloadQueue
import com.aniflow.platform.notifications.DownloadNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Bridges Android platform background APIs with the core download queue and engines (Section 81, 82, 83, 87, 88, 89, 90, 91, 92, 93, 94, 95).
 * Completely decouples DownloadEngine from Android Service, JobScheduler, and Context.
 */
class DownloadRuntimeCoordinator(
    private val context: Context,
    private val queue: DownloadQueue,
    private val strategySelector: RuntimeStrategySelector = RuntimeStrategySelector(),
    private val notificationManager: DownloadNotificationManager = DownloadNotificationManager(context),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) {
    private var isRuntimeRunning = false
    private var throttleJob: Job? = null

    init {
        // Observe queue changes to update summary notifications
        queue.tasksFlow.onEach { tasks ->
            val activeTasks = tasks.filter { it.isActive }
            if (activeTasks.isNotEmpty()) {
                startRuntimeIfNecessary()
                scheduleThrottledNotificationUpdate(tasks)
            } else if (isRuntimeRunning) {
                stopRuntimeIfNecessary()
            }
        }.launchIn(scope)
    }

    fun startRuntimeIfNecessary() {
        if (!isRuntimeRunning) {
            val reqs = RuntimeTaskRequirements(
                isUserInitiated = true,
                isLongRunningDataTransfer = true
            )
            val strategy = strategySelector.selectStrategy(reqs)

            when (strategy) {
                AndroidExecutionMechanism.UserInitiatedDataTransfer -> {
                    // On Android 14+, UIDT is the preferred path
                    DownloadForegroundService.start(context)
                }
                AndroidExecutionMechanism.ForegroundService -> {
                    DownloadForegroundService.start(context)
                }
                AndroidExecutionMechanism.WorkManagerDeferred -> {
                    // Deferred execution
                }
            }
            isRuntimeRunning = true
        }
    }

    fun stopRuntimeIfNecessary() {
        if (isRuntimeRunning) {
            DownloadForegroundService.stop(context)
            isRuntimeRunning = false
            throttleJob?.cancel()
            throttleJob = null
        }
    }

    private fun scheduleThrottledNotificationUpdate(tasks: List<DownloadTask>) {
        if (throttleJob?.isActive == true) return

        throttleJob = scope.launch {
            delay(1000L) // 1 second throttle rate
            val active = tasks.filter { it.isActive }
            val queued = tasks.filter { it.state == DownloadTaskState.Queued }

            val totalSpeed = active.sumOf { it.speedBytesPerSecond }
            val avgProgress = if (active.isNotEmpty()) {
                active.map { it.progressPercent }.average().toInt()
            } else 0

            val speedMb = (totalSpeed.toDouble() / (1024 * 1024))
            val speedText = String.format("%.2f MB/s • %d active, %d queued", speedMb, active.size, queued.size)

            val notification = notificationManager.buildProgressNotification(
                title = "AniFlow Downloading (${active.size})",
                progressPercent = avgProgress,
                speedText = speedText
            )
            notificationManager.showNotification(
                DownloadNotificationManager.ONGOING_NOTIFICATION_ID,
                notification
            )
            throttleJob = null
        }
    }

    /**
     * Recovers tasks following an unexpected process death (Section 90, 91).
     * Any task left in Starting/Downloading state is reconciled back to Queued for safe resume.
     */
    suspend fun recoverFromProcessDeath() {
        val tasks = queue.getAllTasks()
        tasks.forEach { task ->
            if (task.state == DownloadTaskState.Downloading || task.state == DownloadTaskState.Starting) {
                queue.updateTaskState(task.id, DownloadTaskState.Queued)
            }
        }
    }
}
