package com.aniflow.download.core.orchestrator

import com.aniflow.download.core.model.DownloadTask
import com.aniflow.download.core.model.DownloadTaskId
import com.aniflow.download.core.model.DownloadTaskState
import java.io.File

enum class ReconciliationIssueType {
    RecoverableActiveTask, // DB downloading, engine absent -> recover to Queued
    OrphanFileOnDisk,      // File exists on disk, DB missing -> user action
    StaleCompletedTask,    // DB completed, file missing on disk
    UnexpectedEngineTask   // Engine active, DB missing
}

data class ReconciliationIssue(
    val type: ReconciliationIssueType,
    val taskId: DownloadTaskId?,
    val filePath: String?,
    val recommendation: String
)

data class ReconciliationReport(
    val totalChecked: Int,
    val issues: List<ReconciliationIssue>
)

/**
 * Reconciles state across Database, Local Filesystem, and Active Engines upon startup (Section 114, 115, 116, 117).
 */
class DownloadReconciliationEngine {

    fun reconcile(
        dbTasks: List<DownloadTask>,
        activeEngineTaskIds: Set<DownloadTaskId>,
        downloadDirectories: List<File>
    ): ReconciliationReport {
        val issues = mutableListOf<ReconciliationIssue>()

        // 1. Check DB tasks vs Engine state
        dbTasks.forEach { task ->
            if (task.isActive && !activeEngineTaskIds.contains(task.id)) {
                issues += ReconciliationIssue(
                    type = ReconciliationIssueType.RecoverableActiveTask,
                    taskId = task.id,
                    filePath = task.destination.finalFilePath,
                    recommendation = "Requeue task from interrupted state"
                )
            } else if (task.state == DownloadTaskState.Completed) {
                val finalFile = File(task.destination.finalFilePath)
                if (!finalFile.exists()) {
                    issues += ReconciliationIssue(
                        type = ReconciliationIssueType.StaleCompletedTask,
                        taskId = task.id,
                        filePath = task.destination.finalFilePath,
                        recommendation = "Mark task missing or allow re-download"
                    )
                }
            }
        }

        // 2. Check for unexpected engine tasks
        activeEngineTaskIds.forEach { engineTaskId ->
            if (dbTasks.none { it.id == engineTaskId }) {
                issues += ReconciliationIssue(
                    type = ReconciliationIssueType.UnexpectedEngineTask,
                    taskId = engineTaskId,
                    filePath = null,
                    recommendation = "Stop unindexed active task"
                )
            }
        }

        return ReconciliationReport(
            totalChecked = dbTasks.size,
            issues = issues
        )
    }
}
