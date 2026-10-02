package com.aniflow.provider.nyaa

import com.aniflow.core.common.result.AniFlowResult
import com.aniflow.provider.core.ReleaseProvider
import com.aniflow.provider.core.health.ProviderHealth
import com.aniflow.provider.core.model.ProviderSearchRequest
import com.aniflow.provider.nyaa.client.NyaaHttpClient
import com.aniflow.provider.nyaa.client.NyaaUrlBuilder
import com.aniflow.provider.nyaa.config.NyaaProviderConfig
import com.aniflow.provider.nyaa.mapper.NyaaMapper
import com.aniflow.provider.nyaa.parser.NyaaHtmlDetailsParser
import com.aniflow.provider.nyaa.parser.NyaaHtmlSearchParser
import com.aniflow.provider.nyaa.parser.NyaaRssParser
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.InputStreamReader

/**
 * Validates that NyaaProvider conforms strictly to ReleaseProvider contract (Section 117, 118).
 */
class ReleaseProviderContractTest {

    private lateinit var server: MockWebServer
    private lateinit var provider: ReleaseProvider

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val mockBaseUrl = com.aniflow.domain.valueobject.UrlValue.HttpsUrl(server.url("/").toString())
        val config = NyaaProviderConfig(baseUrl = mockBaseUrl)

        val httpClient = NyaaHttpClient(config, OkHttpClient())
        val urlBuilder = NyaaUrlBuilder(config)
        val searchParser = NyaaHtmlSearchParser(mockBaseUrl.rawValue)
        val detailsParser = NyaaHtmlDetailsParser(mockBaseUrl.rawValue)
        val rssParser = NyaaRssParser()
        val mapper = NyaaMapper()

        provider = NyaaProvider(
            config = config,
            httpClient = httpClient,
            urlBuilder = urlBuilder,
            searchParser = searchParser,
            detailsParser = detailsParser,
            rssParser = rssParser,
            mapper = mapper
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun loadFixture(path: String): String {
        val stream = javaClass.classLoader?.getResourceAsStream(path)
            ?: throw IllegalArgumentException("Fixture not found: $path")
        return InputStreamReader(stream).readText()
    }

    @Test
    fun testProviderDescriptorConformsToContract() {
        val descriptor = provider.descriptor

        assertEquals("nyaa", descriptor.id.value)
        assertEquals("Nyaa.si", descriptor.name)
        assertNotNull(descriptor.baseUrl)
        assertTrue(descriptor.capabilities.supportsSearch)
        assertTrue(descriptor.capabilities.supportsPagination)
        assertTrue(descriptor.capabilities.supportsTorrent)
        assertTrue(descriptor.capabilities.supportsMagnet)
        assertTrue(descriptor.capabilities.supportsHtml)
        assertTrue(descriptor.capabilities.supportsRss)
    }

    @Test
    fun testSearchExecutionConformsToContract() = runTest {
        val htmlFixture = loadFixture("fixtures/nyaa_search_page.html")
        server.enqueue(MockResponse().setResponseCode(200).setBody(htmlFixture))

        val result = provider.search(ProviderSearchRequest(query = "Frieren"))

        assertTrue("Expected Success result", result is AniFlowResult.Success)
        val page = (result as AniFlowResult.Success).data

        assertEquals(1, page.page)
        assertTrue(page.hasNextPage)
        assertEquals(3, page.items.size)

        val firstItem = page.items[0]
        assertEquals("1800001", firstItem.providerReleaseId)
        assertEquals("[SubsPlease] Sousou no Frieren - 28 (1080p) [ABCD1234].mkv", firstItem.title)
        assertTrue(firstItem.isTrusted)
        assertNotNull(firstItem.magnetUri)
    }

    @Test
    fun testGetReleaseDetailsConformsToContract() = runTest {
        val detailsFixture = loadFixture("fixtures/nyaa_details_page.html")
        server.enqueue(MockResponse().setResponseCode(200).setBody(detailsFixture))

        val result = provider.getRelease("1800001")

        assertTrue("Expected Success result", result is AniFlowResult.Success)
        val release = (result as AniFlowResult.Success).data

        assertEquals("1800001", release.providerReleaseId)
        assertEquals("SubsPlease", release.uploader?.displayName)
        assertNotNull(release.description)
        assertEquals(142, release.seeders)
    }

    @Test
    fun testHealthCheckConformsToContract() = runTest {
        val health = provider.healthCheck()
        assertNotNull(health.metrics)
        assertTrue(health is ProviderHealth.Healthy)
    }
}
