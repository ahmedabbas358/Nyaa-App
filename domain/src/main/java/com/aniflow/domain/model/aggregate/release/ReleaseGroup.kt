package com.aniflow.domain.model.aggregate.release

import com.aniflow.domain.identity.ReleaseGroupId
import com.aniflow.domain.model.aggregate.organization.RuleScope

/**
 * Entity representing an anime fansub or encode group (Section 22).
 */
data class ReleaseGroup(
    val id: ReleaseGroupId,
    val provider: ProviderRef,
    val name: String,
    val normalizedName: String
) {
    init {
        require(name.isNotBlank()) { "ReleaseGroup name cannot be blank" }
        require(normalizedName.isNotBlank()) { "ReleaseGroup normalizedName cannot be blank" }
    }
}

/**
 * User preference for a release group (Section 22).
 */
data class ReleaseGroupPreference(
    val groupId: ReleaseGroupId,
    val level: PreferenceLevel = PreferenceLevel.Neutral,
    val priority: Int = 0,
    val scope: RuleScope = RuleScope.Global
)
