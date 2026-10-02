package com.aniflow.domain.model.aggregate.release

import com.aniflow.domain.identity.ProviderId

/**
 * Lightweight reference to an external or internal content provider (Section 23).
 * Keeps domain free from provider-specific implementation details.
 */
data class ProviderRef(
    val providerId: ProviderId,
    val name: String
) {
    init {
        require(name.isNotBlank()) { "ProviderRef name cannot be blank" }
    }

    companion object {
        val NYAA = ProviderRef(ProviderId("nyaa"), "Nyaa")
        val LOCAL = ProviderRef(ProviderId("local"), "Local Library")
        val DIRECT = ProviderRef(ProviderId("direct"), "Direct HTTP")
    }
}
