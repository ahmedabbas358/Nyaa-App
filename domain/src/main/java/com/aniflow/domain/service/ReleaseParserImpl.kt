package com.aniflow.domain.service

import com.aniflow.domain.model.aggregate.release.ReleaseType
import com.aniflow.domain.state.ParseState
import com.aniflow.domain.valueobject.AudioCodec
import com.aniflow.domain.valueobject.AudioTrack
import com.aniflow.domain.valueobject.EpisodeNumber
import com.aniflow.domain.valueobject.EpisodeRange
import com.aniflow.domain.valueobject.LanguageCode
import com.aniflow.domain.valueobject.ParseInfo
import com.aniflow.domain.valueobject.ReleaseTechnicalMetadata
import com.aniflow.domain.valueobject.Resolution
import com.aniflow.domain.valueobject.SeasonNumber
import com.aniflow.domain.valueobject.SubtitleFormat
import com.aniflow.domain.valueobject.SubtitleTrack
import com.aniflow.domain.valueobject.VideoCodec
import java.util.regex.Pattern

/**
 * Tokenization and regex-based release title parser (Section 95).
 */
class ReleaseParserImpl : ReleaseParser {

    private val resolutionPatterns = listOf(
        Pattern.compile("(?i)\\b(2160p|4k|uhd)\\b") to Resolution.R2160p,
        Pattern.compile("(?i)\\b(1440p|2k)\\b") to Resolution.R1440p,
        Pattern.compile("(?i)\\b(1080p|1080i|fhd)\\b") to Resolution.R1080p,
        Pattern.compile("(?i)\\b(720p|hd)\\b") to Resolution.R720p,
        Pattern.compile("(?i)\\b(576p)\\b") to Resolution.R576p,
        Pattern.compile("(?i)\\b(480p|sd)\\b") to Resolution.R480p,
        Pattern.compile("(?i)\\b(360p)\\b") to Resolution.R360p
    )

    private val codecPatterns = listOf(
        Pattern.compile("(?i)\\b(hevc|x265|h265|h\\.265)\\b") to VideoCodec.HEVC,
        Pattern.compile("(?i)\\b(avc|x264|h264|h\\.264)\\b") to VideoCodec.AVC,
        Pattern.compile("(?i)\\b(av1)\\b") to VideoCodec.AV1,
        Pattern.compile("(?i)\\b(vp9)\\b") to VideoCodec.VP9
    )

    private val groupPattern = Pattern.compile("^\\[([^\\]]+)\\]|^\\(([^\\)]+)\\)")
    private val seasonEpisodePattern = Pattern.compile("(?i)\\bS(\\d{1,2})[ ._-]*E(\\d{1,4})\\b")
    private val seasonPattern = Pattern.compile("(?i)\\b(?:Season|S)[ ._-]*(\\d{1,2})\\b")
    private val episodeRangePattern = Pattern.compile("(?i)\\b(?:E|EP|Episodes?)[ ._-]*(\\d{1,4})[ ._-]*(?:~|-|to)[ ._-]*(\\d{1,4})\\b|(?<![a-zA-Z])(\\d{1,4})[ ._-]*(?:~|-)[ ._-]*(\\d{1,4})(?![a-zA-Z0-9])")
    private val standaloneEpisodePattern = Pattern.compile("(?i)(?:\\s-\\s|[ ._]E|[ ._]EP|[ ._]#)(\\d{1,4})(?:v\\d+)?(?![0-9pPkK])")
    private val batchPattern = Pattern.compile("(?i)\\b(batch|complete|collection|seasons?|disc|bdbox)\\b")
    private val moviePattern = Pattern.compile("(?i)\\b(movie|gekijouban)\\b")
    private val ovaPattern = Pattern.compile("(?i)\\b(ova|oad|special|sp)\\b")

