package com.aniflow.domain.intelligence.detector

import com.aniflow.domain.intelligence.model.DetectionResult
import com.aniflow.domain.intelligence.model.MediaType
import com.aniflow.domain.intelligence.model.ParseContext
import com.aniflow.domain.intelligence.model.ReleaseGroupReference
import com.aniflow.domain.intelligence.model.ReleaseToken
import com.aniflow.domain.intelligence.model.TokenEvidence
import com.aniflow.domain.intelligence.model.TokenType
import java.util.Locale

/**
 * Extracts release group name from bracketed leading tokens (Section 26, 27).
 * Filters out technical tokens, hashes, and numbers.
 */
class ReleaseGroupDetector : MetadataDetector<ReleaseGroupReference> {
    override val name: String = "ReleaseGroupDetector"

    private val CRC32_REGEX = Regex("""^[a-f0-9]{8}$""", RegexOption.IGNORE_CASE)

    override fun detect(context: ParseContext): DetectionResult<ReleaseGroupReference> {
        for (token in context.tokens) {
            if (token.isBracketed) {
                val inner = token.unbracketed()
                val lower = inner.lowercase(Locale.ROOT)

                // Skip technical tokens, hashes, and numbers
                if (token.type == TokenType.Resolution ||
                    token.type == TokenType.Codec ||
                    token.type == TokenType.Source ||
                    token.type == TokenType.Audio ||
                    token.type == TokenType.Subtitle ||
                    token.type == TokenType.BitDepth ||
                    token.type == TokenType.Hdr ||
                    token.type == TokenType.BatchMarker ||
                    token.type == TokenType.Language ||
                    token.type == TokenType.Season ||
                    token.type == TokenType.Episode ||
                    token.type == TokenType.Year ||
                    CRC32_REGEX.matches(lower) ||
                    inner.all { it.isDigit() }
                ) {
                    continue
                }

                // If this is a valid bracketed group name candidate
                if (inner.isNotBlank() && inner.length in 2..40) {
                    token.isConsumed = true
                    val ref = ReleaseGroupReference(name = inner, isBracketCandidate = true, confidence = 0.95f)
                    return DetectionResult.found(
                        value = ref,
                        confidence = 0.95f,
                        evidence = TokenEvidence(
                            rawText = token.raw,
                            normalizedText = inner,
                            start = token.startIndex,
                            end = token.endIndex,
                            detector = "ReleaseGroupDetector",
                            confidence = 0.95f
                        )
                    )
                }
            }
        }
        return DetectionResult.empty()
    }
}

/**
 * Detects movie designations to prevent single episodes from being created automatically (Section 29).
 */
class MovieDetector : MetadataDetector<MediaType> {
    override val name: String = "MovieDetector"

    private val MOVIE_KEYWORDS = setOf("movie", "film", "the movie", "gekijouban")

    override fun detect(context: ParseContext): DetectionResult<MediaType> {
        val lower = context.normalizedTitle.lowercase(Locale.ROOT)
        for (kw in MOVIE_KEYWORDS) {
            if (lower.contains(kw)) {
                return DetectionResult.found(
                    value = MediaType.Movie,
                    confidence = 0.96f,
                    evidence = TokenEvidence(
                        rawText = kw,
                        normalizedText = kw,
                        start = 0,
                        end = kw.length,
                        detector = "MovieDetector",
                        confidence = 0.96f
                    )
                )
            }
        }
        return DetectionResult.empty()
    }
}

/**
 * Extracts 4-digit calendar production year, preventing it from being inferred as episode number (Section 12, 28).
 */
class YearDetector : MetadataDetector<Int> {
    override val name: String = "YearDetector"

    override fun detect(context: ParseContext): DetectionResult<Int> {
        for (token in context.tokens) {
            if (token.type == TokenType.Year || (token.raw.length == 4 && token.raw.all { it.isDigit() })) {
                val yr = token.raw.toIntOrNull()
                if (yr != null && yr in 1960..2050) {
                    token.isConsumed = true
                    return DetectionResult.found(
                        value = yr,
                        confidence = 0.94f,
                        evidence = TokenEvidence(
                            rawText = token.raw,
                            normalizedText = token.raw,
                            start = token.startIndex,
                            end = token.endIndex,
                            detector = "YearDetector",
                            confidence = 0.94f
                        )
                    )
                }
            }
        }
        return DetectionResult.empty()
    }
}

