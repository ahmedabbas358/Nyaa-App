package com.aniflow.domain.controlplane.models

import com.aniflow.domain.identity.DownloadProfileId
import com.aniflow.domain.valueobject.ByteSize
import com.aniflow.domain.valueobject.LanguageCode
import com.aniflow.domain.valueobject.Resolution
import com.aniflow.domain.valueobject.VideoCodec

enum class SelectionStrategy {
    QualityFirst,
    SizeFirst,
    UploaderFirst,
    AvailabilityFirst,
    ConsistencyFirst,
    Balanced,
    Manual
}

data class StrategyWeights(
    val resolutionWeight: Int = 30,
    val codecWeight: Int = 20,
    val uploaderWeight: Int = 15,
    val releaseGroupWeight: Int = 10,
    val sourceWeight: Int = 10,
    val seedersWeight: Int = 5,
    val sizeWeight: Int = 5,
    val subtitleWeight: Int = 5
) {
    val totalWeight: Int
        get() = resolutionWeight + codecWeight + uploaderWeight + releaseGroupWeight + sourceWeight + seedersWeight + sizeWeight + subtitleWeight
}

data class FallbackTier(
    val tierNumber: Int,
    val name: String,
    val resolution: Resolution? = null,
    val codec: VideoCodec? = null,
    val allowAnyUploader: Boolean = false,
    val allowAnyReleaseGroup: Boolean = false,
    val maxSizeBytes: Long? = null,
    val minSeeders: Int = 3,
    val explanation: String
)

/**
 * Extended Download Profile (Sections 6, 7, 8, 9).
 * Defines comprehensive quality targets, weights, multi-level fallback tiers, and storage bindings.
 */
data class ExtendedDownloadProfile(
    val id: DownloadProfileId,
    val name: String,
    val strategy: SelectionStrategy = SelectionStrategy.Balanced,
    val weights: StrategyWeights = StrategyWeights(),
    val fallbackTiers: List<FallbackTier> = listOf(
        FallbackTier(1, "Tier 1: Best Quality", Resolution.R1080p, VideoCodec.HEVC, allowAnyUploader = false, explanation = "1080p HEVC with preferred uploader"),
        FallbackTier(2, "Tier 2: Universal Uploader", Resolution.R1080p, VideoCodec.HEVC, allowAnyUploader = true, explanation = "1080p HEVC with any uploader"),
        FallbackTier(3, "Tier 3: Standard Codec", Resolution.R1080p, VideoCodec.AVC, allowAnyUploader = true, explanation = "1080p H.264 fallback"),
        FallbackTier(4, "Tier 4: Compact Resolution", Resolution.R720p, VideoCodec.HEVC, allowAnyUploader = true, explanation = "720p HEVC fallback"),
        FallbackTier(5, "Tier 5: Any Compatible", null, null, allowAnyUploader = true, allowAnyReleaseGroup = true, explanation = "Any compatible release with seeds")
    ),
    val preferredResolution: Resolution = Resolution.R1080p,
    val preferredCodec: VideoCodec = VideoCodec.HEVC,
    val preferredSubtitle: LanguageCode = LanguageCode.ENGLISH,
    val preferredAudio: LanguageCode = LanguageCode.JAPANESE,
    val targetStorageLocationId: String? = null,
    val networkPolicy: NetworkPolicyType = NetworkPolicyType.WiFiOnly,
    val isDefault: Boolean = false
) {
    init {
        require(name.isNotBlank()) { "Profile name cannot be blank" }
        require(fallbackTiers.isNotEmpty()) { "Profile must define at least one fallback tier" }
    }
}
