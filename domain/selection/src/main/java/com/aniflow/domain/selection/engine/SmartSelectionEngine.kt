package com.aniflow.domain.selection.engine

import com.aniflow.domain.selection.context.SelectionCandidate
import com.aniflow.domain.selection.context.SelectionContext
import com.aniflow.domain.selection.eligibility.EligibilityEngine
import com.aniflow.domain.selection.fallback.FallbackTierEngine
import com.aniflow.domain.selection.model.AlternativeCandidate
import com.aniflow.domain.selection.model.CandidateScore
import com.aniflow.domain.selection.model.CandidateTrace
import com.aniflow.domain.selection.model.ExplanationItem
import com.aniflow.domain.selection.model.FallbackExplanation
import com.aniflow.domain.selection.model.RejectedCandidate
import com.aniflow.domain.selection.model.SelectedCandidate
import com.aniflow.domain.selection.model.SelectionSafetyResult
import com.aniflow.domain.selection.model.SelectionSnapshot
import com.aniflow.domain.selection.model.SelectionStatus
import com.aniflow.domain.selection.model.SelectionTrace
import com.aniflow.domain.selection.model.SmartSelectionExplanation
import com.aniflow.domain.selection.model.SmartSelectionResult
import com.aniflow.domain.selection.model.SelectionStrategy
import com.aniflow.domain.selection.policy.SelectionPolicy
import com.aniflow.domain.selection.scoring.ScoreEngine
import com.aniflow.domain.selection.scoring.TieBreaker


/**
 * Step 21 — Full Smart Selection Engine Implementation.
 * Enforces the core rule (Section 1):
 * Hard Constraints -> Eligible Candidates -> User Preferences -> Policy -> Scoring -> Tie-Break -> Selection.
 * No candidate violating a hard constraint can ever win due to a high score.
 */
