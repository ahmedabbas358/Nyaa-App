package com.aniflow.domain.model

import com.aniflow.domain.model.aggregate.release.Release

/**
 * Domain model representing search results across providers and cache.
 */
data class SearchResult(
    val items: List<Release>,
    val page: Int,
    val hasNextPage: Boolean,
    val totalItems: Long? = null,
    val isFromCache: Boolean = false,
    val error: String? = null
) {
    val isEmpty: Boolean get() = items.isEmpty()
    val isNotEmpty: Boolean get() = items.isNotEmpty()
}
