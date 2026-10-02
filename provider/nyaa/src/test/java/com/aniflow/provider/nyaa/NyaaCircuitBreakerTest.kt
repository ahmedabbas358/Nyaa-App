package com.aniflow.provider.nyaa

import com.aniflow.provider.nyaa.policy.CircuitBreaker
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying CircuitBreaker state transitions (Section 54, 124).
 */
class NyaaCircuitBreakerTest {

    @Test
    fun testCircuitBreakerTripsAfterThresholdFailures() = runTest {
        val breaker = CircuitBreaker(failureThreshold = 3, resetTimeoutMs = 1000L)

        assertEquals(CircuitBreaker.State.Closed, breaker.getState())
        assertTrue(breaker.canExecute())

        breaker.recordFailure()
        breaker.recordFailure()
        assertEquals(CircuitBreaker.State.Closed, breaker.getState())
        assertTrue(breaker.canExecute())

        // 3rd failure trips the breaker to Open
        breaker.recordFailure()
        assertEquals(CircuitBreaker.State.Open, breaker.getState())
        assertFalse(breaker.canExecute())
    }

    @Test
    fun testCircuitBreakerResetsOnSuccess() = runTest {
        val breaker = CircuitBreaker(failureThreshold = 2, resetTimeoutMs = 1000L)

        breaker.recordFailure()
        breaker.recordSuccess()

        assertEquals(CircuitBreaker.State.Closed, breaker.getState())
        assertTrue(breaker.canExecute())
    }
}
