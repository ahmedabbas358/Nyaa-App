package com.aniflow.domain.service

import com.aniflow.domain.model.aggregate.organization.DownloadProfile
import com.aniflow.domain.model.aggregate.organization.PreferenceRequirement
import com.aniflow.domain.model.aggregate.organization.Rule
import com.aniflow.domain.model.aggregate.release.PreferenceLevel
import com.aniflow.domain.model.aggregate.release.Release
import com.aniflow.domain.valueobject.Reason
import com.aniflow.domain.valueobject.SelectionExplanation
import com.aniflow.domain.valueobject.SelectionScore
import com.aniflow.domain.valueobject.SelectionState

/**
 * Result of scoring and evaluating a release for selection (Section 110 & 111).
 */
data class SelectionEvaluationResult(
    val score: SelectionScore,
    val state: SelectionState,
    val explanation: SelectionExplanation
)

/**
 * Domain service providing explainable selection scoring and ranking (Section 110 & 111).
 */
object SelectionService {

    fun evaluateRelease(
        release: Release,
        profile: DownloadProfile,
        rules: List<Rule> = emptyList(),
        isDuplicate: Boolean = false,
        isEpisodeMissing: Boolean = false
    ): SelectionEvaluationResult {
        val positive = mutableListOf<Reason>()
        val negative = mutableListOf<Reason>()
        val warnings = mutableListOf<Reason>()

        var qualityScore = 0
        var uploaderScore = 0
        var groupScore = 0
        var languageScore = 0
        var sizeScore = 0
        var seederScore = 0
        var duplicatePenalty = if (isDuplicate) 100 else 0

        // 1. Resolution Check
        when (val req = profile.preferences.resolution) {
            is PreferenceRequirement.Required -> {
                if (release.technical.resolution == req.value) {
                    qualityScore += 30
                    positive.add(Reason("REQ_RES_MATCH", "Matches required resolution (${req.value.displayName})", 30))
                } else {
                    negative.add(Reason("REQ_RES_MISMATCH", "Does not match required resolution (${req.value.displayName})", -50))
                }
            }
            is PreferenceRequirement.Preferred -> {
                if (release.technical.resolution == req.value) {
                    qualityScore += 20
                    positive.add(Reason("PREF_RES_MATCH", "Matches preferred resolution (${req.value.displayName})", 20))
                }
            }
            is PreferenceRequirement.Forbidden -> {
                if (release.technical.resolution == req.value) {
                    negative.add(Reason("FORBIDDEN_RES", "Has forbidden resolution (${req.value.displayName})", -100))
                }
            }
            is PreferenceRequirement.Ignored -> Unit
        }

        // 2. Video Codec Check
        when (val req = profile.preferences.codec) {
            is PreferenceRequirement.Required -> {
                if (release.technical.videoCodec == req.value) {
                    qualityScore += 20
                    positive.add(Reason("REQ_CODEC_MATCH", "Matches required codec (${req.value.displayName})", 20))
                } else {
                    negative.add(Reason("REQ_CODEC_MISMATCH", "Does not match required codec (${req.value.displayName})", -40))
                }
            }
            is PreferenceRequirement.Preferred -> {
                if (release.technical.videoCodec == req.value) {
                    qualityScore += 15
                    positive.add(Reason("PREF_CODEC_MATCH", "Matches preferred codec (${req.value.displayName})", 15))
                }
            }
            is PreferenceRequirement.Forbidden -> {
                if (release.technical.videoCodec == req.value) {
                    negative.add(Reason("FORBIDDEN_CODEC", "Has forbidden codec (${req.value.displayName})", -100))
                }
            }
            is PreferenceRequirement.Ignored -> Unit
        }

        // 3. Audio & Subtitle Language Match
        when (val req = profile.preferences.audioLanguage) {
            is PreferenceRequirement.Preferred -> {
                if (release.technical.audioTracks.any { it.language == req.value }) {
                    languageScore += 10
                    positive.add(Reason("PREF_AUDIO_MATCH", "Has preferred audio language (${req.value.code})", 10))
                }
            }
            else -> Unit
        }
        when (val req = profile.preferences.subtitleLanguage) {
            is PreferenceRequirement.Preferred -> {
                if (release.technical.subtitles.any { it.language == req.value }) {
                    languageScore += 10
                    positive.add(Reason("PREF_SUB_MATCH", "Has preferred subtitle language (${req.value.code})", 10))
                }
            }
            else -> Unit
        }

        // 4. Seeder Health
        val seeders = release.availability.seeders ?: 0
        val minSeeders = profile.preferences.minSeeders ?: 1
        if (seeders >= minSeeders * 3) {
            seederScore += 15
            positive.add(Reason("HIGH_SEEDERS", "Healthy swarm ($seeders seeders)", 15))
        } else if (seeders < minSeeders) {
            seederScore -= 20
            negative.add(Reason("LOW_SEEDERS", "Swarm below minimum seeders ($seeders < $minSeeders)", -20))
        }

        // 5. Size Constraint
        val maxSize = profile.preferences.maxSize
        val releaseSize = release.availability.size
        if (maxSize != null && releaseSize != null) {
            if (releaseSize > maxSize) {
                sizeScore -= 25
                negative.add(Reason("SIZE_EXCEEDED", "Size (${releaseSize.toDisplayString()}) exceeds limit (${maxSize.toDisplayString()})", -25))
            }
        }

        // 6. Rule Evaluation
        val ruleEval = RuleEvaluationService.evaluate(
            release = release,
            rules = rules,
            isEpisodeMissing = isEpisodeMissing,
            duplicateExists = isDuplicate
        )

        val totalRulePenalty = if (ruleEval.priorityDelta < 0) -ruleEval.priorityDelta else 0
        val totalRuleBonus = if (ruleEval.priorityDelta > 0) ruleEval.priorityDelta else 0

        positive.addAll(ruleEval.explanation.positiveReasons)
        negative.addAll(ruleEval.explanation.negativeReasons)
        warnings.addAll(ruleEval.explanation.warnings)

        if (isDuplicate) {
            negative.add(Reason("DUPLICATE_ITEM", "File or release is already known in system", -100))
        }

        val score = SelectionScore(
            qualityMatch = qualityScore,
            uploaderPreference = uploaderScore,
            releaseGroupPreference = groupScore,
            languageMatch = languageScore,
            sizePreference = sizeScore,
            seederPreference = seederScore,
            duplicatePenalty = duplicatePenalty,
            rulePenalty = totalRulePenalty
        )

        val selectionState = when {
            ruleEval.isExplicitlyRejected -> SelectionState.Rejected
            isDuplicate -> SelectionState.Rejected
            ruleEval.isExplicitlySelected -> SelectionState.Selected
            score.totalScore >= 40 -> SelectionState.Preferred
            score.totalScore > 0 -> SelectionState.Selected
            else -> SelectionState.Unselected
        }

        return SelectionEvaluationResult(
            score = score,
            state = selectionState,
            explanation = SelectionExplanation(
                positiveReasons = positive,
                negativeReasons = negative,
                warnings = warnings
            )
        )
    }
}
