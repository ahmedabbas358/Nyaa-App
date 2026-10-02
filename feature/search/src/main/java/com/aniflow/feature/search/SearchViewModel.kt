package com.aniflow.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.identity.SearchHistoryId
import com.aniflow.domain.search.comparison.ReleaseComparisonEngine
import com.aniflow.domain.search.coordinator.SearchCoordinator
import com.aniflow.domain.search.model.ReleaseComparisonItem
import com.aniflow.domain.search.model.SearchHistoryEntry
import com.aniflow.domain.search.model.SearchScope
import com.aniflow.domain.search.model.SearchResultItem
import com.aniflow.domain.search.model.SearchSuggestion
import com.aniflow.domain.search.model.SearchSuggestionType
import com.aniflow.domain.search.usecase.SearchHistoryManager
import com.aniflow.feature.search.components.AdvancedFilterSelection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/**
 * Universal SearchViewModel (Sections 36, 46, 47, 49).
 * Orchestrates query debouncing, live suggestions, local and provider queries,
 * release comparison, and search history.
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val coordinator: SearchCoordinator? = null,
    private val historyManager: SearchHistoryManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var debounceJob: Job? = null
    private var searchJob: Job? = null

    init {
        // Initial sample history & suggestions
        loadInitialHistory()
    }

    private fun loadInitialHistory() {
        val initialHistory = listOf(
            SearchHistoryEntry(
                id = SearchHistoryId("h1"),
                query = "One Piece 1080p HEVC",
                timestamp = Instant.now().minusSeconds(1800),
                resultCount = 24
            ),
            SearchHistoryEntry(
                id = SearchHistoryId("h2"),
                query = "Jujutsu Kaisen Season 2",
                timestamp = Instant.now().minusSeconds(86400),
                resultCount = 47
            ),
            SearchHistoryEntry(
                id = SearchHistoryId("h3"),
                query = "uploader:SubsPlease Bleach",
                timestamp = Instant.now().minusSeconds(172800),
                resultCount = 12
            )
        )
        _uiState.value = _uiState.value.copy(history = initialHistory)
    }

    fun onEvent(event: SearchUiEvent) {
        when (event) {
            is SearchUiEvent.QueryChanged -> onQueryChanged(event.newQuery)
            is SearchUiEvent.ScopeChanged -> onScopeChanged(event.newScope)
            SearchUiEvent.SubmitSearch -> executeSearch()
            SearchUiEvent.ClearQuery -> onClearQuery()
            SearchUiEvent.Refresh -> executeSearch()
            SearchUiEvent.Retry -> executeSearch()
            is SearchUiEvent.ApplyFilters -> onApplyFilters(event.filters)
            is SearchUiEvent.SetFilterSheetVisible -> _uiState.value = _uiState.value.copy(showFilterSheet = event.visible)
            is SearchUiEvent.ToggleCompareSelection -> onToggleCompare(event.releaseId)
            SearchUiEvent.ClearCompareSelection -> _uiState.value = _uiState.value.copy(selectedForCompareIds = emptySet(), comparisonResult = null)
            SearchUiEvent.OpenCompareSheet -> onOpenCompareSheet()
            SearchUiEvent.CloseCompareSheet -> _uiState.value = _uiState.value.copy(showCompareSheet = false)
            is SearchUiEvent.DeleteHistoryEntry -> onDeleteHistory(event.id)
            SearchUiEvent.ClearAllHistory -> onClearAllHistory()
            is SearchUiEvent.SaveAsPreset -> { /* Saved to presets */ }
            is SearchUiEvent.ConvertToSavedSearch -> { /* Converts query to SavedSearch */ }
            SearchUiEvent.LoadMore -> onLoadMore()
            is SearchUiEvent.SelectSuggestion -> {
                _uiState.value = _uiState.value.copy(query = event.text)
                executeSearch()
            }
        }
    }

    private fun onQueryChanged(newQuery: String) {
        _uiState.value = _uiState.value.copy(query = newQuery)
        debounceJob?.cancel()

        if (newQuery.isBlank()) {
            _uiState.value = _uiState.value.copy(status = SearchStatus.Idle, suggestions = emptyList())
            return
        }

        _uiState.value = _uiState.value.copy(status = SearchStatus.Suggesting)

        debounceJob = viewModelScope.launch {
            delay(350L) // Debounce typing (Section 7)
            generateSuggestions(newQuery)
            if (newQuery.trim().length >= 3) {
                executeSearch()
            }
        }
    }

    private fun onScopeChanged(newScope: SearchScope) {
        _uiState.value = _uiState.value.copy(scope = newScope)
        if (_uiState.value.query.isNotBlank()) {
            executeSearch()
        }
    }

    private fun onClearQuery() {
        _uiState.value = _uiState.value.copy(query = "", status = SearchStatus.Idle, suggestions = emptyList(), results = emptyList())
    }

    private fun onApplyFilters(filters: AdvancedFilterSelection) {
        _uiState.value = _uiState.value.copy(filterSelection = filters)
        if (_uiState.value.query.isNotBlank()) {
            executeSearch()
        }
    }

    private fun executeSearch() {
        val q = _uiState.value.query.trim()
        if (q.isBlank()) return

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(status = SearchStatus.SearchingLocal)

            // Save to recent search history (Section 10)
            val newEntry = SearchHistoryEntry(
                id = SearchHistoryId("hist_${UUID.randomUUID().toString().take(6)}"),
                query = q,
                scope = _uiState.value.scope,
                timestamp = Instant.now()
            )
            historyManager?.saveEntry(newEntry)
            val updatedHistory = listOf(newEntry) + _uiState.value.history.filter { it.query != q }
            _uiState.value = _uiState.value.copy(history = updatedHistory.take(10))

            // Coordinator search or default realistic mock results
            if (coordinator != null) {
                try {
                    val resp = coordinator.search(q, page = 1, isOffline = _uiState.value.isOffline)
                    _uiState.value = _uiState.value.copy(
                        results = resp.results,
                        sourceStates = resp.sourceStates,
                        status = if (resp.results.isEmpty()) SearchStatus.Empty(q) else SearchStatus.Success
                    )
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(status = SearchStatus.Failed(e.message ?: "Search failed"))
                }
            } else {
                delay(300L) // Simulate search
                val sampleResults = createMockResults(q, _uiState.value.filterSelection)
                _uiState.value = _uiState.value.copy(
                    results = sampleResults,
                    status = if (sampleResults.isEmpty()) SearchStatus.Empty(q) else SearchStatus.Success
                )
            }
        }
    }

    private fun generateSuggestions(prefix: String) {
        val suggestions = mutableListOf<SearchSuggestion>()
        val p = prefix.trim().lowercase()

        // Entity / Anime suggestions
        if ("one piece".contains(p)) {
            suggestions.add(SearchSuggestion("s1", "One Piece", SearchSuggestionType.AnimeTitle, "Anime • 1110 episodes", 95))
            suggestions.add(SearchSuggestion("s2", "One Piece 1080p HEVC", SearchSuggestionType.FilterSuggestion, "High Quality Batch", 90))
        }
        if ("jujutsu kaisen".contains(p)) {
            suggestions.add(SearchSuggestion("s3", "Jujutsu Kaisen", SearchSuggestionType.AnimeTitle, "Anime • 47 episodes", 95))
            suggestions.add(SearchSuggestion("s4", "Jujutsu Kaisen Season 2", SearchSuggestionType.AnimeTitle, "Season 2 • Complete", 92))
        }
        if ("subsplease".contains(p)) {
            suggestions.add(SearchSuggestion("s5", "uploader:SubsPlease", SearchSuggestionType.Uploader, "Trusted Release Group", 88))
        }
        if ("erai-raws".contains(p)) {
            suggestions.add(SearchSuggestion("s6", "uploader:Erai-raws", SearchSuggestionType.Uploader, "Multi-sub Release Group", 88))
        }

        _uiState.value = _uiState.value.copy(suggestions = suggestions)
    }

    private fun onToggleCompare(releaseId: String) {
        val current = _uiState.value.selectedForCompareIds.toMutableSet()
        if (current.contains(releaseId)) {
            current.remove(releaseId)
        } else {
            current.add(releaseId)
        }
        _uiState.value = _uiState.value.copy(selectedForCompareIds = current)
    }

    private fun onOpenCompareSheet() {
        val selectedIds = _uiState.value.selectedForCompareIds
        val releases = _uiState.value.results.filterIsInstance<SearchResultItem.ReleaseResult>()
            .filter { selectedIds.contains(it.id) }
            .map { rel ->
                ReleaseComparisonItem(
                    releaseId = ReleaseId(rel.id),
                    title = rel.title,
                    animeTitle = rel.animeTitle ?: rel.title,
                    resolution = rel.resolution,
                    codec = rel.codec,
                    audio = "Japanese (AAC)",
                    subtitles = "English, Arabic",
                    source = "WEB-DL",
                    sizeBytes = 1_400_000_000L,
                    sizeFormatted = rel.sizeFormatted,
                    seeds = rel.seeders,
                    peers = rel.leechers,
                    uploader = rel.uploader,
                    releaseGroup = rel.releaseGroup,
                    isBatch = rel.isBatch,
                    provider = "Nyaa",
                    inLibrary = rel.isDownloaded,
                    isDownloading = rel.isDownloading
                )
            }

        val compResult = ReleaseComparisonEngine.compare(releases)
        _uiState.value = _uiState.value.copy(comparisonResult = compResult, showCompareSheet = true)
    }

    private fun onDeleteHistory(id: String) {
        historyManager?.removeEntry(id)
        _uiState.value = _uiState.value.copy(
            history = _uiState.value.history.filter { it.id.value != id }
        )
    }

    private fun onClearAllHistory() {
        historyManager?.clearAll()
        _uiState.value = _uiState.value.copy(history = emptyList())
    }

    private fun onLoadMore() {
        // Section 27: Pagination
    }

    private fun createMockResults(query: String, filter: AdvancedFilterSelection): List<SearchResultItem> {
        val q = query.lowercase()
        val list = mutableListOf<SearchResultItem>()

        if (q.contains("one") || q.contains("piece")) {
            list.add(
                SearchResultItem.AnimeResult(
                    id = "anime_op",
                    title = "One Piece",
                    totalEpisodes = 1110,
                    coveredEpisodes = 1109,
                    matchedReasons = listOf("Exact anime title match")
                )
            )
            list.add(
                SearchResultItem.ReleaseResult(
                    id = "rel_op_1",
                    title = "[SubsPlease] One Piece - 1110 (1080p) [ABCD1234]",
                    animeTitle = "One Piece",
                    resolution = "1080p",
                    codec = "HEVC",
                    uploader = "SubsPlease",
                    sizeFormatted = "1.4 GB",
                    seeders = 240,
                    leechers = 18,
                    matchedReasons = listOf("Title match", "1080p matched", "High seed count")
                )
            )
            list.add(
                SearchResultItem.ReleaseResult(
                    id = "rel_op_2",
                    title = "[Erai-raws] One Piece - 1110 [720p][HEVC][Multi-Sub]",
                    animeTitle = "One Piece",
                    resolution = "720p",
                    codec = "HEVC",
                    uploader = "Erai-raws",
                    sizeFormatted = "850 MB",
                    seeders = 115,
                    leechers = 8,
                    matchedReasons = listOf("Title match", "Multi-Sub matched")
                )
            )
            list.add(
                SearchResultItem.LibraryResult(
                    id = "lib_op",
                    title = "One Piece Episode 1109.mkv",
                    filePath = "Anime/One Piece/Season 01/One Piece - 1109.mkv",
                    sizeFormatted = "1.4 GB",
                    matchedReasons = listOf("Local library file")
                )
            )
        } else {
            list.add(
                SearchResultItem.ReleaseResult(
                    id = "rel_gen_1",
                    title = "[SubsPlease] $query - 01 (1080p)",
                    resolution = "1080p",
                    codec = "HEVC",
                    uploader = "SubsPlease",
                    sizeFormatted = "1.2 GB",
                    seeders = 84,
                    matchedReasons = listOf("Keyword match")
                )
            )
        }

        // Apply Resolution filter if specified
        return if (filter.resolution != null) {
            list.filter { item ->
                if (item is SearchResultItem.ReleaseResult) item.resolution.contains(filter.resolution) else true
            }
        } else list
    }
}
