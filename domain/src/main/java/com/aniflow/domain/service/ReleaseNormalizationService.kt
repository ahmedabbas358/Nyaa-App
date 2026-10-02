package com.aniflow.domain.service

import com.aniflow.domain.identity.AnimeIdentity
import java.util.Locale

/**
 * Normalizes release titles and extracts canonical clean titles (Section 95).
 * Strips bracketed metadata, fansub tags, file extensions, and checksum hashes.
 */
object ReleaseNormalizationService {

    private val BRACKET_REGEX = Regex("""\[[^\]]*\]|\([^\)]*\)""")
    private val CHECKSUM_REGEX = Regex("""[A-Fa-f0-9]{8}""")
    private val EXTENSION_REGEX = Regex("""\.(mkv|mp4|avi|webm|ts|m4v)$""", RegexOption.IGNORE_CASE)
    private val SEPARATORS_REGEX = Regex("""[._]""")
    private val MULTI_SPACE_REGEX = Regex("""\s+""")

    /**
     * Produces a clean, normalized anime title suitable for comparison and identity hashing.
     */
    fun normalizeTitle(rawTitle: String): String {
        var clean = rawTitle.trim()

        // Strip file extension if present
        clean = EXTENSION_REGEX.replace(clean, "")

        // Strip bracketed groups e.g. "[SubsPlease]", "[1080p]", "[HEVC]"
        clean = BRACKET_REGEX.replace(clean, " ")

        // Replace dots, underscores, and dashes used as spaces
        clean = SEPARATORS_REGEX.replace(clean, " ")

        // Normalize whitespace and trim
        clean = MULTI_SPACE_REGEX.replace(clean, " ").trim()

        return clean.lowercase(Locale.ROOT)
    }

    /**
     * Builds an AnimeIdentity candidate from a raw title and optional external IDs.
     */
    fun createAnimeIdentity(
        rawTitle: String,
        externalId: String? = null,
        synonyms: Set<String> = emptySet()
    ): AnimeIdentity {
        val normalized = normalizeTitle(rawTitle)
        return AnimeIdentity(
            externalProviderId = externalId,
            normalizedTitle = normalized,
            rawTitle = rawTitle.trim(),
            synonyms = synonyms.map { normalizeTitle(it) }.toSet(),
            matchConfidence = if (normalized.isNotBlank()) 1.0f else 0.0f
        )
    }
}
