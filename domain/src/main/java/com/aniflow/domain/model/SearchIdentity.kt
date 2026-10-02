package com.aniflow.domain.model

import com.aniflow.domain.identity.ProviderId
import com.aniflow.provider.core.model.ProviderSort
import java.security.MessageDigest

/**
 * Deterministic query identity for caching, duplicate page protection, and request deduplication (Sections 17, 18).
 */
data class SearchIdentity(
    val provider: ProviderId,
    val normalizedQuery: String,
    val sort: ProviderSort = ProviderSort.DEFAULT,
    val filtersHash: String = ""
) {
    /**
     * Computes unique deterministic cache/request key for a specific page.
     */
    fun toCacheKey(page: Int): String {
        val raw = "p=${provider.value}|q=$normalizedQuery|s=${sort.field}_${sort.direction}|f=$filtersHash|page=$page"
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(raw.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        fun create(
            providerId: ProviderId,
            query: String,
            sort: ProviderSort = ProviderSort.DEFAULT,
            trustedOnly: Boolean = false,
            category: String? = null
        ): SearchIdentity {
            val normalized = query.trim().replace(Regex("\\s+"), " ")
            val fHash = "t=$trustedOnly;c=${category ?: "all"}"
            return SearchIdentity(
                provider = providerId,
                normalizedQuery = normalized,
                sort = sort,
                filtersHash = fHash
            )
        }
    }
}
