package com.aniflow.domain.model.aggregate.organization

import com.aniflow.domain.identity.SavedSearchId
import com.aniflow.domain.valueobject.SearchQuery
import java.time.Instant

/**
 * Persisted search specification and query filters (Section 41).
 */
data class SavedSearch(
    val id: SavedSearchId,
    val name: String,
    val query: SearchQuery,
    val isPinned: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    init {
        require(name.isNotBlank()) { "SavedSearch name cannot be blank" }
    }
}
