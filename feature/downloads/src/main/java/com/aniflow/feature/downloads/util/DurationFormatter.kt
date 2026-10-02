package com.aniflow.feature.downloads.util

import java.util.Locale

/**
 * DurationFormatter (Section 72, 75).
 * Formats ETA and durations into human readable strings:
 * - "12s"
 * - "03:24"
 * - "1h 20m"
 * - "2d 4h"
 */
object DurationFormatter {

    fun formatEta(seconds: Long?): String {
        if (seconds == null || seconds < 0) return "Estimating…"
        if (seconds == 0L) return "Complete"

        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        return when {
            hours > 48 -> "${hours / 24}d ${hours % 24}h"
            hours > 0 -> String.format(Locale.US, "%dh %02dm", hours, minutes)
            minutes > 0 -> String.format(Locale.US, "%02d:%02d", minutes, secs)
            else -> "${secs}s"
        }
    }

    fun formatDuration(durationMillis: Long): String {
        val totalSecs = (durationMillis / 1000).coerceAtLeast(0)
        val hours = totalSecs / 3600
        val minutes = (totalSecs % 3600) / 60
        val secs = totalSecs % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${secs}s"
            else -> "${secs}s"
        }
    }
}
