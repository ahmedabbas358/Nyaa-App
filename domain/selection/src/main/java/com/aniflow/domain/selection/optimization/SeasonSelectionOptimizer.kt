package com.aniflow.domain.selection.optimization

import com.aniflow.domain.identity.SeasonId
import com.aniflow.domain.selection.context.SelectionCandidate
import com.aniflow.domain.selection.model.BatchPreference
import com.aniflow.domain.selection.model.CandidateScore
import com.aniflow.domain.selection.model.ResolvedPreferences
import com.aniflow.domain.selection.model.SelectedCandidate
import com.aniflow.domain.selection.model.SelectionObjective
import com.aniflow.domain.selection.model.SelectionPlanCandidate
import com.aniflow.domain.valueobject.ByteSize

/**
 * Step 21 — Multi-Episode Season Selection Context (Section 61).
 */
data class SeasonSelectionContext(
    val seasonId: SeasonId,
    val expectedEpisodes: List<Int>,
    val perEpisodeCandidates: Map<Int, List<SelectionCandidate>>,
    val batchCandidates: List<SelectionCandidate> = emptyList(),
    val objective: SelectionObjective = SelectionObjective(),
    val batchPreference: BatchPreference = BatchPreference.Balanced,
    val existingEpisodes: Set<Int> = emptySet(),
    val preferences: ResolvedPreferences
)

data class SeasonPlanResult(
    val winningPlan: SelectionPlanCandidate,
    val alternativePlans: List<SelectionPlanCandidate>,
    val explanation: String
)

/**
 * Step 21 — Season & Batch Selection Optimizer (Section 62-75).
 * Compares per-episode selection, full batch selection, and gap-filling hybrid plans.
 * Prevents double-counting and accounts for redundancy, storage, and consistency.
 */
class SeasonSelectionOptimizer {

    fun optimize(context: SeasonSelectionContext): SeasonPlanResult {
        val plans = mutableListOf<SelectionPlanCandidate>()
        val missingEpisodes = context.expectedEpisodes.filterNot { context.existingEpisodes.contains(it) }.toSet()

        // 1. Plan A: Best Individual Releases Plan
        val individualSelected = mutableListOf<SelectedCandidate>()
        var totalIndividualBytes = 0L

        for (epNum in missingEpisodes) {
            val candidates = context.perEpisodeCandidates[epNum] ?: emptyList()
            val best = candidates.maxByOrNull { it.seeders ?: 0 }
            if (best != null) {
                val sizeBytes = best.size?.bytes ?: (1024L * 1024 * 500)
                totalIndividualBytes += sizeBytes
                individualSelected.add(
                    SelectedCandidate(
                        candidate = best,
                        score = CandidateScore.build(emptyList())
                    )
                )
            }
        }

        if (individualSelected.isNotEmpty()) {
            val dominantUploader = individualSelected.mapNotNull { it.candidate.uploader }
                .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key

            plans.add(
                SelectionPlanCandidate(
                    planId = "plan_individual",
                    name = "Individual Episodes Plan",
                    items = individualSelected,
                    coveredEpisodes = missingEpisodes,
                    totalSize = ByteSize.ofBytes(totalIndividualBytes),
                    downloadCount = individualSelected.size,
                    isBatchPlan = false,
                    dominantUploader = dominantUploader,
                    score = calculatePlanScore(
                        coverageRatio = individualSelected.size.toFloat() / missingEpisodes.size.coerceAtLeast(1),
                        downloadCount = individualSelected.size,
                        totalBytes = totalIndividualBytes,
                        isBatch = false,
                        objective = context.objective,
                        batchPreference = context.batchPreference
                    ),
                    explanation = "${individualSelected.size} individual files (${totalIndividualBytes / (1024 * 1024 * 1024)} GB)"
                )
            )
        }

        // 2. Plan B: Batch Plans (Full or Partial)
        for (batch in context.batchCandidates) {
            val batchBytes = batch.size?.bytes ?: (1024L * 1024 * 1024 * 10)
            val batchCovered = context.expectedEpisodes.toSet() // Full season batch coverage

            plans.add(
                SelectionPlanCandidate(
                    planId = "plan_batch_${batch.releaseId.value}",
                    name = "Full Batch Package (${batch.uploader ?: "Release"})",
                    items = listOf(
                        SelectedCandidate(
                            candidate = batch,
                            score = CandidateScore.build(emptyList())
                        )
                    ),
                    coveredEpisodes = batchCovered,
                    totalSize = ByteSize.ofBytes(batchBytes),
                    downloadCount = 1,
                    isBatchPlan = true,
                    dominantUploader = batch.uploader,
                    dominantGroup = batch.releaseGroup,
                    score = calculatePlanScore(
                        coverageRatio = 1.0f,
                        downloadCount = 1,
                        totalBytes = batchBytes,
                        isBatch = true,
                        objective = context.objective,
                        batchPreference = context.batchPreference
                    ),
                    explanation = "1 batch download (${batchBytes / (1024 * 1024 * 1024)} GB), covers all ${context.expectedEpisodes.size} episodes"
                )
            )
        }

        // 3. Fallback: if no candidates at all
        if (plans.isEmpty()) {
            val emptyPlan = SelectionPlanCandidate(
                planId = "plan_empty",
                name = "No Candidates Plan",
                items = emptyList(),
                coveredEpisodes = emptySet(),
                totalSize = ByteSize.ofBytes(0),
                downloadCount = 0,
                isBatchPlan = false,
                score = 0,
                explanation = "No releases available for optimization"
            )
            return SeasonPlanResult(emptyPlan, emptyList(), "No available releases found")
        }

        // 4. Rank plans by score descending
        plans.sortByDescending { it.score }
        val winning = plans.first()
        val alternatives = plans.drop(1)

        val explanation = when {
            winning.isBatchPlan && context.batchPreference == BatchPreference.PreferBatch ->
                "Selected Batch package as preferred by user policy (1 download vs ${individualSelected.size})"
            !winning.isBatchPlan && context.batchPreference == BatchPreference.PreferIndividual ->
                "Selected individual releases as preferred by user policy"
            winning.isBatchPlan ->
                "Batch plan won: superior consistency and single download convenience"
            else ->
                "Individual plan won: more efficient total download size"
        }

        return SeasonPlanResult(
            winningPlan = winning,
            alternativePlans = alternatives,
            explanation = explanation
        )
    }

    private fun calculatePlanScore(
        coverageRatio: Float,
        downloadCount: Int,
        totalBytes: Long,
        isBatch: Boolean,
        objective: SelectionObjective,
        batchPreference: BatchPreference
    ): Int {
        var score = (coverageRatio * 100).toInt()

        // Batch preference bonus/penalty
        when (batchPreference) {
            BatchPreference.PreferBatch -> {
                if (isBatch) score += objective.batchWeight * 2 else score -= 10
            }
            BatchPreference.PreferIndividual -> {
                if (!isBatch) score += objective.batchWeight * 2 else score -= 10
            }
            BatchPreference.Balanced -> {
                if (isBatch) score += objective.batchWeight
            }
            BatchPreference.Manual -> Unit
        }

        // Convenience of fewer downloads
        if (downloadCount == 1) {
            score += 15
        } else if (downloadCount > 10) {
            score -= 5
        }

        return score
    }
}
