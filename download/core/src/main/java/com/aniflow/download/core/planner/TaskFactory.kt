package com.aniflow.download.core.planner

import com.aniflow.download.core.model.DownloadEngineSelector
import com.aniflow.download.core.model.DownloadPlanId
import com.aniflow.download.core.model.DownloadPlanItem
import com.aniflow.download.core.model.DownloadTask
import com.aniflow.download.core.model.DownloadTaskId
import com.aniflow.download.core.model.DownloadTaskState
import com.aniflow.download.core.model.RuntimeCapabilities
import java.time.Instant
import java.util.UUID

/**
 * Creates DownloadTask instances from validated DownloadPlanItems (Section 25, 26).
 * Strictly guarantees tasks start in Pending/Queued state and never transition to Downloading immediately.
 */
class TaskFactory(
    private val engineSelector: DownloadEngineSelector = DownloadEngineSelector()
) {

    fun createTask(
        planId: DownloadPlanId,
        item: DownloadPlanItem,
        capabilities: RuntimeCapabilities,
        initialState: DownloadTaskState = DownloadTaskState.Queued
    ): DownloadTask {
        val engineType = engineSelector.selectEngine(item.source, capabilities)
        val taskId = DownloadTaskId("task-${UUID.randomUUID()}")

        return DownloadTask(
            id = taskId,
            planId = planId,
            planItemId = item.id,
            releaseId = item.releaseId,
            episodeId = item.episodeId,
            source = item.source,
            engineType = engineType,
            destination = item.destination,
            priority = item.priority,
            state = initialState,
            downloadedBytes = 0L,
            totalBytes = item.estimatedBytes,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }
}
