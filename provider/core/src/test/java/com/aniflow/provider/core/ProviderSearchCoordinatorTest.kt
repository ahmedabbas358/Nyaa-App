package com.aniflow.provider.core

import com.aniflow.core.common.result.AniFlowResult
import com.aniflow.core.common.result.ErrorType
import com.aniflow.domain.identity.ProviderId
import com.aniflow.domain.valueobject.UrlValue
import com.aniflow.provider.core.coordinator.ProviderSearchCoordinator
import com.aniflow.provider.core.health.ProviderHealth
import com.aniflow.provider.core.health.ProviderHealthMetrics
import com.aniflow.provider.core.model.ProviderCapabilities
import com.aniflow.provider.core.model.ProviderDescriptor
import com.aniflow.provider.core.model.ProviderRelease
import com.aniflow.provider.core.model.ProviderSearchPage
import com.aniflow.provider.core.model.ProviderSearchRequest
import com.aniflow.provider.core.registry.DefaultProviderRegistry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying ProviderSearchCoordinator deduplication and partial failure merging (Section 64, 65, 100, 101, 102).
 */
class ProviderSearchCoordinatorTest {

    @Test
    fun testCoordinatedSearchDeduplicatesByInfoHash() = runTest {
        val sameMagnet = "magnet:?xt=urn:btih:0123456789abcdef0123456789abcdef01234567&dn=Test"

        val release1 = ProviderRelease(
            providerReleaseId = "101",
            title = "Test Release A",
            detailsUrl = null,
            torrentUrl = null,
            magnetUri = sameMagnet,
            sizeBytes = 1000L,
            seeders = 10,
            leechers = 1,
            downloads = 50L,
            publishedAt = null,
            category = null,
            uploader = null,
            releaseGroup = null
        )

        val release2 = ProviderRelease(
            providerReleaseId = "202",
            title = "Test Release B (Identical Hash)",
            detailsUrl = null,
            torrentUrl = null,
            magnetUri = sameMagnet,
            sizeBytes = 1000L,
            seeders = 12,
            leechers = 2,
            downloads = 60L,
            publishedAt = null,
            category = null,
            uploader = null,
            releaseGroup = null
        )

        val providerA = createMockProvider("nyaa", listOf(release1))
        val providerB = createMockProvider("tosho", listOf(release2))

        val registry = DefaultProviderRegistry(listOf(providerA, providerB))
        val coordinator = ProviderSearchCoordinator(registry)

        val merged = coordinator.search(ProviderSearchRequest(query = "Test"))

        // Only one unique release should be returned because infoHash was identical
        assertEquals(1, merged.items.size)
        assertEquals(2, merged.successfulProviders.size)
        assertFalse(merged.isPartial)
    }

    @Test
    fun testPartialFailureHandling() = runTest {
        val release = ProviderRelease(
            providerReleaseId = "101",
            title = "Working Release",
            detailsUrl = null,
            torrentUrl = null,
            magnetUri = "magnet:?xt=urn:btih:1111111111111111111111111111111111111111",
            sizeBytes = 500L,
            seeders = 5,
            leechers = 0,
            downloads = 10L,
            publishedAt = null,
            category = null,
            uploader = null,
            releaseGroup = null
        )

        val successProvider = createMockProvider("nyaa", listOf(release))

        val failingProvider = object : ReleaseProvider {
            override val descriptor = ProviderDescriptor(
                id = ProviderId("failing"),
                name = "Failing Provider",
                baseUrl = UrlValue.HttpsUrl("https://failing.com"),
                capabilities = ProviderCapabilities()
            )
            override suspend fun search(request: ProviderSearchRequest) =
                AniFlowResult.Error(ErrorType.NetworkError("Server down"), "Provider offline")
            override suspend fun getRelease(providerReleaseId: String) =
                AniFlowResult.Error(ErrorType.NetworkError("Failed"), "Failed")
            override suspend fun searchByUploader(uploader: String, request: ProviderSearchRequest) = search(request)
            override suspend fun healthCheck() = ProviderHealth.Healthy(ProviderHealthMetrics())
        }

        val registry = DefaultProviderRegistry(listOf(successProvider, failingProvider))
        val coordinator = ProviderSearchCoordinator(registry)

        val merged = coordinator.search(ProviderSearchRequest(query = "Anime"))

        // Partial results returned without throwing an exception
        assertTrue(merged.isPartial)
        assertEquals(1, merged.items.size)
        assertEquals(1, merged.providerErrors.size)
        assertTrue(merged.providerErrors.containsKey(ProviderId("failing")))
        assertEquals(1, merged.successfulProviders.size)
        assertTrue(merged.successfulProviders.contains(ProviderId("nyaa")))
    }

    private fun createMockProvider(id: String, releases: List<ProviderRelease>): ReleaseProvider {
        return object : ReleaseProvider {
            override val descriptor = ProviderDescriptor(
                id = ProviderId(id),
                name = id.replaceFirstChar { it.uppercase() },
                baseUrl = UrlValue.HttpsUrl("https://$id.com"),
                capabilities = ProviderCapabilities()
            )
            override suspend fun search(request: ProviderSearchRequest) =
                AniFlowResult.Success(ProviderSearchPage(releases, 1, false))
            override suspend fun getRelease(providerReleaseId: String) =
                AniFlowResult.Error(ErrorType.DatabaseError("Not implemented"))
            override suspend fun searchByUploader(uploader: String, request: ProviderSearchRequest) = search(request)
            override suspend fun healthCheck() = ProviderHealth.Healthy(ProviderHealthMetrics())
        }
    }
}
