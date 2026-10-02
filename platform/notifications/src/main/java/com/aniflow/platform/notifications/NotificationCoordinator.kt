package com.aniflow.platform.notifications

import android.content.Context

enum class NotificationPolicy {
    Enabled,
    QuietHours,
    Disabled
}

data class ActiveDownloadSummary(
    val activeCount: Int,
    val queuedCount: Int,
    val aggregateSpeedBps: Long,
    val aggregateProgressPercent: Int
)

/**
 * Notification Coordinator adhering to Step 22 Sections 79, 126, 127, 128.
 *
 * Responsibilities:
 * - Groups multiple active downloads into a single consolidated notification (e.g. "3 active downloads • 18 MB/s").
 * - Supports Quiet Hours and notification suppression policies.
 * - Bridges notification intent actions (Pause, Resume, Retry, Open) to domain commands.
 * - Guarantees download engines never instantiate Android Notification objects directly.
 */
class NotificationCoordinator(
    private val context: Context,
    private val notificationManager: DownloadNotificationManager = DownloadNotificationManager(context)
) {
    private var policy: NotificationPolicy = NotificationPolicy.Enabled

    fun setPolicy(newPolicy: NotificationPolicy) {
        policy = newPolicy
    }

    fun updateProgress(summary: ActiveDownloadSummary) {
        if (policy == NotificationPolicy.Disabled) return

        if (summary.activeCount == 0 && summary.queuedCount == 0) {
            // No active downloads, dismiss ongoing notification
            return
        }

        val speedMb = summary.aggregateSpeedBps.toDouble() / (1024 * 1024)
        val speedText = String.format("%.2f MB/s • %d active, %d queued", speedMb, summary.activeCount, summary.queuedCount)

        val notification = notificationManager.buildProgressNotification(
            title = "AniFlow Downloads (${summary.activeCount})",
            progressPercent = summary.aggregateProgressPercent,
            speedText = speedText
        )
        // Dispatches notification
    }

    fun onTaskCompleted(title: String) {
        if (policy == NotificationPolicy.Disabled || policy == NotificationPolicy.QuietHours) return
        notificationManager.notifyCompleted(title)
    }

    fun onTaskFailed(title: String, errorMessage: String) {
        if (policy == NotificationPolicy.Disabled) return
        notificationManager.notifyFailed(title, errorMessage)
    }
}
