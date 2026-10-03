package com.aniflow.provider.core

import com.aniflow.domain.identity.ProviderId
import com.aniflow.domain.valueobject.UrlValue
import com.aniflow.provider.core.health.ProviderHealth
import com.aniflow.provider.core.health.ProviderHealthMetrics
import com.aniflow.provider.core.model.ProviderCapabilities
import com.aniflow.provider.core.model.ProviderDescriptor
import com.aniflow.provider.core.model.ProviderRelease
import com.aniflow.provider.core.model.ProviderSearchPage
import com.aniflow.provider.core.model.ProviderSearchRequest
import com.aniflow.provider.core.registry.DefaultProviderRegistry
import com.aniflow.core.common.result.AniFlowResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying ProviderRegistry behavior (Section 95, 96, 98).
 */
class ProviderRegistryTest {

    private lateinit var registry: DefaultProviderRegistry

    private val providerA = object : ReleaseProvider {
        override val descriptor = ProviderDescriptor(
            id = ProviderId("nyaa"),
            name = "Nyaa.si",
            baseUrl = UrlValue.HttpsUrl("https://nyaa.si"),
            capabilities = ProviderCapabilities()
        )
        override suspend fun search(request: ProviderSearchRequest) = AniFlowResult.Success(ProviderSearchPage(emptyList(), 1, false))
        override suspend fun getRelease(providerReleaseId: String) = AniFlowResult.Error(com.aniflow.core.common.result.ErrorType.DatabaseError("Not implemented"))
        override suspend fun searchByUploader(uploader: String, request: ProviderSearchRequest) = search(request)
        override suspend fun healthCheck() = ProviderHealth.Healthy(ProviderHealthMetrics())
    }

    private val providerB = object : ReleaseProvider {
        override val descriptor = ProviderDescriptor(
            id = ProviderId("animetosho"),
            name = "AnimeTosho",
            baseUrl = UrlValue.HttpsUrl("https://animetosho.org"),
            capabilities = ProviderCapabilities(uploaderSearch = true)
        )
        override suspend fun search(request: ProviderSearchRequest) = AniFlowResult.Success(ProviderSearchPage(emptyList(), 1, false))
        override suspend fun getRelease(providerReleaseId: String) = AniFlowResult.Error(com.aniflow.core.common.result.ErrorType.DatabaseError("Not implemented"))
        override suspend fun searchByUploader(uploader: String, request: ProviderSearchRequest) = search(request)
        override suspend fun healthCheck() = ProviderHealth.Healthy(ProviderHealthMetrics())
    }

    @Before
    fun setUp() {
        registry = DefaultProviderRegistry(listOf(providerA))
    }

    @Test
    fun testGetRegisteredProvider() {
        val found = registry.get(ProviderId("nyaa"))
        assertNotNull(found)
        assertEquals("Nyaa.si", found?.descriptor?.name)

        val notFound = registry.get(ProviderId("unknown"))
        assertNull(notFound)
    }

    @Test
    fun testRegisterNewProviderDynamically() {
        registry.register(providerB)

        assertEquals(2, registry.all().size)
        assertNotNull(registry.get(ProviderId("animetosho")))
    }

    @Test
    fun testDisableAndEnableProvider() {
        registry.register(providerB)

        assertTrue(registry.isEnabled(ProviderId("nyaa")))
        assertEquals(2, registry.enabled().size)

        registry.setEnabled(ProviderId("nyaa"), false)
        assertFalse(registry.isEnabled(ProviderId("nyaa")))
        assertEquals(1, registry.enabled().size)
        assertEquals("animetosho", registry.enabled().first().descriptor.id.value)

        registry.setEnabled(ProviderId("nyaa"), true)
        assertTrue(registry.isEnabled(ProviderId("nyaa")))
        assertEquals(2, registry.enabled().size)
    }
}
