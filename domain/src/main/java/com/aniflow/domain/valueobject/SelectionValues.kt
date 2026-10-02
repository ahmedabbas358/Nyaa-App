package com.aniflow.domain.valueobject

/**
 * Concrete reason why a Release or Episode was selected (Section 35).
 * Explicitly avoids calling choices "Best", framing them as Selected/Preferred/Compatible.
 */
enum class SelectionReason {
    Manual,
    ProfileMatch,
    RuleMatch,
    PreferredUploader,
    PreferredGroup,
    BestCompatible,
    MissingEpisode,
    BatchSelection
}

/**
 * Transient user or engine selection state for releases in a session (Section 103).
 */
enum class SelectionState {
    Unselected,
    Selected,
    Preferred,
    Rejected,
    ManualOverride
}

/**
 * Individual positive, negative, or warning factor evaluated during selection (Section 51).
 */
data class Reason(
    val code: String,
    val description: String,
    val scoreWeight: Int = 0
)

/**
 * Explainable model for user-facing selection transparency (Section 51 & 111).
 * Displays checkmarks and crosses (e.g. "✓ Preferred uploader", "✕ Size exceeds limit").
 */
data class SelectionExplanation(
    val positiveReasons: List<Reason> = emptyList(),
    val negativeReasons: List<Reason> = emptyList(),
    val warnings: List<Reason> = emptyList()
) {
    val totalScoreDelta: Int
        get() = positiveReasons.sumOf { it.scoreWeight } + negativeReasons.sumOf { it.scoreWeight }
}

/**
 * Deconstructed multi-factor selection score (Section 110 & 111).
 * Avoids opaque "magic numbers" by making every scoring criterion traceable and explainable.
 */
data class SelectionScore(
    val qualityMatch: Int = 0,
    val uploaderPreference: Int = 0,
    val releaseGroupPreference: Int = 0,
    val languageMatch: Int = 0,
    val sizePreference: Int = 0,
    val seederPreference: Int = 0,
    val duplicatePenalty: Int = 0,
    val rulePenalty: Int = 0
) : Comparable<SelectionScore> {

    val totalScore: Int
        get() = qualityMatch +
            uploaderPreference +
            releaseGroupPreference +
            languageMatch +
            sizePreference +
            seederPreference -
            duplicatePenalty -
            rulePenalty

    override fun compareTo(other: SelectionScore): Int = this.totalScore.compareTo(other.totalScore)

    fun toExplanation(): SelectionExplanation {
        val positive = mutableListOf<Reason>()
        val negative = mutableListOf<Reason>()

        if (qualityMatch > 0) positive += Reason("QUALITY_MATCH", "Resolution / Codec preference match", qualityMatch)
        if (uploaderPreference > 0) positive += Reason("UPLOADER_MATCH", "Preferred uploader bonus", uploaderPreference)
        if (releaseGroupPreference > 0) positive += Reason("GROUP_MATCH", "Preferred release group bonus", releaseGroupPreference)
        if (languageMatch > 0) positive += Reason("LANG_MATCH", "Preferred audio/subtitle language match", languageMatch)
        if (seederPreference > 0) positive += Reason("SEEDER_HEALTH", "High seeder availability bonus", seederPreference)

        if (sizePreference < 0) negative += Reason("SIZE_PENALTY", "File size exceeds desired preference", sizePreference)
        if (duplicatePenalty > 0) negative += Reason("DUPLICATE_PENALTY", "Already downloaded or queued elsewhere", -duplicatePenalty)
        if (rulePenalty > 0) negative += Reason("RULE_PENALTY", "Negative rule criteria triggered", -rulePenalty)

        return SelectionExplanation(
            positiveReasons = positive,
            negativeReasons = negative,
            warnings = emptyList()
        )
    }
}
