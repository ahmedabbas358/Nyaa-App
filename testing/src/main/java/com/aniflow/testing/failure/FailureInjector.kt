package com.aniflow.testing.failure

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Enforces STEP 13 Section 76 (Chaos Testing) and Section 77 (Fake Failure Injection).
 *
 * Allows deterministic failure simulation in integration and reliability tests
 * to verify recovery paths for network, database, storage, verification, and queue operations.
 */
enum class FailurePoint {
    ProviderTimeout,
    ProviderRateLimit,
    DatabaseWriteFailure,
    DatabaseLocked,
    StorageWriteFailure,
    DiskFull,
    VerificationChecksumMismatch,
    QueueFailure,
    NetworkDisconnect
}

interface FailureInjector {
    fun shouldFail(point: FailurePoint): Boolean
}

class TestFailureInjector : FailureInjector {

    private val armedPoints = ConcurrentHashMap<FailurePoint, AtomicInteger>()

    /**
     * Arms a failure point to fail a specified number of times.
     */
    fun arm(point: FailurePoint, times: Int = 1) {
        armedPoints[point] = AtomicInteger(times)
    }

    override fun shouldFail(point: FailurePoint): Boolean {
        val counter = armedPoints[point] ?: return false
        val remaining = counter.get()
        return if (remaining > 0) {
            counter.decrementAndGet() >= 0
        } else {
            false
        }
    }

    fun disarm(point: FailurePoint) {
        armedPoints.remove(point)
    }

    fun clearAll() {
        armedPoints.clear()
    }
}
