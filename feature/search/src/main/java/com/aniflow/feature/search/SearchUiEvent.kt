package com.aniflow.feature.search

import com.aniflow.domain.search.model.SearchScope
import com.aniflow.feature.search.components.AdvancedFilterSelection

/**
 * Universal Search UI Events (Section 36, 49).
 */
sealed interface SearchUiEvent {
    data class QueryChanged(val newQuery: String) : SearchUiEvent
    data class ScopeChanged(val newScope: SearchScope) : SearchUiEvent
    data object SubmitSearch : SearchUiEvent
    data object ClearQuery : SearchUiEvent
    data object Refresh : SearchUiEvent
    data object Retry : SearchUiEvent
    data class ApplyFilters(val filters: AdvancedFilterSelection) : SearchUiEvent
    data class SetFilterSheetVisible(val visible: Boolean) : SearchUiEvent
    data class ToggleCompareSelection(val releaseId: String) : SearchUiEvent
    data object ClearCompareSelection : SearchUiEvent
    data object OpenCompareSheet : SearchUiEvent
    data object CloseCompareSheet : SearchUiEvent
    data class DeleteHistoryEntry(val id: String) : SearchUiEvent
    data object ClearAllHistory : SearchUiEvent
    data class SaveAsPreset(val filters: AdvancedFilterSelection) : SearchUiEvent
    data class ConvertToSavedSearch(val query: String) : SearchUiEvent
    data object LoadMore : SearchUiEvent
    data class SelectSuggestion(val text: String) : SearchUiEvent
    data object ToggleGroupedView : SearchUiEvent
    data class OpenBatchSelectionSheet(val groupedAnime: com.aniflow.domain.search.model.SearchResultItem.GroupedAnimeResult) : SearchUiEvent
    data object CloseBatchSelectionSheet : SearchUiEvent
    data class SetUploaderFilter(val uploader: String?) : SearchUiEvent
    data class BatchQueueDownloads(val releases: List<com.aniflow.domain.search.model.SearchResultItem.ReleaseResult>) : SearchUiEvent
    data object DismissBatchSuccessNotification : SearchUiEvent
}
