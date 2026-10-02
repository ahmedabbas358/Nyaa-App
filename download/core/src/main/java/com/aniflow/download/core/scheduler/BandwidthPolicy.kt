package com.aniflow.download.core.scheduler

import com.aniflow.download.core.model.DownloadPriority
import com.aniflow.download.core.model.DownloadTaskId
import kotlin.random.Random

enum class BandwidthAllocationStrategy {
    EqualShare,
    PriorityWeighted,
    FixedPerTask
}

data class BandwidthPolicy(
    val globalDownloadLimitBps: Long = 0L, // 0 = unlimited
    val globalUploadLimitBps: Long = 0L,
    val strategy: BandwidthAllocationStrategy = BandwidthAllocationStrategy.PriorityWeighted
) {
    fun calculateLimitForTask(
        taskId: DownloadTaskId,
        activeTasks: List<Pair<DownloadTaskId, DownloadPriority>>
    ): Long {
        if (globalDownloadLimitBps <= 0L || activeTasks.isEmpty()) return 0L

        return when (strategy) {
            BandwidthAllocationStrategy.EqualShare -> globalDownloadLimitBps / activeTasks.size
            BandwidthAllocationStrategy.FixedPerTask -> (globalDownloadLimitBps / activeTasks.size).coerceAtLeast(100 * 1024)
            BandwidthAllocationStrategy.PriorityWeighted -> {
                // Highest = 4, High = 3, Normal = 2, Low = 1, Lowest = 1 (to prevent starvation!)
                val totalWeight = activeTasks.sumOf { (it.second.value + 1) }
                val taskWeight = activeTasks.firstOrNull { it.first == taskId }?.second?.let { it.value + 1 } ?: 1
                (globalDownloadLimitBps * taskWeight) / totalWeight
            }
        }
    }
}

/**
 * Calculates exponential backoff with jitter for retryable errors (Section 44, 45, 46, 47).
 */
class RetryPolicyManager(
    private val maxRetries: Int = 3,
    private val initialDelayMs: Long = 2000L,
    private val maxDelayMs: Long = 60000L,
    private val backoffMultiplier: Double = 2.0
) {
    fun calculateNextRetryDelayMs(retryCount: Int): Long {
        if (retryCount >= maxRetries) return -1L
        val baseDelay = (initialDelayMs * Math.pow(backoffMultiplier, retryCount.toDouble())).toLong()
        val clampedDelay = baseDelay.coerceAtMost(maxDelayMs)
        // Add 10-20% jitter
        val jitter = (clampedDelay * Random.nextDouble(0.1, 0.2)).toLong()
        return clampedDelay + jitter
    }

    fun canRetry(retryCount: Int, isRetryable: Boolean): Boolean {
        return isRetryable && retryCount < maxRetries
    }
}
