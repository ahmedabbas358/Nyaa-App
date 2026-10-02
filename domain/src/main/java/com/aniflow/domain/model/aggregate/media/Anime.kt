package com.aniflow.domain.model.aggregate.media

import com.aniflow.domain.identity.AnimeId
import com.aniflow.domain.model.aggregate.release.ProviderRef

enum class AnimeType {
    Series,
    Movie,
    OVA,
    ONA,
    Special,
    Music,
    Unknown
}

enum class AnimeStatus {
    Unknown,
    Announced,
    Ongoing,
    Completed,
    Cancelled
}

/**
 * Aggregate Root: Media Identity (Section 5, 6, 7).
 * Pure domain representation of an Anime work.
 * Does not embed releases directly (releases are queried via repository).
 */
data class Anime(
    val id: AnimeId,
    val canonicalTitle: String,
    val alternateTitles: List<String> = emptyList(),
    val normalizedTitle: String,
    val type: AnimeType = AnimeType.Series,
    val status: AnimeStatus = AnimeStatus.Unknown,
    val metadataSource: ProviderRef? = null
) {
    init {
        require(canonicalTitle.isNotBlank()) { "Anime canonicalTitle cannot be blank" }
        require(normalizedTitle.isNotBlank()) { "Anime normalizedTitle cannot be blank" }
    }
}
