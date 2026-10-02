package com.aniflow.domain.selection.model

enum class SelectionCriterion {
    ResolutionMatch,
    CodecMatch,
    UploaderPreference,
    ReleaseGroupPreference,
    SubtitleMatch,
    AudioMatch,
    SourceMatch,
    SizePreference,
    SeedersPreference,
    HDRPreference,
    BitDepthPreference,
    LanguagePreference,
    ConfidenceBonus,
    ConsistencyBonus,
    FallbackTierBonus,
    RuleAdjustment
}

data class ScoreComponent(
    val criterion: SelectionCriterion,
    val rawValue: Double,
    val normalizedPoints: Int,
    val explanation: String
)

/**
 * Transparent, multi-component candidate score (Section 41, 42, 43, 46, 50, 51).
 * Completely explainable without opaque magic numbers.
 */
data class CandidateScore(
    val total: Int,
    val normalizedTotal: Double,
    val components: List<ScoreComponent>
) : Comparable<CandidateScore> {

    override fun compareTo(other: CandidateScore): Int = this.total.compareTo(other.total)

    fun getComponent(criterion: SelectionCriterion): ScoreComponent? =
        components.firstOrNull { it.criterion == criterion }

    companion object {
        val Zero = CandidateScore(0, 0.0, emptyList())

        fun build(components: List<ScoreComponent>): CandidateScore {
            val total = components.sumOf { it.normalizedPoints }
            val normalized = (total.coerceIn(0, 100)).toDouble()
            return CandidateScore(
                total = total,
                normalizedTotal = normalized,
                components = components
            )
        }
    }
}
