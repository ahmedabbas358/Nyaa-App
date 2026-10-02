package com.aniflow.provider.core.coordinator

import com.aniflow.core.common.result.AniFlowResult
import com.aniflow.domain.identity.ProviderId
import com.aniflow.provider.core.error.ProviderError
import com.aniflow.provider.core.model.ProviderRelease
import com.aniflow.provider.core.model.ProviderSearchRequest
import com.aniflow.provider.core.registry.ProviderRegistry
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Result of multi-provider coordinated search (Section 101, 102).
 */
data class MergedSearchResult(
    val items: List<ProviderRelease>,
    val page: Int,
    val hasNextPage: Boolean,
    val providerErrors: Map<ProviderId, ProviderError> = emptyMap(),
    val successfulProviders: Set<ProviderId> = emptySet(),
    val isPartial: Boolean = false
)

/**
 * Coordinates dispatching search requests across multiple providers in parallel,
 * merging, and deduplicating results (Section 64, 100).
 */
class ProviderSearchCoordinator(
    private val registry: ProviderRegistry
) {

    suspend fun search(
        request: ProviderSearchRequest
    ): MergedSearchResult = coroutineScope {
        val enabledProviders = registry.enabled()
        if (enabledProviders.isEmpty()) {
            return@coroutineScope MergedSearchResult(
                items = emptyList(),
                page = request.page,
                hasNextPage = false,
                providerErrors = mapOf(ProviderId("none") to ProviderError.Unavailable("No providers enabled"))
            )
        }

        val deferredResults = enabledProviders.map { provider ->
            async {
                provider.descriptor.id to provider.search(request)
            }
        }.awaitAll()

        val allItems = mutableListOf<ProviderRelease>()
        val errors = mutableMapOf<ProviderId, ProviderError>()
        val successes = mutableSetOf<ProviderId>()
        var anyHasNext = false

        for ((providerId, result) in deferredResults) {
            when (result) {
                is AniFlowResult.Success -> {
                    successes.add(providerId)
                    allItems.addAll(result.data.items)
                    if (result.data.hasNextPage) anyHasNext = true
                }
                is AniFlowResult.Error -> {
                    val error = when (val err = result.error) {
                        is com.aniflow.core.common.result.ErrorType.DatabaseError -> ProviderError.Unknown(err.message)
                        is com.aniflow.core.common.result.ErrorType.NetworkError -> ProviderError.NetworkFailure(result.message)
                        is com.aniflow.core.common.result.ErrorType.ParserError -> ProviderError.ParserFailure(result.message)
                        else -> ProviderError.Unknown(result.message)
                    }
                    errors[providerId] = error
                }
                is AniFlowResult.Loading -> Unit
            }
        }

        // Section 65: Deduplicate releases across sources
        val deduplicated = deduplicateReleases(allItems)

        MergedSearchResult(
            items = deduplicated,
            page = request.page,
            hasNextPage = anyHasNext,
            providerErrors = errors,
            successfulProviders = successes,
            isPartial = errors.isNotEmpty() && successes.isNotEmpty()
        )
    }

    private fun deduplicateReleases(releases: List<ProviderRelease>): List<ProviderRelease> {
        val seenHashes = mutableSetOf<String>()
        val seenProviderKeys = mutableSetOf<String>()
        val result = mutableListOf<ProviderRelease>()

        for (release in releases) {
            val hash = release.magnetUri?.let { extractHashFromMagnet(it) }
            val providerKey = if (release.providerReleaseId != null) {
                "${release.providerReleaseId}"
            } else null

            var isDuplicate = false
            if (hash != null && hash.isNotBlank()) {
                if (seenHashes.contains(hash)) isDuplicate = true else seenHashes.add(hash)
            }
            if (!isDuplicate && providerKey != null) {
                if (seenProviderKeys.contains(providerKey)) isDuplicate = true else seenProviderKeys.add(providerKey)
            }

            if (!isDuplicate) {
                result.add(release)
            }
        }
        return result
    }

    private fun extractHashFromMagnet(uri: String): String? {
        val match = Regex("""xt=urn:btih:([a-fA-F0-9]{40}|[a-zA-Z2-7]{32})""", RegexOption.IGNORE_CASE).find(uri)
        return match?.groupValues?.get(1)?.lowercase()
    }
}
