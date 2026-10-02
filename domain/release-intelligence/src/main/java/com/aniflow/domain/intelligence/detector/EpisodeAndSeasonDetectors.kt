package com.aniflow.domain.intelligence.detector

import com.aniflow.domain.intelligence.model.BatchType
import com.aniflow.domain.intelligence.model.DetectionResult
import com.aniflow.domain.intelligence.model.EpisodeCoverage
import com.aniflow.domain.intelligence.model.EpisodeNumberValue
import com.aniflow.domain.intelligence.model.ParseContext
import com.aniflow.domain.intelligence.model.ParsingWarning
import com.aniflow.domain.intelligence.model.SeasonReference
import com.aniflow.domain.intelligence.model.TokenEvidence
import com.aniflow.domain.intelligence.model.TokenType
import com.aniflow.domain.valueobject.EpisodeNumber
import com.aniflow.domain.valueobject.EpisodeRange
import com.aniflow.domain.valueobject.SeasonNumber
import java.util.Locale

/**
 * Season detection result with support for Part, Cour, and Roman numerals (Section 13, 76).
 */
data class SeasonDetection(
    val seasonNumber: Int?,
    val partNumber: Int? = null,
    val isCour: Boolean = false,
    val isSpecial: Boolean = false,
    val rawText: String? = null
) {
    val effectiveSeason: Int get() = seasonNumber ?: 1
    fun toSeasonReference(): SeasonReference = SeasonReference(
        seasonNumber = seasonNumber,
        partNumber = partNumber,
        isCour = isCour,
        isSpecial = isSpecial,
        rawText = rawText
    )
}

/**
 * Extracts season number while distinguishing Season vs Part and supporting Roman numerals (Section 13, 76).
 */
class SeasonDetector : MetadataDetector<SeasonDetection> {
    override val name: String = "SeasonDetector"

    private val SXX_EXX_REGEX = Regex("""^s(\d{1,2})[\s._-]?e\d{1,3}(?:\.\d)?$""", RegexOption.IGNORE_CASE)
    private val SEASON_REGEX = Regex("""^(?:s|season)[\s._-]?(\d{1,2})$|^(\d{1,2})(?:st|nd|rd|th)[\s._-]?season$""", RegexOption.IGNORE_CASE)
    private val PART_REGEX = Regex("""^(?:part|cour)[\s._-]?(\d{1,2})$""", RegexOption.IGNORE_CASE)
    private val ROMAN_SEASON_REGEX = Regex("""^(?:season|s)[\s._-]?(i|ii|iii|iv|v|vi|vii|viii|ix|x)$""", RegexOption.IGNORE_CASE)

