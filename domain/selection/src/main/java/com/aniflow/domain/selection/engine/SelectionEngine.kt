package com.aniflow.domain.selection.engine

import com.aniflow.domain.identity.EpisodeId
import com.aniflow.domain.intelligence.model.ReleaseCandidate
import com.aniflow.domain.selection.batch.CoverageOptimizationEngine
import com.aniflow.domain.selection.consistency.SeasonSelectionOptimizer
import com.aniflow.domain.selection.eligibility.EligibilityEngine
import com.aniflow.domain.selection.explanation.ExplanationEngine
import com.aniflow.domain.selection.fallback.FallbackTierEngine
import com.aniflow.domain.selection.lock.SelectionLockManager
import com.aniflow.domain.selection.model.BatchSuggestion
import com.aniflow.domain.selection.model.CandidateClassification
import com.aniflow.domain.selection.model.CandidateScore
import com.aniflow.domain.selection.model.ConsistencyPolicy
import com.aniflow.domain.selection.model.EligibilityResult
import com.aniflow.domain.selection.model.EvaluatedCandidate
import com.aniflow.domain.selection.model.MultiEpisodeSelectionContext
import com.aniflow.domain.selection.model.MultiEpisodeSelectionResult
import com.aniflow.domain.selection.model.PreferenceScope
import com.aniflow.domain.selection.model.SelectionContext
import com.aniflow.domain.selection.model.SelectionPolicyType
import com.aniflow.domain.selection.model.SelectionResult
import com.aniflow.domain.selection.model.SelectionResultStatus
import com.aniflow.domain.selection.model.SelectionWarning
import com.aniflow.domain.selection.override.ManualOverrideManager
import com.aniflow.domain.selection.policy.SelectionSafetyGate
import com.aniflow.domain.selection.preference.PreferenceResolver
import com.aniflow.domain.selection.rules.RuleEvaluator
import com.aniflow.domain.selection.scoring.ScoreEngine
import com.aniflow.domain.selection.scoring.TieBreaker
import com.aniflow.domain.selection.simulation.SelectionAudit
import com.aniflow.domain.selection.simulation.SimulationResult
import java.time.Instant

interface SelectionEngine {
    fun evaluate(context: SelectionContext): SelectionResult
    fun evaluateAll(context: MultiEpisodeSelectionContext): MultiEpisodeSelectionResult
    fun simulate(context: MultiEpisodeSelectionContext): SimulationResult
}

