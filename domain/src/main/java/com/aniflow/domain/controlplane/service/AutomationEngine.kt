package com.aniflow.domain.controlplane.service

import com.aniflow.domain.controlplane.models.AdvancedRule
import com.aniflow.domain.controlplane.models.AdvancedRuleAction
import com.aniflow.domain.controlplane.models.AutomationAuditLog
import com.aniflow.domain.controlplane.models.AutomationSafetyLevel
import com.aniflow.domain.controlplane.models.AutomationSimulationResult
import com.aniflow.domain.controlplane.models.AutomationTrigger
import com.aniflow.domain.controlplane.models.NetworkPolicyType
import java.util.UUID

data class AutomationPlan(
    val trigger: AutomationTrigger,
    val candidateTitles: List<String>,
    val selectedReleases: List<String>,
    val actionsToExecute: List<AdvancedRuleAction>,
    val safetyLevel: AutomationSafetyLevel,
    val isBlockedBySafety: Boolean,
    val safetyReason: String?,
    val auditLog: AutomationAuditLog
)

/**
 * AutomationEngine (Sections 19, 20, 21, 22, 23, 42, 45, 46).
 * Coordinates trigger evaluation, safety gates, idempotency guards, dry run simulations,
 * and deterministic plan generation.
 */
class AutomationEngine(
    private val ruleEvaluator: RuleTreeEvaluator = RuleTreeEvaluator()
) {

    private val executedItemLocks = mutableSetOf<String>()

    fun simulate(
        trigger: AutomationTrigger,
        candidates: List<CandidateReleaseContext>,
        rules: List<AdvancedRule>,
        destinationPath: String = "Anime/Downloads"
    ): AutomationSimulationResult {
        val matched = mutableListOf<String>()
        val rejected = mutableListOf<String>()
        val selected = mutableListOf<String>()
        val rulesApplied = mutableSetOf<String>()
        val warnings = mutableListOf<String>()

        var totalBytes = 0L

        for (candidate in candidates) {
            val eval = ruleEvaluator.evaluateAll(rules, candidate)
            if (eval.isMatched) {
                matched.add(candidate.title)
                eval.winningRule?.let { rulesApplied.add(it.name) }

                val shouldSelect = eval.appliedActions.any { it is AdvancedRuleAction.QueueDownload || it is AdvancedRuleAction.StartDownloadImmediately }
                if (shouldSelect) {
                    selected.add(candidate.title)
                    totalBytes += candidate.sizeBytes
                }
            } else {
                rejected.add(candidate.title)
            }
        }

        val requiresConfirmation = totalBytes > (10L * 1024 * 1024 * 1024) // > 10GB threshold
        if (requiresConfirmation) {
            warnings.add("Batch size exceeds 10 GB. User confirmation gate recommended.")
        }

        return AutomationSimulationResult(
            matchedReleases = matched,
            rejectedReleases = rejected,
            selectedReleases = selected,
            rulesApplied = rulesApplied.toList(),
            fallbackTierName = "Tier 1: Preferred Encode",
            estimatedSizeBytes = totalBytes,
            destinationPath = destinationPath,
            networkPolicy = NetworkPolicyType.WiFiOnly,
            warnings = warnings,
            requiresConfirmation = requiresConfirmation
        )
    }

    fun evaluateTrigger(
        trigger: AutomationTrigger,
        candidates: List<CandidateReleaseContext>,
        rules: List<AdvancedRule>,
        activeTaskIdentifiers: Set<String> = emptySet(),
        storageFreeBytes: Long = Long.MAX_VALUE
    ): AutomationPlan {
        val selectedReleases = mutableListOf<String>()
        val actions = mutableListOf<AdvancedRuleAction>()
        var winningRuleName: String? = null

        for (candidate in candidates) {
            // Idempotency check: Guard against duplicate downloads (Section 46)
            val itemKey = "${candidate.animeTitle}_${candidate.episodeNumber ?: 0.0}"
            if (activeTaskIdentifiers.contains(itemKey) || executedItemLocks.contains(itemKey)) {
                continue
            }

            val eval = ruleEvaluator.evaluateAll(rules, candidate)
            if (eval.isMatched) {
                winningRuleName = eval.winningRule?.name
                actions.addAll(eval.appliedActions)
                selectedReleases.add(candidate.title)
                executedItemLocks.add(itemKey)
            }
        }

        // Safety Gate validation (Section 22)
        val isStorageLow = storageFreeBytes < (1024 * 1024 * 500L) // < 500 MB
        val isSafetyBlocked = isStorageLow && selectedReleases.isNotEmpty()
        val safetyReason = if (isStorageLow) "Low storage space available on target volume" else null

        val safetyLevel = when {
            isSafetyBlocked -> AutomationSafetyLevel.AlwaysConfirm
            selectedReleases.size > 5 -> AutomationSafetyLevel.RequireConfirmation
            selectedReleases.isNotEmpty() -> AutomationSafetyLevel.Notify
            else -> AutomationSafetyLevel.Silent
        }

        val auditLog = AutomationAuditLog(
            id = UUID.randomUUID().toString(),
            triggerDescription = trigger.toString(),
            winningRuleName = winningRuleName,
            actionsTaken = actions.map { it.toString() },
            outcome = if (isSafetyBlocked) "BLOCKED_BY_SAFETY" else if (selectedReleases.isNotEmpty()) "PLAN_GENERATED" else "NO_ACTION",
            wasBlockedBySafety = isSafetyBlocked,
            explanation = "Evaluated ${candidates.size} releases against ${rules.size} rules."
        )

        return AutomationPlan(
            trigger = trigger,
            candidateTitles = candidates.map { it.title },
            selectedReleases = selectedReleases,
            actionsToExecute = actions,
            safetyLevel = safetyLevel,
            isBlockedBySafety = isSafetyBlocked,
            safetyReason = safetyReason,
            auditLog = auditLog
        )
    }

    fun clearExecutionLocks() {
        executedItemLocks.clear()
    }
}
