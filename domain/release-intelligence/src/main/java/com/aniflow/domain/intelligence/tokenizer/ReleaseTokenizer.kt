package com.aniflow.domain.intelligence.tokenizer

import com.aniflow.domain.intelligence.model.ReleaseToken
import com.aniflow.domain.intelligence.model.TokenEvidence
import com.aniflow.domain.intelligence.model.TokenType
import java.util.Locale

/**
 * Step 19 — Tokenizer (Section 8, 9).
 * Splits pre-normalized titles into typed, positioned tokens with structured evidence.
 * Does NOT use one giant regex.
 */
object ReleaseTokenizer {

    private val RESOLUTION_PATTERN = Regex("""^(360|480|576|720|900|1080|1440|2160)[pi]?$|^4k$|^8k$""", RegexOption.IGNORE_CASE)
    private val CODEC_PATTERN = Regex("""^(x264|x265|h\.?264|h\.?265|hevc|avc|av1|vp9)$""", RegexOption.IGNORE_CASE)
    private val BIT_DEPTH_PATTERN = Regex("""^(8|10|12)[\s-]?bit$""", RegexOption.IGNORE_CASE)
    private val HDR_PATTERN = Regex("""^(hdr|hdr10\+?|dv|dolby[\s-]?vision|hlg)$""", RegexOption.IGNORE_CASE)
    private val AUDIO_PATTERN = Regex("""^(aac|flac|opus|ac3|eac3|e-ac3|mp3|truehd|dts|dts-hd|atmos|vorbis|dual[\s-]?audio|multi[\s-]?audio)$""", RegexOption.IGNORE_CASE)
    private val SUBTITLE_PATTERN = Regex("""^(softsub|hardsub|ass|ssa|srt|pgs|vtt|subs?|subbed|eng[\s-]?sub|arabic[\s-]?sub)$""", RegexOption.IGNORE_CASE)
    private val SOURCE_PATTERN = Regex("""^(web|web-?dl|webrip|bluray|blu-?ray|bd|bdrip|bdremux|bdmv|dvd|dvdrip|hdtv|tv|cam|hdts|raw)$""", RegexOption.IGNORE_CASE)
    private val SEASON_EPISODE_PATTERN = Regex("""^s(\d{1,2})[\s._-]?e(\d{1,3})$""", RegexOption.IGNORE_CASE)
    private val SEASON_ONLY_PATTERN = Regex("""^s(\d{1,2})$|^season[\s._-]?(\d{1,2})$|^(\d{1,2})(st|nd|rd|th)[\s._-]?season$|^season[\s._-]?(i|ii|iii|iv|v|vi)$""", RegexOption.IGNORE_CASE)
    private val EPISODE_ONLY_PATTERN = Regex("""^(?:e|ep|ep\.|episode)[\s._-]?(\d{1,3}(?:\.\d)?)$""", RegexOption.IGNORE_CASE)
    private val RANGE_PATTERN = Regex("""^(?:e|ep)?(\d{1,3})[\s._-]*[-~–—to]+[\s._-]*(?:e|ep)?(\d{1,3})$""", RegexOption.IGNORE_CASE)
    private val BATCH_MARKER_PATTERN = Regex("""^(batch|complete|full[\s-]?season|season[\s-]?batch|season[\s-]?pack|collection|complete[\s-]?series)$""", RegexOption.IGNORE_CASE)
    private val MOVIE_MARKER_PATTERN = Regex("""^(movie|film|the[\s-]?movie|gekijouban)$""", RegexOption.IGNORE_CASE)
    private val VOLUME_PATTERN = Regex("""^(?:vol|volume)[\s._-]?(\d{1,2})$""", RegexOption.IGNORE_CASE)
    private val YEAR_PATTERN = Regex("""^(19\d\d|20\d\d)$""")
    private val DATE_PATTERN = Regex("""^\d{4}[._-]\d{2}[._-]\d{2}$""")
    private val LANGUAGE_PATTERN = Regex("""^(japanese|english|eng|jpn|jap|arabic|ara|spanish|spa|german|ger|french|fre|ita|italian)$""", RegexOption.IGNORE_CASE)

