package com.aniflow.domain.valueobject

/**
 * Strongly-typed technical metadata for video/audio streams.
 * Prevents loose string comparisons across the domain.
 */

sealed interface Resolution : Comparable<Resolution> {
    val height: Int
    val displayName: String

    data object R360p : Resolution { override val height = 360; override val displayName = "360p" }
    data object R480p : Resolution { override val height = 480; override val displayName = "480p" }
    data object R576p : Resolution { override val height = 576; override val displayName = "576p" }
    data object R720p : Resolution { override val height = 720; override val displayName = "720p" }
    data object R1080p : Resolution { override val height = 1080; override val displayName = "1080p" }
    data object R1440p : Resolution { override val height = 1440; override val displayName = "1440p" }
    data object R2160p : Resolution { override val height = 2160; override val displayName = "4K / 2160p" }
    data class Other(val custom: String, override val height: Int = 0) : Resolution {
        override val displayName = custom
    }
    data object Unknown : Resolution { override val height = 0; override val displayName = "Unknown" }

    override fun compareTo(other: Resolution): Int = this.height.compareTo(other.height)

    companion object {
        fun fromString(value: String?): Resolution {
            if (value.isNullOrBlank()) return Unknown
            val v = value.lowercase()
            return when {
                v.contains("2160") || v.contains("4k") || v.contains("uhd") -> R2160p
                v.contains("1440") || v.contains("2k") -> R1440p
                v.contains("1080") || v.contains("fhd") -> R1080p
                v.contains("720") || v.contains("hd") -> R720p
                v.contains("576") -> R576p
                v.contains("480") || v.contains("sd") -> R480p
                v.contains("360") -> R360p
                else -> Other(value)
            }
        }
    }
}

sealed interface VideoCodec {
    val displayName: String

    data object AVC : VideoCodec { override val displayName = "AVC / H.264" }
    data object HEVC : VideoCodec { override val displayName = "HEVC / H.265" }
    data object AV1 : VideoCodec { override val displayName = "AV1" }
    data object VP9 : VideoCodec { override val displayName = "VP9" }
    data class Other(val name: String) : VideoCodec { override val displayName = name }
    data object Unknown : VideoCodec { override val displayName = "Unknown Codec" }

    companion object {
        fun fromString(value: String?): VideoCodec {
            if (value.isNullOrBlank()) return Unknown
            val v = value.lowercase()
            return when {
                v.contains("hevc") || v.contains("x265") || v.contains("h265") || v.contains("h.265") -> HEVC
                v.contains("avc") || v.contains("x264") || v.contains("h264") || v.contains("h.264") -> AVC
                v.contains("av1") -> AV1
                v.contains("vp9") -> VP9
                else -> Other(value)
            }
        }
    }
}

sealed interface AudioCodec {
    val displayName: String

    data object AAC : AudioCodec { override val displayName = "AAC" }
    data object FLAC : AudioCodec { override val displayName = "FLAC" }
    data object OPUS : AudioCodec { override val displayName = "Opus" }
    data object AC3 : AudioCodec { override val displayName = "AC3" }
    data object EAC3 : AudioCodec { override val displayName = "E-AC3" }
    data object MP3 : AudioCodec { override val displayName = "MP3" }
    data object TrueHD : AudioCodec { override val displayName = "TrueHD" }
    data object DTS : AudioCodec { override val displayName = "DTS" }
    data class Other(val name: String) : AudioCodec { override val displayName = name }
    data object Unknown : AudioCodec { override val displayName = "Unknown" }

    companion object {
        fun fromString(value: String?): AudioCodec {
            if (value.isNullOrBlank()) return Unknown
            val v = value.lowercase()
            return when {
                v.contains("flac") -> FLAC
                v.contains("opus") -> OPUS
                v.contains("aac") -> AAC
                v.contains("eac3") || v.contains("e-ac3") -> EAC3
                v.contains("ac3") -> AC3
                v.contains("mp3") -> MP3
                v.contains("truehd") -> TrueHD
                v.contains("dts") -> DTS
                else -> Other(value)
            }
        }
    }
}

sealed interface AudioChannels {
    val channelCount: Int
    val displayName: String

