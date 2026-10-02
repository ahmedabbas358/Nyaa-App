package com.aniflow.domain.model.aggregate.organization

import com.aniflow.domain.identity.CollectionId
import java.time.Instant

enum class CollectionType {
    Static,
    Dynamic,
    Smart
}

enum class CollectionSource {
    Manual,
    Anime,
    Season,
    Search,
    Rule,
    Uploader,
    ReleaseGroup,
    Custom
}

/**
 * Domain entity representing a user collection (Section 36, 37, 38).
 * Does not embed child rows or releases directly, delegating association to CollectionItem.
 */
data class Collection(
    val id: CollectionId,
    val name: String,
    val type: CollectionType = CollectionType.Static,
    val source: CollectionSource = CollectionSource.Manual,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    init {
        require(name.isNotBlank()) { "Collection name cannot be blank" }
    }
}
