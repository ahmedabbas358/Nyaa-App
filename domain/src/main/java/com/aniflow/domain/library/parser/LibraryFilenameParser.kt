package com.aniflow.domain.library.parser

data class LibraryParseResult(
    val animeTitleCandidate: String?,
    val seasonNumber: Int?,
    val episodeNumber: Double?,
    val episodeTitle: String?,
    val resolution: String?,
    val codec: String?,
    val releaseGroup: String?,
    val isAmbiguous: Boolean
)

/**
 * LibraryFilenameParser (Section 29, 30, 31, 37).
 * Parses local media filenames and folder structures using release intelligence principles.
 * Respects folder hierarchy (e.g., "One Piece/Season 02/03.mkv").
 * Flags ambiguous files (e.g., "Episode 03.mkv" without anime title) as isAmbiguous = true.
 */
class LibraryFilenameParser {

    private val seasonFolderRegex = Regex("""(?i)(?:Season|Series|S)\s*(\d+)""")
    private val sxxExxRegex = Regex("""(?i)S(\d+)\s*E(\d+(?:\.\d+)?)""")
    private val standaloneEpisodeRegex = Regex("""(?i)(?:^|[\s_.\-\[])(?:EP|E|Episode)\s*(\d+(?:\.\d+)?)""")
    private val bracketGroupRegex = Regex("""^\[([^\]]+)\]""")
    private val resolutionRegex = Regex("""(?i)(2160p|1080p|720p|480p|4k|uhd|fhd|hd)""")
    private val codecRegex = Regex("""(?i)(av1|hevc|x265|h\.265|h265|x264|h\.264|h264|avc|vp9)""")

    fun parse(
        fileName: String,
        parentFolder: String? = null,
        grandparentFolder: String? = null
    ): LibraryParseResult {
        val cleanFileName = fileName.substringBeforeLast('.')
        var season: Int? = null
        var episode: Double? = null
        var animeCandidate: String? = null
        var releaseGroup: String? = null

        // 1. Check bracketed release group at start (e.g. "[SubsPlease] ...")
        bracketGroupRegex.find(cleanFileName)?.let { match ->
            releaseGroup = match.groupValues[1].trim()
        }

        // 2. Extract Season & Episode from filename
        val sxxExxMatch = sxxExxRegex.find(cleanFileName)
        if (sxxExxMatch != null) {
            season = sxxExxMatch.groupValues[1].toIntOrNull()
            episode = sxxExxMatch.groupValues[2].toDoubleOrNull()
        } else {
            val epMatch = standaloneEpisodeRegex.find(cleanFileName)
            if (epMatch != null) {
                episode = epMatch.groupValues[1].toDoubleOrNull()
            } else {
                // Heuristic: Check for isolated number following " - " e.g. "Bleach - 03"
                val dashMatch = Regex("""\s-\s(\d+(?:\.\d+)?)(?:\s|\[|$)""").find(cleanFileName)
                if (dashMatch != null) {
                    episode = dashMatch.groupValues[1].toDoubleOrNull()
                }
            }
        }

        // 3. Extract Season from parentFolder if not in filename (e.g. "Season 02/")
        if (season == null && parentFolder != null) {
            val seasonMatch = seasonFolderRegex.find(parentFolder)
            if (seasonMatch != null) {
                season = seasonMatch.groupValues[1].toIntOrNull()
            }
        }

        // 4. Extract Anime candidate from folders and filename
        if (parentFolder != null && !seasonFolderRegex.matches(parentFolder.trim())) {
            // Parent folder is likely the Anime folder (e.g. "One Piece/03.mkv")
            animeCandidate = sanitizeTitle(parentFolder)
        } else if (grandparentFolder != null) {
            // Grandparent is likely Anime (e.g. "One Piece/Season 02/03.mkv")
            animeCandidate = sanitizeTitle(grandparentFolder)
        }

        if (animeCandidate == null) {
            // Try extracting anime title from filename
            val stripped = cleanFileName
                .replace(bracketGroupRegex, "")
                .replace(sxxExxRegex, "")
                .replace(standaloneEpisodeRegex, "")
                .replace(resolutionRegex, "")
                .replace(codecRegex, "")
                .trim(' ', '-', '_', '[', ']')

            val candidateBeforeDash = stripped.substringBefore(" - ").trim()
            if (candidateBeforeDash.isNotBlank() && !candidateBeforeDash.equals("Episode", ignoreCase = true)) {
                animeCandidate = sanitizeTitle(candidateBeforeDash)
            }
        }

        val resolution = resolutionRegex.find(cleanFileName)?.value?.uppercase()
        val codec = codecRegex.find(cleanFileName)?.value?.uppercase()

        // Section 37: Ambiguity detection - If animeCandidate is missing, empty, or generic "Episode"
        val isAmbiguous = animeCandidate.isNullOrBlank() ||
                animeCandidate.equals("Episode", ignoreCase = true) ||
                animeCandidate.matches(Regex("""(?i)Episode\s*\d+"""))

        return LibraryParseResult(
            animeTitleCandidate = if (isAmbiguous) null else animeCandidate,
            seasonNumber = season ?: 1, // Default season 1
            episodeNumber = episode,
            episodeTitle = null,
            resolution = resolution,
            codec = codec,
            releaseGroup = releaseGroup,
            isAmbiguous = isAmbiguous
        )
    }

    private fun sanitizeTitle(raw: String): String {
        return raw.replace('_', ' ')
            .replace(Regex("""\[[^\]]*\]"""), "")
            .replace(Regex("""\([^)]*\)"""), "")
            .trim()
    }
}
