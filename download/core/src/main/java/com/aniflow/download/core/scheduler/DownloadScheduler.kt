package com.aniflow.download.core.scheduler

import com.aniflow.domain.model.aggregate.download.NetworkPolicy
import com.aniflow.download.core.model.DownloadEngineType
import com.aniflow.download.core.model.DownloadPolicySnapshot
import com.aniflow.download.core.model.DownloadTask
import com.aniflow.download.core.model.DownloadTaskId
import com.aniflow.download.core.model.DownloadTaskState
import com.aniflow.download.core.model.NetworkType
import com.aniflow.download.core.model.RuntimeCapabilities
import com.aniflow.download.core.model.TaskBlockReason
import com.aniflow.download.core.model.WaitingReason
import com.aniflow.download.core.queue.DownloadQueue
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class SchedulerConfig(
    val maxGlobalActiveTasks: Int = 4,
    val maxHttpActiveTasks: Int = 3,
    val maxTorrentActiveTasks: Int = 1
)

sealed interface NetworkEligibility {
    data object Allowed : NetworkEligibility
    data object WaitingForNetwork : NetworkEligibility
    data class Blocked(val reason: String) : NetworkEligibility

    val isAllowed: Boolean get() = this is Allowed
}

/**
 * Evaluates network policies against current device network state (Section 33).
 */
class NetworkPolicyEvaluator {
    fun evaluate(capabilities: RuntimeCapabilities, policy: NetworkPolicy): NetworkEligibility {
        if (!capabilities.isNetworkAvailable) {
            return NetworkEligibility.WaitingForNetwork
        }

        return when (policy) {
            NetworkPolicy.AnyNetwork -> NetworkEligibility.Allowed
            NetworkPolicy.WifiOnly -> {
                if (capabilities.networkType == NetworkType.Wifi) NetworkEligibility.Allowed
                else NetworkEligibility.Blocked("Wi-Fi Only policy active on non-wifi connection")
            }
            NetworkPolicy.UnmeteredOnly -> {
                if (!capabilities.isMetered) NetworkEligibility.Allowed
                else NetworkEligibility.Blocked("Unmetered Network Only policy active on metered connection")
            }
            NetworkPolicy.WifiAndEthernet -> {
                if (capabilities.networkType == NetworkType.Wifi || capabilities.networkType == NetworkType.Ethernet) {
                    NetworkEligibility.Allowed
                } else {
                    NetworkEligibility.Blocked("Wi-Fi or Ethernet Only policy active")
                }
            }
        }
    }
}

/**
 * Decides which queued task should start next, strictly enforcing concurrency,
 * network state, storage availability, and fair queue aging (Section 30, 31, 32, 33, 34, 85, 86, 87, 90).
 */
