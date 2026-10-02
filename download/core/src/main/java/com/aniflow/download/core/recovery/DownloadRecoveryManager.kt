package com.aniflow.download.core.recovery

import com.aniflow.download.core.model.DownloadTaskId
import com.aniflow.download.core.model.DownloadTaskState
import com.aniflow.download.core.queue.DownloadQueue
import com.aniflow.download.core.storage.StorageReservationManager
import java.io.File

data class RecoveryScanResult(
    val reconciledTasks: List<DownloadTaskId>,
    val resumedTasks: List<DownloadTaskId>,
    val orphanedFiles: List<File>,
    val freedReservationCount: Int
)

/**
 * Temporary file cleanup diagnostics and safe maintenance (Section 95).
 */
class TemporaryFileCleanup {
    fun scanDirectory(tempDir: File, activePartPaths: Set<String>): List<File> {
        if (!tempDir.exists() || !tempDir.isDirectory) return emptyList()
        val allFiles = tempDir.listFiles() ?: return emptyList()
        return allFiles.filter { file ->
            file.isFile && (file.extension == "part" || file.name.contains(".part")) && file.absolutePath !in activePartPaths
        }
    }

    fun deleteOrphanedFiles(orphans: List<File>): Int {
        var count = 0
        orphans.forEach {
            if (it.delete()) count++
        }
        return count
    }
}

/**
 * DownloadRecoveryManager adhering to Step 22 Sections 54, 55, 56, 95, 96, 97, 103, 104, 105.
 *
 * Responsibilities:
 * - Scans persistent tasks and partial files on startup.
 * - Reconciles tasks left in Downloading/Starting after crash/process death back to Queued with partial bytes intact.
 * - Reconciles orphaned storage space reservations.
 * - Detects orphaned .part files for cleanup.
 */
class DownloadRecoveryManager(
    private val queue: DownloadQueue,
    private val reservationManager: StorageReservationManager? = null,
    private val partialManager: PartialFileIntegrityManager = PartialFileIntegrityManager(),
    private val tempCleanup: TemporaryFileCleanup = TemporaryFileCleanup()
) {

    /**
     * Executes full recovery reconciliation upon application initialization.
     */
    suspend fun recover(): RecoveryScanResult {
        val allTasks = queue.getAllTasks()
        val reconciledTasks = mutableListOf<DownloadTaskId>()
        val resumedTasks = mutableListOf<DownloadTaskId>()

        // 1. Identify tasks left in transient execution states after crash
        for (task in allTasks) {
            if (task.state == DownloadTaskState.Starting ||
                task.state == DownloadTaskState.Downloading ||
                task.state == DownloadTaskState.Moving ||
                task.state == DownloadTaskState.Verifying
            ) {
                val partFile = File(task.destination.tempFilePath)
                val totalBytes = task.totalBytes ?: 0L

                val assessment = partialManager.assessPartialFile(partFile, totalBytes)
                when (assessment) {
                    is PartialFileIntegrityManager.RecoveryAssessment.ResumeEligible -> {
                        // Keep partial bytes, move back to Queued for safe automatic resume
                        queue.updateProgress(
                            taskId = task.id,
                            downloadedBytes = assessment.existingBytes,
                            totalBytes = task.totalBytes,
                            speedBps = 0L,
                            etaSeconds = null
                        )
                        queue.updateTaskState(task.id, DownloadTaskState.Queued)
                        resumedTasks.add(task.id)
                    }
                    is PartialFileIntegrityManager.RecoveryAssessment.CorruptedNeedsReset -> {
                        // Reset corrupted partial and requeue
                        partFile.delete()
                        queue.updateProgress(task.id, 0L, task.totalBytes, 0L, null)
                        queue.updateTaskState(task.id, DownloadTaskState.Queued)
                        reconciledTasks.add(task.id)
                    }
                    PartialFileIntegrityManager.RecoveryAssessment.FreshStartRequired -> {
                        queue.updateProgress(task.id, 0L, task.totalBytes, 0L, null)
                        queue.updateTaskState(task.id, DownloadTaskState.Queued)
                        reconciledTasks.add(task.id)
                    }
                }
            }
        }

        // 2. Reconcile storage reservations against currently active/queued task IDs (Section 17)
        val activeTaskIds = allTasks.filter { !it.isFinished }.map { it.id.value }.toSet()
        val freedReservations = reservationManager?.reconcile(activeTaskIds)?.size ?: 0

        // 3. Scan for orphaned partial files (Section 95 & 96)
        val activePartPaths = allTasks.map { it.destination.tempFilePath }.toSet()
        val tempDirs = allTasks.map { File(it.destination.tempDirectory) }.distinct()
        val orphanedFiles = tempDirs.flatMap { dir ->
            tempCleanup.scanDirectory(dir, activePartPaths)
        }

        return RecoveryScanResult(
            reconciledTasks = reconciledTasks,
            resumedTasks = resumedTasks,
            orphanedFiles = orphanedFiles,
            freedReservationCount = freedReservations
        )
    }
}
