package com.aniflow.domain.selection.model

import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.model.aggregate.organization.Rule

/**
 * Step 21 — Existing Media State (Section 76, 79).
 */
sealed interface ExistingMediaState {
    data object Missing : ExistingMediaState
    data class Satisfied(
        val fileSize: Long,
        val resolution: String? = null,
        val codec: String? = null,
        val filePath: String = ""
    ) : ExistingMediaState
    data class UpgradeEligible(
        val currentFileSize: Long,
        val currentResolution: String? = null,
        val currentCodec: String? = null,
        val filePath: String = ""
    ) : ExistingMediaState
}

/**
 * Step 21 — Existing Download State (Section 77, 78).
 */
sealed interface ExistingDownloadState {
    data object None : ExistingDownloadState
    data class Queued(val taskId: String) : ExistingDownloadState
    data class Downloading(
        val taskId: String,
        val progress: Float = 0f,
        val isLocked: Boolean = false
    ) : ExistingDownloadState
    data class Completed(val taskId: String, val filePath: String) : ExistingDownloadState
}

/**
 * Step 21 — Storage Context (Section 80).
 */
data class StorageContext(
    val availableBytes: Long,
    val reservedBytes: Long = 0,
    val requiredBytes: Long = 0
) {
    val usableBytes: Long get() = (availableBytes - reservedBytes).coerceAtLeast(0L)

    companion object {
        val Unlimited = StorageContext(availableBytes = Long.MAX_VALUE)
    }
}

/**
 * Step 21 — Network Context (Section 81).
 */
data class NetworkContext(
    val isCellular: Boolean = false,
    val isMetered: Boolean = false,
    val allowCellularDownloads: Boolean = false
) {
    companion object {
        val Unconstrained = NetworkContext()
    }
}

/**
 * Step 21 — Provider Context (Section 82).
 */
enum class ProviderHealthState {
    Healthy,
    RateLimited,
    Unavailable
}

data class ProviderContext(
    val health: ProviderHealthState = ProviderHealthState.Healthy,
    val rateLimitResetSeconds: Long? = null
) {
    companion object {
        val Default = ProviderContext()
    }
}

/**
 * Step 21 — Rule Effects (Section 14, 15).
 */
data class RuleBoost(val ruleName: String, val points: Int, val reason: String)
data class RulePenalty(val ruleName: String, val points: Int, val reason: String)
data class RuleExclusion(val ruleName: String, val reason: String)
data class RuleWarning(val ruleName: String, val message: String)

data class CandidateRuleEffect(
    val candidateId: ReleaseId,
    val boosts: List<RuleBoost> = emptyList(),
    val penalties: List<RulePenalty> = emptyList(),
    val exclusions: List<RuleExclusion> = emptyList(),
    val warnings: List<RuleWarning> = emptyList(),
    val forceConfirmation: Boolean = false
) {
    val totalScoreDelta: Int get() = boosts.sumOf { it.points } - penalties.sumOf { it.points }
    val isExcluded: Boolean get() = exclusions.isNotEmpty()
}

data class ResolvedRule(
    val rule: Rule,
    val isEnforced: Boolean = true
)

/**
 * Step 21 — Constraint Match Record (Section 8).
 */
data class ConstraintMatch(
    val constraintName: String,
    val description: String
)

/**
 * Typealias for ResolvedPreferences to seamlessly bridge user selection preferences.
 */
typealias ResolvedPreferences = UserSelectionPreferences
