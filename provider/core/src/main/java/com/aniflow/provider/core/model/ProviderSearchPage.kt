package com.aniflow.provider.core.model

/**
 * Underlying transport used to fetch search results (Section 13).
 * Used for telemetry, diagnostics, and caching strategies.
 */
enum class SearchTransport {
    Html,
    Rss,
    Api,
    Unknown
}

/**
 * Page response containing normalized results from a ReleaseProvider (Section 12).
 */
data class ProviderSearchPage(
    val items: List<ProviderRelease>,
    val page: Int,
    val hasNextPage: Boolean,
    val source: SearchTransport = SearchTransport.Html,
    val totalItemsEstimate: Long? = null
) {
    val isEmpty: Boolean get() = items.isEmpty()
}
