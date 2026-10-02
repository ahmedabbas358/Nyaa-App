package com.aniflow.domain.selection.override

import com.aniflow.domain.identity.EpisodeId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.selection.model.ManualOverride
import com.aniflow.domain.selection.model.PreferenceScope
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages explicit user manual release overrides (Section 33, 34, 35).
 * Explicit user choice has highest authority over all profiles, rules, and engines.
 */
class ManualOverrideManager {

    private val overrides = ConcurrentHashMap<EpisodeId, ManualOverride>()

    fun setOverride(
        episodeId: EpisodeId,
        releaseId: ReleaseId,
        reason: String = "User manual selection",
        scope: PreferenceScope = PreferenceScope.Manual
    ): ManualOverride {
        val override = ManualOverride(
            episodeId = episodeId,
            releaseId = releaseId,
            scope = scope,
            reason = reason,
            createdAt = Instant.now()
        )
        overrides[episodeId] = override
        return override
    }

    fun getOverride(episodeId: EpisodeId): ManualOverride? = overrides[episodeId]

    fun removeOverride(episodeId: EpisodeId): ManualOverride? = overrides.remove(episodeId)

    fun hasOverride(episodeId: EpisodeId): Boolean = overrides.containsKey(episodeId)

    fun clearAll() {
        overrides.clear()
    }
}