    /**
     * Splits and classifies tokens from a pre-normalized title string.
     */
    fun tokenize(preNormalizedTitle: String): List<ReleaseToken> {
        val tokens = mutableListOf<ReleaseToken>()
        var i = 0
        val len = preNormalizedTitle.length

        while (i < len) {
            val c = preNormalizedTitle[i]

            if (c.isWhitespace()) {
                i++
                continue
            }

            // 1. Bracketed block: [ ... ], ( ... ), { ... }
            if (c == '[' || c == '(' || c == '{') {
                val closeChar = when (c) {
                    '[' -> ']'
                    '(' -> ')'
                    else -> '}'
                }
                val start = i
                val closeIdx = preNormalizedTitle.indexOf(closeChar, start + 1)
                if (closeIdx != -1) {
                    val rawBracketed = preNormalizedTitle.substring(start, closeIdx + 1)
                    val token = classifyBracketed(rawBracketed, start, closeIdx + 1)
                    tokens.add(token)
                    i = closeIdx + 1
                    continue
                }
            }

            // 2. Separators: - or ~
            if (c == '-' || c == '~') {
                val start = i
                tokens.add(
                    ReleaseToken(
                        raw = c.toString(),
                        normalized = c.toString(),
                        startIndex = start,
                        endIndex = start + 1,
                        type = TokenType.Separator
                    )
                )
                i++
                continue
            }

            // 3. Regular word / alphanumeric token
            val start = i
            while (i < len && !preNormalizedTitle[i].isWhitespace() &&
                preNormalizedTitle[i] != '[' && preNormalizedTitle[i] != '(' &&
                preNormalizedTitle[i] != '{' && preNormalizedTitle[i] != '-' &&
                preNormalizedTitle[i] != '~'
            ) {
                i++
            }

            val rawWord = preNormalizedTitle.substring(start, i)
            val token = classifyWord(rawWord, start, i)
            tokens.add(token)
        }

        return tokens
    }

    private fun classifyBracketed(raw: String, start: Int, end: Int): ReleaseToken {
        val inner = raw.substring(1, raw.length - 1).trim()
        val innerLower = inner.lowercase(Locale.ROOT)

        val classifications = mutableSetOf(TokenType.ReleaseGroupCandidate)

        var detectedType = TokenType.ReleaseGroupCandidate
        var ruleName = "BracketCandidate"
        var confidence = 0.90f

        when {
            RESOLUTION_PATTERN.matches(innerLower) -> {
                detectedType = TokenType.Resolution
                ruleName = "ResolutionToken"
                confidence = 0.99f
            }
            CODEC_PATTERN.matches(innerLower) -> {
                detectedType = TokenType.Codec
                ruleName = "CodecToken"
                confidence = 0.99f
            }
            BIT_DEPTH_PATTERN.matches(innerLower) -> {
                detectedType = TokenType.BitDepth
                ruleName = "BitDepthToken"
                confidence = 0.99f
            }
            HDR_PATTERN.matches(innerLower) -> {
                detectedType = TokenType.Hdr
                ruleName = "HdrToken"
                confidence = 0.98f
            }
            AUDIO_PATTERN.matches(innerLower) -> {
                detectedType = TokenType.Audio
                ruleName = "AudioToken"
                confidence = 0.98f
            }
            SUBTITLE_PATTERN.matches(innerLower) -> {
                detectedType = TokenType.Subtitle
                ruleName = "SubtitleToken"
                confidence = 0.98f
            }
            SOURCE_PATTERN.matches(innerLower) -> {
                detectedType = TokenType.Source
                ruleName = "SourceToken"
                confidence = 0.98f
            }
            BATCH_MARKER_PATTERN.matches(innerLower) -> {
                detectedType = TokenType.BatchMarker
                ruleName = "BatchToken"
                confidence = 0.95f
            }
            LANGUAGE_PATTERN.matches(innerLower) -> {
                detectedType = TokenType.Language
                ruleName = "LanguageToken"
                confidence = 0.95f
            }
            YEAR_PATTERN.matches(innerLower) -> {
                detectedType = TokenType.Year
                ruleName = "YearToken"
                confidence = 0.95f
            }
            inner.all { it.isDigit() } -> {
                detectedType = TokenType.Number
                ruleName = "NumberToken"
                confidence = 0.90f
            }
        }

        classifications.add(detectedType)

        val evidence = TokenEvidence(
            rawText = raw,
            normalizedText = inner,
            start = start,
            end = end,
            detector = ruleName,
            confidence = confidence
        )

        return ReleaseToken(
            raw = raw,
            normalized = inner,
            startIndex = start,
            endIndex = end,
            type = detectedType,
            classifications = classifications,
            evidence = evidence
        )
    }

