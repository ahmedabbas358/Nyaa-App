package com.aniflow.domain.controlplane.service

import com.aniflow.domain.controlplane.models.AdvancedRule
import com.aniflow.domain.controlplane.models.AdvancedRuleAction
import com.aniflow.domain.controlplane.models.AdvancedRuleCondition
import com.aniflow.domain.controlplane.models.AndNode
import com.aniflow.domain.controlplane.models.ConditionNode
import com.aniflow.domain.controlplane.models.NotNode
import com.aniflow.domain.controlplane.models.OrNode
import com.aniflow.domain.controlplane.models.RuleNode
import com.aniflow.domain.valueobject.Resolution
import com.aniflow.domain.valueobject.VideoCodec

data class CandidateReleaseContext(
    val title: String,
    val animeTitle: String,
    val seasonNumber: Int? = 1,
    val episodeNumber: Double? = null,
    val uploaderName: String? = null,
    val groupName: String? = null,
    val resolution: Resolution = Resolution.R1080p,
    val codec: VideoCodec = VideoCodec.HEVC,
    val sizeBytes: Long = 0L,
    val seeders: Int = 10,
    val isEpisodeMissing: Boolean = false,
    val duplicateExists: Boolean = false,
    val freeSpaceBytes: Long = Long.MAX_VALUE,
    val isWiFiConnected: Boolean = true,
    val batteryPercent: Int = 100,
    val currentHour: Int = 12
)

data class RuleTreeEvaluationResult(
    val isMatched: Boolean,
    val matchedRules: List<AdvancedRule>,
    val appliedActions: List<AdvancedRuleAction>,
    val winningRule: AdvancedRule?,
    val evaluationTrace: List<String>
)

/**
 * RuleTreeEvaluator (Sections 14, 15, 18, 37).
 * Recursively evaluates nested boolean AST rule trees with short-circuit optimization and conflict tracking.
 */
class RuleTreeEvaluator {

    fun evaluateAll(
        rules: List<AdvancedRule>,
        context: CandidateReleaseContext
    ): RuleTreeEvaluationResult {
        val trace = mutableListOf<String>()
        val matched = mutableListOf<AdvancedRule>()

        // Sort by precedence / priority descending
        val sortedRules = rules.filter { it.enabled }.sortedByDescending { it.customPriority }

        for (rule in sortedRules) {
            val matches = evaluateNode(rule.root, context)
            if (matches) {
                trace.add("Rule '${rule.name}' (Priority ${rule.customPriority}) matched successfully")
                matched.add(rule)
            } else {
                trace.add("Rule '${rule.name}' did not match")
            }
        }

        val winningRule = matched.firstOrNull()
        val actions = matched.flatMap { it.actions }

        return RuleTreeEvaluationResult(
            isMatched = matched.isNotEmpty(),
            matchedRules = matched,
            appliedActions = actions,
            winningRule = winningRule,
            evaluationTrace = trace
        )
    }

    fun evaluateNode(node: RuleNode, context: CandidateReleaseContext): Boolean {
        return when (node) {
            is ConditionNode -> evaluateCondition(node.condition, context)
            is AndNode -> {
                // Short-circuit AND: returns false on first false child
                node.children.all { evaluateNode(it, context) }
            }
            is OrNode -> {
                // Short-circuit OR: returns true on first true child
                node.children.any { evaluateNode(it, context) }
            }
            is NotNode -> {
                !evaluateNode(node.child, context)
            }
        }
    }

    private fun evaluateCondition(condition: AdvancedRuleCondition, ctx: CandidateReleaseContext): Boolean {
        return when (condition) {
            is AdvancedRuleCondition.AnimeIs -> ctx.animeTitle.contains(condition.animeTitle, ignoreCase = true)
            is AdvancedRuleCondition.SeasonIs -> ctx.seasonNumber == condition.seasonNumber
            is AdvancedRuleCondition.EpisodeIs -> ctx.episodeNumber == condition.episodeNumber
            is AdvancedRuleCondition.UploaderIs -> ctx.uploaderName?.equals(condition.uploaderName, ignoreCase = true) == true
            is AdvancedRuleCondition.ReleaseGroupIs -> ctx.groupName?.equals(condition.groupName, ignoreCase = true) == true
            is AdvancedRuleCondition.ResolutionIs -> ctx.resolution == condition.resolution
            is AdvancedRuleCondition.CodecIs -> ctx.codec == condition.codec
            is AdvancedRuleCondition.SizeGreaterThan -> ctx.sizeBytes > condition.size.bytes
            is AdvancedRuleCondition.SizeLessThan -> ctx.sizeBytes < condition.size.bytes
            is AdvancedRuleCondition.SeedersLessThan -> ctx.seeders < condition.minSeeders
            is AdvancedRuleCondition.AgeDaysGreaterThan -> true // Evaluated when release date is present
            is AdvancedRuleCondition.EpisodeIsMissing -> ctx.isEpisodeMissing
            is AdvancedRuleCondition.DuplicateExists -> ctx.duplicateExists
            is AdvancedRuleCondition.StorageFreeSpaceLessThan -> ctx.freeSpaceBytes < condition.minFreeBytes
            is AdvancedRuleCondition.NetworkIsWiFi -> ctx.isWiFiConnected
            is AdvancedRuleCondition.NetworkIsMetered -> !ctx.isWiFiConnected
            is AdvancedRuleCondition.BatteryBelowPercent -> ctx.batteryPercent < condition.percent
            is AdvancedRuleCondition.TimeBetweenHours -> ctx.currentHour in condition.startHour..condition.endHour
            else -> true
        }
    }
}
