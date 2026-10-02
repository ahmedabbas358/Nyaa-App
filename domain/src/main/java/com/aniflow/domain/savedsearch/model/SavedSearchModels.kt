package com.aniflow.domain.savedsearch.model

import com.aniflow.domain.identity.ProviderId
import com.aniflow.domain.identity.SavedSearchId
import com.aniflow.domain.identity.SavedSearchRunId
import com.aniflow.domain.valueobject.SearchQuery
import java.time.Instant

/**
 * Structured search expression holding structured tokens and tags (Section 3).
 */
data class SearchExpression(
    val rawText: String,
    val structuredTokens: List<String> = emptyList(),
    val includedTags: Set<String> = emptySet(),
    val excludedTags: Set<String> = emptySet(),
    val resolution: String? = null,
    val codec: String? = null
) {
    fun toSearchQuery(): SearchQuery = SearchQuery.of(rawText)
}

enum class ProviderScopeType {
    Global,
    ProviderSpecific,
    AnimeScoped,
    UploaderScoped
}

data class ProviderScope(
    val type: ProviderScopeType = ProviderScopeType.Global,
    val providerId: ProviderId? = null,
    val scopeTargetId: String? = null
)

data class SearchFilters(
    val category: String? = null,
    val quality: String? = null,
    val uploader: String? = null,
    val releaseGroup: String? = null,
    val minSeeders: Int? = null,
    val maxSizeBytes: Long? = null
)

data class SearchSort(
    val field: String = "seeders",
    val descending: Boolean = true
)

/**
 * SavedSearch Domain Model (Section 3, 4, 5).
 */
data class SavedSearch(
    val id: SavedSearchId,
    val name: String,
    val query: SearchExpression,
    val providerScope: ProviderScope = ProviderScope(),
    val filters: SearchFilters = SearchFilters(),
    val sort: SearchSort = SearchSort(),
    val enabled: Boolean = true,
    val isPinned: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

enum class SearchRunStatus {
    Success,
    Empty,
    Failed,
    RateLimited,
    Offline
}

/**
 * Historical record of a saved search run (Section 6, 102).
 */
data class SavedSearchRun(
    val id: SavedSearchRunId,
    val savedSearchId: SavedSearchId,
    val startedAt: Instant = Instant.now(),
    val completedAt: Instant? = null,
    val resultCount: Int = 0,
    val newReleaseCount: Int = 0,
    val status: SearchRunStatus = SearchRunStatus.Success,
    val error: String? = null
)