/**
 * Extracts clean candidate anime title by isolating title tokens from metadata tags (Section 37, 38, 72, 73, 74, 75).
 * Accurately supports anime titles containing numbers (e.g. "86", "009 Re:Cyborg", "One Piece 2024").
 */
class AnimeTitleExtractor {

    private val FILE_EXTENSIONS = setOf(".mkv", ".mp4", ".avi", ".ts")
    private val CRC32_REGEX = Regex("""^[a-f0-9]{8}$""", RegexOption.IGNORE_CASE)

    fun extractTitle(
        tokens: List<ReleaseToken>,
        groupToken: String?,
        detectedSeason: Int?,
        detectedEpisode: Int?
    ): String {
        val titleParts = mutableListOf<String>()

        for (token in tokens) {
            val raw = token.raw
            val unbracketed = token.unbracketed()

            // Skip consumed tokens (Section 72 Protected tokens)
            if (token.isConsumed) continue

            // Skip bracketed tokens (groups, technical tags, hashes)
            if (token.isBracketed) {
                // Special check: If title was inside the only bracket e.g. [One Piece] [1080p]
                if (groupToken == null && !isTechnicalBracket(unbracketed)) {
                    // Could be group or title; leave for resolver
                }
                continue
            }

            // Skip file extensions
            if (FILE_EXTENSIONS.any { raw.endsWith(it, ignoreCase = true) }) {
                val withoutExt = FILE_EXTENSIONS.fold(raw) { acc, ext ->
                    if (acc.endsWith(ext, ignoreCase = true)) acc.dropLast(ext.length) else acc
                }
                if (withoutExt.isNotBlank() && !isMetadataWord(withoutExt, detectedSeason, detectedEpisode)) {
                    titleParts.add(withoutExt)
                }
                continue
            }

            // Skip standalone separators
            if (token.type == TokenType.Separator) continue

            // Skip detected season / episode patterns
            if (token.type == TokenType.Season || token.type == TokenType.Episode || token.type == TokenType.EpisodeRange) continue

            // Skip technical tokens that might be outside brackets
            if (token.type == TokenType.Resolution ||
                token.type == TokenType.Codec ||
                token.type == TokenType.Audio ||
                token.type == TokenType.Subtitle ||
                token.type == TokenType.Source ||
                token.type == TokenType.BitDepth ||
                token.type == TokenType.Hdr ||
                token.type == TokenType.BatchMarker ||
                token.type == TokenType.Volume
            ) continue

            // Skip detected episode number if it matches standalone episode
            if (detectedEpisode != null && raw.toIntOrNull() == detectedEpisode) continue

            // Retain title words and title numbers (e.g. "86", "2024" in "One Piece 2024")
            titleParts.add(raw)
        }

        val rawClean = titleParts.joinToString(" ").trim()
        val cleaned = cleanupPunctuation(rawClean)

        // Fallback: If title extraction resulted in empty (e.g. [One Piece] without unbracketed words)
        if (cleaned.isBlank()) {
            val firstNonTechBracket = tokens.firstOrNull { it.isBracketed && !isTechnicalBracket(it.unbracketed()) }
            if (firstNonTechBracket != null) {
                return cleanupPunctuation(firstNonTechBracket.unbracketed())
            }
        }

        return cleaned
    }

    private fun isTechnicalBracket(content: String): Boolean {
        val lower = content.lowercase(Locale.ROOT)
        return lower in listOf("1080p", "720p", "480p", "2160p", "4k", "hevc", "x264", "x265", "h264", "h265", "av1",
            "web-dl", "webrip", "bluray", "bd", "dvd", "aac", "flac", "opus", "dual audio", "batch", "complete") ||
                CRC32_REGEX.matches(lower) ||
                lower.endsWith("bit") ||
                lower.startsWith("sub")
    }

    private fun isMetadataWord(word: String, season: Int?, episode: Int?): Boolean {
        val lower = word.lowercase(Locale.ROOT)
        return lower in listOf("1080p", "720p", "hevc", "x264", "batch", "webrip", "bluray") ||
                (episode != null && word.toIntOrNull() == episode)
    }

    private fun cleanupPunctuation(title: String): String {
        return title
            .trim('-', '~', ' ', '.', '_', ':')
            .replace(Regex("""\s+"""), " ")
            .trim()
    }
}
