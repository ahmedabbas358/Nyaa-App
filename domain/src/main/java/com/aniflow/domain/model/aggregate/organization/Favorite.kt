package com.aniflow.domain.model.aggregate.organization

import java.time.Instant

enum class FavoriteTarget {
    Anime,
    Uploader,
    ReleaseGroup,
    Release
}

/**
 * Domain entity representing a user favorite (Section 135).
 * Decoupled from core entities to prevent bloat.
 */
data class Favorite(
    val id: String,
    val targetType: FavoriteTarget,
    val targetId: String,
    val createdAt: Instant = Instant.now()
) {
    init {
        require(id.isNotBlank()) { "Favorite id cannot be blank" }
        require(targetId.isNotBlank()) { "Favorite targetId cannot be blank" }
    }
}

enum class FollowTarget {
    Anime,
    Uploader,
    ReleaseGroup
}

/**
 * Domain entity representing an alert or feed subscription for new releases (Section 136).
 */
data class Follow(
    val id: String,
    val targetType: FollowTarget,
    val targetId: String,
    val notifyOnNewRelease: Boolean = true,
    val createdAt: Instant = Instant.now()
) {
    init {
        require(id.isNotBlank()) { "Follow id cannot be blank" }
        require(targetId.isNotBlank()) { "Follow targetId cannot be blank" }
    }
}
