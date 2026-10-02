package com.aniflow.download.core.storage

import com.aniflow.download.core.model.StorageContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Persistence delegate for crash-resilient storage reservations (Section 17).
 */
interface ReservationPersistenceStore {
    suspend fun saveReservation(taskId: String, reservedBytes: Long)
    suspend fun removeReservation(taskId: String)
    suspend fun loadAllReservations(): Map<String, Long>
    suspend fun clear()
}

/**
 * In-memory fallback or test store for reservation persistence.
 */
class InMemoryReservationStore : ReservationPersistenceStore {
    private val store = ConcurrentHashMap<String, Long>()

    override suspend fun saveReservation(taskId: String, reservedBytes: Long) {
        store[taskId] = reservedBytes
    }

    override suspend fun removeReservation(taskId: String) {
        store.remove(taskId)
    }

    override suspend fun loadAllReservations(): Map<String, Long> {
        return HashMap(store)
    }

    override suspend fun clear() {
        store.clear()
    }
}

/**
 * Storage Space Reservation Manager enforcing Step 22 Sections 13, 14, 15, 16, 17, 18, 129, 130.
 *
 * Responsibilities:
 * - Reserve: Atomically checks available space and claims bytes.
 * - Release: Atomically and idempotently frees bytes (safe against double-release).
 * - Adjust: Modifies an existing reservation (e.g. when actual file size is finalized).
 * - Transfer: Moves reservation ownership from Plan to Task or Task to File.
 * - Reconcile: Purges leaked or orphaned reservations after app crash or process restart.
 */
class StorageReservationManager(
    private val getPhysicalAvailableBytes: suspend () -> Long,
    private val safetyMarginBytes: Long = 500L * 1024 * 1024, // 500 MB default safety margin
    private val persistenceStore: ReservationPersistenceStore = InMemoryReservationStore()
) {

    sealed interface ReservationResult {
        data class Granted(
            val taskId: String,
            val reservedBytes: Long,
            val remainingAvailableBytes: Long,
            val context: StorageContext
        ) : ReservationResult

        data class Rejected(
            val taskId: String,
            val requestedBytes: Long,
            val availableBytes: Long,
            val deficitBytes: Long,
            val context: StorageContext
        ) : ReservationResult

        val isGranted: Boolean get() = this is Granted
    }

    private val mutex = Mutex()
    private val activeReservations = ConcurrentHashMap<String, Long>()

    /**
     * Initializes reservations from durable persistence after process restart (Section 17).
     */
    suspend fun initializeFromPersistence() = mutex.withLock {
        val persisted = persistenceStore.loadAllReservations()
        activeReservations.clear()
        activeReservations.putAll(persisted)
    }

    /**
     * Checks current storage context.
     */
    suspend fun getStorageContext(requiredBytes: Long = 0L): StorageContext = mutex.withLock {
        val physical = getPhysicalAvailableBytes()
        val reserved = activeReservations.values.sum().coerceAtLeast(0L)
        StorageContext(
            availableBytes = physical,
            reservedBytes = reserved,
            requiredBytes = requiredBytes,
            safetyMarginBytes = safetyMarginBytes
        )
    }

    /**
     * Attempts to atomically reserve storage bytes for a task (Section 14 & 15).
     */
    suspend fun reserve(taskId: String, requiredBytes: Long): ReservationResult = mutex.withLock {
        val physical = getPhysicalAvailableBytes()
        // If this task already has a reservation, account for it
        val currentTaskReservation = activeReservations[taskId] ?: 0L
        val otherReservations = (activeReservations.values.sum() - currentTaskReservation).coerceAtLeast(0L)

        val usableSpace = (physical - otherReservations - safetyMarginBytes).coerceAtLeast(0L)

        val context = StorageContext(
            availableBytes = physical,
            reservedBytes = otherReservations,
            requiredBytes = requiredBytes,
            safetyMarginBytes = safetyMarginBytes
        )

        if (requiredBytes <= usableSpace) {
            activeReservations[taskId] = requiredBytes
            persistenceStore.saveReservation(taskId, requiredBytes)
            val remaining = (usableSpace - requiredBytes).coerceAtLeast(0L)
            ReservationResult.Granted(
                taskId = taskId,
                reservedBytes = requiredBytes,
                remainingAvailableBytes = remaining,
                context = context
            )
        } else {
            val deficit = requiredBytes - usableSpace
            ReservationResult.Rejected(
                taskId = taskId,
                requestedBytes = requiredBytes,
                availableBytes = usableSpace,
                deficitBytes = deficit,
                context = context
            )
        }
    }

    /**
     * Idempotently releases a reservation (Sections 16, 129, 130).
     * Calling release twice has no ill effects and will never drive reservation negative.
     */
    suspend fun release(taskId: String): Long = mutex.withLock {
        val removed = activeReservations.remove(taskId) ?: 0L
        if (removed > 0L) {
            persistenceStore.removeReservation(taskId)
        }
        removed
    }

    /**
     * Adjusts an existing reservation (Section 14).
     */
    suspend fun adjust(taskId: String, newBytes: Long): ReservationResult = mutex.withLock {
        val physical = getPhysicalAvailableBytes()
        val otherReservations = (activeReservations.values.sum() - (activeReservations[taskId] ?: 0L)).coerceAtLeast(0L)
        val usableSpace = (physical - otherReservations - safetyMarginBytes).coerceAtLeast(0L)

        val context = StorageContext(
            availableBytes = physical,
            reservedBytes = otherReservations,
            requiredBytes = newBytes,
            safetyMarginBytes = safetyMarginBytes
        )

        if (newBytes <= usableSpace) {
            activeReservations[taskId] = newBytes
            persistenceStore.saveReservation(taskId, newBytes)
            val remaining = (usableSpace - newBytes).coerceAtLeast(0L)
            ReservationResult.Granted(taskId, newBytes, remaining, context)
        } else {
            val deficit = newBytes - usableSpace
            ReservationResult.Rejected(taskId, newBytes, usableSpace, deficit, context)
        }
    }

    /**
     * Transfers reservation from one ID to another (e.g. Plan to Task) (Section 14).
     */
    suspend fun transfer(fromTaskId: String, toTaskId: String): Boolean = mutex.withLock {
        val existing = activeReservations.remove(fromTaskId) ?: return@withLock false
        activeReservations[toTaskId] = existing
        persistenceStore.removeReservation(fromTaskId)
        persistenceStore.saveReservation(toTaskId, existing)
        true
    }

    /**
     * Reconciles reservations against active tasks (Sections 17 & 96).
     * Any reservation for a task that is no longer active is purged.
     */
    suspend fun reconcile(activeTaskIds: Set<String>): Set<String> = mutex.withLock {
        val orphaned = activeReservations.keys.filter { it !in activeTaskIds }.toSet()
        orphaned.forEach { orphanId ->
            activeReservations.remove(orphanId)
            persistenceStore.removeReservation(orphanId)
        }
        orphaned
    }

    suspend fun getTotalReservedBytes(): Long = mutex.withLock {
        activeReservations.values.sum().coerceAtLeast(0L)
    }

    suspend fun getReservedBytes(taskId: String): Long = mutex.withLock {
        activeReservations[taskId] ?: 0L
    }

    suspend fun clear() = mutex.withLock {
        activeReservations.clear()
        persistenceStore.clear()
    }
}
