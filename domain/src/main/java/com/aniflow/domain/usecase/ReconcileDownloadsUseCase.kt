package com.aniflow.domain.usecase

import com.aniflow.domain.model.aggregate.download.DownloadTask
import com.aniflow.domain.repository.DownloadRepository
import com.aniflow.domain.state.DownloadState
import java.time.Instant

data class StartupReconciliationResult(
    val recoveredTasksCount: Int,
    val failedTasksCount: Int
)

/**
 * ReconcileDownloadsUseCase (Section 31, 54).
 * Reconciles state across persisted Database and physical Filesystem on application startup
 * to recover interrupted downloads without relying on volatile in-memory state.
 */
class ReconcileDownloadsUseCase(
    private val downloadRepository: DownloadRepository
) {

    suspend operator fun invoke(
        existingPartFiles: Set<String> = emptySet()
    ): StartupReconciliationResult {
        val allTasks = downloadRepository.getAllTasks()
        var recovered = 0
        var failed = 0

        for (task in allTasks) {
            if (task.state == DownloadState.Downloading || task.state == DownloadState.Starting) {
                // If app was killed while downloading, re-evaluate part file presence
                val hasPart = existingPartFiles.contains(task.id.value)
                val newState = if (hasPart) DownloadState.Paused else DownloadState.Pending
                val updated = task.copy(
                    state = newState,
                    updatedAt = Instant.now()
                )
                downloadRepository.saveTask(updated)
                recovered++
            }
        }

        return StartupReconciliationResult(
            recoveredTasksCount = recovered,
            failedTasksCount = failed
        )
    }
}
