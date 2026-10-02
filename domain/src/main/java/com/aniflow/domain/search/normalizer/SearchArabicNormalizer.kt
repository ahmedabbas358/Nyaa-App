package com.aniflow.domain.search.normalizer

/**
 * NormalizedTextPair (Section 13).
 * Retains both original user query and normalized index tokens.
 */
data class NormalizedTextPair(
    val original: String,
    val normalized: String
)

/**
 * SearchArabicNormalizer (Section 13).
 * Implements non-destructive, semantic-preserving Arabic text normalization:
 * - Normalizes Alef glyph variants (أ, إ, آ, ٱ -> ا)
 * - Normalizes final Yeh (ى -> ي)
 * - Normalizes Teh Marbuta (ة -> ه)
 * - Strips Tashkeel diacritics (Fathah, Dammah, Kasrah, Sukun, Shaddah, Tanween)
 * - Strips Tatweel (Kashida 'ـ')
 * - Preserves original anime titles without destructive over-stemming.
 */
object SearchArabicNormalizer {

    private val TASHKEEL_REGEX = Regex("[\u064B-\u0652\u0670]")
    private val TATWEEL_REGEX = Regex("\u0640")

    fun normalize(input: String): NormalizedTextPair {
        if (input.isBlank()) return NormalizedTextPair(input, "")

        var text = input.trim()

        // 1. Remove diacritics (Tashkeel)
        text = TASHKEEL_REGEX.replace(text, "")

        // 2. Remove Tatweel / Kashida
        text = TATWEEL_REGEX.replace(text, "")

        // 3. Normalize Alef variants
        text = text.replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ٱ', 'ا')

        // 4. Normalize final Yeh / Alef Maqsura
        text = text.replace('ى', 'ي')

        // 5. Normalize Teh Marbuta
        text = text.replace('ة', 'ه')

        // 6. Whitespace and lowercase
        val normalized = text.lowercase().replace(Regex("\\s+"), " ")

        return NormalizedTextPair(
            original = input,
            normalized = normalized
        )
    }

    /**
     * Checks if query matches target with Arabic-aware token or prefix comparison.
     */
    fun matches(query: String, target: String): Boolean {
        val normQuery = normalize(query).normalized
        val normTarget = normalize(target).normalized

        if (normQuery.isBlank() || normTarget.isBlank()) return false
        return normTarget.contains(normQuery)
    }
}
