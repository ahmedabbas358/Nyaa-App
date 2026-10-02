package com.aniflow.domain.selection.rules

import com.aniflow.domain.intelligence.model.ReleaseCandidate
import com.aniflow.domain.model.aggregate.organization.Rule
import com.aniflow.domain.model.aggregate.organization.RuleAction
import com.aniflow.domain.model.aggregate.organization.RuleCondition
import com.aniflow.domain.selection.model.ConstraintViolation
import com.aniflow.domain.valueobject.ByteSize

data class RuleEvaluationResult(
    val matchedRules: List<Rule> = emptyList(),
    val violations: List<ConstraintViolation> = emptyList(),
    val scoreDelta: Int = 0,
    val isExplicitlyRejected: Boolean = false,
    val isExplicitlySelected: Boolean = false
)

/**
 * Evaluates domain rules against candidates in priority order (Section 32, 80).
 */
class RuleEvaluator {

    fun evaluate(candidate: ReleaseCandidate, rules: List<Rule>): RuleEvaluationResult {
        val activeRules = rules.filter { it.enabled }.sortedByDescending { it.priority }
        val matched = mutableListOf<Rule>()
        val violations = mutableListOf<ConstraintViolation>()
        var scoreDelta = 0
        var explicitlyRejected = false
        var explicitlySelected = false

        val release = candidate.release
        val technical = release.technicalMetadata
        val actualBytes = release.rawMetadata["sizeBytes"]?.toLongOrNull() ?: 0L
        val seeders = release.rawMetadata["seeders"]?.toIntOrNull() ?: 0

        for (rule in activeRules) {
            val conditionsMet = rule.conditions.all { condition ->
                when (condition) {
                    is RuleCondition.ResolutionIs -> technical.resolution == condition.resolution
                    is RuleCondition.CodecIs -> technical.videoCodec == condition.codec
                    is RuleCondition.SizeGreaterThan -> actualBytes > condition.size.bytes
                    is RuleCondition.SizeLessThan -> actualBytes < condition.size.bytes
                    is RuleCondition.SeedersLessThan -> seeders < condition.seeders
                    is RuleCondition.LanguageIs -> technical.audioTracks.any { it.language == condition.language } ||
                        technical.subtitles.any { it.language == condition.language }
                    is RuleCondition.UploaderIs -> release.uploader.equals(condition.uploaderId.value, ignoreCase = true)
                    is RuleCondition.ReleaseGroupIs -> release.groupCandidate.equals(condition.groupId.value, ignoreCase = true)
                    is RuleCondition.AnimeIs -> release.animeCandidate.equals(condition.animeId.value, ignoreCase = true)
                    is RuleCondition.EpisodeIsMissing -> true
                    is RuleCondition.DuplicateExists -> false
                }
            }

            if (conditionsMet) {
                matched += rule
                for (action in rule.actions) {
                    when (action) {
                        is RuleAction.Reject -> {
                            violations += ConstraintViolation.RuleViolation(rule.name, action.reason)
                            explicitlyRejected = true
                        }
                        is RuleAction.DoNotSelect -> {
                            violations += ConstraintViolation.RuleViolation(rule.name, "Rule requested not to select")
                            explicitlyRejected = true
                        }
                        is RuleAction.Select -> {
                            explicitlySelected = true
                            scoreDelta += 50
                        }
                        is RuleAction.Prefer -> {
                            scoreDelta += action.scoreBonus
                        }
                        is RuleAction.Avoid -> {
                            scoreDelta -= action.scorePenalty
                        }
                        is RuleAction.IncreasePriority -> {
                            scoreDelta += (action.delta * 10)
                        }
                        is RuleAction.DecreasePriority -> {
                            scoreDelta -= (action.delta * 10)
                        }
                        is RuleAction.ApplyProfile -> {
                            // Handled at planner/orchestrator level
                        }
                    }
                }
            }
        }

        return RuleEvaluationResult(
            matchedRules = matched,
            violations = violations,
            scoreDelta = scoreDelta,
            isExplicitlyRejected = explicitlyRejected,
            isExplicitlySelected = explicitlySelected
        )
    }
}
