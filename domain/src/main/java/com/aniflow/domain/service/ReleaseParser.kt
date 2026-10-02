package com.aniflow.domain.service

import com.aniflow.domain.model.aggregate.release.ReleaseType
import com.aniflow.domain.valueobject.EpisodeRange
import com.aniflow.domain.valueobject.ParseInfo
import com.aniflow.domain.valueobject.ReleaseTechnicalMetadata
import com.aniflow.domain.valueobject.SeasonNumber

/**
 * Result produced by the Release Parser (Section 95).
 */
data class ParsedReleaseInfo(
    val animeTitle: String,
    val normalizedTitle: String,
    val seasonHint: SeasonNumber?,
    val episodeRange: EpisodeRange?,
    val releaseGroup: String?,
    val technical: ReleaseTechnicalMetadata,
    val releaseType: ReleaseType,
    val parseInfo: ParseInfo
)

/**
 * Domain interface for title parsing (Section 95).
 */
interface ReleaseParser {
    fun parse(rawTitle: String): ParsedReleaseInfo
}
