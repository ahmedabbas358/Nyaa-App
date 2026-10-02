package com.aniflow.domain.rules.conflict

import com.aniflow.domain.controlplane.models.AdvancedRule
import com.aniflow.domain.controlplane.models.AdvancedRuleAction
import com.aniflow.domain.controlplane.models.AdvancedRuleCondition
import com.aniflow.domain.controlplane.models.ConditionNode
import com.aniflow.domain.controlplane.models.RuleNode
import com.aniflow.domain.controlplane.models.RulePrecedenceLevel
import com.aniflow.domain.model.aggregate.organization.RuleScope

enum class RuleConflictSeverity {
    Warning,
    Potential,
    Critical
}

data class RuleConflict(
    val ruleA: AdvancedRule,
    val ruleB: AdvancedRule,
    val severity: RuleConflictSeverity,
    val description: String,
    val recommendedResolution: String
)

/**
 * RuleResolutionPolicy defines deterministic ordering and evaluation guarantees (Section 28).
 * Precedence: Global rule < Anime-specific rule < Automation-specific rule < Manual override.
 */
object RuleResolutionPolicy {

    fun sortRules(rules: List<AdvancedRule>): List<AdvancedRule> {
        return rules
            .filter { it.enabled }
            .sortedWith(
                compareByDescending<AdvancedRule> { it.customPriority }
                    .thenByDescending { getScopeWeight(it.scope) }
                    .thenBy { it.name }
            )
    }

    private fun getScopeWeight(scope: RuleScope): Int = when (scope) {
        RuleScope.Global -> 1
        RuleScope.Category -> 2
        RuleScope.Anime -> 3
    }
}

/**
 * RuleConflictDetector analyzes rule definitions to proactively detect semantic contradictions (Section 29).
 */
class RuleConflictDetector {

    fun detectConflicts(rules: List<AdvancedRule>): List<RuleConflict> {
        val conflicts = mutableListOf<RuleConflict>()
        val activeRules = rules.filter { it.enabled }

        for (i in activeRules.indices) {
            for (j in i + 1 until activeRules.size) {
                val ruleA = activeRules[i]
                val ruleB = activeRules[j]

                // Compare scopes and potential collisions
                if (areScopesOverlapping(ruleA.scope, ruleB.scope)) {
                    val conflict = analyzePair(ruleA, ruleB)
                    if (conflict != null) {
                        conflicts.add(conflict)
                    }
                }
            }
        }

        return conflicts
    }

    private fun areScopesOverlapping(scopeA: RuleScope, scopeB: RuleScope): Boolean {
        if (scopeA == RuleScope.Global || scopeB == RuleScope.Global) return true
        return scopeA == scopeB
    }

    private fun analyzePair(ruleA: AdvancedRule, ruleB: AdvancedRule): RuleConflict? {
        val actionsA = ruleA.actions
        val actionsB = ruleB.actions

        val hasRejectA = actionsA.any { it is AdvancedRuleAction.Reject }
        val hasDownloadA = actionsA.any { it is AdvancedRuleAction.QueueDownload || it is AdvancedRuleAction.StartDownloadImmediately }

        val hasRejectB = actionsB.any { it is AdvancedRuleAction.Reject }
        val hasDownloadB = actionsB.any { it is AdvancedRuleAction.QueueDownload || it is AdvancedRuleAction.StartDownloadImmediately }

        // Conflict 1: One rule downloads, other rejects under similar or overlapping conditions
        if ((hasRejectA && hasDownloadB) || (hasRejectB && hasDownloadA)) {
            val conditionsA = extractConditions(ruleA.root)
            val conditionsB = extractConditions(ruleB.root)

            val sharedCondition = conditionsA.intersect(conditionsB.toSet())
            if (sharedCondition.isNotEmpty() || ruleA.customPriority == ruleB.customPriority) {
                return RuleConflict(
                    ruleA = ruleA,
                    ruleB = ruleB,
                    severity = RuleConflictSeverity.Critical,
                    description = "Contradictory actions: Rule '${ruleA.name}' and '${ruleB.name}' have opposite decisions (Queue vs Reject).",
                    recommendedResolution = "Adjust the priority of the desired rule or refine their trigger conditions."
                )
            }
        }

        // Conflict 2: Both rules specify conflicting Profile selections
        val profileA = actionsA.filterIsInstance<AdvancedRuleAction.SelectProfile>().firstOrNull()?.profileId
        val profileB = actionsB.filterIsInstance<AdvancedRuleAction.SelectProfile>().firstOrNull()?.profileId
        if (profileA != null && profileB != null && profileA != profileB && ruleA.customPriority == ruleB.customPriority) {
            return RuleConflict(
                ruleA = ruleA,
                ruleB = ruleB,
                severity = RuleConflictSeverity.Potential,
                description = "Ambiguous profile selection: Rule '${ruleA.name}' selects profile '$profileA' while '${ruleB.name}' selects '$profileB' with identical priority.",
                recommendedResolution = "Assign a distinct custom priority to establish which profile selection wins."
            )
        }

        return null
    }

    private fun extractConditions(node: RuleNode): List<AdvancedRuleCondition> {
        val result = mutableListOf<AdvancedRuleCondition>()
        fun traverse(n: RuleNode) {
            when (n) {
                is ConditionNode -> result.add(n.condition)
                is com.aniflow.domain.controlplane.models.AndNode -> n.children.forEach { traverse(it) }
                is com.aniflow.domain.controlplane.models.OrNode -> n.children.forEach { traverse(it) }
                is com.aniflow.domain.controlplane.models.NotNode -> traverse(n.child)
            }
        }
        traverse(node)
        return result
    }
}
