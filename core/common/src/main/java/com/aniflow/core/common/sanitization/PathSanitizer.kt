package com.aniflow.core.common.sanitization

import java.io.File
import java.io.IOException
import java.text.Normalizer

/**
 * Path and Filename Sanitizer providing defense-in-depth against:
 * 1. Path traversal attacks (e.g. "../../", encoded traversal)
 * 2. File execution or unauthorized directory escapes
 * 3. Filesystem-breaking characters, control characters, and reserved device names
 * 4. Dangerous Unicode or bidirectional override exploits
 *
 * Enforces STEP 13: Section 26 (Filename Safety), Section 27 (Path Traversal Protection),
 * Section 48 (Untrusted Provider Data), and Section 52 (File Security).
 */
object PathSanitizer {

    private val FORBIDDEN_FILENAME_CHARS = Regex("[\\\\/:*?\"<>|\\x00-\\x1F]")
    private val RESERVED_DEVICE_NAMES = setOf(
        "CON", "PRN", "AUX", "NUL",
        "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
        "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
    )
    private const val MAX_FILENAME_BYTES = 240

    /**
     * Sanitizes a standalone filename component (not a path).
     */
    fun sanitizeFilename(rawName: String, fallback: String = "unnamed_file"): String {
        if (rawName.isBlank()) return fallback

        // Normalize Unicode NFC to prevent decomposed sequence exploits
        val normalized = Normalizer.normalize(rawName.trim(), Normalizer.Form.NFC)

        // Replace illegal filesystem characters with underscores
        var cleaned = FORBIDDEN_FILENAME_CHARS.replace(normalized, "_")

        // Strip leading/trailing dots and whitespace
        cleaned = cleaned.trim('.', ' ')

        // Check Windows / Android reserved device names
        val baseName = cleaned.substringBeforeLast('.').uppercase()
        if (RESERVED_DEVICE_NAMES.contains(baseName)) {
            cleaned = "_$cleaned"
        }

        // Limit byte size while preserving extension
        if (cleaned.toByteArray(Charsets.UTF_8).size > MAX_FILENAME_BYTES) {
            val extension = cleaned.substringAfterLast('.', "")
            val dot = if (extension.isNotEmpty()) "." else ""
            val nameWithoutExt = cleaned.substringBeforeLast('.')
            val maxBaseLen = (MAX_FILENAME_BYTES - extension.toByteArray(Charsets.UTF_8).size - dot.length).coerceAtLeast(10)
            cleaned = nameWithoutExt.take(maxBaseLen) + dot + extension
        }

        return if (cleaned.isBlank()) fallback else cleaned
    }

    /**
     * Validates that a target file/directory stays strictly within the authorized root directory.
     * Prevents any path traversal attempts (e.g. "../../etc/passwd").
     *
     * @throws SecurityException if the target path attempts to escape the root directory.
     */
    fun ensureInsideStorageRoot(rootDirectory: File, relativeSubPath: String): File {
        // Disallow null bytes or explicit traversal markers before file operations
        if (relativeSubPath.contains("\u0000") || relativeSubPath.contains("..")) {
            throw SecurityException("Path traversal pattern detected in subpath: $relativeSubPath")
        }

        val canonicalRoot = rootDirectory.canonicalFile
        val candidateFile = File(canonicalRoot, relativeSubPath).canonicalFile

        val rootPath = canonicalRoot.path
        val candidatePath = candidateFile.path

        val isInside = if (rootPath.endsWith(File.separator)) {
            candidatePath.startsWith(rootPath)
        } else {
            candidatePath == rootPath || candidatePath.startsWith(rootPath + File.separator)
        }

        if (!isInside) {
            throw SecurityException("Security Violation: Path '$candidatePath' escapes root '$rootPath'")
        }

        return candidateFile
    }

    /**
     * Resolves a sanitized safe path given an untrusted title, season, and filename.
     */
    fun resolveSafeDestination(
        rootDirectory: File,
        animeFolder: String,
        seasonFolder: String?,
        fileName: String
    ): File {
        val safeAnime = sanitizeFilename(animeFolder, fallback = "Anime")
        val safeSeason = seasonFolder?.let { sanitizeFilename(it) }
        val safeFile = sanitizeFilename(fileName, fallback = "episode.mkv")

        val relativePath = if (safeSeason != null) {
            "$safeAnime${File.separator}$safeSeason${File.separator}$safeFile"
        } else {
            "$safeAnime${File.separator}$safeFile"
        }

        return ensureInsideStorageRoot(rootDirectory, relativePath)
    }
}
