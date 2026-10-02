package com.aniflow.testing

import com.aniflow.core.network.resilience.ProviderCircuitBreaker
import com.aniflow.domain.controlplane.service.AutomationLoopProtector
import com.aniflow.download.core.model.DownloadTaskState
import com.aniflow.download.core.persistence.ProgressPersistenceThrottler
import com.aniflow.download.core.statemachine.DownloadStateMachine
import com.aniflow.download.core.storage.StorageReservationManager
import com.aniflow.testing.failure.FailurePoint
import com.aniflow.testing.failure.TestFailureInjector
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Enforces STEP 13:
 * - Section 12 (Automation Loop Protection)
 * - Section 13 (Idempotency)
 * - Section 20 & 21 (Download State Machine Audit & Invalid State Tests)
 * - Section 25 (Storage Space Reservation)
 * - Section 37 (Progress Persistence Throttling)
 * - Section 43 (Circuit Breaker)
 * - Section 76 & 77 (Chaos Testing & Fake Failure Injection)
 */
class ReliabilityAndChaosTests {

    @Test
    fun testAutomationLoopProtectionAndDepthCeiling() {
        val protector = AutomationLoopProtector(cooldownMillis = 10_000L, maxRecursionDepth = 3)
        val rootExecId = "exec-test-1"

        // Iteration 1: Allowed (depth 1)
        val res1 = protector.evaluateTrigger(rootExecId, "item-1", "hash-1")
        assertTrue("Depth 1 should be allowed", res1 is AutomationLoopProtector.LoopCheckDecision.Allowed)
        protector.releaseLock("item-1")

        // Iteration 2: Allowed (depth 2)
        val res2 = protector.evaluateTrigger(rootExecId, "item-2", "hash-2")
        assertTrue("Depth 2 should be allowed", res2 is AutomationLoopProtector.LoopCheckDecision.Allowed)
        protector.releaseLock("item-2")

        // Iteration 3: Allowed (depth 3)
        val res3 = protector.evaluateTrigger(rootExecId, "item-3", "hash-3")
        assertTrue("Depth 3 should be allowed", res3 is AutomationLoopProtector.LoopCheckDecision.Allowed)
        protector.releaseLock("item-3")

        // Iteration 4: Exceeds max depth (depth 4) -> MUST BE BLOCKED
        val res4 = protector.evaluateTrigger(rootExecId, "item-4", "hash-4")
        assertTrue("Depth 4 must be blocked as a detected loop", res4 is AutomationLoopProtector.LoopCheckDecision.BlockedLoop)
    }

    @Test
    fun testAutomationTriggerIdempotency() {
        val protector = AutomationLoopProtector(cooldownMillis = 10_000L)
        val target = "anime:101:ep:1"
        val triggerHash = "release-1080p-v1"

        // First event accepted
        val first = protector.evaluateTrigger(targetItemKey = target, triggerHash = triggerHash)
        assertTrue("First event allowed", first is AutomationLoopProtector.LoopCheckDecision.Allowed)
        protector.releaseLock(target)

        // Repeat identical trigger 4 times -> all 4 must be suppressed as duplicate
        for (i in 1..4) {
            val duplicate = protector.evaluateTrigger(targetItemKey = target, triggerHash = triggerHash)
            assertEquals("Duplicate event $i must be suppressed", AutomationLoopProtector.LoopCheckDecision.SuppressedDuplicate, duplicate)
        }
    }

    @Test
    fun testDownloadStateMachine_ValidTransitions() {
        // Pending -> Queued -> Starting -> Downloading -> Verifying -> Organizing -> Completed
        var state = DownloadTaskState.Pending
        state = DownloadStateMachine.transition("task-1", state, DownloadTaskState.Queued)
        state = DownloadStateMachine.transition("task-1", state, DownloadTaskState.Starting)
        state = DownloadStateMachine.transition("task-1", state, DownloadTaskState.Downloading)
        state = DownloadStateMachine.transition("task-1", state, DownloadTaskState.Verifying)
        state = DownloadStateMachine.transition("task-1", state, DownloadTaskState.Organizing)
        state = DownloadStateMachine.transition("task-1", state, DownloadTaskState.Completed)

        assertEquals(DownloadTaskState.Completed, state)
    }

