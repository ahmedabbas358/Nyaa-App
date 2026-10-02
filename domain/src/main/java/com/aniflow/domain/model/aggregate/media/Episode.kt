package com.aniflow.domain.model.aggregate.media

import com.aniflow.domain.identity.AnimeId
import com.aniflow.domain.identity.EpisodeId
import com.aniflow.domain.identity.SeasonId
import com.aniflow.domain.valueobject.EpisodeNumber

enum class EpisodeType {
    Main,
    Special,
    OVA,
    ONA,
    Movie,
    Recap,
    Extra,
    Unknown
}

/**
 * Domain entity representing an individual Episode within an Anime / Season (Section 11, 12, 13).
 */
data class Episode(
    val id: EpisodeId,
    val animeId: AnimeId,
    val seasonId: SeasonId?,
    val number: EpisodeNumber,
    val title: String?,
    val type: EpisodeType = EpisodeType.Main
)
