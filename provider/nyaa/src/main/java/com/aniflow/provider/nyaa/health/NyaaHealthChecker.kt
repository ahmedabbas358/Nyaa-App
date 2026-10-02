package com.aniflow.provider.nyaa.health

import com.aniflow.provider.core.health.ProviderHealth
import com.aniflow.provider.core.health.ProviderHealthMetrics
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

/**
 * Tracks runtime health metrics and status diagnostics for Nyaa (Sections 51, 52, 53).
 */
class NyaaHealthChecker {

    private val mutex = Mutex()
    private var lastSuccessAt: Instant? = null
    private var lastFailureAt: Instant? = null
    private var consecutiveFailures: Int = 0
    private var totalLatencyMs: Long = 0L
    private var totalLatencySamples: Long = 0L
    private var lastStatusCode: Int? = null
    private var totalRequests: Long = 0L
    private var totalFailures: Long = 0L
    private var rateLimitedUntil: Instant? = null

    suspend fun recordSuccess(latencyMs: Long, statusCode: Int = 200) = mutex.withLock {
        lastSuccessAt = Instant.now()
        consecutiveFailures = 0
        lastStatusCode = statusCode
        totalRequests++
        totalLatencyMs += latencyMs
        totalLatencySamples++
    }

    suspend fun recordFailure(statusCode: Int?, error: Throwable) = mutex.withLock {
        lastFailureAt = Instant.now()
        consecutiveFailures++
        lastStatusCode = statusCode
        totalRequests++
        totalFailures++
    }

    suspend fun recordRateLimit(retryAfterSeconds: Long?) = mutex.withLock {
        lastFailureAt = Instant.now()
        consecutiveFailures++
        lastStatusCode = 429
        totalRequests++
        totalFailures++
        val delaySec = retryAfterSeconds ?: 60L
        rateLimitedUntil = Instant.now().plusSeconds(delaySec)
    }

    suspend fun getHealth(): ProviderHealth = mutex.withLock {
        val now = Instant.now()
        val avgLatency = if (totalLatencySamples > 0) totalLatencyMs / totalLatencySamples else 0L

        val metrics = ProviderHealthMetrics(
            lastSuccessAt = lastSuccessAt,
            lastFailureAt = lastFailureAt,
            consecutiveFailures = consecutiveFailures,
            averageLatencyMs = avgLatency,
            lastStatusCode = lastStatusCode,
            totalRequests = totalRequests,
            totalFailures = totalFailures
        )

        val rateLimit = rateLimitedUntil
        if (rateLimit != null && now.isBefore(rateLimit)) {
            return@withLock ProviderHealth.RateLimited(rateLimit, metrics)
        }

        return@withLock when {
            consecutiveFailures >= 5 -> ProviderHealth.Unavailable(
                "Nyaa unreachable: $consecutiveFailures consecutive failures",
                metrics
            )
            consecutiveFailures in 1..4 -> ProviderHealth.Degraded(
                "Experiencing intermittent errors ($consecutiveFailures failures)",
                metrics
            )
            else -> ProviderHealth.Healthy(metrics)
        }
    }
}
