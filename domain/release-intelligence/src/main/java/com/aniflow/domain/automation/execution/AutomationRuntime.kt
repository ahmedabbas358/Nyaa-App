package com.aniflow.domain.automation.execution

import com.aniflow.domain.automation.cooldown.CooldownManager
import com.aniflow.domain.automation.model.AutomationAction
import com.aniflow.domain.automation.model.AutomationDryRunResult
import com.aniflow.domain.automation.model.AutomationExecution
import com.aniflow.domain.automation.model.AutomationExecutionKey
import com.aniflow.domain.automation.model.AutomationExecutionResult
import com.aniflow.domain.automation.model.AutomationExecutionState
import com.aniflow.domain.automation.model.AutomationRule
import com.aniflow.domain.automation.model.AutomationTrigger
import com.aniflow.domain.automation.model.SafetyDecision
import com.aniflow.domain.automation.review.ReviewQueueManager
import com.aniflow.domain.automation.safety.AutomationSafetyGate
import com.aniflow.domain.automation.safety.SafetyEvaluationContext
import com.aniflow.domain.controlplane.service.CandidateReleaseContext
import com.aniflow.domain.controlplane.service.RuleTreeEvaluator
import com.aniflow.domain.identity.AutomationExecutionId
import com.aniflow.domain.intelligence.model.NormalizedRelease
import com.aniflow.domain.valueobject.Resolution
import com.aniflow.domain.valueobject.VideoCodec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * AutomationRuntime (Section 26, 27, 28, 46, 47, 48, 49, 64, 66, 105, 144, 145).
 * Central orchestrator connecting Triggers -> Rules -> Selection -> Safety Gate -> Download Planner -> Queue.
 * Never executes HTTP/Torrent transfers directly.
 * Full audit explainability ("Why was this downloaded?" / "Why was this blocked?").
 */
