package com.aniflow.domain.valueobject

/**
 * Value Object representing single or multiple episodes covered by a Release.
 * Invariant: For Range(start, end), start must be <= end.
 */
sealed interface EpisodeRange {

    val isSingle: Boolean
    val isBatch: Boolean
    val count: Int
    fun contains(episode: EpisodeNumber): Boolean
    fun toList(): List<EpisodeNumber>

    /**
     * A release covering exactly one episode (e.g. "Episode 05").
     */
    data class Single(val number: EpisodeNumber) : EpisodeRange {
        override val isSingle: Boolean = true
        override val isBatch: Boolean = false
        override val count: Int = 1

        override fun contains(episode: EpisodeNumber): Boolean = number == episode

        override fun toList(): List<EpisodeNumber> = listOf(number)

        override fun toString(): String = number.displayString
    }

    /**
     * A release covering a contiguous batch range (e.g. "Episodes 01-12").
     */
    data class Range(
        val start: EpisodeNumber,
        val end: EpisodeNumber
    ) : EpisodeRange {
        init {
            require(start <= end) {
                "EpisodeRange start ($start) must be less than or equal to end ($end)"
            }
        }

        override val isSingle: Boolean = start == end
        override val isBatch: Boolean = start != end

        override val count: Int
            get() = if (start.isSpecial || end.isSpecial || start.isDecimal || end.isDecimal) {
                (end.major - start.major + 1).coerceAtLeast(1)
            } else {
                end.major - start.major + 1
            }

        override fun contains(episode: EpisodeNumber): Boolean {
            return episode in start..end
        }

        override fun toList(): List<EpisodeNumber> {
            if (start.isSpecial || start.isDecimal) {
                // Return start and end if non-contiguous or special
                return if (start == end) listOf(start) else listOf(start, end)
            }
            return (start.major..end.major).map { EpisodeNumber.of(it) }
        }

        override fun toString(): String = "${start.displayString}-${end.displayString}"
    }

    /**
     * Non-contiguous or arbitrary multiple episodes (e.g. "Episodes 01, 03, 05").
     */
    data class Disjoint(
        val episodes: Set<EpisodeNumber>
    ) : EpisodeRange {
        init {
            require(episodes.isNotEmpty()) { "Disjoint EpisodeRange cannot be empty" }
        }

        override val isSingle: Boolean = episodes.size == 1
        override val isBatch: Boolean = episodes.size > 1
        override val count: Int = episodes.size

        override fun contains(episode: EpisodeNumber): Boolean = episodes.contains(episode)

        override fun toList(): List<EpisodeNumber> = episodes.sorted()

        override fun toString(): String = episodes.sorted().joinToString(", ") { it.displayString }
    }

    /**
     * Episode coverage could not be parsed from release title or metadata.
     */
    data object Unknown : EpisodeRange {
        override val isSingle: Boolean = false
        override val isBatch: Boolean = false
        override val count: Int = 0

        override fun contains(episode: EpisodeNumber): Boolean = false

        override fun toList(): List<EpisodeNumber> = emptyList()

        override fun toString(): String = "Unknown"
    }

    companion object {
        fun ofSingle(number: Int): EpisodeRange = Single(EpisodeNumber.of(number))
        fun ofSingle(number: EpisodeNumber): EpisodeRange = Single(number)

        fun ofRange(start: Int, end: Int): EpisodeRange {
            val s = EpisodeNumber.of(start)
            val e = EpisodeNumber.of(end)
            return if (s == e) Single(s) else Range(s, e)
        }

        fun ofRange(start: EpisodeNumber, end: EpisodeNumber): EpisodeRange {
            return if (start == end) Single(start) else Range(start, end)
        }

        fun parseOrNull(raw: String): EpisodeRange? {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return null

            // Batch e.g. "01-12" or "01 - 12" or "01~12"
            val rangeRegex = Regex("""^(\d+)\s*[-~_]\s*(\d+)$""")
            rangeRegex.matchEntire(trimmed)?.let { match ->
                val start = match.groupValues[1].toIntOrNull() ?: return null
                val end = match.groupValues[2].toIntOrNull() ?: return null
                return if (start <= end) ofRange(start, end) else null
            }

            // Single episode
            EpisodeNumber.parseOrNull(trimmed)?.let { ep ->
                return Single(ep)
            }

            return null
        }
    }
}