    override fun parse(rawTitle: String): ParsedReleaseInfo {
        var workingTitle = rawTitle.trim()

        // 1. Group Extraction
        var releaseGroup: String? = null
        val groupMatcher = groupPattern.matcher(workingTitle)
        if (groupMatcher.find()) {
            releaseGroup = (groupMatcher.group(1) ?: groupMatcher.group(2))?.trim()
            workingTitle = workingTitle.substring(groupMatcher.end()).trim()
        }

        // 2. Resolution Detection
        var resolution: Resolution? = null
        for ((pattern, res) in resolutionPatterns) {
            val matcher = pattern.matcher(workingTitle)
            if (matcher.find()) {
                resolution = res
                workingTitle = matcher.replaceAll(" ")
                break
            }
        }

        // 3. Codec Detection
        var codec: VideoCodec? = null
        for ((pattern, cdc) in codecPatterns) {
            val matcher = pattern.matcher(workingTitle)
            if (matcher.find()) {
                codec = cdc
                workingTitle = matcher.replaceAll(" ")
                break
            }
        }

        // 4. Audio & Subtitles
        val isDualAudio = workingTitle.contains("dual audio", ignoreCase = true) || workingTitle.contains("dual-audio", ignoreCase = true)
        val isFlac = workingTitle.contains("flac", ignoreCase = true)
        val isAac = workingTitle.contains("aac", ignoreCase = true)
        val audioTracks = mutableListOf<AudioTrack>()
        if (isDualAudio) {
            audioTracks.add(AudioTrack(language = LanguageCode.JAPANESE, codec = if (isFlac) AudioCodec.FLAC else AudioCodec.AAC, channels = null, label = "Japanese"))
            audioTracks.add(AudioTrack(language = LanguageCode.ENGLISH, codec = if (isFlac) AudioCodec.FLAC else AudioCodec.AAC, channels = null, label = "English"))
        } else if (isFlac || isAac) {
            audioTracks.add(AudioTrack(language = LanguageCode.JAPANESE, codec = if (isFlac) AudioCodec.FLAC else AudioCodec.AAC, channels = null, label = "Audio"))
        }

        val subtitles = mutableListOf<SubtitleTrack>()
        if (workingTitle.contains("eng", ignoreCase = true) || workingTitle.contains("english", ignoreCase = true)) {
            subtitles.add(SubtitleTrack(language = LanguageCode.ENGLISH, format = SubtitleFormat.ASS, label = "English"))
        }
        if (workingTitle.contains("ara", ignoreCase = true) || workingTitle.contains("arabic", ignoreCase = true)) {
            subtitles.add(SubtitleTrack(language = LanguageCode.ARABIC, format = SubtitleFormat.ASS, label = "Arabic"))
        }

        // 5. Episode & Season Detection
        var seasonHint: SeasonNumber? = null
        var episodeRange: EpisodeRange? = null
        var releaseType = ReleaseType.SingleEpisode

        val seMatcher = seasonEpisodePattern.matcher(workingTitle)
        if (seMatcher.find()) {
            val s = seMatcher.group(1).toIntOrNull()
            val e = seMatcher.group(2).toIntOrNull()
            if (s != null) seasonHint = SeasonNumber.of(s)
            if (e != null) episodeRange = EpisodeRange.ofSingle(e)
            workingTitle = workingTitle.substring(0, seMatcher.start()) + " " + workingTitle.substring(seMatcher.end())
        } else {
            val sMatcher = seasonPattern.matcher(workingTitle)
            if (sMatcher.find()) {
                val s = sMatcher.group(1).toIntOrNull()
                if (s != null) seasonHint = SeasonNumber.of(s)
                workingTitle = workingTitle.substring(0, sMatcher.start()) + " " + workingTitle.substring(sMatcher.end())
            }

            val rangeMatcher = episodeRangePattern.matcher(workingTitle)
            if (rangeMatcher.find()) {
                val startStr = rangeMatcher.group(1) ?: rangeMatcher.group(3)
                val endStr = rangeMatcher.group(2) ?: rangeMatcher.group(4)
                val start = startStr?.toIntOrNull()
                val end = endStr?.toIntOrNull()
                if (start != null && end != null && start <= end) {
                    episodeRange = EpisodeRange.ofRange(start, end)
                    releaseType = ReleaseType.Batch
                    workingTitle = workingTitle.substring(0, rangeMatcher.start()) + " " + workingTitle.substring(rangeMatcher.end())
                }
            } else {
                val epMatcher = standaloneEpisodePattern.matcher(workingTitle)
                if (epMatcher.find()) {
                    val ep = epMatcher.group(1).toIntOrNull()
                    if (ep != null) {
                        episodeRange = EpisodeRange.ofSingle(ep)
                        workingTitle = workingTitle.substring(0, epMatcher.start()) + " " + workingTitle.substring(epMatcher.end())
                    }
                }
            }
        }

        if (batchPattern.matcher(rawTitle).find()) {
            releaseType = ReleaseType.Batch
        } else if (moviePattern.matcher(rawTitle).find()) {
            releaseType = ReleaseType.Movie
        } else if (ovaPattern.matcher(rawTitle).find()) {
            releaseType = ReleaseType.OVA
        }

        val normalized = ReleaseNormalizationService.normalizeTitle(workingTitle)
        val cleanAnimeTitle = if (normalized.isNotBlank()) workingTitle.trim() else rawTitle.take(30)

        // 6. Confidence Scoring
        var confidence = 1.0
        val warnings = mutableListOf<String>()
        if (episodeRange == null && releaseType == ReleaseType.SingleEpisode) {
            confidence -= 0.35
            warnings.add("No episode number detected")
        }
        if (resolution == null) {
            confidence -= 0.15
            warnings.add("No resolution tag detected")
        }
        if (releaseGroup == null) {
            confidence -= 0.10
            warnings.add("No release group detected")
        }
        confidence = confidence.coerceIn(0.0, 1.0)

        val parseState = when {
            confidence >= 0.85 -> ParseState.Parsed
            confidence >= 0.50 -> ParseState.PartiallyParsed
            else -> ParseState.Ambiguous
        }

        val technical = ReleaseTechnicalMetadata(
            resolution = resolution,
            videoCodec = codec,
            audioTracks = audioTracks,
            subtitles = subtitles,
            source = null,
            bitDepth = null
        )

        return ParsedReleaseInfo(
            animeTitle = cleanAnimeTitle,
            normalizedTitle = normalized,
            seasonHint = seasonHint,
            episodeRange = episodeRange,
            releaseGroup = releaseGroup,
            technical = technical,
            releaseType = releaseType,
            parseInfo = ParseInfo(
                parserVersion = "1.0.0",
                confidence = confidence,
                warnings = warnings,
                state = parseState
            )
        )
    }
}
