package com.aniflow.domain.valueobject

/**
 * Value Object representing an episode number with support for decimal episodes (e.g. 12.5),
 * recap episodes, and special designations (e.g. SP01).
 */
data class EpisodeNumber(
    val major: Int,
    val minor: Int = 0,
    val specialTag: String? = null
) : Comparable<EpisodeNumber> {

    init {
        require(major >= 0) { "Episode major number cannot be negative: $major" }
        require(minor >= 0) { "Episode minor number cannot be negative: $minor" }
    }

    val isSpecial: Boolean get() = !specialTag.isNullOrBlank()
    val isDecimal: Boolean get() = minor > 0
    val value: Int get() = major

    val displayString: String
        get() = when {
            isSpecial && major > 0 -> "${specialTag}${major.toString().padStart(2, '0')}"
            isSpecial -> specialTag ?: "Special"
            isDecimal -> "$major.$minor"
            else -> major.toString().padStart(2, '0')
        }

    override fun compareTo(other: EpisodeNumber): Int {
        if (isSpecial != other.isSpecial) {
            // Specials ordered after main episodes or by convention
            return if (isSpecial) 1 else -1
        }
        val majorCmp = major.compareTo(other.major)
        if (majorCmp != 0) return majorCmp
        val minorCmp = minor.compareTo(other.minor)
        if (minorCmp != 0) return minorCmp
        return (specialTag ?: "").compareTo(other.specialTag ?: "")
    }

    override fun toString(): String = displayString

    companion object {
        fun of(number: Int): EpisodeNumber = EpisodeNumber(major = number)
        fun ofDecimal(major: Int, minor: Int): EpisodeNumber = EpisodeNumber(major = major, minor = minor)
        fun ofSpecial(tag: String, number: Int = 0): EpisodeNumber = EpisodeNumber(major = number, specialTag = tag)

        /**
         * Parses strings like "01", "12", "12.5", "SP01", "SP 1".
         */
        fun parseOrNull(raw: String): EpisodeNumber? {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return null

            // Decimal match e.g. "12.5"
            val decimalRegex = Regex("""^(\d+)\.(\d+)$""")
            decimalRegex.matchEntire(trimmed)?.let { match ->
                val maj = match.groupValues[1].toIntOrNull() ?: return null
                val min = match.groupValues[2].toIntOrNull() ?: return null
                return EpisodeNumber(major = maj, minor = min)
            }

            // Integer match e.g. "01"
            val intRegex = Regex("""^\d+$""")
            if (intRegex.matches(trimmed)) {
                val maj = trimmed.toIntOrNull() ?: return null
                return EpisodeNumber(major = maj)
            }

            // Special match e.g. "SP01", "OVA02"
            val specialRegex = Regex("""^([A-Za-z]+)\s*(\d*)$""")
            specialRegex.matchEntire(trimmed)?.let { match ->
                val tag = match.groupValues[1].uppercase()
                val num = match.groupValues[2].toIntOrNull() ?: 0
                return EpisodeNumber(major = num, specialTag = tag)
            }

            return null
        }
    }
}
