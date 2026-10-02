package com.aniflow.domain.library.matching

import com.aniflow.domain.identity.AnimeId
import com.aniflow.domain.identity.EpisodeId
import com.aniflow.domain.identity.SeasonId
import java.time.Instant

data class EntityMatch<T>(
    val id: T,
    val title: String,
    val confidence: Float
)

enum class MatchConfidence {
    Definite,   // From active DownloadTask or explicit User mapping
    High,       // Strict folder structure + clean filename tokens
    Medium,     // Filename parser alone without folder context
    Low,        // Incomplete / fuzzy match
    Unmatched
}

enum class MatchState {
    Matched,
    PartiallyMatched,
    Ambiguous,      // e.g. "Episode 03.mkv" without anime title -> Placed in Unidentified! (Section 37)
    Unmatched,
    UserMapped,
    Rejected
}

enum class MatchEvidenceSource {
    DownloadTaskMapping,  // Priority 1 (Section 33, 34)
    UserMapping,          // Priority 2
    ReleaseMapping,       // Priority 3
    FolderStructure,      // Priority 4
    FilenameParsing,      // Priority 5
    EmbeddedMetadata,     // Priority 6
    Unknown
}

data class MatchEvidence(
    val source: MatchEvidenceSource,
    val detail: String,
    val confidenceScore: Float
)

/**
 * Result of the identity resolution pipeline (Section 35, 36).
 */
data class LibraryMatchResult(
    val anime: EntityMatch<AnimeId>?,
    val season: EntityMatch<SeasonId>?,
    val episode: EntityMatch<EpisodeId>?,
    val confidence: MatchConfidence,
    val evidence: List<MatchEvidence> = emptyList(),
    val state: MatchState
)

/**
 * User-defined explicit mapping for an anime folder, series or single file pattern (Section 39, 40).
 */
data class UserLibraryMapping(
    val patternOrFolder: String,
    val animeId: AnimeId,
    val animeTitle: String,
    val seasonId: SeasonId? = null,
    val episodeId: EpisodeId? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Double? = null,
    val createdAt: Instant = Instant.now()
)
