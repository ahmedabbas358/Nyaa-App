package com.aniflow.core.common.sanitization

import java.text.Normalizer

/**
 * Sanitizes external filenames to prevent path traversal, reserved names, and filesystem crashes.
 * Enforces Section 121 (Security) and Section 122 (Filename Sanitization).
 */
object FilenameSanitizer {

    private val ILLEGAL_CHARS = Regex("[\\\\/:*?\"<>|\\x00-\\x1F]")
    private val RESERVED_NAMES = setOf(
        "CON", "PRN", "AUX", "NUL",
        "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
        "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
    )
    private const val MAX_FILENAME_LENGTH = 240

    fun sanitize(name: String, fallback: String = "unnamed_file"): String {
        if (name.isBlank()) return fallback

        // Normalize Unicode
        val normalized = Normalizer.normalize(name.trim(), Normalizer.Form.NFC)

        // Strip illegal filesystem characters
        var cleaned = ILLEGAL_CHARS.replace(normalized, "_")

        // Strip leading/trailing dots and spaces
        cleaned = cleaned.trim('.', ' ')

        // Check for Windows reserved names
        val baseName = cleaned.substringBeforeLast('.').uppercase()
        if (RESERVED_NAMES.contains(baseName)) {
            cleaned = "_$cleaned"
        }

        // Enforce length limit while preserving extension
        if (cleaned.length > MAX_FILENAME_LENGTH) {
            val extension = cleaned.substringAfterLast('.', "")
            val dot = if (extension.isNotEmpty()) "." else ""
            val nameWithoutExt = cleaned.substringBeforeLast('.')
            val maxBaseLen = MAX_FILENAME_LENGTH - extension.length - dot.length
            cleaned = nameWithoutExt.take(maxBaseLen.coerceAtLeast(10)) + dot + extension
        }

        return if (cleaned.isBlank()) fallback else cleaned
    }
}