class AutomationRuntime(
    private val safetyGate: AutomationSafetyGate = AutomationSafetyGate(),
    private val cooldownManager: CooldownManager = CooldownManager(),
    private val reviewQueue: ReviewQueueManager = ReviewQueueManager(),
    private val ruleTreeEvaluator: RuleTreeEvaluator = RuleTreeEvaluator()
) {

    private val executions = ConcurrentHashMap<AutomationExecutionId, AutomationExecution>()
    private val _executionsFlow = MutableStateFlow<List<AutomationExecution>>(emptyList())
    fun observeExecutions(): Flow<List<AutomationExecution>> = _executionsFlow.asStateFlow()

    /**
     * Evaluates an incoming trigger against active rules and executes actions (Section 27, 28).
     */
    suspend fun processTrigger(
        trigger: AutomationTrigger,
        rules: List<AutomationRule>,
        candidateReleases: List<NormalizedRelease>,
        availableStorageBytes: Long,
        isWiFi: Boolean = true,
        downloadsTodayCount: Int = 0,
        storageUsedTodayBytes: Long = 0L,
        onQueuePlan: (suspend (NormalizedRelease) -> String)? = null // Delegates to DownloadPlanner & Queue
    ): List<AutomationExecution> {
        val results = mutableListOf<AutomationExecution>()

        for (rule in rules.filter { it.enabled }) {
            if (!isTriggerMatchingRule(trigger, rule.trigger)) {
                continue
            }

            for (release in candidateReleases) {
                val executionId = AutomationExecutionId("exec_${UUID.randomUUID().toString().take(8)}")
                val explainabilityLog = mutableListOf<String>()
                val releaseTitle = release.releaseSource?.title ?: release.rawTitle.ifBlank { release.normalizedTitle }

                explainabilityLog.add("Rule '${rule.name}' triggered by ${trigger::class.simpleName}")

                // 1. Evaluate Rule AST conditions (reusing existing Step 11 evaluator)
                val ruleConditions = rule.conditions
                if (ruleConditions != null) {
                    val candidateContext = CandidateReleaseContext(
                        title = releaseTitle,
                        animeTitle = release.animeCandidate ?: release.normalizedTitle,
                        resolution = release.technical.resolution ?: Resolution.R1080p,
                        codec = release.technical.codec ?: VideoCodec.HEVC
                    )
                    val conditionsPassed = ruleTreeEvaluator.evaluateNode(ruleConditions, candidateContext)
                    if (!conditionsPassed) {
                        explainabilityLog.add("Rule conditions evaluated to false for release '$releaseTitle'")
                        continue
                    }
                    explainabilityLog.add("Rule conditions satisfied.")
                }

                // 2. Cooldown check (Section 37, 38)
                val targetId = releaseTitle
                val executionKey = AutomationExecutionKey(rule.id, trigger.toString(), targetId)
                if (cooldownManager.isCoolingDown(executionKey, rule.cooldown)) {
                    explainabilityLog.add("Execution skipped due to active cooldown window.")
                    val skippedExec = AutomationExecution(
                        id = executionId,
                        ruleId = rule.id,
                        trigger = trigger,
                        targetIdentity = targetId,
                        state = AutomationExecutionState.Skipped,
                        decision = SafetyDecision.Allowed,
                        result = AutomationExecutionResult.NothingFound,
                        explainabilityLog = explainabilityLog,
                        ruleVersionSnapshot = rule.version
                    )
                    recordExecution(skippedExec)
                    results.add(skippedExec)
                    continue
                }

                // 3. Safety Gate Evaluation (Section 33, 34)
                val safetyContext = SafetyEvaluationContext(
                    release = release,
                    isAmbiguous = false,
                    isBatch = release.isBatch,
                    availableStorageBytes = availableStorageBytes,
                    requiredSizeBytes = release.fileInfo.sizeBytes ?: 1024L * 1024 * 1024,
                    isWiFiConnected = isWiFi,
                    downloadsTodayCount = downloadsTodayCount,
                    storageUsedTodayBytes = storageUsedTodayBytes
                )

                val safetyCheck = safetyGate.evaluate(safetyContext, rule.confirmation)
                explainabilityLog.add("Safety Gate Decision: ${safetyCheck.decision} (${safetyCheck.explanation})")

                val execution = when (safetyCheck.decision) {
                    SafetyDecision.Blocked -> {
                        AutomationExecution(
                            id = executionId,
                            ruleId = rule.id,
                            trigger = trigger,
                            targetIdentity = targetId,
                            state = AutomationExecutionState.Blocked,
                            decision = SafetyDecision.Blocked,
                            result = AutomationExecutionResult.Blocked(
                                reason = safetyCheck.blockReason ?: com.aniflow.domain.automation.model.SafetyBlockReason.InsufficientStorage,
                                detail = safetyCheck.explanation
                            ),
                            explainabilityLog = explainabilityLog,
                            ruleVersionSnapshot = rule.version
                        )
                    }
                    SafetyDecision.RequiresConfirmation -> {
                        // Section 54: Route to Review Queue
                        val reviewItem = reviewQueue.createReviewItem(
                            executionId = executionId,
                            issue = "Automation requires user confirmation",
                            candidateReleaseTitle = releaseTitle,
                            reason = safetyCheck.explanation,
                            recommendedAction = "Approve Download"
                        )
                        explainabilityLog.add("Action queued in Review Queue (ID: ${reviewItem.id.value})")

                        AutomationExecution(
                            id = executionId,
                            ruleId = rule.id,
                            trigger = trigger,
                            targetIdentity = targetId,
                            state = AutomationExecutionState.Waiting,
                            decision = SafetyDecision.RequiresConfirmation,
                            result = AutomationExecutionResult.ReviewRequired(reviewItem.id, safetyCheck.explanation),
                            explainabilityLog = explainabilityLog,
                            ruleVersionSnapshot = rule.version
                        )
                    }
                    SafetyDecision.Allowed,
                    SafetyDecision.AllowedWithWarning -> {
                        // Section 28: Automatic Download pipeline invocation
                        val planId = onQueuePlan?.invoke(release) ?: "plan_auto_${UUID.randomUUID().toString().take(6)}"
                        explainabilityLog.add("Successfully dispatched to Download Planner (Plan ID: $planId)")

                        cooldownManager.recordExecution(executionKey, rule.cooldown)

                        AutomationExecution(
                            id = executionId,
                            ruleId = rule.id,
                            trigger = trigger,
                            targetIdentity = targetId,
                            state = AutomationExecutionState.Completed,
                            completedAt = Instant.now(),
                            decision = safetyCheck.decision,
                            result = AutomationExecutionResult.DownloadQueued(planId, listOf("task_$planId")),
                            explainabilityLog = explainabilityLog,
                            ruleVersionSnapshot = rule.version
                        )
                    }
                }

                recordExecution(execution)
                results.add(execution)
            }
        }

        return results
    }

    /**
     * Dry Run Simulation (Section 66, 67, 68).
     * Simulates triggers, rules, and selection without producing real download tasks or filesystem changes.
     */
    fun simulate(
        rule: AutomationRule,
        candidateReleases: List<NormalizedRelease>,
        availableStorageBytes: Long,
        isWiFi: Boolean = true
    ): AutomationDryRunResult {
        var wouldSelect = 0
        var wouldDownload = 0
        var wouldReview = 0
        var wouldSkip = 0
        var totalEstimatedBytes = 0L
        val actions = mutableListOf<String>()

        for (release in candidateReleases) {
            val releaseTitle = release.releaseSource?.title ?: release.rawTitle.ifBlank { release.normalizedTitle }
            val safetyContext = SafetyEvaluationContext(
                release = release,
                availableStorageBytes = availableStorageBytes,
                requiredSizeBytes = release.fileInfo.sizeBytes ?: 1024L * 1024 * 1024,
                isWiFiConnected = isWiFi
            )

            val check = safetyGate.evaluate(safetyContext, rule.confirmation)

            when (check.decision) {
                SafetyDecision.Allowed, SafetyDecision.AllowedWithWarning -> {
                    wouldSelect++
                    wouldDownload++
                    val bytes = release.fileInfo.sizeBytes ?: 1024L * 1024 * 1024
                    totalEstimatedBytes += bytes
                    actions.add("Would queue download: $releaseTitle (${bytes / (1024 * 1024)} MB)")
                }
                SafetyDecision.RequiresConfirmation -> {
                    wouldSelect++
                    wouldReview++
                    actions.add("Would require confirmation: $releaseTitle (${check.explanation})")
                }
                SafetyDecision.Blocked -> {
                    wouldSkip++
                    actions.add("Would block: $releaseTitle (${check.explanation})")
                }
            }
        }

        return AutomationDryRunResult(
            matchesCount = candidateReleases.size,
            wouldSelectCount = wouldSelect,
            wouldDownloadCount = wouldDownload,
            wouldReviewCount = wouldReview,
            wouldSkipCount = wouldSkip,
            estimatedSizeBytes = totalEstimatedBytes,
            actionsPreview = actions
        )
    }

    private fun isTriggerMatchingRule(actual: AutomationTrigger, registered: AutomationTrigger): Boolean {
        return when {
            registered is AutomationTrigger.Manual -> actual is AutomationTrigger.Manual
            registered is AutomationTrigger.NewRelease && actual is AutomationTrigger.NewRelease -> true
            registered is AutomationTrigger.EpisodeAvailable && actual is AutomationTrigger.EpisodeAvailable -> true
            registered is AutomationTrigger.Schedule && actual is AutomationTrigger.Schedule -> registered.scheduleId == actual.scheduleId
            registered is AutomationTrigger.DownloadCompleted && actual is AutomationTrigger.DownloadCompleted -> true
            registered is AutomationTrigger.UpgradeAvailable && actual is AutomationTrigger.UpgradeAvailable -> true
            else -> registered::class == actual::class
        }
    }

    private fun recordExecution(execution: AutomationExecution) {
        executions[execution.id] = execution
        _executionsFlow.value = executions.values.sortedByDescending { it.startedAt }
    }
}
