package com.aniflow.domain.selection.model

import com.aniflow.domain.valueobject.AudioChannels
import com.aniflow.domain.valueobject.BitDepth
import com.aniflow.domain.valueobject.ByteSize
import com.aniflow.domain.valueobject.HdrType
import com.aniflow.domain.valueobject.LanguageCode
import com.aniflow.domain.valueobject.MediaSource
import com.aniflow.domain.valueobject.Resolution
import com.aniflow.domain.valueobject.SubtitleFormat
import com.aniflow.domain.valueobject.VideoCodec

enum class PreferenceMode {
    Required,
    Preferred,
    Neutral,
    Avoid,
    Forbidden
}

data class Preference<T>(
    val mode: PreferenceMode,
    val value: T?,
    val weight: Int = 10
)

enum class ResolutionMatchMode {
    Exact,
    AtLeast,
    AtMost,
    Prefer,
    AllowAny
}

data class ResolutionPreference(
    val mode: ResolutionMatchMode = ResolutionMatchMode.Prefer,
    val target: Resolution = Resolution.R1080p,
    val fallbackResolutions: List<Resolution> = listOf(Resolution.R720p, Resolution.R480p)
)

data class CodecPreference(
    val preferredCodecs: List<VideoCodec> = listOf(VideoCodec.HEVC, VideoCodec.AV1, VideoCodec.AVC),
    val codecRanks: Map<VideoCodec, Int> = mapOf(
        VideoCodec.HEVC to 100,
        VideoCodec.AV1 to 80,
        VideoCodec.AVC to 50
    ),
    val allowUnknown: Boolean = true
) {
    fun getRank(codec: VideoCodec?): Int = when (codec) {
        null -> if (allowUnknown) 10 else -20
        is VideoCodec.Unknown -> if (allowUnknown) 10 else -20
        else -> codecRanks[codec] ?: 20
    }
}

data class UploaderPreference(
    val preferred: Set<String> = emptySet(),
    val neutral: Set<String> = emptySet(),
    val avoid: Set<String> = emptySet(),
    val blocked: Set<String> = emptySet()
) {
    fun getDisposition(uploader: String?): PreferenceMode {
        if (uploader.isNullOrBlank()) return PreferenceMode.Neutral
        val clean = uploader.trim().lowercase()
        return when {
            blocked.any { it.trim().lowercase() == clean } -> PreferenceMode.Forbidden
            preferred.any { it.trim().lowercase() == clean } -> PreferenceMode.Preferred
            avoid.any { it.trim().lowercase() == clean } -> PreferenceMode.Avoid
            else -> PreferenceMode.Neutral
        }
    }
}

data class ReleaseGroupPreference(
    val preferred: Set<String> = emptySet(),
    val neutral: Set<String> = emptySet(),
    val avoid: Set<String> = emptySet(),
    val blocked: Set<String> = emptySet()
) {
    fun getDisposition(group: String?): PreferenceMode {
        if (group.isNullOrBlank()) return PreferenceMode.Neutral
        val clean = group.trim().lowercase()
        return when {
            blocked.any { it.trim().lowercase() == clean } -> PreferenceMode.Forbidden
            preferred.any { it.trim().lowercase() == clean } -> PreferenceMode.Preferred
            avoid.any { it.trim().lowercase() == clean } -> PreferenceMode.Avoid
            else -> PreferenceMode.Neutral
        }
    }
}

data class LanguageRule(
    val primary: List<LanguageCode> = emptyList(),
    val secondary: List<LanguageCode> = emptyList(),
    val allowed: List<LanguageCode> = emptyList(),
    val forbidden: List<LanguageCode> = emptyList(),
    val isRequired: Boolean = false
)

enum class MultiAudioPreference {
    SingleAudioPreferred,
    DualAudioPreferred,
    MultiAudioPreferred,
    Any
}

data class SubtitlePolicy(
    val requiredLanguages: Set<LanguageCode> = setOf(LanguageCode.ENGLISH),
    val preferredLanguages: Set<LanguageCode> = setOf(LanguageCode.ENGLISH, LanguageCode.ARABIC),
    val forbiddenLanguages: Set<LanguageCode> = emptySet(),
    val multipleSubtitlesPreferred: Boolean = false,
    val softsubRequired: Boolean = false,
    val hardsubAvoid: Boolean = false,
    val preferredFormats: Set<SubtitleFormat> = setOf(SubtitleFormat.ASS, SubtitleFormat.SRT)
)

data class SizePolicy(
    val hardMaxBytes: Long? = null,
    val preferredMaxBytes: Long? = null,
    val preferredMinBytes: Long? = null,
    val hardMinBytes: Long? = null
) {
    companion object {
        fun ofGigabytes(hardMaxGb: Double? = null, preferredMaxGb: Double? = null): SizePolicy =
            SizePolicy(
                hardMaxBytes = hardMaxGb?.let { (it * 1024 * 1024 * 1024).toLong() },
                preferredMaxBytes = preferredMaxGb?.let { (it * 1024 * 1024 * 1024).toLong() }
            )
    }
}

enum class AvailabilityAction {
    Allowed,
    Warn,
    Reject
}

data class SeederPolicy(
    val minSeeders: Int = 3,
    val preferredSeeders: Int = 10,
    val onZeroSeeders: AvailabilityAction = AvailabilityAction.Reject,
    val onBelowMinimum: AvailabilityAction = AvailabilityAction.Reject
)

data class SourcePreference(
    val preferred: Set<MediaSource> = setOf(MediaSource.WebRip, MediaSource.BluRay),
    val allowed: Set<MediaSource> = setOf(MediaSource.WebRip, MediaSource.BluRay, MediaSource.TV, MediaSource.HDTV),
    val forbidden: Set<MediaSource> = emptySet()
)

data class HdrPreference(
    val mode: PreferenceMode = PreferenceMode.Neutral,
    val allowedTypes: Set<HdrType> = setOf(HdrType.None, HdrType.HDR10, HdrType.HDR10Plus, HdrType.DolbyVision)
)

data class BitDepthPreference(
    val preferred: BitDepth = BitDepth.Bit10,
    val isRequired: Boolean = false
)

data class AudioChannelsPreference(
    val preferred: AudioChannels = AudioChannels.Stereo,
    val minChannels: Int? = null,
    val maxChannels: Int? = null
)

/**
 * Tiered preference classification for fallback grouping.
 * Enables searching for Tier 1 candidates first, and only falling back to Tier 2 if no Tier 1 candidate is eligible.
 */
data class FallbackTier(
    val tierIndex: Int,
    val name: String,
    val resolution: Resolution? = null,
    val codec: VideoCodec? = null,
    val uploader: String? = null,
    val group: String? = null,
    val source: MediaSource? = null
)