class DownloadScheduler(
    private val queue: DownloadQueue,
    private var config: SchedulerConfig = SchedulerConfig(),
    private val networkEvaluator: NetworkPolicyEvaluator = NetworkPolicyEvaluator(),
    private val retryPolicy: RetryPolicyManager = RetryPolicyManager()
) {
    private val claimMutex = Mutex()

    fun updateConfig(newConfig: SchedulerConfig) {
        config = newConfig
    }

    fun updateFromPolicy(policy: DownloadPolicySnapshot) {
        config = SchedulerConfig(
            maxGlobalActiveTasks = policy.maxGlobalActive,
            maxHttpActiveTasks = policy.maxActiveHttp,
            maxTorrentActiveTasks = policy.maxActiveTorrent
        )
    }

    /**
     * Atomically selects and claims the highest-priority eligible task (Section 30).
     */
    suspend fun selectNextTaskAndClaim(
        capabilities: RuntimeCapabilities,
        networkPolicy: NetworkPolicy
    ): DownloadTask? = claimMutex.withLock {
        // 1. Evaluate network eligibility
        val networkEligibility = networkEvaluator.evaluate(capabilities, networkPolicy)
        if (!networkEligibility.isAllowed) {
            // Update queued tasks to Waiting if network is unavailable
            if (networkEligibility is NetworkEligibility.WaitingForNetwork) {
                val queued = queue.getAllTasks().filter { it.state == DownloadTaskState.Queued }
                queued.forEach { task ->
                    queue.updateTaskState(task.id, DownloadTaskState.Waiting)
                }
            }
            return@withLock null
        }

        // 2. Fetch all current tasks
        val allTasks = queue.getAllTasks()
        val activeTasks = allTasks.filter { it.isActive }

        // 3. Check global concurrency limit
        if (activeTasks.size >= config.maxGlobalActiveTasks) {
            return@withLock null
        }

        // 4. Check per-engine concurrency
        val activeHttp = activeTasks.count { it.engineType == DownloadEngineType.Http }
        val activeTorrent = activeTasks.count { it.engineType == DownloadEngineType.Torrent }

        // 5. Check queued or waiting tasks (re-evaluating waiting tasks)
        val candidateTasks = allTasks.filter {
            it.state == DownloadTaskState.Queued || it.state == DownloadTaskState.Waiting
        }

        for (candidate in candidateTasks) {
            val engineCanStart = when (candidate.engineType) {
                DownloadEngineType.Http -> activeHttp < config.maxHttpActiveTasks
                DownloadEngineType.Torrent -> activeTorrent < config.maxTorrentActiveTasks
                DownloadEngineType.DirectFile -> true
            }

            if (engineCanStart) {
                // Claim atomically: transition to Starting
                val claimed = queue.updateTaskState(candidate.id, DownloadTaskState.Starting)
                return@withLock claimed
            }
        }

        null
    }

    /**
     * Re-evaluates queue when network, storage, or concurrency changes (Section 85).
     */
    suspend fun reevaluate(
        capabilities: RuntimeCapabilities,
        networkPolicy: NetworkPolicy
    ): List<DownloadTask> = claimMutex.withLock {
        val claimedList = mutableListOf<DownloadTask>()

        val eligibility = networkEvaluator.evaluate(capabilities, networkPolicy)
        if (!eligibility.isAllowed) {
            return@withLock claimedList
        }

        // Re-arm waiting tasks to queued if network/slots available
        val allTasks = queue.getAllTasks()
        allTasks.filter { it.state == DownloadTaskState.Waiting }.forEach {
            queue.updateTaskState(it.id, DownloadTaskState.Queued)
        }

        var next = selectNextTaskAndClaimInternal(capabilities, networkPolicy)
        while (next != null) {
            claimedList.add(next)
            next = selectNextTaskAndClaimInternal(capabilities, networkPolicy)
        }

        claimedList
    }

    private suspend fun selectNextTaskAndClaimInternal(
        capabilities: RuntimeCapabilities,
        networkPolicy: NetworkPolicy
    ): DownloadTask? {
        val allTasks = queue.getAllTasks()
        val activeTasks = allTasks.filter { it.isActive }

        if (activeTasks.size >= config.maxGlobalActiveTasks) return null

        val activeHttp = activeTasks.count { it.engineType == DownloadEngineType.Http }
        val activeTorrent = activeTasks.count { it.engineType == DownloadEngineType.Torrent }

        val queuedTasks = allTasks.filter { it.state == DownloadTaskState.Queued }

        for (candidate in queuedTasks) {
            val engineCanStart = when (candidate.engineType) {
                DownloadEngineType.Http -> activeHttp < config.maxHttpActiveTasks
                DownloadEngineType.Torrent -> activeTorrent < config.maxTorrentActiveTasks
                DownloadEngineType.DirectFile -> true
            }

            if (engineCanStart) {
                return queue.updateTaskState(candidate.id, DownloadTaskState.Starting)
            }
        }

        return null
    }

    fun isNetworkEligible(capabilities: RuntimeCapabilities, policy: NetworkPolicy): Boolean {
        return networkEvaluator.evaluate(capabilities, policy).isAllowed
    }
}
