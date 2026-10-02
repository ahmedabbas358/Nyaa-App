package com.aniflow.provider.nyaa.policy

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

/**
 * Circuit Breaker pattern implementation (Section 54).
 * Prevents hammering provider servers when failures cascade.
 */
class CircuitBreaker(
    val failureThreshold: Int = 5,
    val resetTimeoutMs: Long = 30_000L
) {

    enum class State {
        Closed,   // Normal operation
        Open,     // Tripped; all calls immediately rejected
        HalfOpen  // Testing if provider has recovered
    }

    private val mutex = Mutex()
    private var state: State = State.Closed
    private var failureCount: Int = 0
    private var lastFailureTime: Long = 0L

    suspend fun getState(): State = mutex.withLock {
        evaluateState()
        state
    }

    suspend fun canExecute(): Boolean = mutex.withLock {
        evaluateState()
        state != State.Open
    }

    suspend fun recordSuccess() = mutex.withLock {
        failureCount = 0
        state = State.Closed
    }

    suspend fun recordFailure() = mutex.withLock {
        failureCount++
        lastFailureTime = System.currentTimeMillis()
        if (failureCount >= failureThreshold) {
            state = State.Open
        }
    }

    private fun evaluateState() {
        if (state == State.Open) {
            val now = System.currentTimeMillis()
            if (now - lastFailureTime >= resetTimeoutMs) {
                state = State.HalfOpen
            }
        }
    }

    suspend fun reset() = mutex.withLock {
        failureCount = 0
        lastFailureTime = 0L
        state = State.Closed
    }
}
