package com.aniflow.domain.rules.simulation

import com.aniflow.domain.controlplane.models.AdvancedRule
import com.aniflow.domain.controlplane.models.AdvancedRuleAction
import com.aniflow.domain.controlplane.models.AdvancedRuleCondition
import com.aniflow.domain.controlplane.models.ConditionNode
import com.aniflow.domain.controlplane.models.RuleNode
import com.aniflow.domain.controlplane.service.CandidateReleaseContext
import com.aniflow.domain.controlplane.service.RuleTreeEvaluator
import com.aniflow.domain.profile.model.UserProfile
import java.time.Instant

data class ConditionEvaluationStep(
    val conditionDescription: String,
    val matched: Boolean,
    val actualValue: String,
    val expectedValue: String
)

data class SafetySimulationPreview(
    val passed: Boolean,
    val reason: String?,
    val freeSpaceRemainingBytes: Long,
    val isDuplicateDetected: Boolean
)

data class RuleSimulationReport(
    val ruleId: String,
    val ruleName: String,
    val isTriggerMatched: Boolean,
    val overallMatched: Boolean,
    val conditionSteps: List<ConditionEvaluationStep>,
    val appliedActions: List<AdvancedRuleAction>,
    val selectedProfileName: String?,
    val safetyPreview: SafetySimulationPreview,
    val explanation: String,
    val simulatedAt: Instant = Instant.now()
)

/**
 * RuleSimulator enables non-destructive dry runs and testing of rules on sample releases (Section 30, 31, 32).
 * Strictly guarantees ZERO mutation to persistent database or download queue.
 */
class RuleSimulator(
    private val evaluator: RuleTreeEvaluator = RuleTreeEvaluator()
) {

    fun simulate(
        rule: AdvancedRule,
        candidate: CandidateReleaseContext,
        associatedProfile: UserProfile? = null
    ): RuleSimulationReport {
        val steps = mutableListOf<ConditionEvaluationStep>()

        // 1. Evaluate root AST and record individual condition steps
        val overallMatched = evaluateAndRecord(rule.root, candidate, steps)

        // 2. Identify selected profile from rule actions or fallback to associated profile
        val selectedProfileId = rule.actions
            .filterIsInstance<AdvancedRuleAction.SelectProfile>()
            .firstOrNull()?.profileId

        val resolvedProfileName = when {
            selectedProfileId != null -> "Profile: $selectedProfileId (from Rule)"
            associatedProfile != null -> "${associatedProfile.name} (Context)"
            else -> "Default System Profile"
        }

        // 3. Dry-run safety evaluation
        val requiredFreeSpace = 2L * 1024L * 1024L * 1024L // 2GB buffer
        val spaceOk = candidate.freeSpaceBytes > (candidate.sizeBytes + requiredFreeSpace)
        val duplicateOk = !candidate.duplicateExists

        val safetyPassed = spaceOk && duplicateOk
        val safetyReason = when {
            !spaceOk -> "Safety Gate: Insufficient free space. Need ${candidate.sizeBytes + requiredFreeSpace} bytes, available ${candidate.freeSpaceBytes} bytes"
            !duplicateOk -> "Safety Gate: Potential duplicate already exists in library"
            else -> "Safety Gate: All pre-flight safety checks passed"
        }

        val safetyPreview = SafetySimulationPreview(
            passed = safetyPassed,
            reason = safetyReason,
            freeSpaceRemainingBytes = candidate.freeSpaceBytes - candidate.sizeBytes,
            isDuplicateDetected = candidate.duplicateExists
        )

        // 4. Construct human-readable explanation
        val explanation = buildString {
            if (overallMatched) {
                append("Rule '${rule.name}' matched all conditions for release '${candidate.title}'. ")
                if (safetyPassed) {
                    append("Execution would proceed to: ${rule.actions.joinToString { it.javaClass.simpleName }}.")
                } else {
                    append("However, execution would be BLOCKED by Safety Gate: $safetyReason.")
                }
            } else {
                val failedStep = steps.firstOrNull { !it.matched }
                append("Rule '${rule.name}' failed to match. Reason: ${failedStep?.conditionDescription ?: "Condition not met"} (Actual: ${failedStep?.actualValue}).")
            }
        }

        return RuleSimulationReport(
            ruleId = rule.id.value,
            ruleName = rule.name,
            isTriggerMatched = true,
            overallMatched = overallMatched,
            conditionSteps = steps,
            appliedActions = if (overallMatched && safetyPassed) rule.actions else emptyList(),
            selectedProfileName = resolvedProfileName,
            safetyPreview = safetyPreview,
            explanation = explanation
        )
    }

    private fun evaluateAndRecord(
        node: RuleNode,
        ctx: CandidateReleaseContext,
        steps: MutableList<ConditionEvaluationStep>
    ): Boolean {
        return when (node) {
            is ConditionNode -> {
                val matched = evaluator.evaluateNode(node, ctx)
                val (expected, actual) = describeCondition(node.condition, ctx)
                steps.add(
                    ConditionEvaluationStep(
                        conditionDescription = node.condition.javaClass.simpleName,
                        matched = matched,
                        actualValue = actual,
                        expectedValue = expected
                    )
                )
                matched
            }
            is com.aniflow.domain.controlplane.models.AndNode -> {
                var allMatch = true
                for (child in node.children) {
                    val childMatched = evaluateAndRecord(child, ctx, steps)
                    if (!childMatched) allMatch = false
                }
                allMatch
            }
            is com.aniflow.domain.controlplane.models.OrNode -> {
                var anyMatch = false
                for (child in node.children) {
                    val childMatched = evaluateAndRecord(child, ctx, steps)
                    if (childMatched) anyMatch = true
                }
                anyMatch
            }
            is com.aniflow.domain.controlplane.models.NotNode -> {
                val childMatched = evaluateAndRecord(node.child, ctx, steps)
                !childMatched
            }
        }
    }

    private fun describeCondition(
        condition: AdvancedRuleCondition,
        ctx: CandidateReleaseContext
    ): Pair<String, String> {
        return when (condition) {
            is AdvancedRuleCondition.AnimeIs -> condition.animeTitle to ctx.animeTitle
            is AdvancedRuleCondition.ResolutionIs -> condition.resolution.displayName to ctx.resolution.displayName
            is AdvancedRuleCondition.CodecIs -> condition.codec.displayName to ctx.codec.displayName
            is AdvancedRuleCondition.SizeLessThan -> "< ${condition.size.bytes} B" to "${ctx.sizeBytes} B"
            is AdvancedRuleCondition.SizeGreaterThan -> "> ${condition.size.bytes} B" to "${ctx.sizeBytes} B"
            is AdvancedRuleCondition.UploaderIs -> condition.uploaderName to (ctx.uploaderName ?: "Unknown")
            is AdvancedRuleCondition.ReleaseGroupIs -> condition.groupName to (ctx.groupName ?: "Unknown")
            is AdvancedRuleCondition.EpisodeIsMissing -> "Missing = true" to "Missing = ${ctx.isEpisodeMissing}"
            is AdvancedRuleCondition.DuplicateExists -> "Duplicate = true" to "Duplicate = ${ctx.duplicateExists}"
            else -> "Condition" to "Evaluated"
        }
    }
}
