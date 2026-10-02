package com.aniflow.domain.intelligence.detector

import com.aniflow.domain.intelligence.model.AudioTrackDescriptor
import com.aniflow.domain.intelligence.model.DetectionResult
import com.aniflow.domain.intelligence.model.HdrFormat
import com.aniflow.domain.intelligence.model.ParseContext
import com.aniflow.domain.intelligence.model.ParsingWarning
import com.aniflow.domain.intelligence.model.ReleaseToken
import com.aniflow.domain.intelligence.model.SubtitleDescriptor
import com.aniflow.domain.intelligence.model.SubtitleType
import com.aniflow.domain.intelligence.model.TokenEvidence
import com.aniflow.domain.intelligence.model.VideoSource
import com.aniflow.domain.valueobject.AudioChannels
import com.aniflow.domain.valueobject.AudioCodec
import com.aniflow.domain.valueobject.AudioTrack
import com.aniflow.domain.valueobject.BitDepth
import com.aniflow.domain.valueobject.HdrType
import com.aniflow.domain.valueobject.LanguageCode
import com.aniflow.domain.valueobject.MediaSource
import com.aniflow.domain.valueobject.Resolution
import com.aniflow.domain.valueobject.SubtitleFormat
import com.aniflow.domain.valueobject.SubtitleTrack
import com.aniflow.domain.valueobject.VideoCodec
import java.util.Locale

/**
 * Extracts video display resolution (Section 17).
 */
class ResolutionDetector : MetadataDetector<Resolution> {
    override val name: String = "ResolutionDetector"

    override fun detect(context: ParseContext): DetectionResult<Resolution> {
        for (token in context.tokens) {
            val lower = token.normalized.lowercase(Locale.ROOT)
            val res = when {
                lower == "1080p" || lower == "1080" || lower == "1080i" || lower == "fhd" -> Resolution.R1080p
                lower == "720p" || lower == "720" || lower == "hd" -> Resolution.R720p
                lower == "2160p" || lower == "2160" || lower == "4k" || lower == "uhd" -> Resolution.R2160p
                lower == "1440p" || lower == "1440" || lower == "2k" -> Resolution.R1440p
                lower == "900p" || lower == "900" -> Resolution.Other("900p", 900)
                lower == "480p" || lower == "480" || lower == "sd" -> Resolution.R480p
                lower == "576p" || lower == "576" -> Resolution.R576p
                lower == "360p" || lower == "360" -> Resolution.R360p
                lower == "8k" -> Resolution.Other("8K", 4320)
                else -> null
            }
            if (res != null) {
                token.isConsumed = true
                return DetectionResult.found(
                    value = res,
                    confidence = 1.0f,
                    evidence = TokenEvidence(
                        rawText = token.raw,
                        normalizedText = token.normalized,
                        start = token.startIndex,
                        end = token.endIndex,
                        detector = "ResolutionDetector",
                        confidence = 1.0f
                    )
                )
            }
        }
        return DetectionResult.empty()
    }
}

/**
 * Extracts video codec and checks for contradictory codecs (Section 18, 19).
 */
class CodecDetector : MetadataDetector<VideoCodec> {
    override val name: String = "CodecDetector"

    override fun detect(context: ParseContext): DetectionResult<VideoCodec> {
        val detected = mutableListOf<Pair<VideoCodec, ReleaseToken>>()

        for (token in context.tokens) {
            val lower = token.normalized.lowercase(Locale.ROOT)
            val codec = when {
                lower == "hevc" || lower == "x265" || lower == "h265" || lower == "h.265" -> VideoCodec.HEVC
                lower == "avc" || lower == "x264" || lower == "h264" || lower == "h.264" -> VideoCodec.AVC
                lower == "av1" -> VideoCodec.AV1
                lower == "vp9" -> VideoCodec.VP9
                else -> null
            }
            if (codec != null) {
                detected.add(codec to token)
                token.isConsumed = true
            }
        }

        if (detected.isEmpty()) return DetectionResult.empty()

        val distinct = detected.map { it.first }.distinct()
        if (distinct.size > 1) {
            // Contradictory codecs conflict (Section 18, 31, 83)
            return DetectionResult(
                value = null,
                confidence = 0.0f,
                evidence = detected.map {
                    TokenEvidence(
                        rawText = it.second.raw,
                        normalizedText = it.second.normalized,
                        start = it.second.startIndex,
                        end = it.second.endIndex,
                        detector = "CodecDetector.Conflict",
                        confidence = 0.5f
                    )
                },
                warnings = listOf(ParsingWarning("ConflictingCodec", "Conflicting codecs detected: $distinct"))
            )
        }

        val primary = detected.first()
        return DetectionResult.found(
            value = primary.first,
            confidence = 0.98f,
            evidence = TokenEvidence(
                rawText = primary.second.raw,
                normalizedText = primary.second.normalized,
                start = primary.second.startIndex,
                end = primary.second.endIndex,
                detector = "CodecDetector",
                confidence = 0.98f
            )
        )
    }
}

