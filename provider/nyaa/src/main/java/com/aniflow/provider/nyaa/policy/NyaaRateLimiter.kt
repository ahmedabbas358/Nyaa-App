package com.aniflow.provider.nyaa.policy

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

/**
 * Enforces rate limiting and maximum concurrency for Nyaa requests (Section 55).
 */
class NyaaRateLimiter(
    val requestIntervalMs: Long = 500L,
    val maxConcurrentRequests: Int = 2
) {

    private val semaphore = Semaphore(maxConcurrentRequests)
    private val intervalMutex = Mutex()
    private var lastRequestTime = 0L

    suspend fun <T> execute(block: suspend () -> T): T {
        return semaphore.withPermit {
            intervalMutex.withLock {
                val now = System.currentTimeMillis()
                val elapsed = now - lastRequestTime
                if (elapsed < requestIntervalMs) {
                    val waitTime = requestIntervalMs - elapsed
                    delay(waitTime)
                }
                lastRequestTime = System.currentTimeMillis()
            }
            block()
        }
    }
}
