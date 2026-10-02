package com.aniflow.feature.search

import com.aniflow.domain.search.model.ReleaseComparisonResult
import com.aniflow.domain.search.model.SearchHistoryEntry
import com.aniflow.domain.search.model.SearchScope
import com.aniflow.domain.search.model.SearchResultItem
import com.aniflow.domain.search.model.SearchSourceState
import com.aniflow.domain.search.model.SearchSuggestion
import com.aniflow.feature.search.components.AdvancedFilterSelection

/**
 * Discrete search lifecycle state machine (Section 34).
 */
sealed interface SearchStatus {
    data object Idle : SearchStatus
    data object Typing : SearchStatus
    data object Suggesting : SearchStatus
    data object SearchingLocal : SearchStatus
    data object SearchingProvider : SearchStatus
    data object Merging : SearchStatus
    data object Success : SearchStatus
    data class Partial(val message: String, val warningsCount: Int = 0) : SearchStatus
    data class Empty(val query: String) : SearchStatus
    data object Offline : SearchStatus
    data class Failed(val message: String) : SearchStatus
    data object Cancelled : SearchStatus
}

/**
 * Universal Search presentation state (Sections 2, 7, 10, 15, 23, 26, 32).
 */
data class SearchUiState(
    val query: String = "",
    val scope: SearchScope = SearchScope.Universal,
    val status: SearchStatus = SearchStatus.Idle,
    val results: List<SearchResultItem> = emptyList(),
    val suggestions: List<SearchSuggestion> = emptyList(),
    val history: List<SearchHistoryEntry> = emptyList(),
    val filterSelection: AdvancedFilterSelection = AdvancedFilterSelection(),
    val sourceStates: List<SearchSourceState> = emptyList(),
    val isOffline: Boolean = false,
    val selectedForCompareIds: Set<String> = emptySet(),
    val comparisonResult: ReleaseComparisonResult? = null,
    val showCompareSheet: Boolean = false,
    val showFilterSheet: Boolean = false,
    val isGroupedView: Boolean = false,
    val sortBy: String = "Seeds",
    val currentPage: Int = 1,
    val hasNextPage: Boolean = false,
    val isFromCache: Boolean = false
)
