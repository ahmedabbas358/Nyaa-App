package com.aniflow.domain.controlplane.service

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Enforces STEP 13 Section 12 (Automation Loop Protection) and Section 13 (Idempotency).
 *
 * Prevents catastrophic automation loops:
 * DownloadCompleted -> Automation -> Upgrade -> Download -> DownloadCompleted -> Automation...
 *
 * Guarantees idempotency when events are replayed or delivered multiple times.
 */
class AutomationLoopProtector(
    private val cooldownMillis: Long = 60_000L, // 60s cooldown per target item
    private val maxRecursionDepth: Int = 3,       // Max 3 cascades per root event
    private val maxActionsPerExecution: Int = 5  // Max 5 actions per automation run
) {

    sealed interface LoopCheckDecision {
        data class Allowed(val executionId: String, val depth: Int) : LoopCheckDecision
        data class BlockedLoop(val reason: String) : LoopCheckDecision
        data class BlockedCooldown(val remainingMillis: Long) : LoopCheckDecision
        data object SuppressedDuplicate : LoopCheckDecision
    }

    private val inFlightItemLocks = ConcurrentHashMap.newKeySet<String>()
    private val lastTriggerTimestamp = ConcurrentHashMap<String, Long>()
    private val executionDepthMap = ConcurrentHashMap<String, AtomicInteger>()
    private val executionActionCounter = ConcurrentHashMap<String, AtomicInteger>()

    /**
     * Checks if an automation trigger is allowed to proceed for a target item.
     *
     * @param rootExecutionId The correlation ID of the root event (generated if null).
     * @param targetItemKey Unique key identifying the target (e.g. "anime:42:ep:1").
     * @param triggerHash Hash of the event payload to verify idempotency.
     */
    fun evaluateTrigger(
        rootExecutionId: String? = null,
        targetItemKey: String,
        triggerHash: String
    ): LoopCheckDecision {
        val now = System.currentTimeMillis()

        // 1. Idempotency Check: Identical trigger received within short window is suppressed
        val dedupKey = "$targetItemKey:$triggerHash"
        val lastSeen = lastTriggerTimestamp[dedupKey]
        if (lastSeen != null && (now - lastSeen) < cooldownMillis) {
            return LoopCheckDecision.SuppressedDuplicate
        }

        // 2. Per-item concurrency lock: Do not allow two concurrent automations on same episode/anime
        if (!inFlightItemLocks.add(targetItemKey)) {
            return LoopCheckDecision.BlockedLoop("Concurrency lock active for item: $targetItemKey")
        }

        // 3. Cooldown check on item
        val lastItemExecution = lastTriggerTimestamp[targetItemKey]
        if (lastItemExecution != null && (now - lastItemExecution) < cooldownMillis) {
            inFlightItemLocks.remove(targetItemKey)
            return LoopCheckDecision.BlockedCooldown(cooldownMillis - (now - lastItemExecution))
        }

        // 4. Recursion depth check
        val execId = rootExecutionId ?: UUID.randomUUID().toString().take(8)
        val depthTracker = executionDepthMap.computeIfAbsent(execId) { AtomicInteger(0) }
        val currentDepth = depthTracker.incrementAndGet()

        if (currentDepth > maxRecursionDepth) {
            inFlightItemLocks.remove(targetItemKey)
            return LoopCheckDecision.BlockedLoop(
                "Automation recursion depth exceeded limit of $maxRecursionDepth for execution $execId"
            )
        }

        // 5. Action limit check
        val actionCounter = executionActionCounter.computeIfAbsent(execId) { AtomicInteger(0) }
        if (actionCounter.get() >= maxActionsPerExecution) {
            inFlightItemLocks.remove(targetItemKey)
            return LoopCheckDecision.BlockedLoop(
                "Max actions per execution ($maxActionsPerExecution) exceeded for execution $execId"
            )
        }

        // Record timestamp for idempotency & cooldown
        lastTriggerTimestamp[dedupKey] = now
        lastTriggerTimestamp[targetItemKey] = now

        return LoopCheckDecision.Allowed(executionId = execId, depth = currentDepth)
    }

    /**
     * Records an executed action and increments execution counter.
     */
    fun recordAction(executionId: String) {
        executionActionCounter.computeIfAbsent(executionId) { AtomicInteger(0) }.incrementAndGet()
    }

    /**
     * Releases in-flight concurrency lock after automation work finishes.
     */
    fun releaseLock(targetItemKey: String) {
        inFlightItemLocks.remove(targetItemKey)
    }

    /**
     * Resets state for testing purposes.
     */
    fun reset() {
        inFlightItemLocks.clear()
        lastTriggerTimestamp.clear()
        executionDepthMap.clear()
        executionActionCounter.clear()
    }
}
