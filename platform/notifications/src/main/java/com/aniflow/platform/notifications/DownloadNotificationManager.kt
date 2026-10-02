package com.aniflow.platform.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

class DownloadNotificationManager(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val progressChannel = NotificationChannel(
                CHANNEL_PROGRESS,
                "Download Progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Real-time download progress and speed updates"
            }

            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS,
                "Download Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts for completed and failed download tasks"
            }

            notificationManager.createNotificationChannel(progressChannel)
            notificationManager.createNotificationChannel(alertsChannel)
        }
    }

    fun buildProgressNotification(
        title: String,
        progressPercent: Int,
        speedText: String
    ): Notification {
        return NotificationCompat.Builder(context, CHANNEL_PROGRESS)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText("$speedText • $progressPercent%")
            .setProgress(100, progressPercent, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    fun notifyCompleted(title: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Download Complete")
            .setContentText(title)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(title.hashCode(), notification)
    }

    fun notifyFailed(title: String, error: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Download Failed")
            .setContentText("$title: $error")
            .setAutoCancel(true)
            .build()
        notificationManager.notify(title.hashCode(), notification)
    }

    companion object {
        const val CHANNEL_PROGRESS = "aniflow_channel_progress"
        const val CHANNEL_ALERTS = "aniflow_channel_alerts"
        const val ONGOING_NOTIFICATION_ID = 1001
    }
}
