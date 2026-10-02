package com.aniflow.provider.core

import com.aniflow.core.common.result.AniFlowResult
import com.aniflow.provider.core.health.ProviderHealth
import com.aniflow.provider.core.model.ProviderDescriptor
import com.aniflow.provider.core.model.ProviderRelease
import com.aniflow.provider.core.model.ProviderSearchPage
import com.aniflow.provider.core.model.ProviderSearchRequest

/**
 * Universal contract for Release Providers (Section 5, 89).
 * Decouples the rest of AniFlow from Nyaa, AnimeTosho, or any future trackers.
 */
interface ReleaseProvider {

    /**
     * Descriptor declaring identity, base URL, and capabilities of this provider.
     */
    val descriptor: ProviderDescriptor

    /**
     * Executes a search query with pagination, category, and filters.
     */
    suspend fun search(
        request: ProviderSearchRequest
    ): AniFlowResult<ProviderSearchPage>

    /**
     * Fetches enriched release details by the provider's internal release ID.
     */
    suspend fun getRelease(
        providerReleaseId: String
    ): AniFlowResult<ProviderRelease>

    /**
     * Executes a search restricted to releases uploaded by a specific user/account.
     */
    suspend fun searchByUploader(
        uploader: String,
        request: ProviderSearchRequest
    ): AniFlowResult<ProviderSearchPage>

    /**
     * Performs an active health check and reports latency and status.
     */
    suspend fun healthCheck(): ProviderHealth
}
