package com.aniflow.domain.selection.preference

import com.aniflow.domain.intelligence.model.ReleaseCandidate
import com.aniflow.domain.selection.model.DownloadProfile
import com.aniflow.domain.selection.model.PreferenceMode
import com.aniflow.domain.selection.model.PreferenceScope
import com.aniflow.domain.selection.model.ResolutionMatchMode
import com.aniflow.domain.selection.model.SelectionSessionPreferences
import com.aniflow.domain.selection.model.UserSelectionPreferences

/**
 * Resolves effective preferences across hierarchical scopes (Section 27, 28, 29, 30, 31).
 * Precedence rule: Closer scopes override broader scopes (e.g. Session > Anime > Global > Default).
 */
class PreferenceResolver {

    fun resolve(
        candidate: ReleaseCandidate,
        basePreferences: UserSelectionPreferences,
        profile: DownloadProfile?,
        sessionPreferences: SelectionSessionPreferences? = null,
        scope: PreferenceScope = PreferenceScope.Global
    ): UserSelectionPreferences {
        var resolved = profile?.preferences ?: basePreferences

        // Apply session-level overrides if present
        if (sessionPreferences != null && sessionPreferences.hasOverrides()) {
            val resPref = sessionPreferences.overrideResolution?.let {
                resolved.resolution.copy(target = it, mode = ResolutionMatchMode.Prefer)
            } ?: resolved.resolution

            val codecPref = sessionPreferences.overrideCodec?.let {
                resolved.codec.copy(preferredCodecs = listOf(it) + resolved.codec.preferredCodecs.filterNot { c -> c == it })
            } ?: resolved.codec

            val uploaderPref = resolved.uploader.copy(
                preferred = if (sessionPreferences.preferredUploader != null) {
                    resolved.uploader.preferred + sessionPreferences.preferredUploader
                } else resolved.uploader.preferred,
                blocked = if (sessionPreferences.blockedUploader != null) {
                    resolved.uploader.blocked + sessionPreferences.blockedUploader
                } else resolved.uploader.blocked
            )

            val groupPref = resolved.releaseGroup.copy(
                preferred = if (sessionPreferences.preferredGroup != null) {
                    resolved.releaseGroup.preferred + sessionPreferences.preferredGroup
                } else resolved.releaseGroup.preferred
            )

            val sizePolicy = if (sessionPreferences.maxSizeBytes != null) {
                resolved.sizePolicy.copy(hardMaxBytes = sessionPreferences.maxSizeBytes)
            } else resolved.sizePolicy

            val seederPolicy = if (sessionPreferences.minSeeders != null) {
                resolved.seederPolicy.copy(minSeeders = sessionPreferences.minSeeders)
            } else resolved.seederPolicy

            resolved = resolved.copy(
                resolution = resPref,
                codec = codecPref,
                uploader = uploaderPref,
                releaseGroup = groupPref,
                sizePolicy = sizePolicy,
                seederPolicy = seederPolicy
            )
        }

        return resolved
    }
}
