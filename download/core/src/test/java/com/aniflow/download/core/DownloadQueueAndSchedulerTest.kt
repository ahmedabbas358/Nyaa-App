package com.aniflow.download.core

import com.aniflow.domain.identity.EpisodeId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.model.aggregate.download.NetworkPolicy
import com.aniflow.download.core.model.DownloadEngineType
import com.aniflow.download.core.model.DownloadPlanId
import com.aniflow.download.core.model.DownloadPriority
import com.aniflow.download.core.model.DownloadSource
import com.aniflow.download.core.model.DownloadTask
import com.aniflow.download.core.model.DownloadTaskId
import com.aniflow.download.core.model.DownloadTaskState
import com.aniflow.download.core.model.NetworkType
import com.aniflow.download.core.model.PlannedDestination
import com.aniflow.download.core.model.RuntimeCapabilities
import com.aniflow.download.core.model.StorageTarget
import com.aniflow.download.core.queue.DownloadQueue
import com.aniflow.download.core.scheduler.DownloadScheduler
import com.aniflow.download.core.scheduler.SchedulerConfig
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class DownloadQueueAndSchedulerTest {

    private fun createTask(
        id: String,
        priority: DownloadPriority = DownloadPriority.Normal,
        engineType: DownloadEngineType = DownloadEngineType.Http,
        state: DownloadTaskState = DownloadTaskState.Queued
    ): DownloadTask = DownloadTask(
        id = DownloadTaskId(id),
        planId = DownloadPlanId("plan-1"),
        releaseId = ReleaseId("rel-$id"),
        episodeId = EpisodeId("ep-$id"),
        source = DownloadSource.HttpSource("https://example.com/$id.mkv"),
        engineType = engineType,
        destination = PlannedDestination(
            storageTarget = StorageTarget("loc", "Anime", "/downloads"),
            filename = "$id.mkv",
            tempDirectory = "/downloads/temp",
            finalDirectory = "/downloads/final"
        ),
        priority = priority,
        state = state,
        createdAt = Instant.now()
    )

    @Test
    fun queue_ordersTasksByPriorityDescending() = runTest {
        val queue = DownloadQueue()
        val lowTask = createTask("low", DownloadPriority.Low)
        val highestTask = createTask("highest", DownloadPriority.Highest)
        val normalTask = createTask("normal", DownloadPriority.Normal)

        queue.enqueueAll(listOf(lowTask, highestTask, normalTask))

        val all = queue.getAllTasks()
        assertEquals("highest", all[0].id.value)
        assertEquals("normal", all[1].id.value)
        assertEquals("low", all[2].id.value)
    }

    @Test
    fun scheduler_enforcesGlobalConcurrencyLimits() = runTest {
        val queue = DownloadQueue()
        val config = SchedulerConfig(maxGlobalActiveTasks = 2)
        val scheduler = DownloadScheduler(queue, config)

        val task1 = createTask("task-1")
        val task2 = createTask("task-2")
        val task3 = createTask("task-3")

        queue.enqueueAll(listOf(task1, task2, task3))

        val capabilities = RuntimeCapabilities(networkType = NetworkType.Wifi)

        // Claim slot 1
        val claimed1 = scheduler.selectNextTaskAndClaim(capabilities, NetworkPolicy.AnyNetwork)
        assertNotNull(claimed1)
        assertEquals("task-1", claimed1?.id?.value)
        assertEquals(DownloadTaskState.Starting, claimed1?.state)

        // Claim slot 2
        val claimed2 = scheduler.selectNextTaskAndClaim(capabilities, NetworkPolicy.AnyNetwork)
        assertNotNull(claimed2)
        assertEquals("task-2", claimed2?.id?.value)

        // Slot 3 should be null because maxGlobalActiveTasks = 2
        val claimed3 = scheduler.selectNextTaskAndClaim(capabilities, NetworkPolicy.AnyNetwork)
        assertNull("Scheduler must not exceed max active concurrency limit", claimed3)
    }

    @Test
    fun scheduler_blocksWhenWifiOnlyAndNetworkIsCellular() = runTest {
        val queue = DownloadQueue()
        val scheduler = DownloadScheduler(queue)

        val task = createTask("task-wifi")
        queue.enqueue(task)

        // Runtime is on Cellular
        val capabilities = RuntimeCapabilities(networkType = NetworkType.Cellular)

        val claimed = scheduler.selectNextTaskAndClaim(capabilities, NetworkPolicy.WifiOnly)
        assertNull("Scheduler must block task when WifiOnly policy is violated", claimed)
    }
}
