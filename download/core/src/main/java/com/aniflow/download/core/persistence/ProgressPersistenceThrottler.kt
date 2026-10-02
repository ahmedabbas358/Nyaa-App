package com.aniflow.download.core.persistence

import com.aniflow.download.core.model.DownloadTaskState

/**
 * Enforces STEP 13 Section 37 (Progress Persistence Throttling).
 *
 * Protects SQLite / Room and device battery from being inundated with hundreds of
 * database writes per second during high-speed multi-megabyte torrent/HTTP streaming.
 *
 * Rules:
 * 1. Time-based: at least minIntervalMs (default 1000ms) must elapse between periodic writes.
 * 2. Byte/Percent delta: at least 5% progress or 10 MB transferred before saving.
 * 3. State-based: State changes (e.g. Paused, Failed, Completed, Verifying) always persist immediately.
 */
class ProgressPersistenceThrottler(
    private val minIntervalMillis: Long = 1000L,
    private val minPercentDelta: Float = 0.05f, // 5%
    private val minBytesDelta: Long = 10L * 1024 * 1024 // 10 MB
) {

    private data class TaskSnapshot(
        val lastPersistTime: Long,
        val lastBytes: Long,
        val lastProgress: Float,
        val lastState: DownloadTaskState
    )

    private val taskSnapshots = mutableMapOf<String, TaskSnapshot>()

    /**
     * Determines whether the current task state/progress should trigger a database write.
     */
    @Synchronized
    fun shouldPersist(
        taskId: String,
        currentState: DownloadTaskState,
        downloadedBytes: Long,
        totalBytes: Long
    ): Boolean {
        val now = System.currentTimeMillis()
        val currentProgress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f
        val previous = taskSnapshots[taskId]

        if (previous == null) {
            taskSnapshots[taskId] = TaskSnapshot(now, downloadedBytes, currentProgress, currentState)
            return true
        }

        // 1. State change rule: Always persist immediately
        if (previous.lastState != currentState) {
            taskSnapshots[taskId] = TaskSnapshot(now, downloadedBytes, currentProgress, currentState)
            return true
        }

        // 2. Terminal state rule
        if (currentState == DownloadTaskState.Completed ||
            currentState == DownloadTaskState.Failed ||
            currentState == DownloadTaskState.Cancelled ||
            currentState == DownloadTaskState.Paused
        ) {
            taskSnapshots[taskId] = TaskSnapshot(now, downloadedBytes, currentProgress, currentState)
            return true
        }

        // 3. Time elapsed rule
        val timeElapsed = now - previous.lastPersistTime
        val bytesElapsed = downloadedBytes - previous.lastBytes
        val progressElapsed = kotlin.math.abs(currentProgress - previous.lastProgress)

        val shouldWrite = timeElapsed >= minIntervalMillis &&
                (bytesElapsed >= minBytesDelta || progressElapsed >= minPercentDelta)

        if (shouldWrite) {
            taskSnapshots[taskId] = TaskSnapshot(now, downloadedBytes, currentProgress, currentState)
            return true
        }

        return false
    }

    @Synchronized
    fun remove(taskId: String) {
        taskSnapshots.remove(taskId)
    }

    @Synchronized
    fun clear() {
        taskSnapshots.clear()
    }
}
