package com.aniflow.domain.model.aggregate.organization

import com.aniflow.domain.identity.DownloadProfileId
import com.aniflow.domain.valueobject.ByteSize
import com.aniflow.domain.valueobject.LanguageCode
import com.aniflow.domain.valueobject.Resolution
import com.aniflow.domain.valueobject.VideoCodec

/**
 * Requirement level for user preferences (Section 45).
 */
sealed interface PreferenceRequirement<out T> {
    data class Required<T>(val value: T) : PreferenceRequirement<T>
    data class Preferred<T>(val value: T) : PreferenceRequirement<T>
    data object Ignored : PreferenceRequirement<Nothing>
    data class Forbidden<T>(val value: T) : PreferenceRequirement<T>
}

/**
 * Set of download and quality preferences (Section 45).
 */
data class DownloadPreferences(
    val resolution: PreferenceRequirement<Resolution> = PreferenceRequirement.Preferred(Resolution.R1080p),
    val codec: PreferenceRequirement<VideoCodec> = PreferenceRequirement.Preferred(VideoCodec.HEVC),
    val audioLanguage: PreferenceRequirement<LanguageCode> = PreferenceRequirement.Preferred(LanguageCode.JAPANESE),
    val subtitleLanguage: PreferenceRequirement<LanguageCode> = PreferenceRequirement.Preferred(LanguageCode.ENGLISH),
    val uploader: PreferenceRequirement<String> = PreferenceRequirement.Ignored,
    val releaseGroup: PreferenceRequirement<String> = PreferenceRequirement.Ignored,
    val maxSize: ByteSize? = null,
    val minSeeders: Int? = 3
) {
    init {
        if (minSeeders != null) {
            require(minSeeders >= 0) { "minSeeders cannot be negative: $minSeeders" }
        }
    }
}

/**
 * Persisted profile of user download preferences (Section 44).
 */
data class DownloadProfile(
    val id: DownloadProfileId,
    val name: String,
    val preferences: DownloadPreferences = DownloadPreferences(),
    val isDefault: Boolean = false
) {
    init {
        require(name.isNotBlank()) { "DownloadProfile name cannot be blank" }
    }
}
