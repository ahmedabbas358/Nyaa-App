package com.aniflow.domain.search.coordinator

import com.aniflow.domain.identity.SearchSessionId
import com.aniflow.domain.search.deduplication.SearchDeduplicator
import com.aniflow.domain.search.model.SearchQuery
import com.aniflow.domain.search.model.SearchResponse
import com.aniflow.domain.search.model.SearchResultItem
import com.aniflow.domain.search.model.SearchSession
import com.aniflow.domain.search.model.SearchSource
import com.aniflow.domain.search.model.SearchSourceState
import com.aniflow.domain.search.model.SearchSuggestion
import com.aniflow.domain.search.model.SearchSuggestionType
import com.aniflow.domain.search.parser.SearchQueryParser
import com.aniflow.domain.search.ranking.SearchRankingPolicy
import java.time.Instant
import java.util.UUID

interface LocalSearchDataSource {
    suspend fun searchLocal(query: SearchQuery): List<SearchResultItem>
    suspend fun getSuggestions(prefix: String): List<SearchSuggestion>
}

interface ProviderSearchDataSource {
    suspend fun searchProvider(query: SearchQuery, page: Int = 1): List<SearchResultItem.ReleaseResult>
}

/**
 * SearchCoordinator (Sections 14, 15, 27, 28, 45, 46, 47).
 * Central discovery orchestrator coordinating local database FTS, remote providers,
 * query normalization, deduplication, ranking, and session tracking.
 */
class SearchCoordinator(
    private val localSearchSource: LocalSearchDataSource? = null,
    private val providerSearchSource: ProviderSearchDataSource? = null
) {

    suspend fun search(
        rawText: String,
        page: Int = 1,
        isOffline: Boolean = false
    ): SearchResponse {
        val parsedQuery = SearchQueryParser.parse(rawText)
        val sourceStates = mutableListOf<SearchSourceState>()
        val combinedItems = mutableListOf<SearchResultItem>()

        // 1. Local Search (Section 12, 28)
        val localStart = System.currentTimeMillis()
        val localResults = localSearchSource?.searchLocal(parsedQuery) ?: emptyList()
        val localDuration = System.currentTimeMillis() - localStart
        combinedItems.addAll(localResults)
        sourceStates.add(
            SearchSourceState(
                source = SearchSource.Local,
                isAvailable = true,
                resultCount = localResults.size,
                latencyMs = localDuration
            )
        )

        // 2. Provider Search (Section 14, 28, 45)
        if (isOffline) {
            sourceStates.add(
                SearchSourceState(
                    source = SearchSource.Provider,
                    isAvailable = false,
                    resultCount = 0,
                    errorMessage = "Network is offline. Only local results shown."
                )
            )
        } else if (providerSearchSource != null && parsedQuery.normalizedText.length >= 2) {
            val provStart = System.currentTimeMillis()
            try {
                val providerResults = providerSearchSource.searchProvider(parsedQuery, page)
                val provDuration = System.currentTimeMillis() - provStart
                combinedItems.addAll(providerResults)
                sourceStates.add(
                    SearchSourceState(
                        source = SearchSource.Provider,
                        isAvailable = true,
                        resultCount = providerResults.size,
                        latencyMs = provDuration
                    )
                )
            } catch (e: Exception) {
                sourceStates.add(
                    SearchSourceState(
                        source = SearchSource.Provider,
                        isAvailable = false,
                        resultCount = 0,
                        errorMessage = e.message ?: "Provider search failed"
                    )
                )
            }
        }

        // 3. Result Merging & Deduplication (Section 17, 18)
        val deduplicated = SearchDeduplicator.deduplicate(combinedItems)

        // 4. Query Relevance Ranking (Section 19, 20)
        val ranked = SearchRankingPolicy.rank(deduplicated, parsedQuery.rawText, parsedQuery.context)

        // 5. Suggestions Generation (Section 8, 9)
        val suggestions = localSearchSource?.getSuggestions(parsedQuery.normalizedText) ?: emptyList()

        val session = SearchSession(
            id = SearchSessionId("sess_${UUID.randomUUID().toString().take(8)}"),
            query = parsedQuery,
            currentPage = page,
            timestamp = Instant.now()
        )

        return SearchResponse(
            results = ranked,
            suggestions = suggestions,
            sourceStates = sourceStates,
            hasMore = ranked.size >= 30,
            session = session
        )
    }

    suspend fun getSuggestions(prefix: String): List<SearchSuggestion> {
        if (prefix.isBlank()) return emptyList()
        return localSearchSource?.getSuggestions(prefix) ?: emptyList()
    }
}