class SmartSelectionEngine(
    private val eligibilityEngine: EligibilityEngine = EligibilityEngine(),
    private val scoreEngine: ScoreEngine = ScoreEngine(),
    private val tieBreaker: TieBreaker = TieBreaker(),
    private val fallbackTierEngine: FallbackTierEngine = FallbackTierEngine()
) : SelectionEngine {

    override suspend fun select(context: SelectionContext): SmartSelectionResult {
        return executeSelection(context)
    }

    override suspend fun simulateSelection(context: SelectionContext): SmartSelectionResult {
        // Dry-run execution without triggering downloads or side effects (Section 87)
        return executeSelection(context)
    }

    override suspend fun traceSelection(context: SelectionContext): SelectionTrace {
        val traces = mutableListOf<CandidateTrace>()
        val effectivePrefs = context.preferences
        val weights = context.profile?.weights ?: com.aniflow.domain.selection.model.SelectionWeights.Default

        for (candidate in context.candidates) {
            val eligibility = eligibilityEngine.evaluateCandidate(
                candidate = candidate,
                preferences = effectivePrefs,
                profile = context.profile,
                existingState = context.existingMedia
            )

            val score = if (eligibility.eligible) {
                scoreEngine.scoreCandidate(candidate, effectivePrefs, weights)
            } else {
                CandidateScore.Zero
            }

            traces.add(
                CandidateTrace(
                    releaseId = candidate.releaseId.value,
                    title = candidate.rawTitle,
                    eligible = eligibility.eligible,
                    scoreBreakdown = score.components.map { "${it.criterion.name}: ${if (it.normalizedPoints >= 0) "+" else ""}${it.normalizedPoints} (${it.explanation})" },
                    totalScore = score.total,
                    violations = eligibility.violations.map { it.description }
                )
            )
        }

        val result = executeSelection(context)
        return SelectionTrace(
            targetDescription = context.target.toString(),
            candidateTraces = traces,
            selectedReleaseId = result.selected?.candidate?.releaseId?.value,
            winnerReason = result.explanation.summary
        )
    }

    private fun executeSelection(context: SelectionContext): SmartSelectionResult {
        if (context.candidates.isEmpty()) {
            return SmartSelectionResult(
                status = SelectionStatus.NoCandidates,
                selected = null,
                explanation = SmartSelectionExplanation(summary = "No candidates available for evaluation")
            )
        }

        val effectivePrefs = context.preferences
        val weights = context.profile?.weights ?: com.aniflow.domain.selection.model.SelectionWeights.Default
        val tiers = fallbackTierEngine.buildDefaultTiers(effectivePrefs)

        val rejected = mutableListOf<RejectedCandidate>()
        val eligibleScored = mutableListOf<Pair<SelectionCandidate, Pair<CandidateScore, Int>>>()

        // 1. Hard Constraints & Eligibility Evaluation (Section 6, 7, 8, 9)
        for (candidate in context.candidates) {
            val eligibility = eligibilityEngine.evaluateCandidate(
                candidate = candidate,
                preferences = effectivePrefs,
                profile = context.profile,
                existingState = context.existingMedia
            )

            if (!eligibility.eligible) {
                rejected.add(
                    RejectedCandidate(
                        candidate = candidate,
                        violations = eligibility.violations
                    )
                )
            } else {
                // 2. Score eligible candidate (Section 17, 18, 19)
                val score = scoreEngine.scoreCandidate(
                    candidate = candidate,
                    preferences = effectivePrefs,
                    weights = weights
                )

                // 3. Assign Fallback Tier (Section 42-45)
                val tier = fallbackTierEngine.assignTierForCandidate(candidate, tiers, effectivePrefs)
                eligibleScored.add(Pair(candidate, Pair(score, tier)))
            }
        }

        // If no candidates are eligible after hard constraints
        if (eligibleScored.isEmpty()) {
            return SmartSelectionResult(
                status = SelectionStatus.NoEligibleCandidate,
                selected = null,
                rejected = rejected,
                explanation = SmartSelectionExplanation(
                    summary = "No eligible candidates met your hard requirements",
                    rejectedReasons = rejected.associate { it.candidate.releaseId.value to it.violations }
                )
            )
        }

        // 4. Sort eligible candidates by Fallback Tier ASC, then Score DESC, then Deterministic TieBreaker
        eligibleScored.sortWith { a, b ->
            val tierA = a.second.second
            val tierB = b.second.second
            if (tierA != tierB) {
                tierA.compareTo(tierB)
            } else {
                val scoreA = a.second.first.total
                val scoreB = b.second.first.total
                if (scoreA != scoreB) {
                    scoreB.compareTo(scoreA) // Higher score first
                } else {
                    tieBreaker.breakTieCandidates(a.first, b.first, effectivePrefs)
                }
            }
        }

        // Top candidate is the winner
        val top = eligibleScored.first()
        val topCandidate = top.first
        val topScore = top.second.first
        val topTier = top.second.second

        // Alternatives are remaining eligible candidates
        val alternatives = eligibleScored.drop(1).map { (cand, scoreAndTier) ->
            AlternativeCandidate(
                candidate = cand,
                score = scoreAndTier.first,
                fallbackTier = scoreAndTier.second,
                diffFromSelected = listOf(
                    "Score: ${scoreAndTier.first.total} vs ${topScore.total}",
                    "Tier: ${scoreAndTier.second} vs $topTier"
                )
            )
        }

        // 5. Safety Gate Check (Section 40, 41)
        val safetyResult = evaluateSafety(topCandidate, context)

        val selectedReasons = topScore.components.filter { it.normalizedPoints > 0 }.map {
            ExplanationItem(
                code = it.criterion.name,
                text = it.explanation,
                isPositive = true
            )
        }

        val fallbackExp = if (topTier > 1) {
            val tierDef = tiers.firstOrNull { it.tierIndex == topTier }
            FallbackExplanation(
                tier = topTier,
                tierName = tierDef?.name ?: "Tier $topTier",
                relaxedAttributes = listOf(if (topTier == 2) "Uploader" else if (topTier == 3) "Codec" else "Resolution"),
                retainedAttributes = listOf("Compatibility")
            )
        } else null

        val status = when {
            context.profile?.strategy == SelectionStrategy.Manual -> SelectionStatus.ManualSelection
            safetyResult.isBlocked -> SelectionStatus.RequiresReview
            safetyResult.requiresConfirmation -> SelectionStatus.RequiresConfirmation
            topTier > 1 -> SelectionStatus.SelectedWithWarning
            else -> SelectionStatus.Selected
        }

        return SmartSelectionResult(
            status = status,
            selected = SelectedCandidate(
                candidate = topCandidate,
                score = topScore,
                fallbackTier = topTier,
                matchedPreferences = selectedReasons.map { it.text }
            ),
            alternatives = alternatives,
            rejected = rejected,
            fallbackTier = topTier,
            explanation = SmartSelectionExplanation(
                summary = if (topTier > 1) "Selected fallback release from Tier $topTier" else "Selected top-ranking compatible release",
                selectedReasons = selectedReasons,
                rejectedReasons = rejected.associate { it.candidate.releaseId.value to it.violations },
                fallback = fallbackExp
            ),
            safety = safetyResult,
            snapshot = SelectionSnapshot(
                profileName = context.profile?.name,
                strategyName = context.profile?.strategy?.name ?: SelectionStrategy.QualityFirst.name,
                weightsSignature = "res=${weights.resolution},codec=${weights.codec},size=${weights.size}"
            )
        )
    }

    private fun evaluateSafety(
        candidate: SelectionCandidate,
        context: SelectionContext
    ): SelectionSafetyResult {
        val reasons = mutableListOf<String>()

        // 1. Confidence check (Section 40)
        if (candidate.confidence == com.aniflow.domain.intelligence.model.ReleaseConfidence.Low) {
            reasons.add("Episode parsing confidence is low")
        }

        // 2. Storage check (Section 80)
        val required = candidate.size?.bytes ?: 0L
        if (context.storage.usableBytes < required) {
            reasons.add("Insufficient usable storage space (required: ${required / (1024 * 1024)} MB, available: ${context.storage.usableBytes / (1024 * 1024)} MB)")
            return SelectionSafetyResult(SelectionSafetyResult.SafetyStatus.Blocked, reasons)
        }

        // 3. Duplicate download check (Section 77)
        if (context.existingDownloads is com.aniflow.domain.selection.model.ExistingDownloadState.Downloading) {
            reasons.add("An active download task is already in progress for this episode")
            return SelectionSafetyResult(SelectionSafetyResult.SafetyStatus.Blocked, reasons)
        }

        return if (reasons.isNotEmpty()) {
            SelectionSafetyResult(SelectionSafetyResult.SafetyStatus.RequiresConfirmation, reasons)
        } else {
            SelectionSafetyResult(SelectionSafetyResult.SafetyStatus.Allowed)
        }
    }
}
