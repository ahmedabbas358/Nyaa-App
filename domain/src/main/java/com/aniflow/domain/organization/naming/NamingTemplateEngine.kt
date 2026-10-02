package com.aniflow.domain.organization.naming

import com.aniflow.domain.organization.model.NamingContext
import com.aniflow.domain.organization.model.NamingTemplate

data class TemplateValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val previewOutput: String? = null
)

/**
 * NamingTemplateEngine (Section 55, 56, 57, 58, 59, 60, 146).
 * Handles token replacement, fallback on missing values (preventing "null"),
 * and strict path sanitization & traversal protection against `../`.
 */
class NamingTemplateEngine {

    private val supportedTokens = setOf(
        "anime", "season", "seasonNumber", "episode", "absoluteEpisode",
        "episodeTitle", "group", "uploader", "resolution", "codec",
        "audio", "subtitles", "source", "year", "filename", "ext"
    )

    private val invalidPathChars = Regex("""[:*?"<>|]""")

    fun validateTemplate(template: NamingTemplate, sampleContext: NamingContext): TemplateValidationResult {
        val errors = mutableListOf<String>()
        val tokenRegex = Regex("""\{([a-zA-Z0-9]+)\}""")
        val tokens = tokenRegex.findAll(template.rawTemplate).map { it.groupValues[1] }.toList()

        for (token in tokens) {
            if (!supportedTokens.contains(token)) {
                errors.add("Unknown variable in template: '{$token}'")
            }
        }

        if (!tokens.contains("ext")) {
            errors.add("Template must end with '.{ext}' to preserve media file extension")
        }

        return if (errors.isEmpty()) {
            val preview = render(template, sampleContext)
            TemplateValidationResult(isValid = true, previewOutput = preview)
        } else {
            TemplateValidationResult(isValid = false, errors = errors)
        }
    }

    /**
     * Renders a naming template with safe fallbacks and path sanitization (Section 59, 60).
     */
    fun render(template: NamingTemplate, context: NamingContext): String {
        var result = template.rawTemplate

        // Format seasonNumber with leading zero if needed (e.g. 02)
        val formattedSeason = context.seasonNumber?.let { String.format("%02d", it) } ?: "01"
        val formattedEpisode = context.episode ?: context.absoluteEpisode?.toString() ?: "01"

        val replacements = mapOf(
            "anime" to sanitizeSegment(context.anime ?: "Unknown Anime"),
            "season" to sanitizeSegment(context.season ?: "Season $formattedSeason"),
            "seasonNumber" to formattedSeason,
            "episode" to formattedEpisode,
            "absoluteEpisode" to (context.absoluteEpisode?.toString() ?: formattedEpisode),
            "episodeTitle" to (context.episodeTitle?.let { sanitizeSegment(it) } ?: ""),
            "group" to (context.group?.let { sanitizeSegment(it) } ?: ""),
            "uploader" to (context.uploader?.let { sanitizeSegment(it) } ?: ""),
            "resolution" to (context.resolution ?: ""),
            "codec" to (context.codec ?: ""),
            "audio" to (context.audio ?: ""),
            "subtitles" to (context.subtitles ?: ""),
            "source" to (context.source ?: ""),
            "year" to (context.year?.toString() ?: ""),
            "filename" to sanitizeSegment(context.originalFilename.substringBeforeLast('.')),
            "ext" to context.extension.removePrefix(".")
        )

        for ((token, value) in replacements) {
            result = result.replace("{$token}", value)
        }

        // Section 59: Clean up empty brackets or leftover separators from missing values
        // e.g. "[]" or "[ ]" or " - .mkv"
        result = result
            .replace(Regex("""\[\s*\]"""), "")
            .replace(Regex("""\(\s*\)"""), "")
            .replace(Regex("""\s+-\s+\."""), ".")
            .replace(Regex("""\s+\."""), ".")
            .replace(Regex("""\s{2,}"""), " ")
            .trim()

        return sanitizePath(result)
    }

    /**
     * Section 60, 146: Path Sanitization and Traversal Protection.
     * Enforces strictly relative paths inside StorageRoot and rejects path traversal.
     */
    fun sanitizePath(path: String): String {
        // 1. Normalize separators
        var normalized = path.replace('\\', '/').trim()

        // 2. Reject and strip path traversal ../ and ./
        while (normalized.contains("../") || normalized.contains("..\\")) {
            normalized = normalized.replace("../", "").replace("..\\", "")
        }

        // 3. Strip leading slashes to prevent absolute root escape
        normalized = normalized.trimStart('/')

        // 4. Sanitize invalid filesystem characters per segment
        val segments = normalized.split('/')
            .map { segment -> sanitizeSegment(segment) }
            .filter { it.isNotBlank() }

        val cleanPath = segments.joinToString("/")
        return if (cleanPath.length > 240) {
            // Truncate path safely while keeping extension
            val ext = cleanPath.substringAfterLast('.', "")
            val base = cleanPath.substringBeforeLast('.')
            "${base.take(230)}.$ext"
        } else {
            cleanPath
        }
    }

    private fun sanitizeSegment(segment: String): String {
        return segment
            .replace(invalidPathChars, "")
            .replace(Regex("""[.\s]+$"""), "") // No trailing dots or spaces
            .trim()
    }
}
