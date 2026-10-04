package com.aniflow.domain.search.ranking

import com.aniflow.domain.search.model.SearchContext
import com.aniflow.domain.search.model.SearchResultItem
import com.aniflow.domain.search.normalizer.SearchArabicNormalizer

/**
 * SearchRankingPolicy (Section 19, 20, 21).
 * Pure search relevance engine strictly decoupled from Download Selection Engine.
 * Evaluates textual similarity, token containment, context relevance, and availability.
 */
object SearchRankingPolicy {

    fun rank(
        items: List<SearchResultItem>,
        rawQuery: String,
        context: SearchContext? = null
    ): List<SearchResultItem> {
        val normalizedQuery = SearchArabicNormalizer.normalize(rawQuery).normalized

        val rankedList = items.map { item ->
            scoreItem(item, normalizedQuery, context)
        }

        return rankedList.sortedByDescending { it.relevanceScore }
    }

    private fun scoreItem(
        item: SearchResultItem,
        normQuery: String,
        context: SearchContext?
    ): SearchResultItem {
        var score = 0
        val reasons = mutableListOf<String>()

        val normTitle = SearchArabicNormalizer.normalize(item.title).normalized

        // 1. Exact Match (+100)
        if (normTitle == normQuery) {
            score += 100
            reasons.add("Exact title match")
        }
        // 2. Prefix Match (+60)
        else if (normTitle.startsWith(normQuery)) {
            score += 60
            reasons.add("Title starts with query")
        }
        // 3. Substring / Token Match (+30)
        else if (normTitle.contains(normQuery)) {
            score += 30
            reasons.add("Title contains query text")
        } else {
            // Check individual token overlaps
            val queryTokens = normQuery.split(" ").filter { it.length > 1 }
            val matchedTokens = queryTokens.count { normTitle.contains(it) }
            if (matchedTokens > 0) {
                val tokenBonus = (matchedTokens * 15).coerceAtMost(30)
                score += tokenBonus
                reasons.add("Matched $matchedTokens query tokens")
            }
        }

        // 4. Context Relevance (+40) (Section 22)
        if (context != null) {
            context.animeTitle?.let { ctxAnime ->
                val normCtxAnime = SearchArabicNormalizer.normalize(ctxAnime).normalized
                if (normTitle.contains(normCtxAnime)) {
                    score += 40
                    reasons.add("Matches active anime context ('$ctxAnime')")
                }
            }
            if (item is SearchResultItem.EpisodeResult && context.episodeNumber != null) {
                if (item.episodeNumber == context.episodeNumber) {
                    score += 35
                    reasons.add("Matches active episode context (${context.episodeNumber})")
                }
            }
        }

        // 5. Entity Relevance Weights
        when (item) {
            is SearchResultItem.AnimeResult -> {
                score += 25
                reasons.add("Anime entity priority")
            }
            is SearchResultItem.LibraryResult -> {
                score += 20
                reasons.add("Found in local library")
            }
            is SearchResultItem.DownloadResult -> {
                score += 15
                reasons.add("Active download task")
            }
            is SearchResultItem.ReleaseResult -> {
                if (item.seeders > 50) {
                    score += 15
                    reasons.add("Healthy swarm (${item.seeders} seeds)")
                }
            }
            else -> Unit
        }

        return when (item) {
            is SearchResultItem.AnimeResult -> item.copy(relevanceScore = score, matchedReasons = reasons)
            is SearchResultItem.SeasonResult -> item.copy(relevanceScore = score, matchedReasons = reasons)
            is SearchResultItem.EpisodeResult -> item.copy(relevanceScore = score, matchedReasons = reasons)
            is SearchResultItem.ReleaseResult -> item.copy(relevanceScore = score, matchedReasons = reasons)
            is SearchResultItem.UploaderResult -> item.copy(relevanceScore = score, matchedReasons = reasons)
            is SearchResultItem.ReleaseGroupResult -> item.copy(relevanceScore = score, matchedReasons = reasons)
            is SearchResultItem.LibraryResult -> item.copy(relevanceScore = score, matchedReasons = reasons)
            is SearchResultItem.DownloadResult -> item.copy(relevanceScore = score, matchedReasons = reasons)
            is SearchResultItem.CollectionResult -> item.copy(relevanceScore = score, matchedReasons = reasons)
            is SearchResultItem.SavedSearchResult -> item.copy(relevanceScore = score, matchedReasons = reasons)
            is SearchResultItem.GroupedAnimeResult -> item.copy(relevanceScore = score, matchedReasons = reasons)
        }
    }
}
