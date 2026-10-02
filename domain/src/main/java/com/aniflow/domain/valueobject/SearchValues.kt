package com.aniflow.domain.valueobject

import java.time.Instant

/**
 * Sort options for domain search queries (Section 118).
 */
enum class SortOption {
    PublishedDate,
    Seeders,
    Leechers,
    Downloads,
    Size,
    Title
}

enum class SortDirection {
    Ascending,
    Descending
}

data class SearchSorting(
    val option: SortOption = SortOption.PublishedDate,
    val direction: SortDirection = SortDirection.Descending
)

/**
 * Strongly typed search filters (Section 43).
 * Rejects generic Map<String, String> strings in favor of explicit domain parameters.
 */
data class SearchFilters(
    val uploader: String? = null,
    val releaseGroup: String? = null,
    val resolution: Resolution? = null,
    val codec: VideoCodec? = null,
    val audioLanguage: LanguageCode? = null,
    val subtitleLanguage: LanguageCode? = null,
    val minSize: ByteSize? = null,
    val maxSize: ByteSize? = null,
    val minSeeders: Int? = null,
    val publishedAfter: Instant? = null,
    val category: String? = null,
    val trustedOnly: Boolean = false,
    val excludeRemakes: Boolean = false
) {
    init {
        if (minSize != null && maxSize != null) {
            require(minSize <= maxSize) {
                "minSize ($minSize) cannot exceed maxSize ($maxSize)"
            }
        }
        if (minSeeders != null) {
            require(minSeeders >= 0) { "minSeeders cannot be negative: $minSeeders" }
        }
    }
}

/**
 * Pagination request encapsulation (Section 119).
 */
data class PaginationRequest(
    val page: Int = 1,
    val pageSize: Int = 50
) {
    init {
        require(page >= 1) { "Page must be >= 1, but was: $page" }
        require(pageSize in 1..200) { "PageSize must be between 1 and 200, but was: $pageSize" }
    }
}

/**
 * Generic domain page result container (Section 119).
 */
data class PageResult<T>(
    val items: List<T>,
    val page: Int,
    val totalItems: Long? = null,
    val hasNextPage: Boolean = false
) {
    val isEmpty: Boolean get() = items.isEmpty()
}

/**
 * Fully typed search query specification (Section 42).
 */
data class SearchQuery(
    val text: String,
    val exact: Boolean = false,
    val filters: SearchFilters = SearchFilters(),
    val sorting: SearchSorting = SearchSorting(),
    val pagination: PaginationRequest = PaginationRequest()
) {
    companion object {
        fun of(text: String): SearchQuery = SearchQuery(text = text)
    }
}