    data object Mono : AudioChannels { override val channelCount = 1; override val displayName = "1.0 Mono" }
    data object Stereo : AudioChannels { override val channelCount = 2; override val displayName = "2.0 Stereo" }
    data object Surround5_1 : AudioChannels { override val channelCount = 6; override val displayName = "5.1 Surround" }
    data object Surround7_1 : AudioChannels { override val channelCount = 8; override val displayName = "7.1 Surround" }
    data class Other(val channels: String, override val channelCount: Int = 0) : AudioChannels {
        override val displayName = channels
    }
    data object Unknown : AudioChannels { override val channelCount = 0; override val displayName = "Unknown" }

    companion object {
        fun fromString(value: String?): AudioChannels {
            if (value.isNullOrBlank()) return Unknown
            val v = value.lowercase()
            return when {
                v.contains("7.1") -> Surround7_1
                v.contains("5.1") -> Surround5_1
                v.contains("2.0") || v.contains("stereo") -> Stereo
                v.contains("1.0") || v.contains("mono") -> Mono
                else -> Other(value)
            }
        }
    }
}

sealed interface BitDepth {
    val bits: Int

    data object Bit8 : BitDepth { override val bits = 8 }
    data object Bit10 : BitDepth { override val bits = 10 }
    data object Bit12 : BitDepth { override val bits = 12 }
    data object Unknown : BitDepth { override val bits = 0 }

    companion object {
        fun fromInt(bits: Int?): BitDepth = when (bits) {
            8 -> Bit8
            10 -> Bit10
            12 -> Bit12
            else -> Unknown
        }
    }
}

sealed interface HdrType {
    data object None : HdrType
    data object HDR10 : HdrType
    data object HDR10Plus : HdrType
    data object DolbyVision : HdrType
    data object HLG : HdrType
}

sealed interface SubtitleFormat {
    val displayName: String

    data object ASS : SubtitleFormat { override val displayName = "ASS / SSA" }
    data object SRT : SubtitleFormat { override val displayName = "SRT" }
    data object PGS : SubtitleFormat { override val displayName = "PGS (VobSub)" }
    data object VTT : SubtitleFormat { override val displayName = "WebVTT" }
    data class Other(val format: String) : SubtitleFormat { override val displayName = format }
    data object Unknown : SubtitleFormat { override val displayName = "Unknown" }
}

sealed interface MediaSource {
    val displayName: String

    data object TV : MediaSource { override val displayName = "TV" }
    data object WebRip : MediaSource { override val displayName = "WEB-DL / WebRip" }
    data object BluRay : MediaSource { override val displayName = "Blu-ray (BD)" }
    data object DVD : MediaSource { override val displayName = "DVD" }
    data object HDTV : MediaSource { override val displayName = "HDTV" }
    data class Other(val source: String) : MediaSource { override val displayName = source }
    data object Unknown : MediaSource { override val displayName = "Unknown" }

    companion object {
        fun fromString(value: String?): MediaSource {
            if (value.isNullOrBlank()) return Unknown
            val v = value.lowercase()
            return when {
                v.contains("bluray") || v.contains("blu-ray") || v.contains("bd") || v.contains("bdrip") -> BluRay
                v.contains("web") || v.contains("webrip") || v.contains("web-dl") || v.contains("crunchyroll") -> WebRip
                v.contains("hdtv") -> HDTV
                v.contains("tv") -> TV
                v.contains("dvd") || v.contains("dvdrip") -> DVD
                else -> Other(value)
            }
        }
    }
}

data class AudioTrack(
    val language: LanguageCode?,
    val codec: AudioCodec?,
    val channels: AudioChannels?,
    val label: String?
)

data class SubtitleTrack(
    val language: LanguageCode?,
    val format: SubtitleFormat?,
    val label: String?
)

/**
 * Release technical metadata adhering strictly to Section 24.
 */
data class ReleaseTechnicalMetadata(
    val resolution: Resolution?,
    val videoCodec: VideoCodec?,
    val audioTracks: List<AudioTrack> = emptyList(),
    val subtitles: List<SubtitleTrack> = emptyList(),
    val source: MediaSource?,
    val bitDepth: BitDepth?,
    val hdr: HdrType? = HdrType.None,
    val channels: AudioChannels? = null
)
