package com.aniflow.domain.library.probe

import java.time.Duration

data class AudioTrackMetadata(
    val language: String? = null,
    val codec: String? = null,
    val channels: Int? = null,
    val title: String? = null,
    val isDefault: Boolean = false
)

data class SubtitleTrackMetadata(
    val language: String? = null,
    val format: String? = null, // ass, srt, vtt, etc.
    val isForced: Boolean = false,
    val isDefault: Boolean = false,
    val title: String? = null
)

/**
 * Pure domain representation of media stream inspection (Section 25, 26).
 */
data class MediaMetadata(
    val duration: Duration?,
    val width: Int?,
    val height: Int?,
    val videoCodec: String?,
    val audioTracks: List<AudioTrackMetadata> = emptyList(),
    val subtitleTracks: List<SubtitleTrackMetadata> = emptyList(),
    val frameRate: Double? = null,
    val bitDepth: Int? = null,
    val hdr: String? = null,
    val container: String? = null
) {
    val resolutionLabel: String?
        get() = when {
            height == null -> null
            height >= 2160 -> "4K"
            height >= 1080 -> "1080p"
            height >= 720 -> "720p"
            height >= 480 -> "480p"
            else -> "${height}p"
        }
}

enum class ProbeState {
    NotProbed,
    Queued,
    Probing,
    Succeeded,
    Partial,
    Failed,
    Unsupported
}

enum class ProbePolicy {
    Never,
    OnScan,
    OnMatch,
    OnPlayback,
    OnUserRequest,
    AfterDownload
}

/**
 * Discrepancy between what the release title claimed and what the actual media stream contains (Section 103).
 */
data class MediaMetadataConflict(
    val field: String,
    val claimedValue: String,
    val probedValue: String,
    val resolutionAdvice: String
)
