package com.aniflow.domain.library.matching

import com.aniflow.domain.identity.AnimeId
import com.aniflow.domain.identity.EpisodeId
import com.aniflow.domain.identity.SeasonId
import com.aniflow.domain.library.parser.LibraryFilenameParser
import com.aniflow.domain.library.parser.LibraryParseResult
import com.aniflow.domain.library.scanner.DiscoveredPhysicalFile

data class DownloadTaskIdentityContext(
    val animeId: AnimeId,
    val animeTitle: String,
    val seasonId: SeasonId?,
    val seasonNumber: Int?,
    val episodeId: EpisodeId?,
    val episodeNumber: Double?
)

/**
 * LibraryIdentityResolver (Section 32, 33, 34, 35, 36, 37, 39, 40).
 * Enforces the strict identity priority chain:
 * 1. DownloadTask mapping (Strongest evidence)
 * 2. Stored User mapping
 * 3. Release mapping
 * 4. Folder structure + Filename parser
 * 5. Unknown / Ambiguous fallback -> State.Ambiguous (placed in Unidentified)
 */
class LibraryIdentityResolver(
    private val parser: LibraryFilenameParser = LibraryFilenameParser()
) {

    fun resolve(
        file: DiscoveredPhysicalFile,
        taskMapping: DownloadTaskIdentityContext? = null,
        releaseMapping: DownloadTaskIdentityContext? = null,
        userMappings: List<UserLibraryMapping> = emptyList(),
        knownAnimeTitleToId: Map<String, AnimeId> = emptyMap()
    ): LibraryMatchResult {
        val evidenceList = mutableListOf<MatchEvidence>()

        // 1. Priority 1: DownloadTask Mapping (Section 34)
        if (taskMapping != null) {
            evidenceList.add(
                MatchEvidence(
                    source = MatchEvidenceSource.DownloadTaskMapping,
                    detail = "Explicit mapping from originating download task for anime: ${taskMapping.animeTitle}",
                    confidenceScore = 1.0f
                )
            )
            return LibraryMatchResult(
                anime = EntityMatch(taskMapping.animeId, taskMapping.animeTitle, 1.0f),
                season = taskMapping.seasonId?.let { EntityMatch(it, "Season ${taskMapping.seasonNumber ?: 1}", 1.0f) },
                episode = taskMapping.episodeId?.let { EntityMatch(it, "Episode ${taskMapping.episodeNumber ?: 1}", 1.0f) },
                confidence = MatchConfidence.Definite,
                evidence = evidenceList,
                state = MatchState.Matched
            )
        }

        // 2. Priority 2: Stored User Custom Mapping (Section 39, 40)
        val userMatched = userMappings.firstOrNull { mapping ->
            val pattern = mapping.patternOrFolder.lowercase()
            file.relativePath.lowercase().contains(pattern) ||
                    file.name.lowercase().contains(pattern) ||
                    file.parentFolder?.lowercase() == pattern
        }

        if (userMatched != null) {
            evidenceList.add(
                MatchEvidence(
                    source = MatchEvidenceSource.UserMapping,
                    detail = "Custom user manual override matched on pattern: ${userMatched.patternOrFolder}",
                    confidenceScore = 1.0f
                )
            )
            return LibraryMatchResult(
                anime = EntityMatch(userMatched.animeId, userMatched.animeTitle, 1.0f),
                season = userMatched.seasonId?.let { EntityMatch(it, "Season ${userMatched.seasonNumber ?: 1}", 1.0f) },
                episode = userMatched.episodeId?.let { EntityMatch(it, "Episode ${userMatched.episodeNumber ?: 1}", 1.0f) },
                confidence = MatchConfidence.Definite,
                evidence = evidenceList,
                state = MatchState.UserMapped
            )
        }

        // 3. Priority 3: Release Mapping
        if (releaseMapping != null) {
            evidenceList.add(
                MatchEvidence(
                    source = MatchEvidenceSource.ReleaseMapping,
                    detail = "Associated provider release metadata match: ${releaseMapping.animeTitle}",
                    confidenceScore = 0.95f
                )
            )
            return LibraryMatchResult(
                anime = EntityMatch(releaseMapping.animeId, releaseMapping.animeTitle, 0.95f),
                season = releaseMapping.seasonId?.let { EntityMatch(it, "Season ${releaseMapping.seasonNumber ?: 1}", 0.95f) },
                episode = releaseMapping.episodeId?.let { EntityMatch(it, "Episode ${releaseMapping.episodeNumber ?: 1}", 0.95f) },
                confidence = MatchConfidence.High,
                evidence = evidenceList,
                state = MatchState.Matched
            )
        }

        // 4. Priority 4 & 5: Folder structure + Filename Parsing
        val parent = file.parentFolder
        val grandparent = file.relativePath.trimStart('/').split('/').let { parts ->
            if (parts.size >= 3) parts[parts.size - 3] else null
        }
        val parseResult: LibraryParseResult = parser.parse(
            fileName = file.name,
            parentFolder = parent,
            grandparentFolder = grandparent
        )

        // Section 37: Ambiguity Rule - NEVER guess anime from file alone if ambiguous
        if (parseResult.isAmbiguous || parseResult.animeTitleCandidate.isNullOrBlank()) {
            evidenceList.add(
                MatchEvidence(
                    source = MatchEvidenceSource.FilenameParsing,
                    detail = "Ambiguous filename/folder with no confident anime identity detected (${file.name})",
                    confidenceScore = 0.0f
                )
            )
            return LibraryMatchResult(
                anime = null,
                season = null,
                episode = null,
                confidence = MatchConfidence.Unmatched,
                evidence = evidenceList,
                state = MatchState.Ambiguous // Placed in Unidentified!
            )
        }

        val animeTitle = parseResult.animeTitleCandidate
        val matchedAnimeId = knownAnimeTitleToId.entries.firstOrNull { (knownTitle, _) ->
            knownTitle.equals(animeTitle, ignoreCase = true) ||
                    animeTitle.contains(knownTitle, ignoreCase = true)
        }?.value ?: AnimeId(generateSyntheticId(animeTitle))

        evidenceList.add(
            MatchEvidence(
                source = MatchEvidenceSource.FilenameParsing,
                detail = "Parsed anime '$animeTitle', Season ${parseResult.seasonNumber}, Episode ${parseResult.episodeNumber}",
                confidenceScore = 0.85f
            )
        )

        val seasonId = SeasonId("${matchedAnimeId.value}_s${parseResult.seasonNumber ?: 1}")
        val episodeId = parseResult.episodeNumber?.let { EpisodeId("${matchedAnimeId.value}_s${parseResult.seasonNumber ?: 1}_ep$it") }

        return LibraryMatchResult(
            anime = EntityMatch(matchedAnimeId, animeTitle, 0.85f),
            season = EntityMatch(seasonId, "Season ${parseResult.seasonNumber ?: 1}", 0.85f),
            episode = episodeId?.let { EntityMatch(it, "Episode ${parseResult.episodeNumber}", 0.85f) },
            confidence = MatchConfidence.Medium,
            evidence = evidenceList,
            state = MatchState.Matched
        )
    }

    private fun generateSyntheticId(title: String): String =
        title.lowercase().replace(Regex("""[^a-z0-9]"""), "_").trim('_')
}
