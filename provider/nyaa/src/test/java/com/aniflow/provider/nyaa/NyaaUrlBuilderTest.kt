package com.aniflow.provider.nyaa

import com.aniflow.domain.valueobject.SortDirection
import com.aniflow.provider.core.model.ProviderCategory
import com.aniflow.provider.core.model.ProviderSearchFilters
import com.aniflow.provider.core.model.ProviderSearchRequest
import com.aniflow.provider.core.model.ProviderSort
import com.aniflow.provider.core.model.ProviderSortField
import com.aniflow.provider.nyaa.client.NyaaUrlBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying URL building, query encoding, and parameter mapping (Sections 29, 30, 33, 34).
 */
class NyaaUrlBuilderTest {

    private val builder = NyaaUrlBuilder()

    @Test
    fun testDefaultSearchUrl() {
        val request = ProviderSearchRequest(query = "Frieren")
        val url = builder.buildSearchUrl(request)

        assertTrue(url.startsWith("https://nyaa.si/?"))
        assertTrue(url.contains("f=0"))
        assertTrue(url.contains("c=0_0"))
        assertTrue(url.contains("q=Frieren"))
        assertTrue(url.contains("s=id"))
        assertTrue(url.contains("o=desc"))
    }

    @Test
    fun testUrlEncodingSpecialCharacters() {
        val request = ProviderSearchRequest(query = "Attack on Titan [1080p] & Remux")
        val url = builder.buildSearchUrl(request)

        assertTrue(url.contains("q=Attack+on+Titan+%5B1080p%5D+%26+Remux"))
    }

    @Test
    fun testCategoryAndFilterMapping() {
        val request = ProviderSearchRequest(
            query = "One Piece",
            category = ProviderCategory.Anime.EnglishTranslated,
            filters = ProviderSearchFilters.TrustedOnly,
            sort = ProviderSort(ProviderSortField.Seeders, SortDirection.Descending),
            page = 3
        )
        val url = builder.buildSearchUrl(request)

        assertTrue(url.contains("f=2")) // TrustedOnly -> f=2
        assertTrue(url.contains("c=1_2")) // EnglishTranslated -> c=1_2
        assertTrue(url.contains("s=seeders"))
        assertTrue(url.contains("o=desc"))
        assertTrue(url.contains("p=3"))
    }

    @Test
    fun testUploaderSearch() {
        val request = ProviderSearchRequest(query = "Bleach")
        val url = builder.buildSearchUrl(request, uploader = "SubsPlease")

        assertTrue(url.contains("u=SubsPlease"))
    }

    @Test
    fun testRssUrlBuilding() {
        val request = ProviderSearchRequest(
            query = "Jujutsu Kaisen",
            category = ProviderCategory.Anime.Raw
        )
        val rssUrl = builder.buildRssUrl(request)

        assertTrue(rssUrl.startsWith("https://nyaa.si/?page=rss&"))
        assertTrue(rssUrl.contains("c=1_4")) // Anime Raw -> 1_4
        assertTrue(rssUrl.contains("q=Jujutsu+Kaisen"))
    }

    @Test
    fun testDetailsUrlBuilding() {
        val detailsUrl = builder.buildDetailsUrl("1800001")
        assertEquals("https://nyaa.si/view/1800001", detailsUrl)
    }
}
