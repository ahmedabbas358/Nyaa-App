package com.aniflow.feature.library.model

import com.aniflow.domain.identity.LibraryFileId
import com.aniflow.domain.identity.LibraryItemId
import java.time.Instant

data class VideoTrackInfo(
    val codec: String,
    val width: Int,
    val height: Int,
    val frameRate: Float? = null,
    val bitRate: Long? = null
)

data class AudioTrackInfo(
    val language: String? = null,
    val codec: String? = null,
    val channels: Int? = null,
    val title: String? = null
)

data class SubtitleTrackInfo(
    val language: String? = null,
    val format: String? = null, // srt, ass, vtt, etc.
    val isForced: Boolean = false,
    val title: String? = null
)

data class MediaProbeResult(
    val isSuccess: Boolean,
    val durationSeconds: Long = 0L,
    val videoTrack: VideoTrackInfo? = null,
    val audioTracks: List<AudioTrackInfo> = emptyList(),
    val subtitleTracks: List<SubtitleTrackInfo> = emptyList(),
    val containerFormat: String? = null,
    val errorMessage: String? = null
) {
    companion object {
        fun unavailable(reason: String = "Metadata probe unavailable"): MediaProbeResult =
            MediaProbeResult(isSuccess = false, errorMessage = reason)
    }
}

/**
 * MediaAssetGroup (Section 41, 42).
 * Unifies a primary video file with associated sidecar files (subtitles, audio tracks, metadata, thumbnails).
 */
data class MediaAssetGroup(
    val id: String,
    val primaryVideoFile: LibraryFileId?,
    val sidecarSubtitleFiles: List<LibraryFileId> = emptyList(),
    val thumbnailFile: LibraryFileId? = null,
    val metadataFile: LibraryFileId? = null
)

enum class LibrarySeriesStatus {
    NotStarted,
    Incomplete,
    Complete,
    Unknown
}

data class LibraryCoverage(
    val animeTitle: String,
    val seasonNumber: Int,
    val expectedEpisodes: Int,
    val availableEpisodes: Int,
    val missingEpisodeNumbers: List<Int> = emptyList()
) {
    val isComplete: Boolean get() = expectedEpisodes > 0 && availableEpisodes >= expectedEpisodes
}
