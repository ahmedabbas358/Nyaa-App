package com.aniflow.domain.model.aggregate.media

import com.aniflow.domain.identity.AnimeId
import com.aniflow.domain.identity.SeasonId
import com.aniflow.domain.valueobject.SeasonNumber

/**
 * Domain entity representing a Season belonging to an Anime work (Section 9 & 10).
 */
data class Season(
    val id: SeasonId,
    val animeId: AnimeId,
    val number: SeasonNumber,
    val title: String? = null
)