    override fun detect(context: ParseContext): DetectionResult<SeasonDetection> {
        val detectedSeasons = mutableListOf<Pair<SeasonDetection, TokenEvidence>>()

        for (token in context.tokens) {
            val lower = token.normalized.lowercase(Locale.ROOT)

            // 1. SxxExx
            val sxxExxMatch = SXX_EXX_REGEX.find(lower)
            if (sxxExxMatch != null) {
                val num = sxxExxMatch.groupValues[1].toInt()
                val evidence = TokenEvidence(
                    rawText = token.raw,
                    normalizedText = token.normalized,
                    start = token.startIndex,
                    end = token.endIndex,
                    detector = "SeasonDetector.SxxExx",
                    confidence = 0.99f
                )
                detectedSeasons.add(SeasonDetection(seasonNumber = num, rawText = token.raw) to evidence)
                token.isConsumed = true
                continue
            }

            // 2. Sxx or Season xx
            val seasonMatch = SEASON_REGEX.find(lower)
            if (seasonMatch != null) {
                val numStr = seasonMatch.groupValues[1].ifBlank { seasonMatch.groupValues[2] }
                val num = numStr.toIntOrNull()
                if (num != null && num in 1..99) {
                    val evidence = TokenEvidence(
                        rawText = token.raw,
                        normalizedText = token.normalized,
                        start = token.startIndex,
                        end = token.endIndex,
                        detector = "SeasonDetector.ExplicitSeason",
                        confidence = 0.95f
                    )
                    detectedSeasons.add(SeasonDetection(seasonNumber = num, rawText = token.raw) to evidence)
                    token.isConsumed = true
                    continue
                }
            }

            // 3. Roman numerals (Section 76)
            val romanMatch = ROMAN_SEASON_REGEX.find(lower)
            if (romanMatch != null) {
                val roman = romanMatch.groupValues[1].lowercase(Locale.ROOT)
                val num = when (roman) {
                    "i" -> 1
                    "ii" -> 2
                    "iii" -> 3
                    "iv" -> 4
                    "v" -> 5
                    "vi" -> 6
                    "vii" -> 7
                    "viii" -> 8
                    "ix" -> 9
                    "x" -> 10
                    else -> null
                }
                if (num != null) {
                    val evidence = TokenEvidence(
                        rawText = token.raw,
                        normalizedText = token.normalized,
                        start = token.startIndex,
                        end = token.endIndex,
                        detector = "SeasonDetector.RomanNumeral",
                        confidence = 0.95f
                    )
                    detectedSeasons.add(SeasonDetection(seasonNumber = num, rawText = token.raw) to evidence)
                    token.isConsumed = true
                    continue
                }
            }

            // 4. Part / Cour detection (Section 13)
            val partMatch = PART_REGEX.find(lower)
            if (partMatch != null) {
                val partNum = partMatch.groupValues[1].toIntOrNull()
                if (partNum != null) {
                    val isCour = lower.contains("cour")
                    val evidence = TokenEvidence(
                        rawText = token.raw,
                        normalizedText = token.normalized,
                        start = token.startIndex,
                        end = token.endIndex,
                        detector = "SeasonDetector.PartOrCour",
                        confidence = 0.90f
                    )
                    detectedSeasons.add(
                        SeasonDetection(
                            seasonNumber = null,
                            partNumber = partNum,
                            isCour = isCour,
                            rawText = token.raw
                        ) to evidence
                    )
                    token.isConsumed = true
                }
            }
        }

        if (detectedSeasons.isEmpty()) {
            return DetectionResult.empty()
        }

        val distinctSeasons = detectedSeasons.mapNotNull { it.first.seasonNumber }.distinct()
        if (distinctSeasons.size > 1) {
            val primary = detectedSeasons.maxByOrNull { it.second.confidence }!!
            return DetectionResult(
                value = primary.first,
                confidence = 0.60f,
                evidence = detectedSeasons.map { it.second },
                warnings = listOf(
                    ParsingWarning(
                        code = "ConflictingSeason",
                        message = "Found conflicting season candidates: $distinctSeasons"
                    )
                )
            )
        }

        val best = detectedSeasons.first()
        return DetectionResult.found(
            value = best.first,
            confidence = best.second.confidence,
            evidence = best.second
        )
    }
}

/**
 * Detailed episode detection result covering single, range, fractional, and dual numbering (Section 11, 41, 77, 78, 79).
 */
data class EpisodeDetection(
    val coverage: EpisodeCoverage,
    val singleEpisode: Int? = null,
    val fractionalEpisode: Float? = null,
    val absoluteEpisode: Int? = null,
    val isSpecial: Boolean = false,
    val isRange: Boolean = false
) {
    val range: EpisodeRange? get() = when (coverage) {
        is EpisodeCoverage.Range -> EpisodeRange(EpisodeNumber(coverage.from), EpisodeNumber(coverage.to))
        is EpisodeCoverage.Single -> EpisodeRange(EpisodeNumber(coverage.episode), EpisodeNumber(coverage.episode))
        else -> null
    }
}

/**
 * Extracts episode numbers, ranges, fractional episodes, dual numbering, and specials (Section 11, 12, 14, 41, 77, 78, 79, 80).
 */
class EpisodeDetector : MetadataDetector<EpisodeDetection> {
    override val name: String = "EpisodeDetector"

