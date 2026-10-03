package com.aniflow.platform.storage

import com.aniflow.platform.storage.model.StorageLocation
import com.aniflow.platform.storage.model.StorageLocationId
import com.aniflow.platform.storage.model.StorageLocationType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StorageHealthTest {

    @Test
    fun testStorageHealthMonitorThresholds() {
        val monitor = StorageHealthMonitor()
        val location = StorageLocation(
            id = StorageLocationId("sd-1"),
            type = StorageLocationType.UserSelectedTree,
            displayName = "SD Card",
            persistentReference = "content://..."
        )

        // 100GB total, 50GB free = 50% -> Healthy
        val health1 = monitor.evaluateStorage(location, 100_000_000_000L, 50_000_000_000L)
        assertEquals(StorageHealthLevel.Healthy, health1.level)

        // 100GB total, 15GB free = 15% -> Warning (<20%)
        val health2 = monitor.evaluateStorage(location, 100_000_000_000L, 15_000_000_000L)
        assertEquals(StorageHealthLevel.Warning, health2.level)

        // 100GB total, 8GB free = 8% -> Low (<10%)
        val health3 = monitor.evaluateStorage(location, 100_000_000_000L, 8_000_000_000L)
        assertEquals(StorageHealthLevel.Low, health3.level)

        // 100GB total, 3GB free = 3% -> Critical (<5%)
        val health4 = monitor.evaluateStorage(location, 100_000_000_000L, 3_000_000_000L)
        assertEquals(StorageHealthLevel.Critical, health4.level)
        assertFalse(health4.isWritable)
    }

    @Test
    fun testStorageReservationManager() = runBlocking {
        val reservationManager = StorageReservationManager()
        val locId = StorageLocationId("internal")

        // 10GB available on disk
        val available = 10_000_000_000L

        // Reserve 4GB for Task A -> Success
        val r1 = reservationManager.reserve("task-A", locId, 4_000_000_000L, available)
        assertTrue(r1)

        // Reserve 5GB for Task B -> Success (4 + 5 = 9 <= 10)
        val r2 = reservationManager.reserve("task-B", locId, 5_000_000_000L, available)
        assertTrue(r2)

        // Reserve 2GB for Task C -> Fails because only 1GB unreserved space remaining!
        val r3 = reservationManager.reserve("task-C", locId, 2_000_000_000L, available)
        assertFalse(r3)

        // Release Task A -> frees 4GB
        reservationManager.release("task-A")

        // Now Task C can be accommodated!
        val r4 = reservationManager.reserve("task-C", locId, 2_000_000_000L, available)
        assertTrue(r4)
    }
}
