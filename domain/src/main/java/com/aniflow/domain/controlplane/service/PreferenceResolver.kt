package com.aniflow.domain.controlplane.service

import com.aniflow.domain.controlplane.models.ExtendedDownloadProfile
import com.aniflow.domain.valueobject.ByteSize
import com.aniflow.domain.valueobject.LanguageCode
import com.aniflow.domain.valueobject.Resolution
import com.aniflow.domain.valueobject.VideoCodec

data class ResolvedPreferences(
    val targetResolution: Resolution,
    val targetCodec: VideoCodec,
    val targetSubtitleLanguage: LanguageCode,
    val targetAudioLanguage: LanguageCode,
    val preferredUploader: String?,
    val preferredReleaseGroup: String?,
    val maxSizeBytes: Long?,
    val minSeeders: Int,
    val winningScope: String,
    val explanationTrace: List<String>
)

data class ScopedPreferenceInput(
    val animeTitle: String? = null,
    val manualOverrideResolution: Resolution? = null,
    val manualOverrideUploader: String? = null,
    val animeSpecificResolution: Resolution? = null,
    val uploaderPreferenceMap: Map<String, String> = emptyMap(), // Uploader -> "Preferred" | "Avoid"
    val groupPreferenceMap: Map<String, String> = emptyMap(),
    val activeProfile: ExtendedDownloadProfile? = null
)

/**
 * PreferenceResolver (Section 4).
 * Resolves user preferences across multiple nested scopes according to strict precedence:
 * Manual Override > Specific Anime > Uploader > Group > Profile Defaults > System Defaults.
 */
class PreferenceResolver {

    fun resolve(input: ScopedPreferenceInput): ResolvedPreferences {
        val trace = mutableListOf<String>()

        // 1. Resolution resolution
        val resolution = when {
            input.manualOverrideResolution != null -> {
                trace.add("Resolution ${input.manualOverrideResolution} picked from Manual User Override")
                input.manualOverrideResolution
            }
            input.animeSpecificResolution != null -> {
                trace.add("Resolution ${input.animeSpecificResolution} picked from Anime-Specific Preference")
                input.animeSpecificResolution
            }
            input.activeProfile != null -> {
                trace.add("Resolution ${input.activeProfile.preferredResolution} picked from Active Profile '${input.activeProfile.name}'")
                input.activeProfile.preferredResolution
            }
            else -> {
                trace.add("Resolution 1080p picked from System Default")
                Resolution.R1080p
            }
        }

        // 2. Codec resolution
        val codec = input.activeProfile?.preferredCodec ?: VideoCodec.HEVC
        trace.add("Codec $codec resolved from Profile/System default")

        // 3. Uploader preference
        val preferredUploader = input.manualOverrideUploader
            ?: input.uploaderPreferenceMap.entries.firstOrNull { it.value == "Preferred" }?.key
        if (preferredUploader != null) {
            trace.add("Preferred uploader '$preferredUploader' applied")
        }

        // 4. Release Group preference
        val preferredGroup = input.groupPreferenceMap.entries.firstOrNull { it.value == "Preferred" }?.key
        if (preferredGroup != null) {
            trace.add("Preferred group '$preferredGroup' applied")
        }

        return ResolvedPreferences(
            targetResolution = resolution,
            targetCodec = codec,
            targetSubtitleLanguage = input.activeProfile?.preferredSubtitle ?: LanguageCode.ENGLISH,
            targetAudioLanguage = input.activeProfile?.preferredAudio ?: LanguageCode.JAPANESE,
            preferredUploader = preferredUploader,
            preferredReleaseGroup = preferredGroup,
            maxSizeBytes = null,
            minSeeders = 3,
            winningScope = if (input.manualOverrideResolution != null) "ManualOverride" else "Profile",
            explanationTrace = trace
        )
    }
}
