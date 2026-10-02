package com.aniflow.platform.storage

import com.aniflow.platform.storage.model.StorageLocation
import com.aniflow.platform.storage.model.StorageLocationId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class StorageHealthLevel {
    Healthy,   // >= 20% free
    Warning,   // < 20% free -> Notify user
    Low,       // < 10% free -> Prevent new large downloads
    Critical   // < 5% free  -> Pause new queue tasks
}

data class StorageHealthThresholds(
    val warningPercent: Double = 20.0,
    val lowPercent: Double = 10.0,
    val criticalPercent: Double = 5.0,
    val safetyMarginBytes: Long = 1024 * 1024 * 500L // 500 MB minimum margin
)

data class StorageVolumeHealth(
    val locationId: StorageLocationId,
    val displayName: String,
    val totalBytes: Long,
    val availableBytes: Long,
    val usedBytes: Long,
    val freePercent: Double,
    val level: StorageHealthLevel,
    val isWritable: Boolean
)

/**
 * StorageHealthMonitor (Sections 68, 69, 70, 71, 79, 144, 145, 146).
 * Actively assesses storage status, alerts on low storage, and advises the download scheduler.
 */
class StorageHealthMonitor(
    private val thresholds: StorageHealthThresholds = StorageHealthThresholds()
) {
    private val _healthStates = MutableStateFlow<Map<StorageLocationId, StorageVolumeHealth>>(emptyMap())
    val healthStates: StateFlow<Map<StorageLocationId, StorageVolumeHealth>> = _healthStates.asStateFlow()

    fun evaluateStorage(location: StorageLocation, totalBytes: Long, availableBytes: Long): StorageVolumeHealth {
        val usedBytes = (totalBytes - availableBytes).coerceAtLeast(0L)
        val freePercent = if (totalBytes > 0) {
            (availableBytes.toDouble() / totalBytes.toDouble()) * 100.0
        } else {
            100.0
        }

        val level = when {
            availableBytes < thresholds.safetyMarginBytes || freePercent <= thresholds.criticalPercent -> StorageHealthLevel.Critical
            freePercent <= thresholds.lowPercent -> StorageHealthLevel.Low
            freePercent <= thresholds.warningPercent -> StorageHealthLevel.Warning
            else -> StorageHealthLevel.Healthy
        }

        val health = StorageVolumeHealth(
            locationId = location.id,
            displayName = location.displayName,
            totalBytes = totalBytes,
            availableBytes = availableBytes,
            usedBytes = usedBytes,
            freePercent = freePercent,
            level = level,
            isWritable = location.isWritable && level != StorageHealthLevel.Critical
        )

        _healthStates.value = _healthStates.value + (location.id to health)
        return health
    }

    fun canAcceptNewDownload(locationId: StorageLocationId, requiredBytes: Long): Boolean {
        val health = _healthStates.value[locationId] ?: return true
        if (health.level == StorageHealthLevel.Critical) return false
        val projectedAvailable = health.availableBytes - requiredBytes
        return projectedAvailable >= thresholds.safetyMarginBytes
    }
}
