package com.aniflow.domain.selection.preference

import com.aniflow.domain.selection.model.PreferenceMode
import com.aniflow.domain.selection.model.ResolutionMatchMode
import com.aniflow.domain.selection.model.ResolutionPreference
import com.aniflow.domain.selection.model.ResolvedPreferences
import com.aniflow.domain.selection.model.UserSelectionPreferences
import com.aniflow.domain.valueobject.Resolution
import com.aniflow.domain.valueobject.VideoCodec

/**
 * Step 21 — Preference Layer Hierarchy (Section 10, 11).
 */
enum class PreferenceLayer(val priority: Int) {
    Global(1),
    Provider(2),
    UploaderGroup(3),
    CollectionProfile(4),
    Anime(5),
    Season(6),
    ExplicitRule(7),
    UserLock(8),
    ManualOverride(9)
}

data class LayeredResolutionPreference(
    val layer: PreferenceLayer,
    val resolution: Resolution,
    val isRequired: Boolean = false
)

data class LayeredCodecPreference(
    val layer: PreferenceLayer,
    val codecs: List<VideoCodec>,
    val isRequired: Boolean = false
)

data class LayeredUploaderPreference(
    val layer: PreferenceLayer,
    val preferredUploaders: Set<String> = emptySet(),
    val forbiddenUploaders: Set<String> = emptySet()
)

data class LayeredReleaseGroupPreference(
    val layer: PreferenceLayer,
    val preferredGroups: Set<String> = emptySet(),
    val forbiddenGroups: Set<String> = emptySet()
)

/**
 * Step 21 — Preference Precedence Policy (Section 11, 12).
 * Deterministically merges layered preferences according to strict precedence rules:
 * Manual Override > User Lock > Explicit Rule > Season > Anime > Profile > Uploader/Group > Provider > Global.
 */
class PreferencePrecedencePolicy {

    fun resolveResolution(
        layers: List<LayeredResolutionPreference>,
        defaultResolution: Resolution = Resolution.R1080p
    ): ResolutionPreference {
        if (layers.isEmpty()) return ResolutionPreference(target = defaultResolution)

        // Sort descending by priority (ManualOverride > UserLock > ... > Global)
        val sorted = layers.sortedByDescending { it.layer.priority }
        val highest = sorted.first()

        val mode = if (highest.isRequired) ResolutionMatchMode.Exact else ResolutionMatchMode.Prefer
        val fallbacks = sorted.drop(1).map { it.resolution }.distinct().filter { it != highest.resolution }

        return ResolutionPreference(
            mode = mode,
            target = highest.resolution,
            fallbackResolutions = fallbacks.ifEmpty { listOf(Resolution.R720p, Resolution.R480p) }
        )
    }

    fun resolveCodecs(
        layers: List<LayeredCodecPreference>,
        defaultCodecs: List<VideoCodec> = listOf(VideoCodec.HEVC, VideoCodec.AV1, VideoCodec.AVC)
    ): List<VideoCodec> {
        if (layers.isEmpty()) return defaultCodecs

        val sorted = layers.sortedByDescending { it.layer.priority }
        val merged = mutableListOf<VideoCodec>()
        for (layer in sorted) {
            for (c in layer.codecs) {
                if (!merged.contains(c)) {
                    merged.add(c)
                }
            }
        }
        return merged.ifEmpty { defaultCodecs }
    }

    fun resolveUploaders(
        layers: List<LayeredUploaderPreference>
    ): Pair<Set<String>, Set<String>> {
        val preferred = mutableSetOf<String>()
        val forbidden = mutableSetOf<String>()

        // Ascending to let higher layers overwrite lower layers
        val sorted = layers.sortedBy { it.layer.priority }
        for (layer in sorted) {
            forbidden.addAll(layer.forbiddenUploaders)
            preferred.removeAll(layer.forbiddenUploaders)
            preferred.addAll(layer.preferredUploaders)
            forbidden.removeAll(layer.preferredUploaders)
        }

        return Pair(preferred, forbidden)
    }

    fun mergeLayers(
        basePreferences: ResolvedPreferences,
        resolutionLayers: List<LayeredResolutionPreference> = emptyList(),
        codecLayers: List<LayeredCodecPreference> = emptyList(),
        uploaderLayers: List<LayeredUploaderPreference> = emptyList()
    ): ResolvedPreferences {
        val res = if (resolutionLayers.isNotEmpty()) resolveResolution(resolutionLayers) else basePreferences.resolution
        val codecs = if (codecLayers.isNotEmpty()) resolveCodecs(codecLayers) else basePreferences.codec.preferredCodecs
        val (prefUpl, forbUpl) = if (uploaderLayers.isNotEmpty()) resolveUploaders(uploaderLayers) else Pair(basePreferences.uploader.preferred, basePreferences.uploader.blocked)

        return basePreferences.copy(
            resolution = res,
            codec = basePreferences.codec.copy(preferredCodecs = codecs),
            uploader = basePreferences.uploader.copy(
                preferred = prefUpl,
                blocked = forbUpl
            )
        )
    }
}