    @Test
    fun testDownloadStateMachine_IllegalTransitionsRejected() {
        // 1. Completed -> Downloading (Strictly forbidden)
        try {
            DownloadStateMachine.transition("task-1", DownloadTaskState.Completed, DownloadTaskState.Downloading)
            fail("Completed -> Downloading must throw IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("Illegal state transition"))
        }

        // 2. Downloading -> Completed directly without Verifying/Organizing (Strictly forbidden)
        try {
            DownloadStateMachine.transition("task-2", DownloadTaskState.Downloading, DownloadTaskState.Completed)
            fail("Downloading -> Completed directly must throw IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("Illegal state transition"))
        }

        // 3. Cancelled -> Starting (Strictly forbidden)
        try {
            DownloadStateMachine.transition("task-3", DownloadTaskState.Cancelled, DownloadTaskState.Starting)
            fail("Cancelled -> Starting must throw IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("Illegal state transition"))
        }
    }

    @Test
    fun testStorageReservationPreventsOverAllocation() = runBlocking {
        // Physical available = 10 GB, Safety margin = 500 MB -> Usable = 9.5 GB
        val totalSpace = 10L * 1024 * 1024 * 1024
        val reservationManager = StorageReservationManager(
            getPhysicalAvailableBytes = { totalSpace },
            safetyMarginBytes = 500L * 1024 * 1024
        )

        val taskASize = 6L * 1024 * 1024 * 1024 // 6 GB
        val taskBSize = 6L * 1024 * 1024 * 1024 // 6 GB

        // Task A tries to reserve 6 GB -> Granted
        val resA = reservationManager.tryReserve("task-A", taskASize)
        assertTrue("Task A should be granted 6 GB", resA is StorageReservationManager.ReservationResult.Granted)

        // Task B tries to reserve 6 GB concurrently -> Rejected (only 3.5 GB left)
        val resB = reservationManager.tryReserve("task-B", taskBSize)
        assertTrue("Task B must be rejected to prevent out-of-disk failure", resB is StorageReservationManager.ReservationResult.Rejected)

        // When Task A finishes or cancels, space is freed
        reservationManager.releaseReservation("task-A")

        // Now Task B can successfully reserve
        val resB2 = reservationManager.tryReserve("task-B", taskBSize)
        assertTrue("Task B is granted space after Task A release", resB2 is StorageReservationManager.ReservationResult.Granted)
    }

    @Test
    fun testCircuitBreakerStateTransitions() = runBlocking {
        val breaker = ProviderCircuitBreaker(providerId = "nyaa", failureThreshold = 3, openCooldownMillis = 500L)

        assertEquals(ProviderCircuitBreaker.State.Healthy, breaker.state)
        assertTrue(breaker.canExecute())

        // 1st failure -> Degraded
        breaker.recordFailure()
        // 2nd failure -> Degraded
        breaker.recordFailure()
        assertEquals(ProviderCircuitBreaker.State.Degraded, breaker.state)

        // 3rd failure -> Open
        breaker.recordFailure()
        assertEquals(ProviderCircuitBreaker.State.Open, breaker.state)
        assertFalse("Circuit is OPEN: requests must be blocked immediately", breaker.canExecute())

        // 429 Rate limit immediate transition
        breaker.reset()
        breaker.recordFailure(statusCode = 429)
        assertEquals(ProviderCircuitBreaker.State.RateLimited, breaker.state)
    }

    @Test
    fun testProgressPersistenceThrottler() {
        val throttler = ProgressPersistenceThrottler(minIntervalMillis = 1000L, minPercentDelta = 0.05f)

        // Initial write is allowed
        assertTrue(throttler.shouldPersist("task-1", DownloadTaskState.Downloading, 0, 100_000_000L))

        // Tiny update 10ms later with 0.1% change should be throttled (false)
        assertFalse(throttler.shouldPersist("task-1", DownloadTaskState.Downloading, 100_000L, 100_000_000L))

        // State change to Paused must persist immediately
        assertTrue(throttler.shouldPersist("task-1", DownloadTaskState.Paused, 100_000L, 100_000_000L))
    }

    @Test
    fun testFailureInjector() {
        val injector = TestFailureInjector()
        assertFalse(injector.shouldFail(FailurePoint.ProviderTimeout))

        injector.arm(FailurePoint.ProviderTimeout, times = 2)
        assertTrue(injector.shouldFail(FailurePoint.ProviderTimeout))
        assertTrue(injector.shouldFail(FailurePoint.ProviderTimeout))
        assertFalse(injector.shouldFail(FailurePoint.ProviderTimeout)) // Depleted
    }
}
