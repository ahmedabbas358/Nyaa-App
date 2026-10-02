package com.aniflow.domain.valueobject

import com.aniflow.domain.state.ParseState

/**
 * Metadata and confidence metrics produced by the Release Parser (Section 67).
 * Invariant: confidence must be in range [0.0, 1.0].
 */
data class ParseInfo(
    val parserVersion: String,
    val confidence: Double,
    val warnings: List<String> = emptyList(),
    val state: ParseState = ParseState.Parsed
) {
    init {
        require(confidence in 0.0..1.0) {
            "ParseInfo confidence must be between 0.0 and 1.0, but was: $confidence"
        }
        require(parserVersion.isNotBlank()) {
            "parserVersion cannot be blank"
        }
    }

    val isHighConfidence: Boolean get() = confidence >= 0.85
    val needsManualReview: Boolean get() = confidence < 0.65 || state == ParseState.Ambiguous

    companion object {
        fun perfect(version: String = "1.0.0"): ParseInfo = ParseInfo(
            parserVersion = version,
            confidence = 1.0,
            state = ParseState.Parsed
        )

        fun high(version: String = "1.0.0", warnings: List<String> = emptyList()): ParseInfo = ParseInfo(
            parserVersion = version,
            confidence = 0.9,
            warnings = warnings,
            state = ParseState.Parsed
        )

        fun ambiguous(version: String = "1.0.0", reason: String): ParseInfo = ParseInfo(
            parserVersion = version,
            confidence = 0.5,
            warnings = listOf(reason),
            state = ParseState.Ambiguous
        )

        fun unparsed(): ParseInfo = ParseInfo(
            parserVersion = "unparsed",
            confidence = 0.0,
            state = ParseState.Unparsed
        )
    }
}
