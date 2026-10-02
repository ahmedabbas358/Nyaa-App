package com.aniflow.feature.downloads.util

import java.util.Locale

/**
 * ByteSizeFormatter (Section 74).
 * Single shared formatter ensuring consistent units across all screens.
 * Uses standard decimal SI units (1 KB = 1000 B, 1 MB = 1000 KB) with clean precision.
 */
object ByteSizeFormatter {

    fun format(bytes: Long): String {
        if (bytes < 0) return "0 B"
        if (bytes < 1000) return "$bytes B"

        val exp = (Math.log10(bytes.toDouble()) / Math.log10(1000.0)).toInt().coerceIn(1, 4)
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val value = bytes / Math.pow(1000.0, exp.toDouble())

        return if (value >= 100.0 || exp == 1) {
            String.format(Locale.US, "%.0f %s", value, units[exp])
        } else {
            String.format(Locale.US, "%.1f %s", value, units[exp])
        }
    }

    fun formatSpeed(bytesPerSec: Long): String {
        if (bytesPerSec <= 0) return "0 KB/s"
        return "${format(bytesPerSec)}/s"
    }
}
