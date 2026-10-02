package com.aniflow.domain.search.usecase

import com.aniflow.domain.search.comparison.ReleaseComparisonEngine
import com.aniflow.domain.search.coordinator.SearchCoordinator
import com.aniflow.domain.search.model.ReleaseComparisonItem
import com.aniflow.domain.search.model.ReleaseComparisonResult
import com.aniflow.domain.search.model.SearchHistoryEntry
import com.aniflow.domain.search.model.SearchQuery
import com.aniflow.domain.search.model.SearchResponse
import com.aniflow.domain.search.model.SearchSuggestion
import com.aniflow.domain.search.parser.SearchQueryParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Universal Search Use Case (Section 7, 37).
 */
class SearchUniversalUseCase(
    private val coordinator: SearchCoordinator
) {
    suspend operator fun invoke(
        queryText: String,
        page: Int = 1,
        isOffline: Boolean = false
    ): SearchResponse {
        return coordinator.search(rawText = queryText, page = page, isOffline = isOffline)
    }
}

/**
 * Suggestions Use Case (Section 8, 9, 37).
 */
class GetSearchSuggestionsUseCase(
    private val coordinator: SearchCoordinator
) {
    suspend operator fun invoke(prefix: String): List<SearchSuggestion> {
        return coordinator.getSuggestions(prefix)
    }
}

/**
 * In-memory Search History Repository & Use Cases (Section 10, 37, 44).
 */
class SearchHistoryManager {
    private val history = ConcurrentHashMap<String, SearchHistoryEntry>()
    private val _historyFlow = MutableStateFlow<List<SearchHistoryEntry>>(emptyList())
    fun observeHistory(): Flow<List<SearchHistoryEntry>> = _historyFlow.asStateFlow()

    fun saveEntry(entry: SearchHistoryEntry) {
        history[entry.id.value] = entry
        _historyFlow.value = history.values.sortedByDescending { it.timestamp }
    }

    fun removeEntry(id: String) {
        history.remove(id)
        _historyFlow.value = history.values.sortedByDescending { it.timestamp }
    }

    fun clearAll() {
        history.clear()
        _historyFlow.value = emptyList()
    }

    fun getEntries(): List<SearchHistoryEntry> {
        return history.values.sortedByDescending { it.timestamp }
    }
}

class GetSearchHistoryUseCase(private val historyManager: SearchHistoryManager) {
    operator fun invoke(): List<SearchHistoryEntry> = historyManager.getEntries()
}

class SaveSearchHistoryUseCase(private val historyManager: SearchHistoryManager) {
    operator fun invoke(entry: SearchHistoryEntry) = historyManager.saveEntry(entry)
}

class ClearSearchHistoryUseCase(private val historyManager: SearchHistoryManager) {
    operator fun invoke(id: String? = null) {
        if (id != null) {
            historyManager.removeEntry(id)
        } else {
            historyManager.clearAll()
        }
    }
}

/**
 * Release Comparison Use Case (Section 32, 37).
 */
class CompareReleasesUseCase {
    operator fun invoke(items: List<ReleaseComparisonItem>): ReleaseComparisonResult {
        return ReleaseComparisonEngine.compare(items)
    }
}

/**
 * Query Parser Use Case (Section 3, 4, 37).
 */
class ParseStructuredQueryUseCase {
    operator fun invoke(rawQuery: String): SearchQuery {
        return SearchQueryParser.parse(rawQuery)
    }
}