    private val SXX_EXX_REGEX = Regex("""^s\d{1,2}[\s._-]?e(\d{1,3}(?:\.\d)?)$""", RegexOption.IGNORE_CASE)
    private val EXPLICIT_EP_REGEX = Regex("""^(?:e|ep|ep\.|episode)[\s._-]?(\d{1,3}(?:\.\d)?)$""", RegexOption.IGNORE_CASE)
    private val RANGE_REGEX = Regex("""^(?:e|ep)?(\d{1,3})[\s._-]*[-~–—to]+[\s._-]*(?:e|ep)?(\d{1,3})$""", RegexOption.IGNORE_CASE)
    private val SPECIAL_REGEX = Regex("""^(?:sp|ova|oad|ona|nced|ncop|special)[\s._-]?(\d{0,3})$""", RegexOption.IGNORE_CASE)
    private val DUAL_NUMBERING_REGEX = Regex("""^s\d{1,2}e(\d{1,3})[\s._-]+(\d{1,4})$""", RegexOption.IGNORE_CASE)

    override fun detect(context: ParseContext): DetectionResult<EpisodeDetection> {
        // Priority 1: Dual numbering e.g. S02E03 - 27 (Section 79)
        for (i in context.tokens.indices) {
            val token = context.tokens[i]
            val lower = token.normalized.lowercase(Locale.ROOT)

            // Direct token match: S02E03-27
            val dualMatch = DUAL_NUMBERING_REGEX.find(lower)
            if (dualMatch != null) {
                val seasonal = dualMatch.groupValues[1].toInt()
                val absolute = dualMatch.groupValues[2].toInt()
                token.isConsumed = true
                return DetectionResult.found(
                    value = EpisodeDetection(
                        coverage = EpisodeCoverage.Single(seasonal),
                        singleEpisode = seasonal,
                        absoluteEpisode = absolute
                    ),
                    confidence = 0.99f,
                    evidence = TokenEvidence(
                        rawText = token.raw,
                        normalizedText = token.normalized,
                        start = token.startIndex,
                        end = token.endIndex,
                        detector = "EpisodeDetector.DualNumbering",
                        confidence = 0.99f
                    )
                )
            }

            // SxxExx followed by separator and absolute number
            val sxxMatch = SXX_EXX_REGEX.find(lower)
            if (sxxMatch != null) {
                val epStr = sxxMatch.groupValues[1]
                val isFractional = epStr.contains('.')
                val epNum = epStr.toFloat().toInt()
                val frac = if (isFractional) epStr.toFloat() else null
                token.isConsumed = true

                // Check next tokens for separator and absolute number: " - 27"
                var absNum: Int? = null
                if (i + 2 < context.tokens.size) {
                    val next1 = context.tokens[i + 1]
                    val next2 = context.tokens[i + 2]
                    if (next1.raw == "-" && next2.raw.all { it.isDigit() } && next2.raw.length in 1..4) {
                        absNum = next2.raw.toIntOrNull()
                        if (absNum != null) {
                            next1.isConsumed = true
                            next2.isConsumed = true
                        }
                    }
                }

                return DetectionResult.found(
                    value = EpisodeDetection(
                        coverage = EpisodeCoverage.Single(epNum),
                        singleEpisode = epNum,
                        fractionalEpisode = frac,
                        absoluteEpisode = absNum
                    ),
                    confidence = 0.99f,
                    evidence = TokenEvidence(
                        rawText = token.raw,
                        normalizedText = token.normalized,
                        start = token.startIndex,
                        end = token.endIndex,
                        detector = "EpisodeDetector.SxxExx",
                        confidence = 0.99f
                    )
                )
            }
        }

        // Priority 2: Explicit E/EP/Episode marker (e.g. E01, Ep03, Episode 12.5)
        for (token in context.tokens) {
            val lower = token.normalized.lowercase(Locale.ROOT)
            val match = EXPLICIT_EP_REGEX.find(lower)
            if (match != null) {
                val epStr = match.groupValues[1]
                val isFractional = epStr.contains('.')
                val epNum = epStr.toFloat().toInt()
                val frac = if (isFractional) epStr.toFloat() else null
                token.isConsumed = true

                return DetectionResult.found(
                    value = EpisodeDetection(
                        coverage = EpisodeCoverage.Single(epNum),
                        singleEpisode = epNum,
                        fractionalEpisode = frac
                    ),
                    confidence = 0.98f,
                    evidence = TokenEvidence(
                        rawText = token.raw,
                        normalizedText = token.normalized,
                        start = token.startIndex,
                        end = token.endIndex,
                        detector = "EpisodeDetector.ExplicitPrefix",
                        confidence = 0.98f
                    )
                )
            }
        }

        // Priority 3: Episode Range (e.g. 01-12, 01~02, 01 – 12, E01-E12) (Section 14)
        for (token in context.tokens) {
            val lower = token.normalized.lowercase(Locale.ROOT)
            val match = RANGE_REGEX.find(lower)
            if (match != null) {
                val start = match.groupValues[1].toInt()
                val end = match.groupValues[2].toInt()
                if (start <= end && end - start in 1..300) {
                    token.isConsumed = true
                    return DetectionResult.found(
                        value = EpisodeDetection(
                            coverage = EpisodeCoverage.Range(from = start, to = end),
                            singleEpisode = null,
                            isRange = true
                        ),
                        confidence = 0.97f,
                        evidence = TokenEvidence(
                            rawText = token.raw,
                            normalizedText = token.normalized,
                            start = token.startIndex,
                            end = token.endIndex,
                            detector = "EpisodeDetector.Range",
                            confidence = 0.97f
                        )
                    )
                }
            }
        }

        // Priority 4: Special / OVA / OAD / SP01 (Section 42, 80)
        for (token in context.tokens) {
            val lower = token.normalized.lowercase(Locale.ROOT)
            val match = SPECIAL_REGEX.find(lower)
            if (match != null) {
                val num = match.groupValues[1].toIntOrNull() ?: 1
                token.isConsumed = true
                return DetectionResult.found(
                    value = EpisodeDetection(
                        coverage = EpisodeCoverage.Single(num),
                        singleEpisode = num,
                        isSpecial = true
                    ),
                    confidence = 0.94f,
                    evidence = TokenEvidence(
                        rawText = token.raw,
                        normalizedText = token.normalized,
                        start = token.startIndex,
                        end = token.endIndex,
                        detector = "EpisodeDetector.Special",
                        confidence = 0.94f
                    )
                )
            }
        }

        // Priority 5: Standalone numeric episode token context-aware (Section 11, 12, 78)
        // Ambiguous numbers check (Section 12):
        // "Anime 2024 1080p" -> Year = 2024, NOT Episode 2024!
        for (i in context.tokens.indices) {
            val token = context.tokens[i]
            if (token.isConsumed) continue

            if (token.type == TokenType.Number || (token.raw.all { it.isDigit() } && token.raw.length in 1..3)) {
                val num = token.raw.toIntOrNull() ?: continue
                val rawLen = token.raw.length

                // Safety: 4 digits are calendar years, never standalone episodes
                if (rawLen == 4 && num in 1960..2050) continue
                // Safety: common resolution heights are not episodes
                if (num in listOf(360, 480, 576, 720, 900, 1080, 1440, 2160)) continue

                if (num in 0..999) {
                    val prevToken = if (i > 0) context.tokens[i - 1] else null
                    val isAfterSeparator = prevToken?.type == TokenType.Separator || prevToken?.raw == "-"

                    // If token is 3 digits like 001, it is an absolute episode number (Section 78)
                    val isAbsolute = rawLen == 3

                    val conf = if (isAfterSeparator) 0.92f else 0.82f
                    token.isConsumed = true

                    return DetectionResult.found(
                        value = EpisodeDetection(
                            coverage = EpisodeCoverage.Single(num),
                            singleEpisode = num,
                            absoluteEpisode = if (isAbsolute) num else null
                        ),
                        confidence = conf,
                        evidence = TokenEvidence(
                            rawText = token.raw,
                            normalizedText = token.normalized,
                            start = token.startIndex,
                            end = token.endIndex,
                            detector = if (isAfterSeparator) "EpisodeDetector.PostSeparatorNumber" else "EpisodeDetector.StandaloneNumber",
                            confidence = conf
                        )
                    )
                }
            }
        }

        return DetectionResult.empty()
    }
}

