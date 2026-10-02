package com.aniflow.domain.intelligence.model

import com.aniflow.domain.valueobject.AudioChannels
import com.aniflow.domain.valueobject.AudioCodec
import com.aniflow.domain.valueobject.BitDepth
import com.aniflow.domain.valueobject.HdrType
import com.aniflow.domain.valueobject.LanguageCode
import com.aniflow.domain.valueobject.MediaSource
import com.aniflow.domain.valueobject.Resolution
import com.aniflow.domain.valueobject.SubtitleFormat
import com.aniflow.domain.valueobject.VideoCodec

/**
 * Step 19 — Technical Metadata (Section 81).
 * Strongly-typed domain representation of video, audio, subtitles, and container properties.
 */
data class TechnicalMetadata(
    val resolution: Resolution?,
    val codec: VideoCodec?,
    val bitDepth: Int? = null,
    val source: VideoSource? = null,
    val audio: List<AudioTrackDescriptor> = emptyList(),
    val subtitles: List<SubtitleDescriptor> = emptyList(),
    val hdr: HdrFormat? = null,
    val colorDepth: Int? = null,
    val frameRate: FrameRate? = null,
    val multiAudio: Boolean = false
) {
    val videoCodec: VideoCodec? get() = codec
    val audioTracks: List<com.aniflow.domain.valueobject.AudioTrack>
        get() = audio.map {
            com.aniflow.domain.valueobject.AudioTrack(
                language = it.language?.let { l -> LanguageCode(l) },
                codec = it.codec,
                channels = it.channels,
                label = it.label
            )
        }
    val subtitleTracks: List<com.aniflow.domain.valueobject.SubtitleTrack>
        get() = subtitles.map {
            com.aniflow.domain.valueobject.SubtitleTrack(
                language = it.language?.let { l -> LanguageCode(l) },
                format = it.format,
                label = it.label
            )
        }
    val mediaSource: MediaSource? get() = source?.toMediaSource()
}

/**
 * Canonical Video Source (Section 24).
 */
enum class VideoSource(val displayName: String) {
    WebDL("WEB-DL"),
    WebRip("WEBRip"),
    BluRay("BluRay"),
    BDRemux("BDRemux"),
    BDMV("BDMV"),
    DVD("DVD"),
    HDTV("HDTV"),
    TV("TV"),
    CAM("CAM"),
    HDTS("HDTS"),
    Unknown("Unknown");

    fun toMediaSource(): MediaSource = when (this) {
        WebDL, WebRip -> MediaSource.WebRip
        BluRay, BDRemux, BDMV -> MediaSource.BluRay
        DVD -> MediaSource.DVD
        HDTV -> MediaSource.HDTV
        TV -> MediaSource.TV
        else -> MediaSource.Unknown
    }

    companion object {
        fun fromString(value: String?): VideoSource {
            if (value.isNullOrBlank()) return Unknown
            val v = value.lowercase().trim()
            return when {
                v.contains("bdremux") -> BDRemux
                v.contains("bdmv") -> BDMV
                v.contains("bluray") || v.contains("blu-ray") || v.contains("bdrip") || v == "bd" -> BluRay
                v.contains("web-dl") || v.contains("webdl") -> WebDL
                v.contains("webrip") || v.contains("web") -> WebRip
                v.contains("hdtv") -> HDTV
                v.contains("dvdrip") || v.contains("dvd") -> DVD
                v.contains("hdts") -> HDTS
                v.contains("cam") -> CAM
                v == "tv" -> TV
                else -> Unknown
            }
        }
    }
}

/**
 * Audio Track Descriptor (Section 20, 21).
 */
data class AudioTrackDescriptor(
    val codec: AudioCodec? = null,
    val channels: AudioChannels? = null,
    val language: String? = null,
    val label: String? = null,
    val isDefault: Boolean = false
)

/**
 * Subtitle Descriptor (Section 22).
 */
data class SubtitleDescriptor(
    val language: String? = null,
    val type: SubtitleType = SubtitleType.SoftSub,
    val format: SubtitleFormat? = null,
    val isEmbedded: Boolean = true,
    val label: String? = null
)

enum class SubtitleType {
    SoftSub,
    HardSub,
    External,
    Unknown
}

/**
 * HDR Formats (Section 81).
 */
enum class HdrFormat(val displayName: String) {
    HDR10("HDR10"),
    HDR10Plus("HDR10+"),
    DolbyVision("Dolby Vision"),
    HLG("HLG");

    fun toHdrType(): HdrType = when (this) {
        HDR10 -> HdrType.HDR10
        HDR10Plus -> HdrType.HDR10Plus
        DolbyVision -> HdrType.DolbyVision
        HLG -> HdrType.HLG
    }
}

/**
 * Video Frame Rate (Section 81).
 */
data class FrameRate(
    val fps: Double,
    val isVariable: Boolean = false
) {
    val displayName: String get() = "${fps}fps"
}

/**
 * Release Technical Summary Formatter (Section 61).
 * Example: "1080p • HEVC • 10-bit • WEB-DL • Dual Audio • EN Subs"
 */
object TechnicalSummary {

    fun format(technical: TechnicalMetadata): String {
        val parts = mutableListOf<String>()

        technical.resolution?.displayName?.let { if (it != "Unknown") parts.add(it) }
        technical.codec?.displayName?.let { if (it != "Unknown Codec") parts.add(it.split(" / ").first()) }
        technical.bitDepth?.let { parts.add("${it}-bit") }
        technical.source?.let { if (it != VideoSource.Unknown) parts.add(it.displayName) }
        technical.hdr?.let { parts.add(it.displayName) }

        if (technical.multiAudio) {
            parts.add("Dual Audio")
        } else if (technical.audio.isNotEmpty()) {
            val audioInfo = technical.audio.firstOrNull()?.codec?.displayName
            if (audioInfo != null && audioInfo != "Unknown") {
                parts.add(audioInfo)
            }
        }

        if (technical.subtitles.isNotEmpty()) {
            val subLang = technical.subtitles.firstOrNull()?.language
            if (!subLang.isNullOrBlank()) {
                parts.add("${subLang.uppercase()} Subs")
            } else {
                parts.add("Subs")
            }
        }

        return if (parts.isEmpty()) "Not detected" else parts.joinToString(" • ")
    }
}
