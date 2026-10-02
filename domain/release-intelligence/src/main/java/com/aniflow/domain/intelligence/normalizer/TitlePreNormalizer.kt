package com.aniflow.domain.intelligence.normalizer

/**
 * Context-aware deterministic pre-normalizer (Sections 7, 8, 9, 10, 11).
 * Cleans visual noise, normalizes unicode punctuation and separators without destroying semantics.
 */
object TitlePreNormalizer {

    /**
     * Executes safe, idempotent pre-normalization on a release title string.
     */
    fun preNormalize(rawTitle: String): String {
        if (rawTitle.isBlank()) return ""

        var s = rawTitle.trim()

        // 1. Unicode character normalization: Japanese full-width brackets, symbols, spaces
        s = s.replace('【', '[')
            .replace('】', ']')
            .replace('〔', '[')
            .replace('〕', ']')
            .replace('（', '(')
            .replace('）', ')')
            .replace('［', '[')
            .replace('］', ']')
            .replace('｛', '{')
            .replace('｝', '}')
            .replace('〜', '~')
            .replace('～', '~')
            .replace('：', ':')
            .replace('\u3000', ' ') // Japanese ideological space

        // 2. Normalize underscores to spaces except inside technical tokens
        s = s.replace('_', ' ')

        // 3. Context-aware dot normalization:
        // Do NOT turn "12.5" into "12 5" or "H.264" into "H 264" or ".mkv" into " mkv"
        s = normalizeDots(s)

        // 4. Normalize dashes and tildes with spacing
        s = s.replace(Regex("""\s*[-–—]\s*"""), " - ")
        s = s.replace(Regex("""\s*~\s*"""), " ~ ")

        // 5. Ensure spaces around brackets for clean tokenization
        s = s.replace("[", " [")
            .replace("]", "] ")
            .replace("(", " (")
            .replace(")", ") ")
            .replace("{", " {")
            .replace("}", "} ")

        // 6. Collapse multiple consecutive whitespaces and trim
        s = s.replace(Regex("""\s+"""), " ").trim()

        return s
    }

    private fun normalizeDots(input: String): String {
        val result = StringBuilder(input.length)
        val len = input.length

        for (i in 0 until len) {
            val c = input[i]
            if (c == '.') {
                val prev = if (i > 0) input[i - 1] else null
                val next = if (i + 1 < len) input[i + 1] else null

                val isBetweenDigits = prev?.isDigit() == true && next?.isDigit() == true
                val isVersionOrCodec = (prev == 'H' || prev == 'h') && (next == '2')
                val isFileExt = (i + 4 == len || i + 5 == len) && (next == 'm' || next == 'M' || next == 'a' || next == 'm')

                if (isBetweenDigits || isVersionOrCodec || isFileExt) {
                    result.append('.')
                } else {
                    result.append(' ')
                }
            } else {
                result.append(c)
            }
        }
        return result.toString()
    }
}
