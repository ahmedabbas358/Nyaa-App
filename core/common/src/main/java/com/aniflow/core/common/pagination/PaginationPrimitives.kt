package com.aniflow.core.common.pagination

/**
 * Universal pagination primitive for paginated lists across Provider, Repository, and UI.
 */
data class Page<T>(
    val items: List<T>,
    val pageNumber: Int,
    val hasNextPage: Boolean,
    val totalItems: Long? = null
) {
    val isEmpty: Boolean get() = items.isEmpty()
    val isNotEmpty: Boolean get() = items.isNotEmpty()
}

/**
 * UI / Coordination state tracking for incremental page loading.
 */
sealed interface PaginationState {
    data object Idle : PaginationState
    data object LoadingNextPage : PaginationState
    data object Refreshing : PaginationState
    data class Error(val message: String) : PaginationState
    data object Exhausted : PaginationState
}
