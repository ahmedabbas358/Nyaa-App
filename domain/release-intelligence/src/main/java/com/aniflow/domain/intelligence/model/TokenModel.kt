package com.aniflow.domain.intelligence.model

/**
 * Step 19 — Token Classification (Section 8).
 * Pure domain token types recognizing all structural and technical components in release titles.
 */
enum class TokenType {
    Text,
    Number,
    Season,
    Episode,
    EpisodeRange,
    Resolution,
    Codec,
    BitDepth,
    Hdr,
    Audio,
    Subtitle,
    Source,
    Language,
    Year,
    Date,
    BatchMarker,
    CompleteMarker,
    Volume,
    ReleaseGroupCandidate,
    Chapter,
    Separator,
    Unknown;

    // Aliases for compatibility
    companion object {
        val Bracketed = ReleaseGroupCandidate
        val SeasonPattern = Season
        val EpisodePattern = Episode
        val TitleText = Text
        val ReleaseGroup = ReleaseGroupCandidate
    }
}

/**
 * Step 19 — Token Evidence (Section 9, 32).
 * Preserves evidence for every classification decision with exact span coordinates and detector name.
 */
data class TokenEvidence(
    val rawText: String,
    val normalizedText: String,
    val start: Int,
    val end: Int,
    val detector: String,
    val confidence: Float
) {
    // Backward compatibility constructor with field & rule
    constructor(
        field: String,
        matchedText: String,
        rule: String,
        confidence: Double
    ) : this(
        rawText = matchedText,
        normalizedText = matchedText,
        start = 0,
        end = matchedText.length,
        detector = field,
        confidence = confidence.toFloat()
    )

    val matchedText: String get() = rawText
    val field: String get() = detector
    val rule: String get() = detector
}

typealias Evidence = TokenEvidence

/**
 * Step 19 — Release Token (Section 8, 9, 72).
 * Concrete token produced by ReleaseTokenizer, with support for token consumption tracking.
 */
data class ReleaseToken(
    val raw: String,
    val normalized: String,
    val startIndex: Int,
    val endIndex: Int,
    val type: TokenType,
    val classifications: Set<TokenType> = setOf(type),
    val evidence: TokenEvidence? = null,
    var isConsumed: Boolean = false
) {
    val length: Int get() = endIndex - startIndex
    val isBracketed: Boolean get() = (raw.startsWith("[") && raw.endsWith("]")) ||
            (raw.startsWith("(") && raw.endsWith(")")) ||
            (raw.startsWith("{") && raw.endsWith("}"))

    fun unbracketed(): String {
        return if (isBracketed && raw.length >= 2) {
            raw.substring(1, raw.length - 1).trim()
        } else {
            raw
        }
    }
}

typealias TitleToken = ReleaseToken

/**
 * Structured warning emitted during parsing without discarding the release (Section 63, 70).
 */
typealias ParseWarning = ParsingWarning

/**
 * Explicit conflict recorded when multiple detectors suggest incompatible values (Section 31).
 */
typealias ParseConflict = MetadataConflict

/**
 * Context passed across the entire detector pipeline (Section 10, 68).
 */
data class ParseContext(
    val rawTitle: String,
    val normalizedTitle: String,
    val tokens: List<ReleaseToken>,
    val providerMetadata: Map<String, String> = emptyMap(),
    val uploader: String? = null,
    val category: String? = null,
    val detectedFields: MutableMap<String, Any?> = mutableMapOf(),
    val config: ReleaseIntelligenceConfig = ReleaseIntelligenceConfig.DEFAULT
)

/**
 * Configurable thresholds and settings for release intelligence (Section 34, 65, 105).
 */
data class ReleaseIntelligenceConfig(
    val parserVersion: Int = 1,
    val fuzzyIdentityThreshold: Float = 0.85f,
    val minConfidenceForAutoAccept: Float = 0.70f
) {
    companion object {
        val DEFAULT = ReleaseIntelligenceConfig()
    }
}

/**
 * Generic result returned by all metadata detectors (Section 10, 32).
 */
data class DetectionResult<T>(
    val value: T?,
    val confidence: Float,
    val evidence: List<TokenEvidence> = emptyList(),
    val warnings: List<ParsingWarning> = emptyList()
) {
    val isFound: Boolean get() = value != null && confidence > 0.0f

    companion object {
        fun <T> empty(): DetectionResult<T> = DetectionResult(null, 0.0f)

        fun <T> found(value: T, confidence: Float, evidence: TokenEvidence): DetectionResult<T> =
            DetectionResult(value, confidence, listOf(evidence))

        fun <T> found(value: T, confidence: Double, evidence: TokenEvidence): DetectionResult<T> =
            DetectionResult(value, confidence.toFloat(), listOf(evidence))
    }
}
