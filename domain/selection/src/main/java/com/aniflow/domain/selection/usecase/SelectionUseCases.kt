package com.aniflow.domain.selection.usecase

import com.aniflow.domain.selection.context.SelectionCandidate
import com.aniflow.domain.selection.context.SelectionContext
import com.aniflow.domain.selection.context.SelectionTarget
import com.aniflow.domain.selection.engine.SelectionEngine
import com.aniflow.domain.selection.model.BatchPreference
import com.aniflow.domain.selection.model.ExistingMediaState
import com.aniflow.domain.selection.model.ResolvedPreferences
import com.aniflow.domain.selection.model.SelectionObjective
import com.aniflow.domain.selection.model.SmartSelectionResult
import com.aniflow.domain.selection.optimization.SeasonPlanResult
import com.aniflow.domain.selection.optimization.SeasonSelectionContext
import com.aniflow.domain.selection.optimization.SeasonSelectionOptimizer

/**
 * Step 21 — Select Episode Release Use Case (Section 110).
 * Primary entry point for selecting a release for an episode through the Smart Selection Engine.
 */
class SelectEpisodeReleaseUseCase(
    private val selectionEngine: SelectionEngine
) {
    suspend operator fun invoke(
        target: SelectionTarget.EpisodeTarget,
        candidates: List<SelectionCandidate>,
        preferences: ResolvedPreferences,
        profile: com.aniflow.domain.model.aggregate.organization.DownloadProfile? = null,
        existingMedia: ExistingMediaState = ExistingMediaState.Missing
    ): SmartSelectionResult {
        val context = SelectionContext(
            target = target,
            candidates = candidates,
            profile = profile,
            preferences = preferences,
            existingMedia = existingMedia
        )
        return selectionEngine.select(context)
    }
}

/**
 * Step 21 — Select Season Plan Use Case (Section 111).
 * Optimizes release choices across an entire season (batch vs individual vs consistency).
 */
class SelectSeasonPlanUseCase(
    private val optimizer: SeasonSelectionOptimizer = SeasonSelectionOptimizer()
) {
    operator fun invoke(
        seasonContext: SeasonSelectionContext
    ): SeasonPlanResult {
        return optimizer.optimize(seasonContext)
    }
}

/**
 * Step 21 — Select Batch Use Case (Section 112).
 * Compares candidate batch packages against individual releases for a targeted set of episodes.
 */
class SelectBatchUseCase(
    private val optimizer: SeasonSelectionOptimizer = SeasonSelectionOptimizer()
) {
    operator fun invoke(
        seasonContext: SeasonSelectionContext,
        preferBatch: Boolean = true
    ): SeasonPlanResult {
        val adjustedContext = seasonContext.copy(
            batchPreference = if (preferBatch) BatchPreference.PreferBatch else BatchPreference.PreferIndividual
        )
        return optimizer.optimize(adjustedContext)
    }
}