/**
 * Volume Detector (Section 30).
 * Isolates volume numbers (Vol.1, Volume 01) to prevent volume numbers from being interpreted as episodes.
 */
class VolumeDetector : MetadataDetector<Int> {
    override val name: String = "VolumeDetector"

    private val VOLUME_REGEX = Regex("""^(?:vol|volume)[\s._-]?(\d{1,3})$""", RegexOption.IGNORE_CASE)

    override fun detect(context: ParseContext): DetectionResult<Int> {
        for (token in context.tokens) {
            val lower = token.normalized.lowercase(Locale.ROOT)
            val match = VOLUME_REGEX.find(lower)
            if (match != null) {
                val vol = match.groupValues[1].toIntOrNull()
                if (vol != null) {
                    token.isConsumed = true
                    return DetectionResult.found(
                        value = vol,
                        confidence = 0.96f,
                        evidence = TokenEvidence(
                            rawText = token.raw,
                            normalizedText = token.normalized,
                            start = token.startIndex,
                            end = token.endIndex,
                            detector = "VolumeDetector",
                            confidence = 0.96f
                        )
                    )
                }
            }
        }
        return DetectionResult.empty()
    }
}

/**
 * Batch Detector (Section 15, 16).
 * Recognizes batch indicators and classifies batch types with context confidence.
 */
