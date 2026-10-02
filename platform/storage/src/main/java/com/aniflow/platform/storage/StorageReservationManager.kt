package com.aniflow.platform.storage

import com.aniflow.platform.storage.model.StorageLocationId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

data class StorageReservation(
    val reservationId: String,
    val taskId: String,
    val locationId: StorageLocationId,
    val reservedBytes: Long,
    val expiresAt: Instant
)

/**
 * StorageReservationManager (Sections 73, 74, 75, 148).
 * Manages concurrent logical storage reservations so parallel tasks do not simultaneously
 * exhaust available space before files are written.
 */
class StorageReservationManager(
    private val defaultTtlMillis: Long = 1000 * 60 * 60 // 1 hour TTL
) {
    private val mutex = Mutex()
    private val activeReservations = mutableMapOf<String, StorageReservation>()

    suspend fun reserve(
        taskId: String,
        locationId: StorageLocationId,
        bytes: Long,
        availableDiskBytes: Long
    ): Boolean = mutex.withLock {
        cleanExpired()
        val currentlyReserved = activeReservations.values
            .filter { it.locationId == locationId }
            .sumOf { it.reservedBytes }

        val remainingEffectiveSpace = availableDiskBytes - currentlyReserved

        if (remainingEffectiveSpace >= bytes) {
            val reservation = StorageReservation(
                reservationId = "res-${taskId}-${System.currentTimeMillis()}",
                taskId = taskId,
                locationId = locationId,
                reservedBytes = bytes,
                expiresAt = Instant.now().plusMillis(defaultTtlMillis)
            )
            activeReservations[taskId] = reservation
            true
        } else {
            false
        }
    }

    suspend fun release(taskId: String) = mutex.withLock {
        activeReservations.remove(taskId)
    }

    suspend fun getReservedBytes(locationId: StorageLocationId): Long = mutex.withLock {
        cleanExpired()
        activeReservations.values
            .filter { it.locationId == locationId }
            .sumOf { it.reservedBytes }
    }

    suspend fun clear() = mutex.withLock {
        activeReservations.clear()
    }

    private fun cleanExpired() {
        val now = Instant.now()
        activeReservations.entries.removeAll { it.value.expiresAt.isBefore(now) }
    }
}
