package com.aniflow.provider.core.health

import java.time.Instant

/**
 * Health status representation for a ReleaseProvider (Section 51).
 */
sealed interface ProviderHealth {
    val metrics: ProviderHealthMetrics

    data class Healthy(override val metrics: ProviderHealthMetrics) : ProviderHealth
    data class Degraded(val reason: String, override val metrics: ProviderHealthMetrics) : ProviderHealth
    data class Unavailable(val reason: String, override val metrics: ProviderHealthMetrics) : ProviderHealth
    data class RateLimited(val resetAt: Instant?, override val metrics: ProviderHealthMetrics) : ProviderHealth
}

/**
 * Diagnostic metrics tracked per provider (Section 53).
 */
data class ProviderHealthMetrics(
    val lastSuccessAt: Instant? = null,
    val lastFailureAt: Instant? = null,
    val consecutiveFailures: Int = 0,
    val averageLatencyMs: Long = 0L,
    val lastStatusCode: Int? = null,
    val totalRequests: Long = 0L,
    val totalFailures: Long = 0L
) {
    val failureRate: Double
        get() = if (totalRequests > 0) totalFailures.toDouble() / totalRequests else 0.0
}
