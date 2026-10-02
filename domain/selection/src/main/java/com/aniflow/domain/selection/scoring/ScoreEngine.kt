package com.aniflow.domain.selection.scoring

import com.aniflow.domain.intelligence.model.ReleaseCandidate
import com.aniflow.domain.selection.model.BitDepthPreference
import com.aniflow.domain.selection.model.CandidateScore
import com.aniflow.domain.selection.model.PreferenceMode
import com.aniflow.domain.selection.model.ResolutionMatchMode
import com.aniflow.domain.selection.model.ScoreComponent
import com.aniflow.domain.selection.model.SelectionCriterion
import com.aniflow.domain.selection.model.SelectionWeights
import com.aniflow.domain.selection.model.UserSelectionPreferences
import com.aniflow.domain.valueobject.BitDepth
import com.aniflow.domain.valueobject.Resolution

/**
 * Calculates transparent, multi-component scores for eligible release candidates (Section 41, 42, 43, 44, 45, 46, 47, 49, 50, 51).
 * Normalizes metrics to 0.0..1.0 before applying configurable weights to prevent double-counting.
 */
class ScoreEngine {

    fun score(
        candidate: ReleaseCandidate,
        preferences: UserSelectionPreferences,
        weights: SelectionWeights,
        previousEpisodeUploader: String? = null,
        previousEpisodeGroup: String? = null
    ): CandidateScore {
        val components = mutableListOf<ScoreComponent>()
        val release = candidate.release
        val technical = release.technicalMetadata
        val actualBytes = release.rawMetadata["sizeBytes"]?.toLongOrNull()
        val seeders = release.rawMetadata["seeders"]?.toIntOrNull() ?: 0

        // 1. Resolution Match
        val targetRes = preferences.resolution.target
        val actualRes = technical.resolution
        val resRatio: Double = when {
            actualRes == targetRes -> 1.0
            actualRes != null && actualRes.height > targetRes.height -> 0.85
            actualRes != null && preferences.resolution.fallbackResolutions.contains(actualRes) -> {
                val index = preferences.resolution.fallbackResolutions.indexOf(actualRes)
                0.70 - (index * 0.15).coerceAtLeast(0.0)
            }
            else -> 0.20
        }
        val resPoints = (resRatio * weights.resolution).toInt()
        components += ScoreComponent(
            criterion = SelectionCriterion.ResolutionMatch,
            rawValue = resRatio,
            normalizedPoints = resPoints,
            explanation = if (actualRes == targetRes) "Exact target resolution (${targetRes.displayName})" else "Resolution: ${actualRes?.displayName ?: "Unknown"}"
        )

        // 2. Codec Match (normalized ranks: max rank is 100)
        val codecRank = preferences.codec.getRank(technical.videoCodec)
        val codecRatio = (codecRank.coerceIn(0, 100)) / 100.0
        val codecPoints = (codecRatio * weights.codec).toInt()
        components += ScoreComponent(
            criterion = SelectionCriterion.CodecMatch,
            rawValue = codecRatio,
            normalizedPoints = codecPoints,
            explanation = "Codec ${technical.videoCodec?.displayName ?: "Unknown"} (rank $codecRank/100)"
        )

        // 3. Uploader Preference
        val uploaderDisp = preferences.uploader.getDisposition(release.uploader)
        val uploaderPoints = when (uploaderDisp) {
            PreferenceMode.Preferred -> weights.uploader
            PreferenceMode.Neutral -> (weights.uploader * 0.4).toInt()
            PreferenceMode.Avoid -> -((weights.uploader * 0.5).toInt())
            PreferenceMode.Forbidden -> -weights.uploader
            PreferenceMode.Required -> weights.uploader
        }
        components += ScoreComponent(
            criterion = SelectionCriterion.UploaderPreference,
            rawValue = if (uploaderDisp == PreferenceMode.Preferred) 1.0 else 0.0,
            normalizedPoints = uploaderPoints,
            explanation = when (uploaderDisp) {
                PreferenceMode.Preferred -> "Preferred uploader: ${release.uploader}"
                PreferenceMode.Avoid -> "Avoid uploader penalty: ${release.uploader}"
                else -> "Uploader: ${release.uploader ?: "Unknown"}"
            }
        )

        // 4. Release Group Preference
        val groupDisp = preferences.releaseGroup.getDisposition(release.groupCandidate)
        val groupPoints = when (groupDisp) {
            PreferenceMode.Preferred -> weights.releaseGroup
            PreferenceMode.Neutral -> (weights.releaseGroup * 0.4).toInt()
            PreferenceMode.Avoid -> -((weights.releaseGroup * 0.5).toInt())
            PreferenceMode.Forbidden -> -weights.releaseGroup
            PreferenceMode.Required -> weights.releaseGroup
        }
        components += ScoreComponent(
            criterion = SelectionCriterion.ReleaseGroupPreference,
            rawValue = if (groupDisp == PreferenceMode.Preferred) 1.0 else 0.0,
            normalizedPoints = groupPoints,
            explanation = when (groupDisp) {
                PreferenceMode.Preferred -> "Preferred release group: ${release.groupCandidate}"
                PreferenceMode.Avoid -> "Avoid release group penalty: ${release.groupCandidate}"
                else -> "Release group: ${release.groupCandidate ?: "Unknown"}"
            }
        )

        // 5. Subtitle Match
        var subPoints = 0
        var subExplanation = "No preferred subtitle match"
        val matchedSubtitles = technical.subtitles.filter { preferences.subtitles.preferredLanguages.contains(it.language) }
        if (matchedSubtitles.isNotEmpty()) {
            subPoints = weights.subtitle
            subExplanation = "Preferred subtitles: ${matchedSubtitles.mapNotNull { it.language?.code }.joinToString()}"
        }
        components += ScoreComponent(
            criterion = SelectionCriterion.SubtitleMatch,
            rawValue = if (matchedSubtitles.isNotEmpty()) 1.0 else 0.0,
            normalizedPoints = subPoints,
            explanation = subExplanation
        )

        // 6. Audio Match
        var audioPoints = 0
        var audioExplanation = "Audio: standard"
        val matchedAudio = technical.audioTracks.filter { preferences.audioLanguage.primary.contains(it.language) }
        if (matchedAudio.isNotEmpty()) {
            audioPoints = weights.audio
            audioExplanation = "Primary audio track: ${matchedAudio.mapNotNull { it.language?.code }.joinToString()}"
        }
        components += ScoreComponent(
            criterion = SelectionCriterion.AudioMatch,
            rawValue = if (matchedAudio.isNotEmpty()) 1.0 else 0.0,
            normalizedPoints = audioPoints,
            explanation = audioExplanation
        )

        // 7. Source Match
        val sourceRatio = when {
            release.source != null && preferences.source.preferred.contains(release.source) -> 1.0
            release.source != null && preferences.source.allowed.contains(release.source) -> 0.6
            else -> 0.3
        }
        val sourcePoints = (sourceRatio * weights.source).toInt()
        components += ScoreComponent(
            criterion = SelectionCriterion.SourceMatch,
            rawValue = sourceRatio,
            normalizedPoints = sourcePoints,
            explanation = "Source: ${release.source?.displayName ?: "Unknown"}"
        )

        // 8. Size Preference
        val prefMax = preferences.sizePolicy.preferredMaxBytes
        val sizePoints = when {
            actualBytes == null -> 0
            prefMax != null && actualBytes <= prefMax -> weights.size
            prefMax != null && actualBytes > prefMax -> {
                val excessRatio = (actualBytes - prefMax).toDouble() / prefMax
                -((excessRatio.coerceAtMost(1.0) * weights.size).toInt())
            }
            else -> (weights.size * 0.5).toInt()
        }
        components += ScoreComponent(
            criterion = SelectionCriterion.SizePreference,
            rawValue = if (prefMax != null && actualBytes != null && actualBytes <= prefMax) 1.0 else 0.0,
            normalizedPoints = sizePoints,
            explanation = if (actualBytes != null) {
                "${actualBytes / (1024 * 1024)} MB (preferred max: ${prefMax?.let { it / (1024 * 1024) } ?: "none"} MB)"
            } else "Size unknown"
        )

        // 9. Availability / Seeders Preference
        val prefSeeders = preferences.seederPolicy.preferredSeeders
        val seedRatio = (seeders.toDouble() / prefSeeders).coerceIn(0.0, 1.0)
        val seedPoints = (seedRatio * weights.availability).toInt()
        components += ScoreComponent(
            criterion = SelectionCriterion.SeedersPreference,
            rawValue = seedRatio,
            normalizedPoints = seedPoints,
            explanation = "$seeders seeds (target $prefSeeders+)"
        )

        // 10. Bit Depth Preference
        if (technical.bitDepth == BitDepth.Bit10 && preferences.bitDepth.preferred == BitDepth.Bit10) {
            components += ScoreComponent(
                criterion = SelectionCriterion.BitDepthPreference,
                rawValue = 1.0,
                normalizedPoints = weights.bitDepth,
                explanation = "10-bit depth matched"
            )
        }

        // 11. Cross-Episode Consistency Bonus (Section 108, 109, 161)
        if (previousEpisodeUploader != null && release.uploader.equals(previousEpisodeUploader, ignoreCase = true)) {
            components += ScoreComponent(
                criterion = SelectionCriterion.ConsistencyBonus,
                rawValue = 1.0,
                normalizedPoints = weights.consistency,
                explanation = "Consistent uploader across season ($previousEpisodeUploader)"
            )
        }

        // 12. Confidence Bonus (Section 95)
        if (candidate.confidence >= 0.85) {
            components += ScoreComponent(
                criterion = SelectionCriterion.ConfidenceBonus,
                rawValue = candidate.confidence,
                normalizedPoints = 5,
                explanation = "High parser confidence bonus"
            )
        }

        return CandidateScore.build(components)
    }

