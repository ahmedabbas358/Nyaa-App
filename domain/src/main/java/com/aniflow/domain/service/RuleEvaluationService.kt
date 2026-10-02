package com.aniflow.domain.service

import com.aniflow.domain.model.aggregate.organization.Rule
import com.aniflow.domain.model.aggregate.organization.RuleAction
import com.aniflow.domain.model.aggregate.organization.RuleCondition
import com.aniflow.domain.model.aggregate.organization.RuleEvaluation
import com.aniflow.domain.model.aggregate.release.Release
import com.aniflow.domain.valueobject.Reason
import com.aniflow.domain.valueobject.SelectionExplanation

/**
 * Domain service executing business rules against candidate releases (Section 50).
 */
object RuleEvaluationService {

    fun evaluate(
        release: Release,
        rules: List<Rule>,
        isEpisodeMissing: Boolean = false,
        duplicateExists: Boolean = false
    ): RuleEvaluation {
        val matched = mutableListOf<Rule>()
        val rejected = mutableListOf<Rule>()
        val actions = mutableListOf<RuleAction>()
        var priorityDelta = 0
        val positiveReasons = mutableListOf<Reason>()
        val negativeReasons = mutableListOf<Reason>()
        val warnings = mutableListOf<Reason>()

        val enabledRules = rules.filter { it.enabled }.sortedByDescending { it.priority }

        for (rule in enabledRules) {
            val isMatch = rule.conditions.all { condition ->
                when (condition) {
                    is RuleCondition.AnimeIs -> release.animeIdentity?.canonicalId == condition.animeId
                    is RuleCondition.UploaderIs -> release.uploader?.id == condition.uploaderId
                    is RuleCondition.ReleaseGroupIs -> release.releaseGroup?.id == condition.groupId
                    is RuleCondition.ResolutionIs -> release.technical.resolution == condition.resolution
                    is RuleCondition.CodecIs -> release.technical.videoCodec == condition.codec
                    is RuleCondition.SizeGreaterThan -> {
                        val size = release.availability.size
                        size != null && size > condition.size
                    }
                    is RuleCondition.SizeLessThan -> {
                        val size = release.availability.size
                        size != null && size < condition.size
                    }
                    is RuleCondition.SeedersLessThan -> {
                        val seeders = release.availability.seeders ?: 0
                        seeders < condition.seeders
                    }
                    is RuleCondition.LanguageIs -> {
                        release.technical.audioTracks.any { it.language == condition.language } ||
                            release.technical.subtitles.any { it.language == condition.language }
                    }
                    is RuleCondition.EpisodeIsMissing -> isEpisodeMissing
                    is RuleCondition.DuplicateExists -> duplicateExists
                }
            }

            if (isMatch) {
                matched.add(rule)
                actions.addAll(rule.actions)
                for (action in rule.actions) {
                    when (action) {
                        is RuleAction.Prefer -> {
                            priorityDelta += action.scoreBonus
                            positiveReasons.add(Reason("RULE_PREFER", "Rule '${rule.name}' preferred this release", action.scoreBonus))
                        }
                        is RuleAction.Avoid -> {
                            priorityDelta -= action.scorePenalty
                            negativeReasons.add(Reason("RULE_AVOID", "Rule '${rule.name}' suggests avoiding this release", -action.scorePenalty))
                        }
                        is RuleAction.Reject -> {
                            negativeReasons.add(Reason("RULE_REJECT", "Rule '${rule.name}' rejected: ${action.reason}"))
                        }
                        is RuleAction.IncreasePriority -> priorityDelta += action.delta
                        is RuleAction.DecreasePriority -> priorityDelta -= action.delta
                        is RuleAction.Select -> positiveReasons.add(Reason("RULE_SELECT", "Rule '${rule.name}' auto-selected"))
                        is RuleAction.DoNotSelect -> negativeReasons.add(Reason("RULE_DO_NOT_SELECT", "Rule '${rule.name}' disabled auto-selection"))
                        is RuleAction.ApplyProfile -> warnings.add(Reason("RULE_PROFILE", "Applied profile override: ${action.profileId.value}"))
                    }
                }
            } else {
                rejected.add(rule)
            }
        }

        return RuleEvaluation(
            matchedRules = matched,
            rejectedRules = rejected,
            actions = actions,
            priorityDelta = priorityDelta,
            explanation = SelectionExplanation(
                positiveReasons = positiveReasons,
                negativeReasons = negativeReasons,
                warnings = warnings
            )
        )
    }
}
