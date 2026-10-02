package com.aniflow.domain.player.model

import com.aniflow.domain.identity.AnimeId
import com.aniflow.domain.identity.EpisodeId
import com.aniflow.domain.identity.LibraryMediaId
import com.aniflow.domain.valueobject.AudioChannels
import com.aniflow.domain.valueobject.LanguageCode

/**
 * Pure domain representation of a playable item (Section 13, 20).
 * Free from any Android or ExoPlayer/Media3 dependencies.
 */
data class PlaybackItem(
    val mediaId: LibraryMediaId,
    val animeId: AnimeId,
    val animeTitle: String,
    val seasonNumber: Int,
    val episodeNumber: Double,
    val episodeTitle: String?,
    val mediaFilePath: String,
    val durationMs: Long = 0L,
    val initialPositionMs: Long = 0L,
    val audioTracks: List<AudioTrackInfo> = emptyList(),
    val subtitleTracks: List<SubtitleTrackInfo> = emptyList()
)

sealed interface PlaybackState {
    data object Idle : PlaybackState
    data object Buffering : PlaybackState
    data class Playing(val positionMs: Long, val durationMs: Long) : PlaybackState
    data class Paused(val positionMs: Long, val durationMs: Long) : PlaybackState
    data object Ended : PlaybackState
    data class Error(val message: String) : PlaybackState
}

data class AudioTrackInfo(
    val id: String,
    val label: String,
    val language: LanguageCode?,
    val channels: AudioChannels? = null,
    val isSelected: Boolean = false
)

data class SubtitleTrackInfo(
    val id: String,
    val label: String,
    val language: LanguageCode?,
    val isSelected: Boolean = false
)

data class SubtitleStyle(
    val fontSizeSp: Int = 18,
    val textColorHex: String = "#FFFFFF",
    val backgroundColorHex: String = "#00000000",
    val outline: Boolean = true,
    val delayMs: Long = 0L
)

enum class PlayerOrientation {
    Auto,
    SensorLandscape,
    Portrait
}

/**
 * Player preferences integrating with user profiles and application settings (Section 17).
 */
data class PlayerPreferences(
    val defaultPlaybackSpeed: Float = 1.0f,
    val preferredAudioLanguage: LanguageCode = LanguageCode.JAPANESE,
    val preferredSubtitleLanguage: LanguageCode = LanguageCode.ENGLISH,
    val autoPlayNext: Boolean = true,
    val autoResume: Boolean = true,
    val skipIntro: Boolean = false,
    val skipOutro: Boolean = false,
    val subtitleStyle: SubtitleStyle = SubtitleStyle(),
    val audioDelayMs: Long = 0L,
    val orientation: PlayerOrientation = PlayerOrientation.SensorLandscape
)

/**
 * Dedicated Episode Playback Queue (Section 19).
 * Explicitly separate from DownloadQueue.
 */
data class PlaybackQueue(
    val currentItem: PlaybackItem?,
    val upcomingItems: List<PlaybackItem> = emptyList()
) {
    val hasNext: Boolean get() = upcomingItems.isNotEmpty()

    fun enqueue(item: PlaybackItem): PlaybackQueue =
        copy(upcomingItems = upcomingItems + item)

    fun removeAt(index: Int): PlaybackQueue =
        if (index in upcomingItems.indices) {
            val list = upcomingItems.toMutableList()
            list.removeAt(index)
            copy(upcomingItems = list)
        } else this

    fun advance(): Pair<PlaybackItem?, PlaybackQueue> {
        val next = upcomingItems.firstOrNull()
        val rest = upcomingItems.drop(1)
        return next to PlaybackQueue(currentItem = next, upcomingItems = rest)
    }
}

/**
 * Outcome when attempting to play next episode (Section 18).
 */
sealed interface NextEpisodeResolution {
    data class AvailableLocally(val nextItem: PlaybackItem) : NextEpisodeResolution
    data class NotDownloaded(val animeId: AnimeId, val seasonNumber: Int, val nextEpisodeNumber: Double) : NextEpisodeResolution
    data object EndOfSeries : NextEpisodeResolution
}