class BatchDetector : MetadataDetector<BatchType> {
    override val name: String = "BatchDetector"

    private val COMPLETE_SERIES_KEYWORDS = setOf("complete series", "all seasons", "series batch", "complete collection")
    private val SEASON_BATCH_KEYWORDS = setOf("season batch", "season pack", "complete season", "full season", "s01 batch", "s02 batch")
    private val GENERAL_BATCH_KEYWORDS = setOf("batch", "complete", "pack", "collection")

    override fun detect(context: ParseContext): DetectionResult<BatchType> {
        val lowerTitle = context.normalizedTitle.lowercase(Locale.ROOT)

        for (kw in COMPLETE_SERIES_KEYWORDS) {
            if (lowerTitle.contains(kw)) {
                return DetectionResult.found(
                    value = BatchType.SeriesBatch,
                    confidence = 0.97f,
                    evidence = TokenEvidence(
                        rawText = kw,
                        normalizedText = kw,
                        start = 0,
                        end = kw.length,
                        detector = "BatchDetector.CompleteSeries",
                        confidence = 0.97f
                    )
                )
            }
        }

        for (kw in SEASON_BATCH_KEYWORDS) {
            if (lowerTitle.contains(kw)) {
                return DetectionResult.found(
                    value = BatchType.SeasonBatch,
                    confidence = 0.96f,
                    evidence = TokenEvidence(
                        rawText = kw,
                        normalizedText = kw,
                        start = 0,
                        end = kw.length,
                        detector = "BatchDetector.SeasonBatch",
                        confidence = 0.96f
                    )
                )
            }
        }

        for (token in context.tokens) {
            val normLower = token.normalized.lowercase(Locale.ROOT)
            if (GENERAL_BATCH_KEYWORDS.contains(normLower) || token.type == TokenType.BatchMarker) {
                token.isConsumed = true
                return DetectionResult.found(
                    value = BatchType.EpisodeRange,
                    confidence = 0.95f,
                    evidence = TokenEvidence(
                        rawText = token.raw,
                        normalizedText = token.normalized,
                        start = token.startIndex,
                        end = token.endIndex,
                        detector = "BatchDetector.BatchMarker",
                        confidence = 0.95f
                    )
                )
            }
        }

        return DetectionResult.empty()
    }
}
