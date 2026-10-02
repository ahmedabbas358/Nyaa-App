package com.aniflow.domain.model.aggregate.organization

import com.aniflow.domain.identity.CollectionId
import com.aniflow.domain.identity.CollectionItemId
import java.time.Instant

enum class CollectionItemType {
    Anime,
    Season,
    Episode,
    Release,
    Download,
    LibraryItem
}

/**
 * Polymorphic item association for Collections (Section 39 & 40).
 * Prevents bloating Collection with dozens of disparate foreign keys.
 */
data class CollectionItem(
    val id: CollectionItemId,
    val collectionId: CollectionId,
    val itemType: CollectionItemType,
    val itemId: String,
    val order: Int = 0,
    val addedAt: Instant = Instant.now()
) {
    init {
        require(itemId.isNotBlank()) { "CollectionItem itemId cannot be blank" }
    }
}
