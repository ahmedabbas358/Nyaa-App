package com.aniflow.domain.selection.model

import com.aniflow.domain.identity.EpisodeId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.intelligence.model.NormalizedRelease
import com.aniflow.domain.intelligence.model.ReleaseCandidate
import com.aniflow.domain.valueobject.Reason
import java.time.Instant

/**
 * Classification of evaluated candidates (Section 2, 59).
 * System strictly avoids the absolute concept of "Best Release" and uses transparent tiers.
 */
enum class CandidateClassification {
    Preferred,
    Compatible,
    Alternative,
    Warning,
    Ineligible
}

enum class SelectionResultStatus {
    Ready,
    NeedsConfirmation,
    NoEligibleCandidate,
    ConflictingRules,
    Ambiguous,
    ManualOverrideApplied,
    Locked
}

/**
 * Transparent explanation for an evaluated release candidate (Section 71, 72, 73).
 */
data class CandidateExplanation(
    val summary: String,
    val positiveReasons: List<Reason> = emptyList(),
    val negativeReasons: List<Reason> = emptyList(),
    val warnings: List<Reason> = emptyList(),
    val constraints: List<ConstraintEvaluation> = emptyList()
)

/**
 * Evaluated candidate with classification, score, eligibility, and human-readable explanation.
 */
data class EvaluatedCandidate(
    val candidate: ReleaseCandidate,
    val classification: CandidateClassification,
    val score: CandidateScore,
    val eligibility: EligibilityResult,
    val explanation: CandidateExplanation,
    val fallbackTier: Int = 1
)

/**
 * User-facing selection explanation for the final decision.
 */
data class SelectionExplanation(
    val summary: String,
    val positiveReasons: List<Reason> = emptyList(),
    val negativeReasons: List<Reason> = emptyList(),
    val warnings: List<Reason> = emptyList(),
    val constraints: List<ConstraintEvaluation> = emptyList()
)

/**
 * Output of single-episode candidate evaluation (Section 60).
 */
data class SelectionResult(
    val selected: ReleaseCandidate?,
    val rankedCandidates: List<EvaluatedCandidate>,
    val explanation: SelectionExplanation,
    val status: SelectionResultStatus,
    val requiresConfirmation: Boolean = false,
    val isLocked: Boolean = false,
    val isManualOverride: Boolean = false
) {
    val hasSelection: Boolean get() = selected != null
}

/**
 * Recommendation to download a batch release covering multiple episodes instead of individual files (Section 63, 64, 65).
 */
data class BatchSuggestion(
    val batchRelease: NormalizedRelease,
    val coveredEpisodeNumbers: List<Int>,
    val missingEpisodeNumbers: List<Int>,
    val existingEpisodeNumbers: List<Int>,
    val totalBatchSizeBytes: Long?,
    val totalIndividualSizeBytes: Long?,
    val explanation: String
)

/**
 * Output of multi-episode selection across an entire season or batch (Section 159).
 */
data class MultiEpisodeSelectionResult(
    val selections: Map<EpisodeId, SelectionResult>,
    val unresolved: List<EpisodeId>,
    val warnings: List<SelectionWarning>,
    val totalEstimatedBytes: Long,
    val batchSuggestions: List<BatchSuggestion> = emptyList()
)

/**
 * Selection lock entity protecting a choice from being automatically replaced upon refresh (Section 118, 119, 120).
 */
data class SelectionLock(
    val episodeId: EpisodeId,
    val releaseId: ReleaseId,
    val lockedAt: Instant = Instant.now()
)

/**
 * Manual override entity explicitly chosen by user, superseding all engine policies (Section 33, 34, 35).
 */
data class ManualOverride(
    val episodeId: EpisodeId,
    val releaseId: ReleaseId,
    val scope: PreferenceScope = PreferenceScope.Manual,
    val reason: String = "User manual selection",
    val createdAt: Instant = Instant.now()
)
