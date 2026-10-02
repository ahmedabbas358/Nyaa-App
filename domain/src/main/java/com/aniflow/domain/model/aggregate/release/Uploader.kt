package com.aniflow.domain.model.aggregate.release

import com.aniflow.domain.identity.UploaderId
import com.aniflow.domain.model.aggregate.organization.RuleScope

enum class PreferenceLevel {
    Preferred,
    Neutral,
    Avoid,
    Blocked
}

/**
 * Entity representing an uploader on a specific provider (Section 20).
 * Identity is tied to Provider + UploaderId.
 */
data class Uploader(
    val id: UploaderId,
    val provider: ProviderRef,
    val name: String,
    val normalizedName: String
) {
    init {
        require(name.isNotBlank()) { "Uploader name cannot be blank" }
        require(normalizedName.isNotBlank()) { "Uploader normalizedName cannot be blank" }
    }
}

/**
 * User preference for an uploader (Section 21).
 * Kept strictly decoupled from the Uploader entity itself.
 */
data class UploaderPreference(
    val uploaderId: UploaderId,
    val level: PreferenceLevel = PreferenceLevel.Neutral,
    val priority: Int = 0,
    val scope: RuleScope = RuleScope.Global
)
