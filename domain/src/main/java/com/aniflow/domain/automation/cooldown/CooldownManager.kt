package com.aniflow.domain.automation.cooldown

import com.aniflow.domain.automation.model.AutomationExecutionKey
import com.aniflow.domain.automation.model.CooldownPolicy
import com.aniflow.domain.automation.model.CooldownScope
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Cooldown and Execution Lock Manager (Section 36, 37, 38, 96, 97).
 * Prevents rule spam, infinite automation loops, and duplicate actions across crashes.
 */
class CooldownManager {

    // Key -> Expiration Instant
    private val cooldowns = ConcurrentHashMap<String, Instant>()
    // Lock Key -> Expiration Instant (Expirable locks prevent stale deadlocks after process death)
    private val activeLocks = ConcurrentHashMap<String, Instant>()

    fun isCoolingDown(
        key: AutomationExecutionKey,
        policy: CooldownPolicy,
        now: Instant = Instant.now()
    ): Boolean {
        val cooldownKey = buildCooldownKey(key, policy.scope)
        val expireTime = cooldowns[cooldownKey] ?: return false
        return if (now.isBefore(expireTime)) {
            true
        } else {
            cooldowns.remove(cooldownKey)
            false
        }
    }

    fun recordExecution(
        key: AutomationExecutionKey,
        policy: CooldownPolicy,
        now: Instant = Instant.now()
    ) {
        val cooldownKey = buildCooldownKey(key, policy.scope)
        cooldowns[cooldownKey] = now.plusSeconds(policy.durationSeconds)
    }

    fun tryAcquireLock(
        lockKey: String,
        lockDurationSeconds: Long = 300L, // 5 minutes default lease
        now: Instant = Instant.now()
    ): Boolean {
        val currentExpiry = activeLocks[lockKey]
        if (currentExpiry != null && now.isBefore(currentExpiry)) {
            return false // Active lease held
        }
        activeLocks[lockKey] = now.plusSeconds(lockDurationSeconds)
        return true
    }

    fun releaseLock(lockKey: String) {
        activeLocks.remove(lockKey)
    }

    fun clearStaleLocks(now: Instant = Instant.now()) {
        val iterator = activeLocks.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now.isAfter(entry.value)) {
                iterator.remove()
            }
        }
    }

    private fun buildCooldownKey(key: AutomationExecutionKey, scope: CooldownScope): String {
        return when (scope) {
            CooldownScope.PerRule -> "rule:${key.ruleId.value}"
            CooldownScope.PerAnime -> "anime:${key.targetIdentity.substringBefore('_')}"
            CooldownScope.PerEpisode -> "ep:${key.targetIdentity}"
            CooldownScope.PerRelease -> "rel:${key.triggerIdentity}"
            CooldownScope.Global -> "global"
        }
    }
}
