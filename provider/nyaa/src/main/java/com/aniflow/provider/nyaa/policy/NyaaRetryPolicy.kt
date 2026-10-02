package com.aniflow.provider.nyaa.policy

import com.aniflow.provider.core.error.ProviderError
import kotlinx.coroutines.delay

/**
 * Handles controlled exponential backoff for retriable provider errors (Section 25, 77).
 */
class NyaaRetryPolicy(
    val maxRetries: Int = 3,
    val initialDelayMs: Long = 1000L,
    val maxDelayMs: Long = 15_000L,
    val backoffMultiplier: Double = 2.0
) {

    suspend fun <T> executeWithRetry(block: suspend (attempt: Int) -> T): T {
        var attempt = 0
        var currentDelay = initialDelayMs

        while (true) {
            try {
                attempt++
                return block(attempt)
            } catch (e: ProviderError) {
                if (!isRetriable(e) || attempt > maxRetries) {
                    throw e
                }

                val jitter = (0..200).random()
                val sleepTime = if (e is ProviderError.RateLimited && e.retryAfterSeconds != null) {
                    (e.retryAfterSeconds * 1000L) + jitter
                } else {
                    currentDelay.coerceAtMost(maxDelayMs) + jitter
                }

                delay(sleepTime)
                currentDelay = (currentDelay * backoffMultiplier).toLong()
            }
        }
    }

    private fun isRetriable(error: ProviderError): Boolean {
        return error.retryable
    }
}
