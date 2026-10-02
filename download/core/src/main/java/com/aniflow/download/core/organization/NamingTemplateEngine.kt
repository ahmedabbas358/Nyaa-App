package com.aniflow.download.core.organization

import com.aniflow.core.common.sanitization.FilenameSanitizer

/**
 * Metadata context passed to Naming and Directory template engines (Section 29, 30, 31, 32).
 */
data class MediaNamingContext(
    val anime: String,
    val season: Int? = 1,
    val episode: Double? = null,
    val episodeTitle: String? = null,
    val group: String? = null,
    val uploader: String? = null,
    val resolution: String? = null,
    val codec: String? = null,
    val audio: String? = null,
    val subtitles: String? = null,
    val source: String? = null,
    val originalFilename: String? = null,
    val year: Int? = null,
    val extension: String = "mkv"
)

/**
 * NamingTemplateEngine (Section 29, 31, 32).
 * Formats filenames using customizable patterns like:
 * "S{season}E{episode} - {title} [{resolution}][{codec}]"
 * or "{anime} - {episode} [{group}]"
 */
class NamingTemplateEngine(
    val defaultFileTemplate: String = "S{season}E{episode} - {episodeTitle} [{resolution}]",
    val defaultDirectoryTemplate: String = "{anime}/Season {season}"
) {

    fun formatFileName(
        context: MediaNamingContext,
        template: String = defaultFileTemplate
    ): String {
        var result = template

        // {anime}
        result = replaceToken(result, "anime", context.anime)

        // {season}
        val seasonStr = context.season?.let { String.format("%02d", it) } ?: "01"
        result = replaceToken(result, "season", seasonStr)

        // {episode}
        val epStr = context.episode?.let { ep ->
            if (ep % 1.0 == 0.0) String.format("%02d", ep.toInt()) else ep.toString()
        } ?: "01"
        result = replaceToken(result, "episode", epStr)

        // {episodeTitle}
        result = replaceToken(result, "episodeTitle", context.episodeTitle ?: "Episode $epStr")

        // {group}
        result = replaceToken(result, "group", context.group ?: "")

        // {uploader}
        result = replaceToken(result, "uploader", context.uploader ?: "")

        // {resolution}
        result = replaceToken(result, "resolution", context.resolution ?: "")

        // {codec}
        result = replaceToken(result, "codec", context.codec ?: "")

        // {audio}
        result = replaceToken(result, "audio", context.audio ?: "")

        // {subtitles}
        result = replaceToken(result, "subtitles", context.subtitles ?: "")

        // {source}
        result = replaceToken(result, "source", context.source ?: "")

        // {filename}
        val rawBaseName = context.originalFilename?.substringBeforeLast('.') ?: ""
        result = replaceToken(result, "filename", rawBaseName)

        // {year}
        result = replaceToken(result, "year", context.year?.toString() ?: "")

        // Cleanup empty brackets/tags: e.g. "[]", "()", double spaces
        result = result
            .replace("\\[\\s*\\]".toRegex(), "")
            .replace("\\(\\s*\\)".toRegex(), "")
            .replace("\\s{2,}".toRegex(), " ")
            .trim('-', ' ', '_')

        if (result.isBlank()) {
            result = "${context.anime} - S${seasonStr}E${epStr}"
        }

        val ext = if (context.extension.startsWith(".")) context.extension else ".${context.extension}"
        val sanitized = FilenameSanitizer.sanitize("$result$ext")
        return sanitized
    }

    fun formatDirectoryPath(
        context: MediaNamingContext,
        template: String = defaultDirectoryTemplate
    ): String {
        var result = template

        // Normalize anime directory name (e.g. "One.Piece" to "One Piece" consistency)
        val normalizedAnime = context.anime.replace('.', ' ').trim()
        result = replaceToken(result, "anime", normalizedAnime)

        val seasonStr = context.season?.let { String.format("%02d", it) } ?: "01"
        result = replaceToken(result, "season", seasonStr)
        result = replaceToken(result, "year", context.year?.toString() ?: "")

        val segments = result.split('/', '\\').filter { it.isNotBlank() }
        val sanitizedSegments = segments.map { FilenameSanitizer.sanitize(it) }
        return sanitizedSegments.joinToString("/")
    }

    private fun replaceToken(template: String, key: String, value: String): String {
        return template.replace("{$key}", value)
    }
}
