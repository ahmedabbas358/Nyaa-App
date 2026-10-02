package com.aniflow.domain.selection.lock

import com.aniflow.domain.identity.EpisodeId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.selection.model.ExistingMediaState
import com.aniflow.domain.selection.model.SelectionLock
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages selection locks to protect chosen candidates from being automatically replaced (Section 118, 119, 120, 122, 123).
 */
class SelectionLockManager {

    private val locks = ConcurrentHashMap<EpisodeId, SelectionLock>()

    fun lock(episodeId: EpisodeId, releaseId: ReleaseId): SelectionLock {
        val lock = SelectionLock(
            episodeId = episodeId,
            releaseId = releaseId,
            lockedAt = Instant.now()
        )
        locks[episodeId] = lock
        return lock
    }

    fun unlock(episodeId: EpisodeId): SelectionLock? = locks.remove(episodeId)

    fun isLocked(episodeId: EpisodeId): Boolean = locks.containsKey(episodeId)

    fun getLock(episodeId: EpisodeId): SelectionLock? = locks[episodeId]

    /**
     * Protects active and completed downloads from unexpected automatic substitution (Section 122, 123).
     */
    fun shouldProtectFromAutoReplacement(episodeId: EpisodeId, state: ExistingMediaState): Boolean {
        if (isLocked(episodeId)) return true
        return state == ExistingMediaState.Downloading || state == ExistingMediaState.Downloaded
    }

    fun clearAll() {
        locks.clear()
    }
}
