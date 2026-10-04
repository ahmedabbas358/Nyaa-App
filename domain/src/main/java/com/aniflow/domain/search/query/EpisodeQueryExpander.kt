package com.aniflow.domain.search.query

/**
 * EpisodeQueryExpander (Sections 11, 12, 13).
 *
 * Implements Nyaa's documented episode search behavior where an unpadded episode query
 * (e.g. "Anime 2") might succeed where a zero-padded query ("Anime 02") fails, and vice versa.
 *
 * Provides an adaptive query expansion hierarchy:
 * 1. Exact provider-friendly query (e.g. "Frieren 02")
 * 2. Unpadded / Normalized numeric variant (e.g. "Frieren 2")
 * 3. Episode marker variants (e.g. "Frieren Episode 02", "Frieren E02")
 * 4. Season & Episode notation (e.g. "Frieren S01E02")
 */
object EpisodeQueryExpander {

    private val ZERO_PADDED_EPISODE_REGEX = Regex("""(?i)\b(?:(ep|episode|e)\s+)?0(\d)\b""")
    private val SINGLE_DIGIT_EPISODE_REGEX = Regex("""(?i)\b(?:(ep|episode|e)\s+)?([1-9])\b""")
    private val GENERAL_EPISODE_REGEX = Regex("""(?i)\b(?:ep|episode|e)\s*(\d+)\b""")

    /**
     * Expands a search query into ordered, deduplicated query candidates.
     *
     * @param baseQuery The original user or system query (e.g. "Frieren 02", "Sousou no Frieren S1 05")
     * @param season Optional season number if known
     * @param episode Optional episode number if known
     * @return List of query strings ordered from highest provider relevance to fallback variants
     */
    fun expand(
        baseQuery: String,
        season: Int? = null,
        episode: Int? = null
    ): List<String> {
        val trimmed = baseQuery.trim()
        if (trimmed.isBlank()) return emptyList()

        val candidates = linkedSetOf<String>()

        // 1. Primary candidate is always the exact query
        candidates.add(trimmed)

        // If explicit episode is provided, generate standard variants directly
        if (episode != null) {
            val paddedEp = String.format("%02d", episode)
            val unpaddedEp = episode.toString()
            val cleanTitle = cleanBaseTitle(trimmed)

            if (cleanTitle.isNotBlank()) {
                // S01E02 / S1E2 format
                season?.let { s ->
                    val paddedSeason = String.format("%02d", s)
                    candidates.add("$cleanTitle S${paddedSeason}E$paddedEp")
                    candidates.add("$cleanTitle S${s}E$paddedEp")
                    candidates.add("$cleanTitle S${s}E$unpaddedEp")
                }

                // Standard padded & unpadded variants
                candidates.add("$cleanTitle $paddedEp")
                candidates.add("$cleanTitle $unpaddedEp")
                candidates.add("$cleanTitle - $paddedEp")
                candidates.add("$cleanTitle Episode $paddedEp")
                candidates.add("$cleanTitle Episode $unpaddedEp")
                candidates.add("$cleanTitle E$paddedEp")
            }
            return candidates.toList()
        }

        // Otherwise, inspect the base query for numeric/episode patterns
        // Case A: Query contains zero-padded episode (e.g. "One Piece 02" or "One Piece Episode 02")
        val paddedMatch = ZERO_PADDED_EPISODE_REGEX.find(trimmed)
        if (paddedMatch != null) {
            val prefix = paddedMatch.groupValues[1]
            val digit = paddedMatch.groupValues[2]

            // 1. Unpadded variant: "One Piece 2"
            val unpadded = trimmed.replaceRange(paddedMatch.range, digit)
            candidates.add(unpadded)

            // 2. Explicit episode marker variants: "One Piece Episode 02", "One Piece Episode 2"
            val epPadded = trimmed.replaceRange(paddedMatch.range, "Episode 0$digit")
            val epUnpadded = trimmed.replaceRange(paddedMatch.range, "Episode $digit")
            candidates.add(epPadded)
            candidates.add(epUnpadded)

            // 3. Clean title zero-padded if prefix was present
            if (prefix.isNotBlank()) {
                val barePadded = trimmed.replaceRange(paddedMatch.range, "0$digit")
                candidates.add(barePadded)
            }
        }

        // Case B: Query contains single digit episode (e.g. "One Piece 2" or "One Piece Episode 2")
        val singleDigitMatch = SINGLE_DIGIT_EPISODE_REGEX.find(trimmed)
        if (singleDigitMatch != null && paddedMatch == null) {
            val prefix = singleDigitMatch.groupValues[1]
            val digit = singleDigitMatch.groupValues[2]

            // 1. Zero-padded variant: "One Piece 02"
            val padded = trimmed.replaceRange(singleDigitMatch.range, "0$digit")
            candidates.add(padded)

            // 2. Explicit episode marker variants: "One Piece Episode 02", "One Piece Episode 2"
            val epPadded = trimmed.replaceRange(singleDigitMatch.range, "Episode 0$digit")
            val epUnpadded = trimmed.replaceRange(singleDigitMatch.range, "Episode $digit")
            candidates.add(epPadded)
            candidates.add(epUnpadded)

            // 3. Bare single digit if prefix was present
            if (prefix.isNotBlank()) {
                val bareDigit = trimmed.replaceRange(singleDigitMatch.range, digit)
                candidates.add(bareDigit)
            }
        }

        // Case C: "Episode X" -> "X"
        val generalEpMatch = GENERAL_EPISODE_REGEX.find(trimmed)
        if (generalEpMatch != null) {
            val epNum = generalEpMatch.groupValues[1]
            val plainNum = trimmed.replaceRange(generalEpMatch.range, epNum)
            candidates.add(plainNum)
        }

        return candidates.toList()
    }

    /**
     * Strips existing episode or season markers to extract the pure anime title.
     */
    fun cleanBaseTitle(query: String): String {
        return query
            .replace(Regex("""(?i)\bS\d{1,2}E\d{1,3}\b"""), "")
            .replace(Regex("""(?i)\b(?:Season|S)\s*\d+\b"""), "")
            .replace(Regex("""(?i)\b(?:Episode|EP|E)\s*\d+\b"""), "")
            .replace(Regex("""(?i)\b\d{1,3}\b"""), "")
            .replace(Regex("""(?i)\b(?:1080p|720p|480p|2160p|4k)\b"""), "")
            .replace(Regex("""(?i)\b(?:hevc|x265|x264|avc|av1)\b"""), "")
            .replace(Regex("""\[.*?\]|\(.*?\)|-"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }
}
