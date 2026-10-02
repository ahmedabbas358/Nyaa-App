package com.aniflow.download.core

import com.aniflow.domain.identity.EpisodeId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.download.core.model.DownloadEngineType
import com.aniflow.download.core.model.DownloadPlanId
import com.aniflow.download.core.model.DownloadPriority
import com.aniflow.download.core.model.DownloadSource
import com.aniflow.download.core.model.DownloadTask
import com.aniflow.download.core.model.DownloadTaskId
import com.aniflow.download.core.model.DownloadTaskState
import com.aniflow.download.core.model.PlannedDestination
import com.aniflow.download.core.model.StorageTarget
import com.aniflow.download.core.orchestrator.DownloadReconciliationEngine
import com.aniflow.download.core.orchestrator.ReconciliationIssueType
import com.aniflow.download.core.verification.IntegrityVerificationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Instant

class ReconciliationAndVerificationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun testTask(id: String, state: DownloadTaskState, path: String): DownloadTask = DownloadTask(
        id = DownloadTaskId(id),
        planId = DownloadPlanId("plan-1"),
        releaseId = ReleaseId("rel-$id"),
        episodeId = EpisodeId("ep-$id"),
        source = DownloadSource.HttpSource("https://example.com/$id.mkv"),
        engineType = DownloadEngineType.Http,
        destination = PlannedDestination(
            storageTarget = StorageTarget("loc", "Anime", tempFolder.root.absolutePath),
            filename = "$id.mkv",
            tempDirectory = tempFolder.root.absolutePath,
            finalDirectory = tempFolder.root.absolutePath
        ),
        priority = DownloadPriority.Normal,
        state = state,
        createdAt = Instant.now()
    )

    @Test
    fun reconciliation_detectsInterruptedActiveTasksForRecovery() {
        val reconciliation = DownloadReconciliationEngine()

        // DB says task is Downloading, but engine has no active task running
        val interruptedTask = testTask("task-interrupted", DownloadTaskState.Downloading, "/test/path.mkv")
        val activeEngineIds = emptySet<DownloadTaskId>()

        val report = reconciliation.reconcile(
            dbTasks = listOf(interruptedTask),
            activeEngineTaskIds = activeEngineIds,
            downloadDirectories = emptyList()
        )

        assertEquals(1, report.issues.size)
        assertEquals(ReconciliationIssueType.RecoverableActiveTask, report.issues.first().type)
        assertEquals(interruptedTask.id, report.issues.first().taskId)
    }

    @Test
    fun integrityVerification_verifiesCorrectFileSizes() {
        val verifier = IntegrityVerificationEngine()
        val file = tempFolder.newFile("test_video.mkv")
        file.writeBytes(ByteArray(1024)) // 1024 bytes

        // Matches
        val success = verifier.verifyFileSize(file, 1024L)
        assertTrue(success.isVerified)

        // Mismatches
        val failure = verifier.verifyFileSize(file, 2048L)
        assertFalse(failure.isVerified)
        assertTrue(failure.failureReason!!.contains("mismatch"))
    }
}