class DefaultSelectionEngine(
    private val eligibilityEngine: EligibilityEngine = EligibilityEngine(),
    private val preferenceResolver: PreferenceResolver = PreferenceResolver(),
    private val scoreEngine: ScoreEngine = ScoreEngine(),
    private val tieBreaker: TieBreaker = TieBreaker(),
    private val fallbackTierEngine: FallbackTierEngine = FallbackTierEngine(),
    private val ruleEvaluator: RuleEvaluator = RuleEvaluator(),
    private val safetyGate: SelectionSafetyGate = SelectionSafetyGate(),
    private val explanationEngine: ExplanationEngine = ExplanationEngine(),
    private val manualOverrideManager: ManualOverrideManager = ManualOverrideManager(),
    private val selectionLockManager: SelectionLockManager = SelectionLockManager(),
    private val coverageOptimizationEngine: CoverageOptimizationEngine = CoverageOptimizationEngine(),
    private val seasonSelectionOptimizer: SeasonSelectionOptimizer = SeasonSelectionOptimizer()
) : SelectionEngine {

    val algorithmVersion: Int = 1

    override fun evaluate(context: SelectionContext): SelectionResult {
        val episodeId = context.episode.id

        // 1. Check for manual user override (Highest authority - Section 33)
        val override = manualOverrideManager.getOverride(episodeId)
        if (override != null) {
            val candidate = context.candidates.firstOrNull { it.release.id == override.releaseId }
            if (candidate != null) {
                val score = scoreEngine.score(
                    candidate = candidate,
                    preferences = context.preferences,
                    weights = context.profile?.weights ?: com.aniflow.domain.selection.model.SelectionWeights.Default
                )
                val eligibility = EligibilityResult.Eligible
                val explanation = explanationEngine.explainCandidate(
                    candidate = candidate,
                    classification = CandidateClassification.Preferred,
                    score = score,
                    eligibility = eligibility
                )
                val evaluated = EvaluatedCandidate(
                    candidate = candidate,
                    classification = CandidateClassification.Preferred,
                    score = score,
                    eligibility = eligibility,
                    explanation = explanation
                )
                val selectionExplanation = explanationEngine.explainSelection(
                    selected = evaluated,
                    allRanked = listOf(evaluated),
                    isManualOverride = true
                )
                return SelectionResult(
                    selected = candidate,
                    rankedCandidates = listOf(evaluated),
                    explanation = selectionExplanation,
                    status = SelectionResultStatus.ManualOverrideApplied,
                    isManualOverride = true
                )
            }
        }

        // 2. Check for selection lock (Section 118, 122)
        val lock = selectionLockManager.getLock(episodeId)
        val isLocked = selectionLockManager.shouldProtectFromAutoReplacement(episodeId, context.existingState)

        // 3. Resolve effective preferences
        val effectivePrefs = preferenceResolver.resolve(
            candidate = context.candidates.firstOrNull() ?: return emptyResult(),
            basePreferences = context.preferences,
            profile = context.profile,
            sessionPreferences = context.sessionPreferences,
            scope = PreferenceScope.Global
        )

        // 4. Build fallback tiers
        val tiers = if (effectivePrefs.fallbackTiers.isNotEmpty()) {
            effectivePrefs.fallbackTiers
        } else {
            fallbackTierEngine.buildDefaultTiers(effectivePrefs)
        }

        // 5. Evaluate eligibility, rules, fallback tiers, and scoring for all candidates
        val weights = context.profile?.weights ?: com.aniflow.domain.selection.model.SelectionWeights.Default
        val evaluatedCandidates = mutableListOf<EvaluatedCandidate>()

        for (candidate in context.candidates) {
            // Rule engine check
            val ruleResult = ruleEvaluator.evaluate(candidate, context.rules)

            // Hard constraints eligibility check
            val baseEligibility = eligibilityEngine.evaluate(
                candidate = candidate,
                preferences = effectivePrefs,
                profile = context.profile,
                existingState = context.existingState
            )

            val combinedViolations = baseEligibility.violations + ruleResult.violations
            val isEligible = combinedViolations.isEmpty() && !ruleResult.isExplicitlyRejected
            val finalEligibility = baseEligibility.copy(
                eligible = isEligible,
                violations = combinedViolations
            )

            if (!isEligible) {
                val score = CandidateScore.Zero
                val explanation = explanationEngine.explainCandidate(
                    candidate = candidate,
                    classification = CandidateClassification.Ineligible,
                    score = score,
                    eligibility = finalEligibility
                )
                evaluatedCandidates += EvaluatedCandidate(
                    candidate = candidate,
                    classification = CandidateClassification.Ineligible,
                    score = score,
                    eligibility = finalEligibility,
                    explanation = explanation
                )
            } else {
                // Calculate base score
                val baseScore = scoreEngine.score(
                    candidate = candidate,
                    preferences = effectivePrefs,
                    weights = weights,
                    previousEpisodeUploader = context.previousEpisodeUploader,
                    previousEpisodeGroup = context.previousEpisodeGroup
                )

                // Adjust by rules
                val finalScore = baseScore.copy(total = baseScore.total + ruleResult.scoreDelta)

                // Assign tier
                val tierIndex = fallbackTierEngine.assignTier(candidate, tiers, effectivePrefs)

                val classification = when {
                    tierIndex == 1 && finalScore.total >= 70 -> CandidateClassification.Preferred
                    finalScore.total >= 50 -> CandidateClassification.Compatible
                    finalEligibility.warnings.isNotEmpty() -> CandidateClassification.Warning
                    else -> CandidateClassification.Alternative
                }

                val explanation = explanationEngine.explainCandidate(
                    candidate = candidate,
                    classification = classification,
                    score = finalScore,
                    eligibility = finalEligibility
                )

                evaluatedCandidates += EvaluatedCandidate(
                    candidate = candidate,
                    classification = classification,
                    score = finalScore,
                    eligibility = finalEligibility,
                    explanation = explanation,
                    fallbackTier = tierIndex
                )
            }
        }

        // 6. Separate eligible from ineligible
        val eligibleCandidates = evaluatedCandidates.filter { it.eligibility.eligible }
        val ineligibleCandidates = evaluatedCandidates.filter { !it.eligibility.eligible }

        if (eligibleCandidates.isEmpty()) {
            val selectionExplanation = explanationEngine.explainSelection(null, evaluatedCandidates)
            return SelectionResult(
                selected = null,
                rankedCandidates = evaluatedCandidates,
                explanation = selectionExplanation,
                status = SelectionResultStatus.NoEligibleCandidate
            )
        }

        // 7. Sort eligible candidates by Tier ASC, Score DESC, then TieBreaker
        val sortedEligible = eligibleCandidates.sortedWith { a, b ->
            if (a.fallbackTier != b.fallbackTier) {
                a.fallbackTier.compareTo(b.fallbackTier)
            } else if (a.score.total != b.score.total) {
                b.score.total.compareTo(a.score.total)
            } else {
                tieBreaker.breakTie(a.candidate, b.candidate, effectivePrefs)
            }
        }

        val allRanked = sortedEligible + ineligibleCandidates
        val topEligible = sortedEligible.first()

        // 8. Policy check
        val policyType = context.profile?.policy ?: SelectionPolicyType.BestCompatible
        val selectedCandidate = when (policyType) {
            SelectionPolicyType.ManualOnly -> null
            SelectionPolicyType.StrictProfile -> {
                if (topEligible.fallbackTier > 1 || topEligible.eligibility.warnings.isNotEmpty()) null else topEligible.candidate
            }
            else -> topEligible.candidate
        }

        val safetyStatus = safetyGate.evaluateSafety(
            selected = topEligible,
            policy = policyType
        )

        val finalStatus = if (isLocked) SelectionResultStatus.Locked else safetyStatus
        val selectionExplanation = explanationEngine.explainSelection(
            selected = if (selectedCandidate != null) topEligible else null,
            allRanked = allRanked,
            isLocked = isLocked
        )

        return SelectionResult(
            selected = selectedCandidate,
            rankedCandidates = allRanked,
            explanation = selectionExplanation,
            status = finalStatus,
            requiresConfirmation = finalStatus == SelectionResultStatus.NeedsConfirmation,
            isLocked = isLocked
        )
    }

    override fun evaluateAll(context: MultiEpisodeSelectionContext): MultiEpisodeSelectionResult {
        val selections = mutableMapOf<EpisodeId, SelectionResult>()
        val unresolved = mutableListOf<EpisodeId>()
        val allWarnings = mutableListOf<SelectionWarning>()
        var totalBytes: Long = 0L

        // Determine dominant uploader across season if season consistency policy active
        val dominantUploader = if (context.consistencyPolicy != ConsistencyPolicy.PerEpisode) {
            seasonSelectionOptimizer.determineDominantUploader(context.episodes, context.preferences)
        } else null

        var previousUploader: String? = dominantUploader

        context.episodes.forEach { epSet ->
            val singleContext = SelectionContext(
                episode = epSet.episode,
                candidates = epSet.candidates,
                profile = context.profile,
                preferences = context.preferences,
                rules = context.rules,
                existingState = epSet.existingState,
                sessionPreferences = context.sessionPreferences,
                previousEpisodeUploader = previousUploader
            )

            val result = evaluate(singleContext)
            selections[epSet.episode.id] = result

            if (result.selected == null) {
                unresolved += epSet.episode.id
            } else {
                val size = result.selected.release.rawMetadata["sizeBytes"]?.toLongOrNull() ?: 0L
                totalBytes += size
                previousUploader = result.selected.release.uploader
            }

            allWarnings += result.explanation.warnings.map {
                SelectionWarning(it.code, it.description)
            }
        }

        // Batch optimization check (Section 63, 64, 65)
        val batchSuggestions = mutableListOf<BatchSuggestion>()
        val allReleases = context.episodes.flatMap { it.candidates }.map { it.release }.distinctBy { it.id }
        allReleases.filter { it.isBatch }.forEach { batchRelease ->
            val suggestion = coverageOptimizationEngine.analyzeBatchCoverage(batchRelease, context.episodes)
            if (suggestion != null) {
                batchSuggestions += suggestion
            }
        }

        return MultiEpisodeSelectionResult(
            selections = selections,
            unresolved = unresolved,
            warnings = allWarnings,
            totalEstimatedBytes = totalBytes,
            batchSuggestions = batchSuggestions
        )
    }

    override fun simulate(context: MultiEpisodeSelectionContext): SimulationResult {
        val multiResult = evaluateAll(context)
        val audits = mutableListOf<SelectionAudit>()
        val uploaderCounts = mutableMapOf<String, Int>()

        var selectedCount = 0
        var preferredCount = 0
        var fallbackCount = 0

        multiResult.selections.forEach { (epId, result) ->
            val selected = result.selected
            if (selected != null) {
                selectedCount++
                val uploader = selected.release.uploader ?: "Unknown"
                uploaderCounts[uploader] = (uploaderCounts[uploader] ?: 0) + 1

                val topEvaluated = result.rankedCandidates.firstOrNull { it.candidate == selected }
                if (topEvaluated != null && topEvaluated.fallbackTier > 1) {
                    fallbackCount++
                } else {
                    preferredCount++
                }

                audits += SelectionAudit(
                    episodeId = epId,
                    releaseId = selected.release.id,
                    profileId = context.profile?.id,
                    algorithmVersion = algorithmVersion,
                    score = topEvaluated?.score?.total ?: 0,
                    reasons = result.explanation.positiveReasons.map { it.description }
                )
            }
        }

        return SimulationResult(
            totalEpisodes = context.episodes.size,
            selectedCount = selectedCount,
            preferredCount = preferredCount,
            fallbackCount = fallbackCount,
            unresolvedCount = multiResult.unresolved.size,
            totalEstimatedBytes = multiResult.totalEstimatedBytes,
            warnings = multiResult.warnings,
            uploaderBreakdown = uploaderCounts,
            auditTrail = audits
        )
    }

    private fun emptyResult() = SelectionResult(
        selected = null,
        rankedCandidates = emptyList(),
        explanation = explanationEngine.explainSelection(null, emptyList()),
        status = SelectionResultStatus.NoEligibleCandidate
    )
}
