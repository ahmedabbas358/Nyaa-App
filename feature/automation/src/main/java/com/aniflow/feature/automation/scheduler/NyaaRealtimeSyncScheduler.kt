package com.aniflow.feature.automation.scheduler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import com.aniflow.core.common.result.AniFlowResult
import com.aniflow.domain.controlplane.models.ComparisonExpression
import com.aniflow.domain.controlplane.models.ComparisonOperator
import com.aniflow.domain.controlplane.models.SearchExpression
import com.aniflow.domain.controlplane.models.SearchField
import com.aniflow.domain.valueobject.SearchFilters
import com.aniflow.domain.valueobject.EpisodeRange
import com.aniflow.domain.model.SearchRequest
import com.aniflow.domain.model.aggregate.release.ReleaseSource
import com.aniflow.domain.usecase.QueueDownloadUseCase
import com.aniflow.domain.usecase.SearchReleasesCoordinatorUseCase

/**
 * Sync Frequency Options (Sub-Minute down to seconds or minutes).
 */
enum class SyncInterval(val seconds: Long, val displayName: String) {
    ULTRA_FAST_30S(30L, "30 Seconds (Ultra-Fast)"),
    FAST_1M(60L, "1 Minute"),
    STANDARD_2M(120L, "2 Minutes"),
    RELAXED_5M(300L, "5 Minutes"),
    POWER_SAVER_15M(900L, "15 Minutes")
}

/**
 * Target watcher entity for tracking anime or uploaders.
 */
data class NyaaWatcherTarget(
    val id: String,
    val query: String,
    val preferredUploader: String? = null,
    val preferredResolution: String? = "1080p",
    val autoDownload: Boolean = false,
    val destinationFolder: String = "Anime"
)

/**
 * Event emitted when a sync cycle discovers new releases.
 */
sealed interface NyaaSyncEvent {
    data class NewReleaseFound(
        val watcherId: String,
        val animeTitle: String,
        val episodeNumber: String?,
        val uploader: String,
        val resolution: String,
        val magnetUri: String,
        val sizeFormatted: String,
        val autoQueued: Boolean
    ) : NyaaSyncEvent

    data class SyncCycleCompleted(
        val timestamp: Instant,
        val totalWatchersChecked: Int,
        val newReleasesCount: Int
    ) : NyaaSyncEvent

    data class SyncFailure(
        val watcherId: String,
        val errorMessage: String
    ) : NyaaSyncEvent
}

/**
 * Status of the Realtime Periodic Sync Engine.
 */
data class NyaaSyncEngineState(
    val isRunning: Boolean = false,
    val interval: SyncInterval = SyncInterval.FAST_1M,
    val activeWatchersCount: Int = 0,
    val lastSyncTimestamp: Instant? = null,
    val totalReleasesDiscovered: Int = 0,
    val isWifiOnly: Boolean = true
)

/**
 * High-performance, sub-minute real-time sync scheduler for Nyaa.si.
 * Coordinates periodic polling, deduplication, and automated batching.
 */
