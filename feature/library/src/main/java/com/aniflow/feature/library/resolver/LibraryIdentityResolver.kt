package com.aniflow.feature.library.resolver

import com.aniflow.feature.library.parser.LocalMediaParseResult
import com.aniflow.feature.library.parser.MediaFilenameParser
import com.aniflow.feature.library.scanner.ScannedFile

data class ResolvedMediaIdentity(
    val animeTitle: String,
    val seasonNumber: Int,
    val episodeNumber: Double?,
    val episodeTitle: String?,
    val resolution: String?,
    val codec: String?,
    val releaseGroup: String?,
    val matchSource: MatchSource,
    val isUnidentified: Boolean = false
)

enum class MatchSource {
    DownloadTaskMapping,  // Priority 1
    ReleaseMapping,       // Priority 2
    StoredUserMapping,    // Priority 3
    FilenameParser,       // Priority 4
    AnimeIdentityMatch,   // Priority 5
    UnidentifiedFallback  // Fallback (Section 54)
}

/**
 * LibraryIdentityResolver (Sections 51, 52, 53, 54, 55, 56).
 * Enforces strict priority resolution chain. Never assigns ambiguous files (e.g. "Episode 03.mkv")
 * to a guessed anime; assigns them to the "Unidentified" library collection instead.
 */
class LibraryIdentityResolver(
    private val parser: MediaFilenameParser = MediaFilenameParser()
) {

    fun resolve(
        file: ScannedFile,
        taskMapping: ResolvedMediaIdentity? = null,
        releaseMapping: ResolvedMediaIdentity? = null,
        userMappings: Map<String, ResolvedMediaIdentity> = emptyMap(), // Key: Folder or File pattern
        knownAnimeTitles: Set<String> = emptySet()
    ): ResolvedMediaIdentity {
        // Priority 1: Existing DownloadTask mapping
        if (taskMapping != null) {
            return taskMapping.copy(matchSource = MatchSource.DownloadTaskMapping)
        }

        // Priority 2: Existing Release mapping
        if (releaseMapping != null) {
            return releaseMapping.copy(matchSource = MatchSource.ReleaseMapping)
        }

        // Priority 3: Stored Library / User custom mapping
        val userMatched = userMappings[file.parentFolder] ?: userMappings[file.name]
        if (userMatched != null) {
            return userMatched.copy(matchSource = MatchSource.StoredUserMapping)
        }

        // Priority 4: Filename Parser
        val parsed: LocalMediaParseResult = parser.parse(file.name, file.parentFolder)

        if (parsed.animeTitle != null && !parsed.isAmbiguous) {
            // Priority 5: Known Anime title canonicalization
            val canonicalTitle = knownAnimeTitles.firstOrNull { it.equals(parsed.animeTitle, ignoreCase = true) }
                ?: parsed.animeTitle

            return ResolvedMediaIdentity(
                animeTitle = canonicalTitle,
                seasonNumber = parsed.seasonNumber,
                episodeNumber = parsed.episodeNumber,
                episodeTitle = parsed.episodeTitle,
                resolution = parsed.resolution,
                codec = parsed.codec,
                releaseGroup = parsed.releaseGroup,
                matchSource = if (canonicalTitle != parsed.animeTitle) MatchSource.AnimeIdentityMatch else MatchSource.FilenameParser
            )
        }

        // Section 53 & 54: Ambiguous / Unknown file placed in "Unidentified" collection
        return ResolvedMediaIdentity(
            animeTitle = "Unidentified",
            seasonNumber = 1,
            episodeNumber = parsed.episodeNumber,
            episodeTitle = file.name,
            resolution = parsed.resolution,
            codec = parsed.codec,
            releaseGroup = parsed.releaseGroup,
            matchSource = MatchSource.UnidentifiedFallback,
            isUnidentified = true
        )
    }
}
