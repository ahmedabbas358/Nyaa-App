package com.aniflow.domain.search.parser

import com.aniflow.domain.search.model.SearchExpression
import com.aniflow.domain.search.model.SearchField
import com.aniflow.domain.search.model.SearchOperator
import com.aniflow.domain.search.model.SearchQuery
import com.aniflow.domain.search.model.SearchScope
import com.aniflow.domain.search.model.SearchValue
import com.aniflow.domain.search.normalizer.SearchArabicNormalizer

/**
 * SearchQueryParser (Section 3, 4).
 * Extracts structured AST tokens (Resolution, Codec, Uploader, Season, Episode, Batch)
 * from free-form user query strings.
 */
object SearchQueryParser {

    private val RESOLUTION_REGEX = Regex("\\b(2160p|4k|1080p|720p|480p)\\b", RegexOption.IGNORE_CASE)
    private val CODEC_REGEX = Regex("\\b(hevc|h\\.?265|x265|avc|h\\.?264|x264|av1|vp9)\\b", RegexOption.IGNORE_CASE)
    private val UPLOADER_PREFIX_REGEX = Regex("uploader:([\\w-]+)", RegexOption.IGNORE_CASE)
    private val GROUP_PREFIX_REGEX = Regex("group:([\\w-]+)", RegexOption.IGNORE_CASE)
    private val SEASON_PREFIX_REGEX = Regex("(?:season|s):(\\d+)", RegexOption.IGNORE_CASE)
    private val EPISODE_PREFIX_REGEX = Regex("(?:episode|ep|e):(\\d+(?:\\.\\d+)?)", RegexOption.IGNORE_CASE)
    private val BATCH_REGEX = Regex("\\b(batch|complete|seasons?)\\b", RegexOption.IGNORE_CASE)

    fun parse(rawText: String, scope: SearchScope = SearchScope.Universal): SearchQuery {
        val conditions = mutableListOf<SearchExpression>()
        var remainingText = rawText.trim()

        // 1. Extract Uploader prefix
        UPLOADER_PREFIX_REGEX.find(remainingText)?.let { match ->
            val uploader = match.groupValues[1]
            conditions.add(
                SearchExpression.Condition(SearchField.Uploader, SearchOperator.Equals, SearchValue.Text(uploader))
            )
            remainingText = remainingText.replace(match.value, "").trim()
        }

        // 2. Extract Group prefix
        GROUP_PREFIX_REGEX.find(remainingText)?.let { match ->
            val group = match.groupValues[1]
            conditions.add(
                SearchExpression.Condition(SearchField.ReleaseGroup, SearchOperator.Equals, SearchValue.Text(group))
            )
            remainingText = remainingText.replace(match.value, "").trim()
        }

        // 3. Extract Season
        SEASON_PREFIX_REGEX.find(remainingText)?.let { match ->
            val season = match.groupValues[1].toDoubleOrNull() ?: 1.0
            conditions.add(
                SearchExpression.Condition(SearchField.Season, SearchOperator.Equals, SearchValue.Number(season))
            )
            remainingText = remainingText.replace(match.value, "").trim()
        }

        // 4. Extract Episode
        EPISODE_PREFIX_REGEX.find(remainingText)?.let { match ->
            val ep = match.groupValues[1].toDoubleOrNull() ?: 1.0
            conditions.add(
                SearchExpression.Condition(SearchField.Episode, SearchOperator.Equals, SearchValue.Number(ep))
            )
            remainingText = remainingText.replace(match.value, "").trim()
        }

        // 5. Extract Resolution
        RESOLUTION_REGEX.find(remainingText)?.let { match ->
            val res = match.groupValues[1].lowercase()
            conditions.add(
                SearchExpression.Condition(SearchField.Resolution, SearchOperator.Equals, SearchValue.Text(res))
            )
            remainingText = remainingText.replace(match.value, "").trim()
        }

        // 6. Extract Codec
        CODEC_REGEX.find(remainingText)?.let { match ->
            val rawCodec = match.groupValues[1].uppercase()
            val codec = if (rawCodec.contains("265") || rawCodec == "HEVC") "HEVC" else if (rawCodec.contains("264") || rawCodec == "AVC") "H.264" else rawCodec
            conditions.add(
                SearchExpression.Condition(SearchField.Codec, SearchOperator.Equals, SearchValue.Text(codec))
            )
            remainingText = remainingText.replace(match.value, "").trim()
        }

        // 7. Extract Batch indicator
        if (BATCH_REGEX.containsMatchIn(remainingText)) {
            conditions.add(
                SearchExpression.Condition(SearchField.Batch, SearchOperator.Equals, SearchValue.BooleanVal(true))
            )
            remainingText = BATCH_REGEX.replace(remainingText, "").trim()
        }

        // 8. Remaining text is treated as Title keyword
        val cleanedTitle = remainingText.replace(Regex("\\s+"), " ").trim()
        if (cleanedTitle.isNotBlank()) {
            conditions.add(
                SearchExpression.Condition(SearchField.Title, SearchOperator.Contains, SearchValue.Text(cleanedTitle))
            )
        }

        val expression = when {
            conditions.isEmpty() -> null
            conditions.size == 1 -> conditions.first()
            else -> SearchExpression.And(conditions)
        }

        val normalized = SearchArabicNormalizer.normalize(rawText).normalized

        return SearchQuery(
            rawText = rawText,
            normalizedText = normalized,
            expression = expression,
            scope = scope
        )
    }
}
