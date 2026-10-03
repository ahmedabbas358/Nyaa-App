package com.aniflow.domain.diagnostics

import com.aniflow.core.common.result.AniFlowResult
import com.aniflow.domain.model.aggregate.download.DownloadTask
import com.aniflow.domain.repository.DownloadRepository
import com.aniflow.domain.repository.LibraryRepository
import com.aniflow.domain.state.DownloadState
import java.time.Instant

/**
 * Diagnostics Bundle enforcing STEP 13 Section 79 (Support Diagnostics)
 * and Section 98 (Support Bundle).
 *
 * Guaranteed to NEVER leak user passwords, tokens, private URIs, or credentials.
 */
data class SupportDiagnosticsBundle(
    val appVersion: String = "1.0.0",
    val buildNumber: Int = 100,
    val osVersion: String = "Android API 34",
    val databaseVersion: Int = 1,
    val providerHealth: Map<String, String>,
    val activeTasksCount: Int,
    val failedTasksCount: Int,
    val storageAvailableBytes: Long,
    val timestamp: Instant = Instant.now()
) {
    fun toSanitizedText(): String {
        return buildString {
            appendLine("=== AniFlow Diagnostics Report ===")
            appendLine("Timestamp: $timestamp")
            appendLine("App Version: $appVersion ($buildNumber)")
            appendLine("OS: $osVersion | DB Version: $databaseVersion")
            appendLine("Active Tasks: $activeTasksCount | Failed Tasks: $failedTasksCount")
            appendLine("Available Storage: ${storageAvailableBytes / (1024 * 1024)} MB")
            appendLine("Providers Health:")
            providerHealth.forEach { (provider, status) ->
                appendLine("  - $provider: $status")
            }
            appendLine("==================================")
        }
    }
}

/**
 * Safe Repair Tools enforcing STEP 13 Section 80 (Safe Repair Tools).
 * Provides safe, non-destructive reconciliation and repair mechanisms.
 */
class SafeRepairService(
    private val downloadRepository: DownloadRepository,
    private val libraryRepository: LibraryRepository
) {

    /**
     * Reconciles downloads stuck in transient downloading/starting states after process death.
     */
    suspend fun reconcileDownloads(): Int {
        val allTasks = downloadRepository.getAllTasks()
        var recoveredCount = 0

        for (task in allTasks) {
            if (task.state == DownloadState.Downloading || task.state == DownloadState.Pending) {
                val recovered = task.copy(state = DownloadState.Queued)
                downloadRepository.saveTask(recovered)
                recoveredCount++
            }
        }

        return recoveredCount
    }

    /**
     * Retries all tasks currently in Failed state.
     */
    suspend fun retryFailedTasks(): Int {
        val allTasks = downloadRepository.getAllTasks()
        var retriedCount = 0

        for (task in allTasks) {
            if (task.state == DownloadState.Failed) {
                val retried = task.copy(state = DownloadState.Queued)
                downloadRepository.saveTask(retried)
                retriedCount++
            }
        }

        return retriedCount
    }
}