    /**
     * Overload for SelectionCandidate (Step 21 Domain Model).
     */
    fun scoreCandidate(
        candidate: com.aniflow.domain.selection.context.SelectionCandidate,
        preferences: UserSelectionPreferences,
        weights: SelectionWeights,
        previousEpisodeUploader: String? = null,
        previousEpisodeGroup: String? = null
    ): CandidateScore {
        val components = mutableListOf<ScoreComponent>()
        val technical = candidate.technical
        val actualBytes = candidate.size?.bytes
        val seeders = candidate.seeders ?: 0

        // 1. Resolution Match
        val targetRes = preferences.resolution.target
        val actualRes = technical.resolution
        val resRatio: Double = when {
            actualRes == targetRes -> 1.0
            actualRes != null && actualRes.height > targetRes.height -> 0.85
            actualRes != null && preferences.resolution.fallbackResolutions.contains(actualRes) -> {
                val index = preferences.resolution.fallbackResolutions.indexOf(actualRes)
                0.70 - (index * 0.15).coerceAtLeast(0.0)
            }
            else -> 0.20
        }
        val resPoints = (resRatio * weights.resolution).toInt()
        components += ScoreComponent(
            criterion = SelectionCriterion.ResolutionMatch,
            rawValue = resRatio,
            normalizedPoints = resPoints,
            explanation = if (actualRes == targetRes) "Exact target resolution (${targetRes.displayName})" else "Resolution: ${actualRes?.displayName ?: "Unknown"}"
        )

        // 2. Codec Match
        val codecRank = preferences.codec.getRank(technical.videoCodec)
        val codecRatio = (codecRank.coerceIn(0, 100)) / 100.0
        val codecPoints = (codecRatio * weights.codec).toInt()
        components += ScoreComponent(
            criterion = SelectionCriterion.CodecMatch,
            rawValue = codecRatio,
            normalizedPoints = codecPoints,
            explanation = "Codec ${technical.videoCodec?.displayName ?: "Unknown"} (rank $codecRank/100)"
        )

        // 3. Uploader Preference
        val uploaderDisp = preferences.uploader.getDisposition(candidate.uploader)
        val uploaderPoints = when (uploaderDisp) {
            PreferenceMode.Preferred -> weights.uploader
            PreferenceMode.Neutral -> (weights.uploader * 0.4).toInt()
            PreferenceMode.Avoid -> -((weights.uploader * 0.5).toInt())
            PreferenceMode.Forbidden -> -weights.uploader
            PreferenceMode.Required -> weights.uploader
        }
        components += ScoreComponent(
            criterion = SelectionCriterion.UploaderPreference,
            rawValue = if (uploaderDisp == PreferenceMode.Preferred) 1.0 else 0.0,
            normalizedPoints = uploaderPoints,
            explanation = when (uploaderDisp) {
                PreferenceMode.Preferred -> "Preferred uploader: ${candidate.uploader}"
                PreferenceMode.Avoid -> "Avoid uploader penalty: ${candidate.uploader}"
                else -> "Uploader: ${candidate.uploader ?: "Unknown"}"
            }
        )

        // 4. Release Group Preference
        val groupDisp = preferences.releaseGroup.getDisposition(candidate.releaseGroup)
        val groupPoints = when (groupDisp) {
            PreferenceMode.Preferred -> weights.releaseGroup
            PreferenceMode.Neutral -> (weights.releaseGroup * 0.4).toInt()
            PreferenceMode.Avoid -> -((weights.releaseGroup * 0.5).toInt())
            PreferenceMode.Forbidden -> -weights.releaseGroup
            PreferenceMode.Required -> weights.releaseGroup
        }
        components += ScoreComponent(
            criterion = SelectionCriterion.ReleaseGroupPreference,
            rawValue = if (groupDisp == PreferenceMode.Preferred) 1.0 else 0.0,
            normalizedPoints = groupPoints,
            explanation = when (groupDisp) {
                PreferenceMode.Preferred -> "Preferred release group: ${candidate.releaseGroup}"
                PreferenceMode.Avoid -> "Avoid release group penalty: ${candidate.releaseGroup}"
                else -> "Group: ${candidate.releaseGroup ?: "Unknown"}"
            }
        )

        // 5. Source Match
        val source = technical.source
        val sourcePoints = when {
            source != null && preferences.source.preferred.contains(source) -> weights.source
            source != null && preferences.source.allowed.contains(source) -> (weights.source * 0.6).toInt()
            else -> 0
        }
        components += ScoreComponent(
            criterion = SelectionCriterion.SourceMatch,
            rawValue = if (source != null && preferences.source.preferred.contains(source)) 1.0 else 0.5,
            normalizedPoints = sourcePoints,
            explanation = "Source: ${source?.displayName ?: "Unknown"}"
        )

        // 6. Subtitles
        val hasPreferredSub = preferences.subtitles.preferredLanguages.any { prefLang ->
            technical.subtitles.any { it.language == prefLang }
        }
        val subPoints = if (hasPreferredSub) weights.subtitle else (weights.subtitle * 0.3).toInt()
        components += ScoreComponent(
            criterion = SelectionCriterion.SubtitleMatch,
            rawValue = if (hasPreferredSub) 1.0 else 0.3,
            normalizedPoints = subPoints,
            explanation = if (hasPreferredSub) "Matches preferred subtitles" else "Subtitles: general"
        )

        // 7. Audio Language
        val hasPreferredAudio = preferences.audioLanguage.primary.any { prefLang ->
            technical.audioTracks.any { it.language == prefLang }
        }
        val audioPoints = if (hasPreferredAudio) weights.audio else (weights.audio * 0.4).toInt()
        components += ScoreComponent(
            criterion = SelectionCriterion.AudioMatch,
            rawValue = if (hasPreferredAudio) 1.0 else 0.4,
            normalizedPoints = audioPoints,
            explanation = if (hasPreferredAudio) "Preferred audio track match" else "Audio: general"
        )

        // 8. Size Efficiency
        val prefMax = preferences.sizePolicy.preferredMaxBytes
        val sizePoints = when {
            actualBytes == null -> 0
            prefMax != null && actualBytes <= prefMax -> weights.size
            prefMax != null && actualBytes > prefMax -> {
                val excessRatio = ((actualBytes - prefMax).toDouble() / prefMax).coerceAtMost(1.0)
                (weights.size * (1.0 - excessRatio)).toInt()
            }
            else -> (weights.size * 0.5).toInt()
        }
        components += ScoreComponent(
            criterion = SelectionCriterion.SizePreference,
            rawValue = 1.0,
            normalizedPoints = sizePoints,
            explanation = "Size: ${actualBytes?.let { it / (1024 * 1024) } ?: "?"} MB"
        )

        // 9. Availability / Seeders
        val seederTarget = preferences.seederPolicy.preferredSeeders
        val seederRatio = (seeders.toDouble() / seederTarget).coerceIn(0.0, 1.5)
        val seederPoints = (seederRatio * weights.availability).toInt().coerceAtMost(weights.availability + 5)
        components += ScoreComponent(
            criterion = SelectionCriterion.SeedersPreference,
            rawValue = seederRatio,
            normalizedPoints = seederPoints,
            explanation = "$seeders seeds (target $seederTarget)"
        )

        // 10. Consistency
        if (previousEpisodeUploader != null && candidate.uploader.equals(previousEpisodeUploader, ignoreCase = true)) {
            components += ScoreComponent(
                criterion = SelectionCriterion.ConsistencyBonus,
                rawValue = 1.0,
                normalizedPoints = weights.consistency,
                explanation = "Consistent uploader ($previousEpisodeUploader)"
            )
        }

        return CandidateScore.build(components)
    }
}

