package com.aniflow.domain.selection.policy
 
import com.aniflow.domain.selection.model.SelectionStrategy
import com.aniflow.domain.selection.model.SelectionWeights



/**
 * Step 21 — Tie Breaker Rules (Section 56, 57).
 */
enum class TieBreakerRule {
    HardCriterionSatisfaction,
    PreferenceMatch,
    BetterAvailability,
    SmallerSize,
    CompleteCoverage,
    DeterministicId
}

/**
 * Step 21 — Fallback Policy (Section 42-46).
 */
data class FallbackPolicy(
    val maxTiers: Int = 5,
    val allowRelaxUploader: Boolean = true,
    val allowRelaxCodec: Boolean = true,
    val allowRelaxResolution: Boolean = true,
    val suggestRelaxationsOnFailure: Boolean = true
) {
    companion object {
        val Default = FallbackPolicy()
        val Strict = FallbackPolicy(maxTiers = 1, allowRelaxUploader = false, allowRelaxCodec = false, allowRelaxResolution = false)
    }
}

/**
 * Step 21 — Unknown Metadata Policy (Section 39).
 */
enum class UnknownMetadataPolicy {
    AllowUnknown,
    PenalizeUnknown,
    RejectUnknown
}

/**
 * Step 21 — Selection Policy (Section 19).
 * Unifies strategy, weight profiles, deterministic tie breaking, and fallback behavior.
 */
data class SelectionPolicy(
    val strategy: SelectionStrategy = SelectionStrategy.QualityFirst,
    val weights: SelectionWeights = SelectionWeights.Default,
    val tieBreaker: List<TieBreakerRule> = listOf(
        TieBreakerRule.HardCriterionSatisfaction,
        TieBreakerRule.PreferenceMatch,
        TieBreakerRule.BetterAvailability,
        TieBreakerRule.SmallerSize,
        TieBreakerRule.CompleteCoverage,
        TieBreakerRule.DeterministicId
    ),
    val fallbackPolicy: FallbackPolicy = FallbackPolicy.Default,
    val unknownMetadataPolicy: UnknownMetadataPolicy = UnknownMetadataPolicy.PenalizeUnknown
) {
    companion object {
        val QualityFirst = SelectionPolicy(
            strategy = SelectionStrategy.QualityFirst,
            weights = SelectionWeights.QualityFirst
        )

        val SizeFirst = SelectionPolicy(
            strategy = SelectionStrategy.SizeFirst,
            weights = SelectionWeights.SizeFirst
        )

        val Balanced = SelectionPolicy(
            strategy = SelectionStrategy.Balanced,
            weights = SelectionWeights.Balanced
        )

        val Manual = SelectionPolicy(
            strategy = SelectionStrategy.Manual
        )
    }
}
