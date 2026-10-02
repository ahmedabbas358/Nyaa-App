package com.aniflow.provider.core.model

import com.aniflow.domain.valueobject.SortDirection

/**
 * Standard sorting fields supported across providers (Section 11).
 */
enum class ProviderSortField {
    Date,
    Id,
    Comments,
    Size,
    Seeders,
    Leechers,
    Downloads
}

/**
 * Provider-agnostic sort specification.
 */
data class ProviderSort(
    val field: ProviderSortField = ProviderSortField.Date,
    val direction: SortDirection = SortDirection.Descending
) {
    companion object {
        val DEFAULT = ProviderSort(ProviderSortField.Date, SortDirection.Descending)
    }
}

/**
 * High-level provider search filters (Section 10).
 */
enum class ProviderSearchFilters {
    NoFilter,
    TrustedOnly,
    NoRemakes
}

/**
 * Encapsulates search request parameters sent to ReleaseProvider (Section 9).
 */
data class ProviderSearchRequest(
    val query: String = "",
    val page: Int = 1,
    val pageSize: Int? = null,
    val category: ProviderCategory? = null,
    val filters: ProviderSearchFilters = ProviderSearchFilters.NoFilter,
    val sort: ProviderSort = ProviderSort.DEFAULT
) {
    init {
        require(page >= 1) { "Page must be >= 1, but was: $page" }
        if (pageSize != null) {
            require(pageSize in 1..200) { "PageSize must be in 1..200, but was: $pageSize" }
        }
    }
}
