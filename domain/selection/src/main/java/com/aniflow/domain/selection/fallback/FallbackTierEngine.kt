package com.aniflow.domain.selection.fallback

import com.aniflow.domain.intelligence.model.ReleaseCandidate
import com.aniflow.domain.selection.model.FallbackMode
import com.aniflow.domain.selection.model.FallbackTier
import com.aniflow.domain.selection.model.PreferenceMode
import com.aniflow.domain.selection.model.UserSelectionPreferences
import com.aniflow.domain.valueobject.Resolution
import com.aniflow.domain.valueobject.VideoCodec

/**
 * Organizes release candidates into discrete fallback tiers (Section 84, 85, 86, 87, 88).
 * Ensures Tier 1 candidates are prioritized before considering lower tiers.
 */
class FallbackTierEngine {

    fun buildDefaultTiers(preferences: UserSelectionPreferences): List<FallbackTier> {
        val tiers = mutableListOf<FallbackTier>()
        val targetRes = preferences.resolution.target
        val topCodec = preferences.codec.preferredCodecs.firstOrNull() ?: VideoCodec.HEVC
        val preferredUploader = preferences.uploader.preferred.firstOrNull()

        var tierIndex = 1

        // Tier 1: Ideal combo (Target resolution + preferred codec + preferred uploader if configured)
        tiers += FallbackTier(
            tierIndex = tierIndex++,
            name = "Tier 1: Preferred Target Combo",
            resolution = targetRes,
            codec = topCodec,
            uploader = preferredUploader
        )

        // Tier 2: Target resolution + preferred codec + Any uploader
        tiers += FallbackTier(
            tierIndex = tierIndex++,
            name = "Tier 2: Target Resolution & Codec (Any Uploader)",
            resolution = targetRes,
            codec = topCodec
        )

        // Tier 3: Target resolution + Any compatible codec
        tiers += FallbackTier(
            tierIndex = tierIndex++,
            name = "Tier 3: Target Resolution (Any Codec)",
            resolution = targetRes
        )

        // Tier 4+: Fallback resolutions
        preferences.resolution.fallbackResolutions.forEach { fallbackRes ->
            tiers += FallbackTier(
                tierIndex = tierIndex++,
                name = "Tier $tierIndex: Fallback Resolution (${fallbackRes.displayName})",
                resolution = fallbackRes
            )
        }

        return tiers
    }

    fun assignTier(candidate: ReleaseCandidate, tiers: List<FallbackTier>, preferences: UserSelectionPreferences): Int {
        val rel = candidate.release
        val technical = rel.technicalMetadata

        for (tier in tiers) {
            val resMatch = tier.resolution == null || technical.resolution == tier.resolution
            val codecMatch = tier.codec == null || technical.videoCodec == tier.codec
            val uploaderMatch = tier.uploader == null || rel.uploader.equals(tier.uploader, ignoreCase = true)
            val groupMatch = tier.group == null || rel.groupCandidate.equals(tier.group, ignoreCase = true)

            if (resMatch && codecMatch && uploaderMatch && groupMatch) {
                return tier.tierIndex
            }
        }

        return tiers.size + 1
    }

    /**
     * Overload for SelectionCandidate (Step 21 Domain Model).
     */
    fun assignTierForCandidate(
        candidate: com.aniflow.domain.selection.context.SelectionCandidate,
        tiers: List<FallbackTier>,
        preferences: UserSelectionPreferences
    ): Int {
        val technical = candidate.technical

        for (tier in tiers) {
            val resMatch = tier.resolution == null || technical.resolution == tier.resolution
            val codecMatch = tier.codec == null || technical.videoCodec == tier.codec
            val uploaderMatch = tier.uploader == null || candidate.uploader.equals(tier.uploader, ignoreCase = true)
            val groupMatch = tier.group == null || candidate.releaseGroup.equals(tier.group, ignoreCase = true)

            if (resMatch && codecMatch && uploaderMatch && groupMatch) {
                return tier.tierIndex
            }
        }

        return tiers.size + 1
    }

    fun filterByFallbackMode(

        candidates: List<Pair<ReleaseCandidate, Int>>,
        mode: FallbackMode
    ): List<Pair<ReleaseCandidate, Int>> = when (mode) {
        FallbackMode.NoFallback -> candidates.filter { it.second == 1 }
        FallbackMode.ControlledFallback -> {
            val minTier = candidates.minOfOrNull { it.second } ?: 1
            candidates.filter { it.second == minTier }
        }
        FallbackMode.AnyCompatible -> candidates
    }
}