/**
 * Extracts bit-depth encoding (Section 19).
 */
class BitDepthDetector : MetadataDetector<Int> {
    override val name: String = "BitDepthDetector"

    override fun detect(context: ParseContext): DetectionResult<Int> {
        for (token in context.tokens) {
            val lower = token.normalized.lowercase(Locale.ROOT)
            val depth = when {
                lower == "10bit" || lower == "10-bit" || lower == "10 bit" -> 10
                lower == "8bit" || lower == "8-bit" || lower == "8 bit" -> 8
                lower == "12bit" || lower == "12-bit" || lower == "12 bit" -> 12
                else -> null
            }
            if (depth != null) {
                token.isConsumed = true
                return DetectionResult.found(
                    value = depth,
                    confidence = 0.99f,
                    evidence = TokenEvidence(
                        rawText = token.raw,
                        normalizedText = token.normalized,
                        start = token.startIndex,
                        end = token.endIndex,
                        detector = "BitDepthDetector",
                        confidence = 0.99f
                    )
                )
            }
        }
        return DetectionResult.empty()
    }
}

/**
 * Extracts HDR profile (Section 81).
 */
class HdrDetector : MetadataDetector<HdrFormat> {
    override val name: String = "HdrDetector"

    override fun detect(context: ParseContext): DetectionResult<HdrFormat> {
        for (token in context.tokens) {
            val lower = token.normalized.lowercase(Locale.ROOT)
            val hdr = when {
                lower == "hdr10+" || lower == "hdr10plus" -> HdrFormat.HDR10Plus
                lower == "hdr10" || lower == "hdr" -> HdrFormat.HDR10
                lower == "dv" || lower == "dolby vision" || lower == "dolby-vision" -> HdrFormat.DolbyVision
                lower == "hlg" -> HdrFormat.HLG
                else -> null
            }
            if (hdr != null) {
                token.isConsumed = true
                return DetectionResult.found(
                    value = hdr,
                    confidence = 0.99f,
                    evidence = TokenEvidence(
                        rawText = token.raw,
                        normalizedText = token.normalized,
                        start = token.startIndex,
                        end = token.endIndex,
                        detector = "HdrDetector",
                        confidence = 0.99f
                    )
                )
            }
        }
        return DetectionResult.empty()
    }
}

/**
 * Audio detection result with multi-audio classification (Section 20, 21).
 */
data class AudioDetectionResult(
    val tracks: List<AudioTrackDescriptor>,
    val multiAudio: Boolean
)

/**
 * Extracts audio codec and dual/multi audio metadata (Section 20, 21).
 */
class AudioDetector : MetadataDetector<AudioDetectionResult> {
    override val name: String = "AudioDetector"

    private val DUAL_AUDIO_REGEX = Regex("""^(?:dual[\s-]?audio|multi[\s-]?audio|japanese\s*\+\s*english|english[\s/]+japanese)$""", RegexOption.IGNORE_CASE)

