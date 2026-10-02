package com.aniflow.domain.selection.model

import com.aniflow.domain.valueobject.Resolution
import com.aniflow.domain.valueobject.VideoCodec

/**
 * Scope hierarchy for preference resolution (Section 27 & 28).
 * Scopes closer to the candidate or session context can override broader scopes.
 * Hierarchy:
 * SystemDefault -> Global -> Provider -> Anime -> Season -> Collection -> SearchSession -> Manual
 */
enum class PreferenceScope(val precedence: Int) {
    SystemDefault(0),
    Global(10),
    Provider(20),
    Anime(30),
    Season(40),
    Collection(50),
    Uploader(60),
    ReleaseGroup(70),
    SearchSession(80),
    Manual(100);

    fun hasHigherPrecedenceThan(other: PreferenceScope): Boolean = this.precedence > other.precedence
}

data class ScopedPreference<T>(
    val value: T,
    val scope: PreferenceScope,
    val isHardConstraint: Boolean = false
)

/**
 * Ephemeral session preferences (Section 90 & 91).
 * Applied during an active search/browse session without modifying the persisted DownloadProfile.
 */
data class SelectionSessionPreferences(
    val sessionId: String,
    val overrideResolution: Resolution? = null,
    val overrideCodec: VideoCodec? = null,
    val preferredUploader: String? = null,
    val blockedUploader: String? = null,
    val preferredGroup: String? = null,
    val maxSizeBytes: Long? = null,
    val minSeeders: Int? = null,
    val customWeights: SelectionWeights? = null
) {
    fun hasOverrides(): Boolean =
        overrideResolution != null ||
            overrideCodec != null ||
            preferredUploader != null ||
            blockedUploader != null ||
            preferredGroup != null ||
            maxSizeBytes != null ||
            minSeeders != null ||
            customWeights != null
}
