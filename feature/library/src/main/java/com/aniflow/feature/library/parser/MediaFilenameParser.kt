package com.aniflow.feature.library.parser

data class LocalMediaParseResult(
    val animeTitle: String?,
    val seasonNumber: Int = 1,
    val episodeNumber: Double?,
    val episodeTitle: String?,
    val releaseGroup: String?,
    val resolution: String?,
    val codec: String?,
    val isAmbiguous: Boolean
)

/**
 * MediaFilenameParser (Section 50).
 * Common parser for local media filenames to extract anime identity and episode numbers.
 */
class MediaFilenameParser {

    private val seasonEpisodeRegex = Regex("""(?i)(?:s|season\s*)(\d{1,2})\s*(?:e|ep|episode\s*|\s*-\s*)(\d{1,4}(?:\.\d)?)""")
    private val episodeOnlyRegex = Regex("""(?i)(?:^|[\s\-_\[(])(?:ep|episode|\s*-\s*)(\d{1,4}(?:\.\d)?)(?:$|[\s\-_\])])""")
    private val standaloneNumberRegex = Regex("""(?i)(?:^|[\s\-_])(\d{1,4}(?:\.\d)?)(?:$|[\s\-_\[.])""")
    private val groupRegex = Regex("""^\[([^\]]+)\]""")
    private val tagRegex = Regex("""\[([^\]]+)\]""")

    fun parse(fileName: String, parentFolderName: String? = null): LocalMediaParseResult {
        val baseName = fileName.substringBeforeLast('.')
        var cleanName = baseName

        // Extract group
        val groupMatch = groupRegex.find(cleanName)
        val releaseGroup = groupMatch?.groupValues?.get(1)?.trim()
        if (groupMatch != null) {
            cleanName = cleanName.substring(groupMatch.range.last + 1).trim()
        }

        // Extract tags for resolution / codec
        var resolution: String? = null
        var codec: String? = null
        tagRegex.findAll(baseName).forEach { match ->
            val tag = match.groupValues[1].lowercase()
            if (tag.contains("1080p") || tag.contains("720p") || tag.contains("2160p") || tag.contains("4k") || tag.contains("480p")) {
                resolution = tag.uppercase()
            }
            if (tag.contains("hevc") || tag.contains("x265") || tag.contains("h265")) {
                codec = "HEVC"
            } else if (tag.contains("x264") || tag.contains("h264") || tag.contains("avc")) {
                codec = "H.264"
            } else if (tag.contains("av1")) {
                codec = "AV1"
            }
        }

        // Try SxxExx
        val seMatch = seasonEpisodeRegex.find(cleanName)
        if (seMatch != null) {
            val season = seMatch.groupValues[1].toIntOrNull() ?: 1
            val episode = seMatch.groupValues[2].toDoubleOrNull()
            val beforeSe = cleanName.substring(0, seMatch.range.first).trim('-', ' ', '_')
            val anime = if (beforeSe.isNotBlank()) beforeSe else inferAnimeFromFolder(parentFolderName)

            return LocalMediaParseResult(
                animeTitle = anime,
                seasonNumber = season,
                episodeNumber = episode,
                episodeTitle = null,
                releaseGroup = releaseGroup,
                resolution = resolution,
                codec = codec,
                isAmbiguous = anime == null
            )
        }

        // Try Episode only pattern: e.g. "One Piece - 01"
        val epMatch = episodeOnlyRegex.find(cleanName)
        if (epMatch != null) {
            val episode = epMatch.groupValues[1].toDoubleOrNull()
            val anime = cleanName.substring(0, epMatch.range.first).trim('-', ' ', '_').ifBlank {
                inferAnimeFromFolder(parentFolderName)
            }
            val season = inferSeasonFromFolder(parentFolderName) ?: 1

            return LocalMediaParseResult(
                animeTitle = anime,
                seasonNumber = season,
                episodeNumber = episode,
                episodeTitle = null,
                releaseGroup = releaseGroup,
                resolution = resolution,
                codec = codec,
                isAmbiguous = anime == null
            )
        }

        // Standalone number: e.g. "01.mkv"
        val standAlone = standaloneNumberRegex.find(cleanName)
        val episodeNum = standAlone?.groupValues?.get(1)?.toDoubleOrNull()
        val folderAnime = inferAnimeFromFolder(parentFolderName)
        val folderSeason = inferSeasonFromFolder(parentFolderName) ?: 1

        return LocalMediaParseResult(
            animeTitle = folderAnime,
            seasonNumber = folderSeason,
            episodeNumber = episodeNum,
            episodeTitle = null,
            releaseGroup = releaseGroup,
            resolution = resolution,
            codec = codec,
            isAmbiguous = folderAnime == null
        )
    }

    private fun inferAnimeFromFolder(folderName: String?): String? {
        if (folderName == null || folderName.isBlank()) return null
        val lower = folderName.lowercase()
        if (lower.startsWith("season") || lower.startsWith("s0") || lower.startsWith("s1")) {
            return null // Folder is just a season folder, not the anime name
        }
        return folderName.replace('.', ' ').trim()
    }

    private fun inferSeasonFromFolder(folderName: String?): Int? {
        if (folderName == null) return null
        val match = Regex("""(?i)(?:season\s*|s)(\d{1,2})""").find(folderName)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }
}
