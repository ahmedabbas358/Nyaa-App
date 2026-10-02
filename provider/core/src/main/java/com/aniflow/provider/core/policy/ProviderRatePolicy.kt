package com.aniflow.provider.core.policy

/**
 * Rate limiting constraints per provider (Section 55).
 * Prevents unintentional flood / DoS to public trackers.
 */
data class ProviderRatePolicy(
    val requestIntervalMs: Long = 500L,
    val maxConcurrentRequests: Int = 2,
    val maxBurst: Int = 4,
    val backoffMultiplier: Double = 1.5,
    val maxBackoffDelayMs: Long = 30_000L
) {
    companion object {
        val CONSERVATIVE = ProviderRatePolicy(
            requestIntervalMs = 1000L,
            maxConcurrentRequests = 1,
            maxBurst = 2
        )
    }
}
