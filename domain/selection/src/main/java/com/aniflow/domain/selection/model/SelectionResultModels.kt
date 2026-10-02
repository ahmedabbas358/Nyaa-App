package com.aniflow.domain.selection.model

import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.selection.context.SelectionCandidate
import com.aniflow.domain.selection.context.SelectionTarget
import com.aniflow.domain.valueobject.ByteSize
import java.time.Instant

/**
 * Step 21 — Selection Status (Section 59).
 */
enum class SelectionStatus {
    NoCandidates,
    NoEligibleCandidate,
    Selected,
    SelectedWithWarning,
    RequiresReview,
    RequiresConfirmation,
    ManualSelection
}

/**
 * Step 21 — Selection Safety Result (Section 41).
 */
data class SelectionSafetyResult(
    val status: SafetyStatus,
    val reasons: List<String> = emptyList()
) {
    enum class SafetyStatus {
        Allowed,
        AllowedWithWarning,
        RequiresConfirmation,
        Blocked
    }

    val isBlocked: Boolean get() = status == SafetyStatus.Blocked
    val requiresConfirmation: Boolean get() = status == SafetyStatus.RequiresConfirmation
}

/**
 * Step 21 — Selected Candidate Wrapper (Section 58).
 */
data class SelectedCandidate(
    val candidate: SelectionCandidate,
    val score: CandidateScore,
    val fallbackTier: Int = 1,
    val matchedPreferences: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

/**
 * Step 21 — Alternative Candidate Wrapper (Section 58).
 */
data class AlternativeCandidate(
    val candidate: SelectionCandidate,
    val score: CandidateScore,
    val fallbackTier: Int = 1,
    val diffFromSelected: List<String> = emptyList()
)

/**
 * Step 21 — Rejected Candidate Wrapper (Section 55, 58).
 */
data class RejectedCandidate(
    val candidate: SelectionCandidate,
    val violations: List<ConstraintViolation> = emptyList()
)

/**
 * Step 21 — Final Selection Result (Section 58).
 */
data class SmartSelectionResult(
    val status: SelectionStatus,
    val selected: SelectedCandidate?,
    val alternatives: List<AlternativeCandidate> = emptyList(),
    val rejected: List<RejectedCandidate> = emptyList(),
    val fallbackTier: Int? = null,
    val explanation: SmartSelectionExplanation,
    val safety: SelectionSafetyResult = SelectionSafetyResult(SelectionSafetyResult.SafetyStatus.Allowed),
    val snapshot: SelectionSnapshot? = null
) {
    val hasWinner: Boolean get() = selected != null
}

/**
 * Step 21 — User-Facing Structured Explanation (Section 53, 54, 55, 117).
 */
data class ExplanationItem(
    val code: String,
    val text: String,
    val isPositive: Boolean
)

data class FallbackExplanation(
    val tier: Int,
    val tierName: String,
    val relaxedAttributes: List<String>,
    val retainedAttributes: List<String>
)

data class SmartSelectionExplanation(
    val summary: String,
    val selectedReasons: List<ExplanationItem> = emptyList(),
    val alternativeReasons: Map<String, List<ExplanationItem>> = emptyMap(),
    val rejectedReasons: Map<String, List<ConstraintViolation>> = emptyMap(),
    val fallback: FallbackExplanation? = null,
    val warnings: List<String> = emptyList()
)

/**
 * Step 21 — Selection Snapshot (Section 84).
 */
data class SelectionSnapshot(
    val profileName: String?,
    val strategyName: String,
    val weightsSignature: String,
    val parserVersion: Int = 1,
    val timestamp: Instant = Instant.now()
)

/**
 * Step 21 — Selection Trace for Developer Mode (Section 86).
 */
data class CandidateTrace(
    val releaseId: String,
    val title: String,
    val eligible: Boolean,
    val scoreBreakdown: List<String>,
    val totalScore: Int,
    val violations: List<String> = emptyList()
)

data class SelectionTrace(
    val targetDescription: String,
    val candidateTraces: List<CandidateTrace>,
    val selectedReleaseId: String?,
    val winnerReason: String?
)

/**
 * Step 21 — Manual Override Models (Section 49, 50, 52).
 */
enum class OverrideMode {
    UseThis,
    ExcludeThis,
    LockThis,
    PreferThis
}

enum class OverrideScope {
    ThisSelection,
    ThisEpisode,
    ThisSeason,
    ThisAnime,
    ThisProfile,
    Global
}

enum class OverrideExpiration {
    OneTime,
    UntilChanged,
    UntilDate,
    Permanent
}

data class SelectionOverride(
    val target: SelectionTarget,
    val releaseId: ReleaseId?,
    val mode: OverrideMode,
    val scope: OverrideScope = OverrideScope.ThisEpisode,
    val expiration: OverrideExpiration = OverrideExpiration.Permanent,
    val createdAt: Instant = Instant.now()
)

/**
 * Step 21 — Season & Batch Optimization Models (Section 64, 65, 70).
 */
data class SelectionObjective(
    val qualityWeight: Int = 30,
    val consistencyWeight: Int = 25,
    val totalSizeWeight: Int = 20,
    val batchWeight: Int = 15,
    val availabilityWeight: Int = 10
)

enum class BatchPreference {
    PreferBatch,
    PreferIndividual,
    Balanced,
    Manual
}

data class SelectionPlanCandidate(
    val planId: String,
    val name: String,
    val items: List<SelectedCandidate>,
    val coveredEpisodes: Set<Int>,
    val totalSize: ByteSize?,
    val downloadCount: Int,
    val isBatchPlan: Boolean,
    val dominantUploader: String? = null,
    val dominantGroup: String? = null,
    val score: Int = 0,
    val explanation: String = ""
)
