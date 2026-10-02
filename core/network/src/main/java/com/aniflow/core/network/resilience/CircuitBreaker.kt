package com.aniflow.core.network.resilience

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Enforces STEP 13 Section 29 (Rate Limiting), Section 30 (Provider Health),
 * and Section 43 (Circuit Breaker).
 *
 * States:
 * Healthy -> Degraded -> Open -> HalfOpen -> Healthy
 */
class ProviderCircuitBreaker(
    val providerId: String,
    private val failureThreshold: Int = 4,
    private val openCooldownMillis: Long = 30_000L
) {

    enum class State {
        Healthy,
        Degraded,
        RateLimited,
        Open,
        HalfOpen
    }

    private val mutex = Mutex()
    var state: State = State.Healthy
        private set

    var consecutiveFailures: Int = 0
        private set

    var lastFailureTimestamp: Long = 0L
        private set

    var totalSuccesses: Long = 0L
        private set

    var total429s: Long = 0L
        private set

    suspend fun canExecute(): Boolean {
        mutex.withLock {
            val now = System.currentTimeMillis()
            return when (state) {
                State.Healthy, State.Degraded -> true
                State.RateLimited -> {
                    // Check if rate limit cooldown elapsed
                    if (now - lastFailureTimestamp >= openCooldownMillis) {
                        state = State.HalfOpen
                        true
                    } else {
                        false
                    }
                }
                State.Open -> {
                    if (now - lastFailureTimestamp >= openCooldownMillis) {
                        state = State.HalfOpen
                        true
                    } else {
                        false
                    }
                }
                State.HalfOpen -> true // Allow 1 probe request
            }
        }
    }

    suspend fun recordSuccess() {
        mutex.withLock {
            totalSuccesses++
            consecutiveFailures = 0
            state = State.Healthy
        }
    }

    suspend fun recordFailure(statusCode: Int? = null) {
        mutex.withLock {
            consecutiveFailures++
            lastFailureTimestamp = System.currentTimeMillis()

            if (statusCode == 429) {
                total429s++
                state = State.RateLimited
                return
            }

            if (consecutiveFailures >= failureThreshold) {
                state = State.Open
            } else if (consecutiveFailures >= 2) {
                state = State.Degraded
            }
        }
    }

    suspend fun reset() {
        mutex.withLock {
            state = State.Healthy
            consecutiveFailures = 0
            lastFailureTimestamp = 0L
        }
    }
}
