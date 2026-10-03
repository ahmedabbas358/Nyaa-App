package com.aniflow.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aniflow.domain.identity.LibraryMediaId
import com.aniflow.domain.player.coordinator.PlayerCoordinator
import com.aniflow.domain.player.model.AudioTrackInfo
import com.aniflow.domain.player.model.PlaybackItem
import com.aniflow.domain.player.model.PlaybackQueue
import com.aniflow.domain.player.model.PlaybackState
import com.aniflow.domain.player.model.PlayerPreferences
import com.aniflow.domain.player.model.SubtitleStyle
import com.aniflow.domain.player.model.SubtitleTrackInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class PlayerUiState(
    val currentItem: PlaybackItem? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val speed: Float = 1.0f,
    val audioTracks: List<AudioTrackInfo> = emptyList(),
    val subtitleTracks: List<SubtitleTrackInfo> = emptyList(),
    val selectedAudioTrackId: String? = null,
    val selectedSubtitleTrackId: String? = null,
    val queue: PlaybackQueue = PlaybackQueue(currentItem = null),
    val preferences: PlayerPreferences = PlayerPreferences(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * PlayerViewModel (Section 156).
 * Pure presentation controller connecting UI to PlayerCoordinator.
 * Zero leaks of ExoPlayer or Media3 classes.
 */
class PlayerViewModel(
    private val playerCoordinator: PlayerCoordinator? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState(isLoading = true))
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        observeCoordinator()
    }

    private fun observeCoordinator() {
        if (playerCoordinator == null) {
            _uiState.value = _uiState.value.copy(isLoading = false)
            return
        }

        playerCoordinator.playbackState
            .onEach { state ->
                when (state) {
                    is PlaybackState.Buffering -> {
                        _uiState.value = _uiState.value.copy(isLoading = true)
                    }
                    is PlaybackState.Playing -> {
                        _uiState.value = _uiState.value.copy(
                            isPlaying = true,
                            positionMs = state.positionMs,
                            durationMs = state.durationMs,
                            isLoading = false
                        )
                    }
                    is PlaybackState.Paused -> {
                        _uiState.value = _uiState.value.copy(
                            isPlaying = false,
                            positionMs = state.positionMs,
                            durationMs = state.durationMs,
                            isLoading = false
                        )
                    }
                    is PlaybackState.Ended -> {
                        _uiState.value = _uiState.value.copy(
                            isPlaying = false,
                            isLoading = false
                        )
                    }
                    is PlaybackState.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isPlaying = false,
                            isLoading = false,
                            errorMessage = state.message
                        )
                    }
                    PlaybackState.Idle -> {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                }
            }
            .catch { ex ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = ex.message ?: "Player error occurred"
                )
            }
            .launchIn(viewModelScope)

        playerCoordinator.playbackQueue
            .onEach { queue ->
                _uiState.value = _uiState.value.copy(
                    queue = queue,
                    currentItem = queue.currentItem,
                    audioTracks = queue.currentItem?.audioTracks ?: emptyList(),
                    subtitleTracks = queue.currentItem?.subtitleTracks ?: emptyList(),
                    durationMs = queue.currentItem?.durationMs ?: _uiState.value.durationMs
                )
            }
            .launchIn(viewModelScope)

        playerCoordinator.currentPreferences
            .onEach { prefs ->
                _uiState.value = _uiState.value.copy(
                    preferences = prefs,
                    speed = prefs.defaultPlaybackSpeed
                )
            }
            .launchIn(viewModelScope)
    }

    fun togglePlayPause() {
        viewModelScope.launch {
            if (_uiState.value.isPlaying) {
                playerCoordinator?.pause()
            } else {
                playerCoordinator?.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        viewModelScope.launch {
            playerCoordinator?.seekTo(positionMs)
        }
    }

    fun seekRelative(deltaMs: Long) {
        val newPos = (_uiState.value.positionMs + deltaMs).coerceIn(0L, _uiState.value.durationMs)
        seekTo(newPos)
    }

    fun setSpeed(speed: Float) {
        viewModelScope.launch {
            playerCoordinator?.setSpeed(speed)
            _uiState.value = _uiState.value.copy(speed = speed)
        }
    }

    fun setAudioTrack(trackId: String) {
        viewModelScope.launch {
            playerCoordinator?.setAudioTrack(trackId)
            _uiState.value = _uiState.value.copy(selectedAudioTrackId = trackId)
        }
    }

    fun setSubtitleTrack(trackId: String?) {
        viewModelScope.launch {
            playerCoordinator?.setSubtitleTrack(trackId)
            _uiState.value = _uiState.value.copy(selectedSubtitleTrackId = trackId)
        }
    }

    fun setSubtitleDelay(delayMs: Long) {
        viewModelScope.launch {
            playerCoordinator?.setSubtitleDelay(delayMs)
            val updatedStyle = _uiState.value.preferences.subtitleStyle.copy(delayMs = delayMs)
            _uiState.value = _uiState.value.copy(
                preferences = _uiState.value.preferences.copy(subtitleStyle = updatedStyle)
            )
        }
    }

    fun setSubtitleFontSize(fontSizeSp: Int) {
        viewModelScope.launch {
            val updatedStyle = _uiState.value.preferences.subtitleStyle.copy(fontSizeSp = fontSizeSp)
            val updatedPrefs = _uiState.value.preferences.copy(subtitleStyle = updatedStyle)
            playerCoordinator?.updatePreferences(updatedPrefs)
            _uiState.value = _uiState.value.copy(preferences = updatedPrefs)
        }
    }

    fun playNextEpisode(): Boolean {
        var played = false
        viewModelScope.launch {
            played = playerCoordinator?.playNextEpisode() ?: false
        }
        return played
    }

    fun removeFromQueue(index: Int) {
        val updatedQueue = _uiState.value.queue.removeAt(index)
        _uiState.value = _uiState.value.copy(queue = updatedQueue)
    }
}
