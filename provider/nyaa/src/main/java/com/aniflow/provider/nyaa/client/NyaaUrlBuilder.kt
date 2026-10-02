package com.aniflow.provider.nyaa.client

import com.aniflow.domain.valueobject.SortDirection
import com.aniflow.provider.core.model.ProviderSearchFilters
import com.aniflow.provider.core.model.ProviderSearchRequest
import com.aniflow.provider.core.model.ProviderSortField
import com.aniflow.provider.nyaa.config.NyaaProviderConfig
import com.aniflow.provider.nyaa.model.NyaaCategory
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Builds valid, encoded query URLs for Nyaa.si (Sections 29, 30, 33, 34, 35, 89).
 */
class NyaaUrlBuilder(
    private val config: NyaaProviderConfig = NyaaProviderConfig.DEFAULT
) {

    private val base: String
        get() = config.baseUrl.rawValue.trimEnd('/')

    /**
     * Builds standard HTML search query URL.
     */
    fun buildSearchUrl(request: ProviderSearchRequest, uploader: String? = null): String {
        val queryParams = mutableListOf<String>()

        // 1. Filter: 0 = No filter, 1 = No remakes, 2 = Trusted only
        val filterCode = when (request.filters) {
            ProviderSearchFilters.NoFilter -> "0"
            ProviderSearchFilters.NoRemakes -> "1"
            ProviderSearchFilters.TrustedOnly -> "2"
        }
        queryParams.add("f=$filterCode")

        // 2. Category
        val nyaaCategory = NyaaCategory.fromProviderCategory(request.category)
        queryParams.add("c=${nyaaCategory.code}")

        // 3. Query string
        if (request.query.isNotBlank()) {
            val encodedQuery = URLEncoder.encode(request.query.trim(), StandardCharsets.UTF_8.name())
            queryParams.add("q=$encodedQuery")
        }

        // 4. Sort field & order
        val sortCode = mapSortField(request.sort.field)
        val orderCode = when (request.sort.direction) {
            SortDirection.Ascending -> "asc"
            SortDirection.Descending -> "desc"
        }
        queryParams.add("s=$sortCode")
        queryParams.add("o=$orderCode")

        // 5. Page
        if (request.page > 1) {
            queryParams.add("p=${request.page}")
        }

        // 6. Uploader filter if requested
        if (!uploader.isNullOrBlank()) {
            val encodedUser = URLEncoder.encode(uploader.trim(), StandardCharsets.UTF_8.name())
            queryParams.add("u=$encodedUser")
        }

        return "$base/?${queryParams.joinToString("&")}"
    }

    /**
     * Builds RSS feed URL with equivalent query filters (Section 27).
     */
    fun buildRssUrl(request: ProviderSearchRequest, uploader: String? = null): String {
        val searchUrl = buildSearchUrl(request, uploader)
        return searchUrl.replace("$base/?", "$base/?page=rss&")
    }

    /**
     * Builds details view URL.
     */
    fun buildDetailsUrl(releaseId: String): String {
        return "$base/view/${releaseId.trim()}"
    }

    /**
     * Builds dedicated user upload page URL.
     */
    fun buildUserUrl(username: String): String {
        val encodedUser = URLEncoder.encode(username.trim(), StandardCharsets.UTF_8.name())
        return "$base/user/$encodedUser"
    }

    private fun mapSortField(field: ProviderSortField): String {
        return when (field) {
            ProviderSortField.Date -> "id" // Nyaa sorts by release ID for chronological date
            ProviderSortField.Id -> "id"
            ProviderSortField.Comments -> "comments"
            ProviderSortField.Size -> "size"
            ProviderSortField.Seeders -> "seeders"
            ProviderSortField.Leechers -> "leechers"
            ProviderSortField.Downloads -> "downloads"
        }
    }
}
