package com.aniflow.domain.selection.policy

import com.aniflow.domain.intelligence.model.ReleaseCandidate
import com.aniflow.domain.selection.model.CandidateClassification
import com.aniflow.domain.selection.model.EvaluatedCandidate
import com.aniflow.domain.selection.model.SelectionPolicyType
import com.aniflow.domain.selection.model.SelectionResultStatus

data class ConfidenceThresholds(
    val highConfidenceThreshold: Double = 0.85,
    val mediumConfidenceThreshold: Double = 0.65
)

/**
 * Evaluates candidate parsing confidence and identifies ambiguity (Section 95, 96, 97).
 */
class ConfidencePolicy(
    private val thresholds: ConfidenceThresholds = ConfidenceThresholds()
) {
    fun isHighConfidence(candidate: ReleaseCandidate): Boolean =
        candidate.confidence >= thresholds.highConfidenceThreshold

    fun requiresConfirmation(candidate: ReleaseCandidate): Boolean =
        candidate.confidence < thresholds.mediumConfidenceThreshold
}

/**
 * Safety gate preventing automated selection on ambiguous or risky candidates (Section 98).
 */
class SelectionSafetyGate(
    private val confidencePolicy: ConfidencePolicy = ConfidencePolicy()
) {
    fun evaluateSafety(
        selected: EvaluatedCandidate?,
        policy: SelectionPolicyType
    ): SelectionResultStatus {
        if (selected == null) {
            return SelectionResultStatus.NoEligibleCandidate
        }

        if (policy == SelectionPolicyType.ManualOnly) {
            return SelectionResultStatus.Ready
        }

        val release = selected.candidate.release

        // 1. Check for critical parse conflicts
        if (release.conflicts.isNotEmpty()) {
            return SelectionResultStatus.ConflictingRules
        }

        // 2. Check for low confidence ambiguity
        if (confidencePolicy.requiresConfirmation(selected.candidate)) {
            return SelectionResultStatus.NeedsConfirmation
        }

        // 3. Ambiguous episode range
        if (selected.candidate.episodeNumber == null && !release.isBatch) {
            return SelectionResultStatus.Ambiguous
        }

        return SelectionResultStatus.Ready
    }
}