    private fun classifyWord(raw: String, start: Int, end: Int): ReleaseToken {
        val norm = raw.trim()
        val lower = norm.lowercase(Locale.ROOT)
        val classifications = mutableSetOf<TokenType>()

        var detectedType = TokenType.Text
        var ruleName = "WordToken"
        var confidence = 0.80f

        when {
            SEASON_EPISODE_PATTERN.matches(lower) -> {
                classifications.add(TokenType.Season)
                classifications.add(TokenType.Episode)
                detectedType = TokenType.Episode
                ruleName = "SxxExxPattern"
                confidence = 0.99f
            }
            SEASON_ONLY_PATTERN.matches(lower) -> {
                detectedType = TokenType.Season
                ruleName = "SeasonPattern"
                confidence = 0.95f
            }
            EPISODE_ONLY_PATTERN.matches(lower) -> {
                detectedType = TokenType.Episode
                ruleName = "EpisodePrefixPattern"
                confidence = 0.95f
            }
            RANGE_PATTERN.matches(lower) -> {
                classifications.add(TokenType.EpisodeRange)
                classifications.add(TokenType.BatchMarker)
                detectedType = TokenType.EpisodeRange
                ruleName = "EpisodeRangePattern"
                confidence = 0.95f
            }
            VOLUME_PATTERN.matches(lower) -> {
                detectedType = TokenType.Volume
                ruleName = "VolumePattern"
                confidence = 0.95f
            }
            RESOLUTION_PATTERN.matches(lower) -> {
                detectedType = TokenType.Resolution
                ruleName = "ResolutionToken"
                confidence = 0.99f
            }
            CODEC_PATTERN.matches(lower) -> {
                detectedType = TokenType.Codec
                ruleName = "CodecToken"
                confidence = 0.99f
            }
            BIT_DEPTH_PATTERN.matches(lower) -> {
                detectedType = TokenType.BitDepth
                ruleName = "BitDepthToken"
                confidence = 0.99f
            }
            HDR_PATTERN.matches(lower) -> {
                detectedType = TokenType.Hdr
                ruleName = "HdrToken"
                confidence = 0.98f
            }
            AUDIO_PATTERN.matches(lower) -> {
                detectedType = TokenType.Audio
                ruleName = "AudioToken"
                confidence = 0.98f
            }
            SUBTITLE_PATTERN.matches(lower) -> {
                detectedType = TokenType.Subtitle
                ruleName = "SubtitleToken"
                confidence = 0.98f
            }
            SOURCE_PATTERN.matches(lower) -> {
                detectedType = TokenType.Source
                ruleName = "SourceToken"
                confidence = 0.98f
            }
            BATCH_MARKER_PATTERN.matches(lower) -> {
                detectedType = TokenType.BatchMarker
                ruleName = "BatchToken"
                confidence = 0.95f
            }
            MOVIE_MARKER_PATTERN.matches(lower) -> {
                detectedType = TokenType.Chapter
                ruleName = "MovieToken"
                confidence = 0.95f
            }
            LANGUAGE_PATTERN.matches(lower) -> {
                detectedType = TokenType.Language
                ruleName = "LanguageToken"
                confidence = 0.95f
            }
            DATE_PATTERN.matches(lower) -> {
                detectedType = TokenType.Date
                ruleName = "DateToken"
                confidence = 0.95f
            }
            YEAR_PATTERN.matches(lower) -> {
                detectedType = TokenType.Year
                ruleName = "YearToken"
                confidence = 0.95f
            }
            norm.all { it.isDigit() } -> {
                detectedType = TokenType.Number
                ruleName = "NumberToken"
                confidence = 0.90f
            }
            else -> {
                detectedType = TokenType.Text
                ruleName = "TitleTextToken"
                confidence = 0.85f
            }
        }

        classifications.add(detectedType)

        val evidence = TokenEvidence(
            rawText = raw,
            normalizedText = norm,
            start = start,
            end = end,
            detector = ruleName,
            confidence = confidence
        )

        return ReleaseToken(
            raw = raw,
            normalized = norm,
            startIndex = start,
            endIndex = end,
            type = detectedType,
            classifications = classifications,
            evidence = evidence
        )
    }
}
