package com.aniflow.domain.selection.explanation

import com.aniflow.domain.intelligence.model.ReleaseCandidate
import com.aniflow.domain.selection.model.CandidateClassification
import com.aniflow.domain.selection.model.CandidateExplanation
import com.aniflow.domain.selection.model.CandidateScore
import com.aniflow.domain.selection.model.ConstraintEvaluation
import com.aniflow.domain.selection.model.EligibilityResult
import com.aniflow.domain.selection.model.EvaluatedCandidate
import com.aniflow.domain.selection.model.SelectionExplanation
import com.aniflow.domain.valueobject.Reason

/**
 * Builds user-facing, explainable justifications for candidates and selections (Section 71, 72, 73, 156).
 */
class ExplanationEngine {

    fun explainCandidate(
        candidate: ReleaseCandidate,
        classification: CandidateClassification,
        score: CandidateScore,
        eligibility: EligibilityResult
    ): CandidateExplanation {
        val positive = mutableListOf<Reason>()
        val negative = mutableListOf<Reason>()
        val warnings = mutableListOf<Reason>()
        val constraints = mutableListOf<ConstraintEvaluation>()

        // 1. Map eligibility violations
        eligibility.violations.forEach { violation ->
            constraints += ConstraintEvaluation(
                name = violation::class.simpleName ?: "HardConstraint",
                satisfied = false,
                isHardConstraint = true,
                message = violation.description
            )
            negative += Reason(
                code = "HARD_CONSTRAINT_VIOLATION",
                description = "✕ ${violation.description}"
            )
        }

        // 2. Map eligibility warnings
        eligibility.warnings.forEach { warn ->
            warnings += Reason(
                code = warn.code,
                description = "• ${warn.message}"
            )
        }

        // 3. Map score components into positive/negative reasons
        score.components.forEach { comp ->
            if (comp.normalizedPoints > 0) {
                positive += Reason(
                    code = comp.criterion.name,
                    description = "✓ ${comp.explanation}",
                    scoreWeight = comp.normalizedPoints
                )
            } else if (comp.normalizedPoints < 0) {
                negative += Reason(
                    code = comp.criterion.name,
                    description = "• ${comp.explanation}",
                    scoreWeight = comp.normalizedPoints
                )
            }
        }

        val summary = when (classification) {
            CandidateClassification.Preferred -> "Top match based on your preferences"
            CandidateClassification.Compatible -> "Fully compatible, ranked alternative"
            CandidateClassification.Alternative -> "Eligible fallback candidate"
            CandidateClassification.Warning -> "Compatible with warnings"
            CandidateClassification.Ineligible -> "Excluded: does not meet requirements"
        }

        return CandidateExplanation(
            summary = summary,
            positiveReasons = positive,
            negativeReasons = negative,
            warnings = warnings,
            constraints = constraints
        )
    }

    fun explainSelection(
        selected: EvaluatedCandidate?,
        allRanked: List<EvaluatedCandidate>,
        isManualOverride: Boolean = false,
        isLocked: Boolean = false
    ): SelectionExplanation {
        if (selected == null) {
            return SelectionExplanation(
                summary = "No release candidates met your criteria",
                positiveReasons = emptyList(),
                negativeReasons = listOf(Reason("NO_ELIGIBLE", "All available candidates failed one or more requirements")),
                warnings = emptyList()
            )
        }

        val positive = mutableListOf<Reason>()
        val negative = mutableListOf<Reason>()
        val warnings = mutableListOf<Reason>()

        if (isManualOverride) {
            positive += Reason("MANUAL_OVERRIDE", "✓ Explicitly chosen by user")
        } else if (isLocked) {
            positive += Reason("LOCKED_SELECTION", "✓ Selection is locked")
        }

        positive += selected.explanation.positiveReasons
        negative += selected.explanation.negativeReasons
        warnings += selected.explanation.warnings

        val rel = selected.candidate.release
        val summary = "Selected ${rel.uploader?.let { "[$it] " } ?: ""}${rel.normalizedTitle}"

        return SelectionExplanation(
            summary = summary,
            positiveReasons = positive,
            negativeReasons = negative,
            warnings = warnings,
            constraints = selected.explanation.constraints
        )
    }
}