    override fun detect(context: ParseContext): DetectionResult<AudioDetectionResult> {
        val tracks = mutableListOf<AudioTrackDescriptor>()
        var foundCodec: AudioCodec? = null
        var isMultiAudio = false

        for (token in context.tokens) {
            val lower = token.normalized.lowercase(Locale.ROOT)
            when {
                lower == "flac" -> { foundCodec = AudioCodec.FLAC; token.isConsumed = true }
                lower == "aac" -> { foundCodec = AudioCodec.AAC; token.isConsumed = true }
                lower == "opus" -> { foundCodec = AudioCodec.OPUS; token.isConsumed = true }
                lower == "ac3" -> { foundCodec = AudioCodec.AC3; token.isConsumed = true }
                lower == "eac3" || lower == "e-ac3" -> { foundCodec = AudioCodec.EAC3; token.isConsumed = true }
                lower == "truehd" -> { foundCodec = AudioCodec.TrueHD; token.isConsumed = true }
                lower == "dts" -> { foundCodec = AudioCodec.DTS; token.isConsumed = true }
                lower == "mp3" -> { foundCodec = AudioCodec.MP3; token.isConsumed = true }
                DUAL_AUDIO_REGEX.matches(lower) -> {
                    isMultiAudio = true
                    token.isConsumed = true
                }
            }
        }

        // Check full title for Japanese + English pattern
        val titleLower = context.normalizedTitle.lowercase(Locale.ROOT)
        if (titleLower.contains("dual audio") || titleLower.contains("dual-audio") ||
            titleLower.contains("multi audio") || titleLower.contains("multi-audio") ||
            (titleLower.contains("japanese") && titleLower.contains("english") && titleLower.contains("+"))
        ) {
            isMultiAudio = true
        }

        if (isMultiAudio) {
            tracks.add(AudioTrackDescriptor(codec = foundCodec, channels = AudioChannels.Stereo, language = "ja", label = "Japanese (Original)", isDefault = true))
            tracks.add(AudioTrackDescriptor(codec = foundCodec, channels = AudioChannels.Stereo, language = "en", label = "English (Dub)"))
            return DetectionResult.found(
                value = AudioDetectionResult(tracks, multiAudio = true),
                confidence = 0.95f,
                evidence = TokenEvidence(
                    rawText = "Dual Audio",
                    normalizedText = "Dual Audio",
                    start = 0,
                    end = 10,
                    detector = "AudioDetector.MultiAudio",
                    confidence = 0.95f
                )
            )
        }

        if (foundCodec != null) {
            tracks.add(AudioTrackDescriptor(codec = foundCodec, channels = AudioChannels.Stereo, label = foundCodec.displayName))
            return DetectionResult.found(
                value = AudioDetectionResult(tracks, multiAudio = false),
                confidence = 0.90f,
                evidence = TokenEvidence(
                    rawText = foundCodec.displayName,
                    normalizedText = foundCodec.displayName,
                    start = 0,
                    end = foundCodec.displayName.length,
                    detector = "AudioDetector.Codec",
                    confidence = 0.90f
                )
            )
        }

        return DetectionResult.empty()
    }
}

/**
 * Extracts subtitle track information (Section 22).
 */
class SubtitleDetector : MetadataDetector<List<SubtitleDescriptor>> {
    override val name: String = "SubtitleDetector"

    override fun detect(context: ParseContext): DetectionResult<List<SubtitleDescriptor>> {
        val subs = mutableListOf<SubtitleDescriptor>()

        for (token in context.tokens) {
            val lower = token.normalized.lowercase(Locale.ROOT)
            when {
                lower.contains("eng sub") || lower == "english sub" || lower == "english subs" -> {
                    subs.add(SubtitleDescriptor(language = "en", type = SubtitleType.SoftSub, format = SubtitleFormat.ASS, label = "English Subtitles"))
                    token.isConsumed = true
                }
                lower.contains("ara sub") || lower == "arabic sub" || lower == "arabic subs" -> {
                    subs.add(SubtitleDescriptor(language = "ar", type = SubtitleType.SoftSub, format = SubtitleFormat.ASS, label = "Arabic Subtitles"))
                    token.isConsumed = true
                }
                lower == "softsub" || lower == "softsubs" -> {
                    subs.add(SubtitleDescriptor(type = SubtitleType.SoftSub, format = SubtitleFormat.ASS, label = "Soft Subtitles"))
                    token.isConsumed = true
                }
                lower == "hardsub" || lower == "hardsubs" -> {
                    subs.add(SubtitleDescriptor(type = SubtitleType.HardSub, label = "Hardcoded Subtitles"))
                    token.isConsumed = true
                }
                lower == "ass" || lower == "ssa" -> {
                    subs.add(SubtitleDescriptor(format = SubtitleFormat.ASS, label = "Advanced SubStation Alpha"))
                    token.isConsumed = true
                }
                lower == "srt" -> {
                    subs.add(SubtitleDescriptor(format = SubtitleFormat.SRT, label = "SubRip"))
                    token.isConsumed = true
                }
                lower == "pgs" -> {
                    subs.add(SubtitleDescriptor(format = SubtitleFormat.PGS, label = "Presentation Graphic Stream"))
                    token.isConsumed = true
                }
            }
        }

        return if (subs.isNotEmpty()) {
            DetectionResult.found(
                value = subs,
                confidence = 0.92f,
                evidence = TokenEvidence(
                    rawText = subs.first().label ?: "Subtitles",
                    normalizedText = subs.first().label ?: "Subtitles",
                    start = 0,
                    end = 5,
                    detector = "SubtitleDetector",
                    confidence = 0.92f
                )
            )
        } else DetectionResult.empty()
    }
}

