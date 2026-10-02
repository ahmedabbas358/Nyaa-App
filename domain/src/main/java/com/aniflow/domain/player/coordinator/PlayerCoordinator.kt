package com.aniflow.domain.player.coordinator

import com.aniflow.domain.identity.LibraryMediaId
import com.aniflow.domain.player.model.NextEpisodeResolution
import com.aniflow.domain.player.model.PlaybackItem
import com.aniflow.domain.player.model.PlaybackQueue
import com.aniflow.domain.player.model.PlaybackState
import com.aniflow.domain.player.model.PlayerPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Pure domain coordinator contract for player control (Section 13, 20).
 * Orchestrates playback, queue progression, and watch state updates without leaking Media3 to domain.
 */
interface PlayerCoordinator {
    val playbackState: StateFlow<PlaybackState>
    val playbackQueue: StateFlow<PlaybackQueue>
    val currentPreferences: StateFlow<PlayerPreferences>

    suspend fun prepare(item: PlaybackItem)
    suspend fun play()
    suspend fun pause()
    suspend fun seekTo(positionMs: Long)
    suspend fun setSpeed(speed: Float)
    suspend fun setAudioTrack(trackId: String)
    suspend fun setSubtitleTrack(trackId: String?)
    suspend fun setSubtitleDelay(delayMs: Long)
    suspend fun setAudioDelay(delayMs: Long)
    suspend fun updatePreferences(preferences: PlayerPreferences)
    suspend fun resolveNextEpisode(): NextEpisodeResolution
    suspend fun playNextEpisode(): Boolean
    suspend fun syncWatchProgress(positionMs: Long, durationMs: Long, isCompleted: Boolean)
    suspend fun stopAndRelease()
}
