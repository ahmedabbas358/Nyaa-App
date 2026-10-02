package com.aniflow.core.ui.util

import java.text.DecimalFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Standardized UI Formatters (Sections 117, 118, 119, 120, 121).
 * Single source of truth for numbers, bytes, speeds, ETAs, and dates across all screens.
 */
object UiFormatters {

    private val decFormatOne = DecimalFormat("#,##0.0")
    private val decFormatTwo = DecimalFormat("#,##0.00")

    /**
     * File size formatter: B, KB, MB, GB, TB using binary 1024 base.
     */
    fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var unitIndex = 0
        while (value >= 1024.0 && unitIndex < units.size - 1) {
            value /= 1024.0
            unitIndex++
        }
        return if (unitIndex == 0) {
            "${bytes} B"
        } else if (value >= 100.0) {
            "${value.toInt()} ${units[unitIndex]}"
        } else {
            "${decFormatOne.format(value)} ${units[unitIndex]}"
        }
    }

    /**
     * Transfer speed formatter: B/s, KB/s, MB/s, GB/s.
     */
    fun formatSpeed(bytesPerSec: Long): String {
        if (bytesPerSec <= 0L) return "0 B/s"
        return "${formatBytes(bytesPerSec)}/s"
    }

    /**
     * ETA formatter (e.g. "45s", "02:14", "1h 24m", "Unknown").
     */
    fun formatEta(remainingSeconds: Long?): String {
        if (remainingSeconds == null || remainingSeconds <= 0L || remainingSeconds >= 86400 * 7) {
            return "—"
        }
        val hours = remainingSeconds / 3600
        val minutes = (remainingSeconds % 3600) / 60
        val seconds = remainingSeconds % 60

        return when {
            hours > 0 -> String.format(Locale.US, "%dh %02dm", hours, minutes)
            minutes > 0 -> String.format(Locale.US, "%02d:%02d", minutes, seconds)
            else -> "${seconds}s"
        }
    }

    /**
     * Relative date formatter (Today, Yesterday, MMM d, or MMM d yyyy).
     */
    fun formatDate(epochMillis: Long): String {
        if (epochMillis <= 0L) return ""
        val itemDate = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        val today = LocalDate.now(ZoneId.systemDefault())

        val daysBetween = ChronoUnit.DAYS.between(itemDate, today)
        return when {
            daysBetween == 0L -> "Today"
            daysBetween == 1L -> "Yesterday"
            itemDate.year == today.year -> itemDate.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))
            else -> itemDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))
        }
    }

    /**
     * Clean episode numbering formatter (e.g. 1 -> "01", 12.5 -> "12.5").
     */
    fun formatEpisodeNumber(ep: Double?): String {
        if (ep == null) return "—"
        return if (ep % 1.0 == 0.0) {
            String.format(Locale.US, "%02d", ep.toInt())
        } else {
            ep.toString()
        }
    }
}
