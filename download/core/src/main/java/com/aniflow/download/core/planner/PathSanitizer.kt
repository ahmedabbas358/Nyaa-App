package com.aniflow.download.core.planner

/**
 * Sanitizes filenames and directory paths according to Section 24.
 * Strictly prevents path traversal (..), null bytes, reserved device names, and invalid characters.
 */
class PathSanitizer {

    private val illegalChars = Regex("[\\\\/:*?\"<>|\\x00-\\x1F]")
    private val reservedNames = setOf(
        "CON", "PRN", "AUX", "NUL",
        "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
        "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
    )

    fun sanitizeFilename(raw: String, defaultExtension: String = ".mkv"): String {
        var clean = raw.trim()
            .replace(illegalChars, "_")
            .replace(Regex("\\.{2,}"), ".") // Prevent ".."
            .trimStart('.', ' ')
            .trimEnd('.', ' ')

        val baseNameUpper = clean.substringBeforeLast('.').uppercase()
        if (reservedNames.contains(baseNameUpper)) {
            clean = "_$clean"
        }

        if (clean.isBlank()) {
            clean = "download_${System.currentTimeMillis()}$defaultExtension"
        }

        // Limit to 255 bytes/chars
        if (clean.length > 240) {
            val ext = if (clean.contains('.')) ".${clean.substringAfterLast('.')}" else defaultExtension
            clean = clean.take(240 - ext.length) + ext
        }

        return clean
    }

    fun sanitizeDirectoryPath(raw: String): String {
        return raw.split('/', '\\')
            .filter { it.isNotBlank() && it != ".." && it != "." }
            .joinToString("/") { sanitizeFilename(it, "") }
    }
}
