package com.aniflow.platform.monitoring

import java.util.concurrent.atomic.AtomicLong

/**
 * Enforces STEP 14 Section 37 (Release Monitoring) and Section 38 (Health Dashboard).
 *
 * Tracks client-side health metrics:
 * 1. Crash-free session rate
 * 2. Download completion rate
 * 3. Provider availability rate
 */
data class ReleaseHealthMetrics(
    val totalSessions: Long,
    val totalCrashes: Long,
    val crashFreeRatePercent: Double,
    val totalDownloadsStarted: Long,
    val totalDownloadsCompleted: Long,
    val downloadSuccessRatePercent: Double,
    val providerRequestsTotal: Long,
    val providerRequestsSuccessful: Long,
    val providerAvailabilityPercent: Double
)

class ReleaseHealthMonitor {

    private val sessionCount = AtomicLong(0)
    private val crashCount = AtomicLong(0)

    private val downloadsStarted = AtomicLong(0)
    private val downloadsCompleted = AtomicLong(0)

    private val providerRequests = AtomicLong(0)
    private val providerSuccesses = AtomicLong(0)

    fun recordSessionStart() = sessionCount.incrementAndGet()
    fun recordCrash() = crashCount.incrementAndGet()

    fun recordDownloadStarted() = downloadsStarted.incrementAndGet()
    fun recordDownloadCompleted() = downloadsCompleted.incrementAndGet()

    fun recordProviderRequest(isSuccess: Boolean) {
        providerRequests.incrementAndGet()
        if (isSuccess) providerSuccesses.incrementAndGet()
    }

    fun getHealthMetrics(): ReleaseHealthMetrics {
        val sessions = sessionCount.get().coerceAtLeast(1)
        val crashes = crashCount.get()
        val crashFree = ((sessions - crashes).coerceAtLeast(0).toDouble() / sessions.toDouble()) * 100.0

        val started = downloadsStarted.get().coerceAtLeast(1)
        val completed = downloadsCompleted.get()
        val downloadSuccess = (completed.toDouble() / started.toDouble()) * 100.0

        val reqs = providerRequests.get().coerceAtLeast(1)
        val succ = providerSuccesses.get()
        val providerAvail = (succ.toDouble() / reqs.toDouble()) * 100.0

        return ReleaseHealthMetrics(
            totalSessions = sessionCount.get(),
            totalCrashes = crashes,
            crashFreeRatePercent = crashFree.coerceIn(0.0, 100.0),
            totalDownloadsStarted = downloadsStarted.get(),
            totalDownloadsCompleted = completed,
            downloadSuccessRatePercent = downloadSuccess.coerceIn(0.0, 100.0),
            providerRequestsTotal = providerRequests.get(),
            providerRequestsSuccessful = succ,
            providerAvailabilityPercent = providerAvail.coerceIn(0.0, 100.0)
        )
    }
}
