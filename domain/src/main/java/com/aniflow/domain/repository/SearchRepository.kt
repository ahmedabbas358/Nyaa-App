package com.aniflow.domain.repository

import com.aniflow.domain.model.SearchRequest
import com.aniflow.domain.model.SearchResult

/**
 * Domain contract for Release Searching (Section 51).
 * Abstracts provider fetching, parsing, normalizations, and persistence behind a clean repository interface.
 */
interface SearchRepository {

    /**
     * Executes a search against remote providers and local persistence cache.
     */
    suspend fun search(
        request: SearchRequest
    ): SearchResult
}
