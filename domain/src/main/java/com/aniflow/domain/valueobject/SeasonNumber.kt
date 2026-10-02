package com.aniflow.domain.valueobject

/**
 * Value Object representing a Season number.
 * Accommodates standard numeric seasons (Season 1, Season 2), Season 0 (often Specials),
 * explicit Specials collections, and Unknown.
 */
sealed interface SeasonNumber : Comparable<SeasonNumber> {

    val isMain: Boolean
    val isSpecials: Boolean
    val isUnknown: Boolean
    val numericValue: Int?

    /**
     * Standard television/anime broadcast season (e.g. Season 1, Season 2).
     */
    data class Main(val number: Int) : SeasonNumber {
        init {
            require(number >= 0) { "Season number cannot be negative: $number" }
        }

        override val isMain: Boolean = true
        override val isSpecials: Boolean = number == 0
        override val isUnknown: Boolean = false
        override val numericValue: Int = number

        override fun compareTo(other: SeasonNumber): Int = when (other) {
            is Main -> this.number.compareTo(other.number)
            is Specials -> -1 // Main seasons precede pure specials
            is Unknown -> -1
        }

        override fun toString(): String = if (number == 0) "Specials (S00)" else "Season $number"
    }

    /**
     * Designated Specials, OVAs, or unnumbered companion material.
     */
    data object Specials : SeasonNumber {
        override val isMain: Boolean = false
        override val isSpecials: Boolean = true
        override val isUnknown: Boolean = false
        override val numericValue: Int = 0

        override fun compareTo(other: SeasonNumber): Int = when (other) {
            is Main -> 1
            is Specials -> 0
            is Unknown -> -1
        }

        override fun toString(): String = "Specials"
    }

    /**
     * Season could not be determined or parsed from release metadata.
     */
    data object Unknown : SeasonNumber {
        override val isMain: Boolean = false
        override val isSpecials: Boolean = false
        override val isUnknown: Boolean = true
        override val numericValue: Int? = null

        override fun compareTo(other: SeasonNumber): Int = when (other) {
            is Unknown -> 0
            else -> 1
        }

        override fun toString(): String = "Unknown Season"
    }

    companion object {
        fun of(number: Int): SeasonNumber = if (number == 0) Specials else Main(number)

        fun parseOrNull(raw: String): SeasonNumber? {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return null

            // Check for explicit "Specials" or "Special"
            if (trimmed.contains("special", ignoreCase = true) || trimmed.equals("s0", ignoreCase = true) || trimmed.equals("s00", ignoreCase = true)) {
                return Specials
            }

            // S01 or Season 1
            val regex = Regex("""^(?:s|season)?\s*(\d+)$""", RegexOption.IGNORE_CASE)
            regex.matchEntire(trimmed)?.let { match ->
                val num = match.groupValues[1].toIntOrNull() ?: return null
                return of(num)
            }

            return null
        }
    }
}