/**
 * Extracts media source (Section 24, 25).
 */
class SourceDetector : MetadataDetector<VideoSource> {
    override val name: String = "SourceDetector"

    override fun detect(context: ParseContext): DetectionResult<VideoSource> {
        for (token in context.tokens) {
            val lower = token.normalized.lowercase(Locale.ROOT)
            val src = when {
                lower == "bdremux" -> VideoSource.BDRemux
                lower == "bdmv" -> VideoSource.BDMV
                lower == "bluray" || lower == "blu-ray" || lower == "bd" || lower == "bdrip" -> VideoSource.BluRay
                lower == "web-dl" || lower == "webdl" -> VideoSource.WebDL
                lower == "webrip" || lower == "web" -> VideoSource.WebRip
                lower == "hdtv" -> VideoSource.HDTV
                lower == "tv" -> VideoSource.TV
                lower == "dvd" || lower == "dvdrip" -> VideoSource.DVD
                lower == "cam" -> VideoSource.CAM
                lower == "hdts" -> VideoSource.HDTS
                else -> null
            }
            if (src != null) {
                token.isConsumed = true
                return DetectionResult.found(
                    value = src,
                    confidence = 0.96f,
                    evidence = TokenEvidence(
                        rawText = token.raw,
                        normalizedText = token.normalized,
                        start = token.startIndex,
                        end = token.endIndex,
                        detector = "SourceDetector",
                        confidence = 0.96f
                    )
                )
            }
        }
        return DetectionResult.empty()
    }
}

/**
 * Language classification separating Audio, Subtitle, and Title language (Section 23).
 */
data class LanguageDetectionResult(
    val audioLanguages: List<String> = emptyList(),
    val subtitleLanguages: List<String> = emptyList(),
    val titleLanguage: String? = null
)

/**
 * Language Detector (Section 23).
 */
class LanguageDetector : MetadataDetector<LanguageDetectionResult> {
    override val name: String = "LanguageDetector"

    override fun detect(context: ParseContext): DetectionResult<LanguageDetectionResult> {
        val audioLangs = mutableListOf<String>()
        val subLangs = mutableListOf<String>()

        for (token in context.tokens) {
            val lower = token.normalized.lowercase(Locale.ROOT)
            when {
                lower.contains("eng sub") || lower == "english sub" -> subLangs.add("English")
                lower.contains("ara sub") || lower == "arabic sub" -> subLangs.add("Arabic")
                lower.contains("dual audio") -> {
                    audioLangs.add("Japanese")
                    audioLangs.add("English")
                }
                lower == "japanese" || lower == "jpn" -> audioLangs.add("Japanese")
                lower == "english" || lower == "eng" -> audioLangs.add("English")
            }
        }

        if (audioLangs.isEmpty() && subLangs.isEmpty()) return DetectionResult.empty()

        return DetectionResult.found(
            value = LanguageDetectionResult(audioLangs.distinct(), subLangs.distinct()),
            confidence = 0.90f,
            evidence = TokenEvidence(
                rawText = "Languages",
                normalizedText = "Languages",
                start = 0,
                end = 9,
                detector = "LanguageDetector",
                confidence = 0.90f
            )
        )
    }
}
