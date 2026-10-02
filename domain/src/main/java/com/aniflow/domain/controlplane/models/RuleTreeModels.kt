package com.aniflow.domain.controlplane.models

import com.aniflow.domain.identity.AnimeId
import com.aniflow.domain.identity.DownloadProfileId
import com.aniflow.domain.identity.ReleaseGroupId
import com.aniflow.domain.identity.RuleId
import com.aniflow.domain.identity.UploaderId
import com.aniflow.domain.model.aggregate.organization.RuleScope
import com.aniflow.domain.valueobject.ByteSize
import com.aniflow.domain.valueobject.LanguageCode
import com.aniflow.domain.valueobject.Resolution
import com.aniflow.domain.valueobject.VideoCodec

sealed interface RuleNode

data class ConditionNode(
    val condition: AdvancedRuleCondition
) : RuleNode

data class AndNode(
    val children: List<RuleNode>
) : RuleNode

data class OrNode(
    val children: List<RuleNode>
) : RuleNode

data class NotNode(
    val child: RuleNode
) : RuleNode

sealed interface AdvancedRuleCondition {
    data class AnimeIs(val animeTitle: String) : AdvancedRuleCondition
    data class SeasonIs(val seasonNumber: Int) : AdvancedRuleCondition
    data class EpisodeIs(val episodeNumber: Double) : AdvancedRuleCondition
    data class UploaderIs(val uploaderName: String) : AdvancedRuleCondition
    data class ReleaseGroupIs(val groupName: String) : AdvancedRuleCondition
    data class ResolutionIs(val resolution: Resolution) : AdvancedRuleCondition
    data class CodecIs(val codec: VideoCodec) : AdvancedRuleCondition
    data class SubtitleLanguageIs(val language: LanguageCode) : AdvancedRuleCondition
    data class AudioLanguageIs(val language: LanguageCode) : AdvancedRuleCondition
    data class SizeGreaterThan(val size: ByteSize) : AdvancedRuleCondition
    data class SizeLessThan(val size: ByteSize) : AdvancedRuleCondition
    data class SeedersLessThan(val minSeeders: Int) : AdvancedRuleCondition
    data class AgeDaysGreaterThan(val days: Int) : AdvancedRuleCondition
    data object EpisodeIsMissing : AdvancedRuleCondition
    data object DuplicateExists : AdvancedRuleCondition
    data class StorageFreeSpaceLessThan(val minFreeBytes: Long) : AdvancedRuleCondition
    data object NetworkIsWiFi : AdvancedRuleCondition
    data object NetworkIsMetered : AdvancedRuleCondition
    data class BatteryBelowPercent(val percent: Int) : AdvancedRuleCondition
    data class TimeBetweenHours(val startHour: Int, val endHour: Int) : AdvancedRuleCondition
}

sealed interface AdvancedRuleAction {
    data class SelectProfile(val profileId: DownloadProfileId) : AdvancedRuleAction
    data class Prefer(val scoreBonus: Int = 15) : AdvancedRuleAction
    data class Avoid(val scorePenalty: Int = 15) : AdvancedRuleAction
    data class Reject(val reason: String) : AdvancedRuleAction
    data object QueueDownload : AdvancedRuleAction
    data object StartDownloadImmediately : AdvancedRuleAction
    data object PauseDownload : AdvancedRuleAction
    data class SetPriority(val delta: Int = 1) : AdvancedRuleAction
    data class SetDestination(val relativePath: String) : AdvancedRuleAction
    data class SetStorageLocation(val locationId: String) : AdvancedRuleAction
    data class SetNetworkPolicy(val policy: NetworkPolicyType) : AdvancedRuleAction
    data class RequireConfirmation(val reason: String) : AdvancedRuleAction
    data class Notify(val message: String) : AdvancedRuleAction
    data object Skip : AdvancedRuleAction
}

enum class RulePrecedenceLevel(val priorityValue: Int) {
    ManualOverride(100),
    UserLock(90),
    SafetyRules(80),
    SpecificScopedRule(70),
    GeneralRule(50),
    ProfileDefaults(30),
    GlobalDefaults(10)
}

/**
 * Advanced Rule Tree (Sections 14, 15, 18, 37).
 * Encapsulates hierarchical boolean expressions (AND, OR, NOT) and associated actions.
 */
data class AdvancedRule(
    val id: RuleId,
    val name: String,
    val scope: RuleScope = RuleScope.Global,
    val root: RuleNode,
    val actions: List<AdvancedRuleAction>,
    val precedence: RulePrecedenceLevel = RulePrecedenceLevel.GeneralRule,
    val customPriority: Int = precedence.priorityValue,
    val enabled: Boolean = true
) {
    init {
        require(name.isNotBlank()) { "Rule name cannot be blank" }
        require(actions.isNotEmpty()) { "Rule must define at least one action" }
    }
}
