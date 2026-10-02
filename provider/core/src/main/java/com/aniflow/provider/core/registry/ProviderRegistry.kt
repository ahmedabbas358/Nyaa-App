package com.aniflow.provider.core.registry

import com.aniflow.domain.identity.ProviderId
import com.aniflow.provider.core.ReleaseProvider
import java.util.concurrent.ConcurrentHashMap

/**
 * Registry holding and managing registered ReleaseProvider instances (Section 95).
 */
interface ProviderRegistry {
    fun get(providerId: ProviderId): ReleaseProvider?
    fun all(): List<ReleaseProvider>
    fun enabled(): List<ReleaseProvider>
    fun isEnabled(providerId: ProviderId): Boolean
    fun setEnabled(providerId: ProviderId, enabled: Boolean)
    fun register(provider: ReleaseProvider)
}

/**
 * Thread-safe default in-memory implementation of ProviderRegistry (Section 96).
 */
class DefaultProviderRegistry(
    initialProviders: List<ReleaseProvider> = emptyList()
) : ProviderRegistry {

    private val providers = ConcurrentHashMap<String, ReleaseProvider>()
    private val enabledStatus = ConcurrentHashMap<String, Boolean>()

    init {
        for (provider in initialProviders) {
            register(provider)
        }
    }

    override fun get(providerId: ProviderId): ReleaseProvider? {
        return providers[providerId.value]
    }

    override fun all(): List<ReleaseProvider> {
        return providers.values.toList()
    }

    override fun enabled(): List<ReleaseProvider> {
        return providers.values.filter { isEnabled(it.descriptor.id) }
    }

    override fun isEnabled(providerId: ProviderId): Boolean {
        return enabledStatus[providerId.value] ?: true
    }

    override fun setEnabled(providerId: ProviderId, enabled: Boolean) {
        enabledStatus[providerId.value] = enabled
    }

    override fun register(provider: ReleaseProvider) {
        providers[provider.descriptor.id.value] = provider
        if (!enabledStatus.containsKey(provider.descriptor.id.value)) {
            enabledStatus[provider.descriptor.id.value] = true
        }
    }
}
