package com.aniflow.domain.model

import com.aniflow.domain.controlplane.models.SearchExpression
import com.aniflow.domain.valueobject.SearchFilters
import com.aniflow.domain.valueobject.SearchSorting

sealed interface ProviderScope {
    data object All : ProviderScope
    data object DefaultOnly : ProviderScope
    data class Specific(val providerId: String) : ProviderScope
}

/**
 * SearchRequest (Section 7).
 * Encapsulates the UI request using domain SearchExpression AST and filters
 * instead of constructing raw provider-specific HTTP queries in presentation.
 */
data class SearchRequest(
    val query: SearchExpression,
    val page: Int = 1,
    val sort: SearchSorting = SearchSorting(),
    val filters: SearchFilters = SearchFilters(),
    val providerScope: ProviderScope = ProviderScope.All
)
