package com.aniflow.provider.core.model

/**
 * Declares the capabilities supported by a specific ReleaseProvider (Step 18 Section 3).
 * Enables capability-driven UI where presentation adapts dynamically without
 * hardcoded provider checks.
 */
data class ProviderCapabilities(
    val search: Boolean = true,
    val pagination: Boolean = true,
    val sorting: Set<ProviderSortField> = setOf(
        ProviderSortField.Date,
        ProviderSortField.Seeders,
        ProviderSortField.Leechers,
        ProviderSortField.Downloads,
        ProviderSortField.Size
    ),
    val categoryFiltering: Boolean = true,
    val uploaderSearch: Boolean = true,
    val details: Boolean = true,
    val torrent: Boolean = true,
    val magnet: Boolean = true,
    val rss: Boolean = true,
    val supportsTrustedFilter: Boolean = true,
    val supportsRemakeFilter: Boolean = true
) {
    // Backwards-compatible aliases
    val supportsSearch: Boolean get() = search
    val supportsPagination: Boolean get() = pagination
    val supportsSorting: Boolean get() = sorting.isNotEmpty()
    val supportsCategories: Boolean get() = categoryFiltering
    val supportsUploaderSearch: Boolean get() = uploaderSearch
    val supportsReleaseDetails: Boolean get() = details
    val supportsTorrent: Boolean get() = torrent
    val supportsMagnet: Boolean get() = magnet
    val supportsRss: Boolean get() = rss
    val supportsHtml: Boolean get() = true

    companion object {
        val NYAA = ProviderCapabilities(
            search = true,
            pagination = true,
            sorting = setOf(
                ProviderSortField.Date,
                ProviderSortField.Seeders,
                ProviderSortField.Leechers,
                ProviderSortField.Downloads,
                ProviderSortField.Size,
                ProviderSortField.Comments
            ),
            categoryFiltering = true,
            uploaderSearch = true,
            details = true,
            torrent = true,
            magnet = true,
            rss = true,
            supportsTrustedFilter = true,
            supportsRemakeFilter = true
        )

        val MINIMAL = ProviderCapabilities(
            search = true,
            pagination = false,
            sorting = emptySet(),
            categoryFiltering = false,
            uploaderSearch = false,
            details = false,
            torrent = false,
            magnet = true,
            rss = false,
            supportsTrustedFilter = false,
            supportsRemakeFilter = false
        )
    }
}