class NyaaRealtimeSyncScheduler(
    private val searchCoordinatorUseCase: SearchReleasesCoordinatorUseCase? = null,
    private val queueDownloadUseCase: QueueDownloadUseCase? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private var syncJob: Job? = null
    private val watchers = ConcurrentHashMap<String, NyaaWatcherTarget>()
    private val seenReleaseGuids = ConcurrentHashMap.newKeySet<String>()

    private val _engineState = MutableStateFlow(NyaaSyncEngineState())
    val engineState: StateFlow<NyaaSyncEngineState> = _engineState.asStateFlow()

    private val _countdownSeconds = MutableStateFlow(60L)
    val countdownSeconds: StateFlow<Long> = _countdownSeconds.asStateFlow()

    private val _events = MutableSharedFlow<NyaaSyncEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<NyaaSyncEvent> = _events.asSharedFlow()

    /**
     * Set the periodic polling interval (e.g. 30s, 1m, 2m, etc.)
     */
    fun setSyncInterval(interval: SyncInterval) {
        _engineState.value = _engineState.value.copy(interval = interval)
        _countdownSeconds.value = interval.seconds
        if (_engineState.value.isRunning) {
            start() // Restart with new interval
        }
    }

    /**
     * Register or update an anime/uploader watcher.
     */
    fun addOrUpdateWatcher(target: NyaaWatcherTarget) {
        watchers[target.id] = target
        _engineState.value = _engineState.value.copy(activeWatchersCount = watchers.size)
    }

    /**
     * Remove an existing watcher.
     */
    fun removeWatcher(id: String) {
        watchers.remove(id)
        _engineState.value = _engineState.value.copy(activeWatchersCount = watchers.size)
    }

    /**
     * Start the real-time periodic sync loop (updating countdown second-by-second).
     */
    fun start() {
        syncJob?.cancel()
        _engineState.value = _engineState.value.copy(isRunning = true)
        _countdownSeconds.value = _engineState.value.interval.seconds

        syncJob = scope.launch {
            // Run an initial sync immediately on start
            executeSyncCycle()

            while (isActive) {
                delay(1000L)
                val current = _countdownSeconds.value
                if (current <= 1L) {
                    _countdownSeconds.value = _engineState.value.interval.seconds
                    executeSyncCycle()
                } else {
                    _countdownSeconds.value = current - 1L
                }
            }
        }
    }

    /**
     * Stop the periodic sync engine.
     */
    fun stop() {
        syncJob?.cancel()
        syncJob = null
        _engineState.value = _engineState.value.copy(isRunning = false)
    }

    /**
     * Force an immediate on-demand sync run.
     */
    fun triggerImmediateSync() {
        scope.launch {
            executeSyncCycle()
            _countdownSeconds.value = _engineState.value.interval.seconds
        }
    }

    private suspend fun executeSyncCycle() {
        val now = Instant.now()
        var newReleasesCount = 0

        // If no specific watchers configured, monitor general Nyaa releases
        val targetsToCheck = if (watchers.isEmpty()) {
            listOf(
                NyaaWatcherTarget(
                    id = "general_anime",
                    query = "",
                    preferredResolution = "1080p",
                    autoDownload = false,
                    destinationFolder = "Anime/Downloads"
                )
            )
        } else {
            watchers.values.toList()
        }

        for (target in targetsToCheck) {
            try {
                if (searchCoordinatorUseCase != null) {
                    val searchExpr = SearchExpression(
                        root = ComparisonExpression(
                            field = SearchField.Anime,
                            operator = ComparisonOperator.Contains,
                            value = target.query
                        )
                    )
                    val searchReq = SearchRequest(
                        query = searchExpr,
                        page = 1,
                        filters = SearchFilters(trustedOnly = false, excludeRemakes = false)
                    )
                    searchCoordinatorUseCase(searchReq).collect { result ->
                        if (result is AniFlowResult.Success) {
                            for (rel in result.data.items) {
                                val hash = (rel.source as? ReleaseSource.Torrent)?.infoHash?.hexString ?: rel.id.value
                                if (!seenReleaseGuids.contains(hash)) {
                                    seenReleaseGuids.add(hash)
                                    newReleasesCount++

                                    val magnet = (rel.source as? ReleaseSource.Torrent)?.magnetUri?.rawValue
                                    var autoQueued = false

                                    if (target.autoDownload && !magnet.isNullOrBlank()) {
                                        val uploaderMatches = target.preferredUploader == null ||
                                            (rel.uploader?.name?.contains(target.preferredUploader, ignoreCase = true) == true)
                                        val resMatches = target.preferredResolution == null ||
                                            (rel.technical.resolution?.displayName?.contains(target.preferredResolution, ignoreCase = true) == true)

                                        if (uploaderMatches && resMatches) {
                                            queueDownloadUseCase?.queueTorrent(
                                                title = rel.title,
                                                magnetUri = magnet,
                                                releaseId = rel.id.value,
                                                destinationFolder = target.destinationFolder
                                            )
                                            autoQueued = true
                                        }
                                    }

                                    _events.emit(
                                        NyaaSyncEvent.NewReleaseFound(
                                            watcherId = target.id,
                                            animeTitle = rel.animeIdentity?.rawTitle ?: rel.title,
                                            episodeNumber = when (val ep = rel.episodeRange) {
                                                is EpisodeRange.Single -> ep.number.major.toString()
                                                is EpisodeRange.Range -> "${ep.start.major}-${ep.end.major}"
                                                else -> null
                                            },
                                            uploader = rel.uploader?.name ?: "Nyaa",
                                            resolution = rel.technical.resolution?.displayName ?: "1080p",
                                            magnetUri = magnet ?: "",
                                            sizeFormatted = rel.availability.size?.bytes?.let { bytes ->
                                                val mb = bytes / (1024.0 * 1024.0)
                                                if (mb >= 1024) String.format("%.2f GB", mb / 1024.0) else String.format("%.1f MB", mb)
                                            } ?: "Unknown",
                                            autoQueued = autoQueued
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _events.emit(NyaaSyncEvent.SyncFailure(target.id, e.message ?: "Unknown sync error"))
            }
        }

        _engineState.value = _engineState.value.copy(
            lastSyncTimestamp = now,
            totalReleasesDiscovered = _engineState.value.totalReleasesDiscovered + newReleasesCount
        )

        _events.emit(
            NyaaSyncEvent.SyncCycleCompleted(
                timestamp = now,
                totalWatchersChecked = targetsToCheck.size,
                newReleasesCount = newReleasesCount
            )
        )
    }
}
