package com.aniflow.domain.model.aggregate.organization

import com.aniflow.domain.identity.AnimeId
import com.aniflow.domain.identity.DownloadProfileId
import com.aniflow.domain.identity.ReleaseGroupId
import com.aniflow.domain.identity.RuleId
import com.aniflow.domain.identity.UploaderId
import com.aniflow.domain.valueobject.ByteSize
import com.aniflow.domain.valueobject.LanguageCode
import com.aniflow.domain.valueobject.Resolution
import com.aniflow.domain.valueobject.SelectionExplanation
import com.aniflow.domain.valueobject.VideoCodec

enum class RuleScope {
    Global,
    Provider,
    Anime,
    Season,
    Collection,
    Uploader,
    ReleaseGroup
}

sealed interface RuleCondition {
    data class AnimeIs(val animeId: AnimeId) : RuleCondition
    data class UploaderIs(val uploaderId: UploaderId) : RuleCondition
    data class ReleaseGroupIs(val groupId: ReleaseGroupId) : RuleCondition
    data class ResolutionIs(val resolution: Resolution) : RuleCondition
    data class CodecIs(val codec: VideoCodec) : RuleCondition
    data class SizeGreaterThan(val size: ByteSize) : RuleCondition
    data class SizeLessThan(val size: ByteSize) : RuleCondition
    data class SeedersLessThan(val seeders: Int) : RuleCondition
    data class LanguageIs(val language: LanguageCode) : RuleCondition
    data object EpisodeIsMissing : RuleCondition
    data object DuplicateExists : RuleCondition
}

sealed interface RuleAction {
    data class Prefer(val scoreBonus: Int = 15) : RuleAction
    data class Avoid(val scorePenalty: Int = 15) : RuleAction
    data class Reject(val reason: String) : RuleAction
    data class IncreasePriority(val delta: Int = 1) : RuleAction
    data class DecreasePriority(val delta: Int = 1) : RuleAction
    data object Select : RuleAction
    data object DoNotSelect : RuleAction
    data class ApplyProfile(val profileId: DownloadProfileId) : RuleAction
}

/**
 * Domain rule entity for intelligent automation and filtering (Section 46).
 */
data class Rule(
    val id: RuleId,
    val name: String,
    val scope: RuleScope = RuleScope.Global,
    val conditions: List<RuleCondition> = emptyList(),
    val actions: List<RuleAction> = emptyList(),
    val priority: Int = 0,
    val enabled: Boolean = true
) {
    init {
        require(name.isNotBlank()) { "Rule name cannot be blank" }
        require(conditions.isNotEmpty()) { "Rule must have at least one condition" }
        require(actions.isNotEmpty()) { "Rule must have at least one action" }
    }
}

/**
 * Explainable result of evaluating rules against a release (Section 50).
 */
data class RuleEvaluation(
    val matchedRules: List<Rule> = emptyList(),
    val rejectedRules: List<Rule> = emptyList(),
    val actions: List<RuleAction> = emptyList(),
    val priorityDelta: Int = 0,
    val explanation: SelectionExplanation = SelectionExplanation()
) {
    val isExplicitlyRejected: Boolean get() = actions.any { it is RuleAction.Reject || it is RuleAction.DoNotSelect }
    val isExplicitlySelected: Boolean get() = actions.any { it is RuleAction.Select }
}
