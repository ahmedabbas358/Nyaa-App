package com.aniflow.domain.usecase

import com.aniflow.domain.model.SearchRequest
import com.aniflow.domain.model.SearchResult
import com.aniflow.domain.repository.SearchRepository

/**
 * Domain Use Case for searching releases (Section 50).
 * Presentation layers (SearchViewModel) invoke this UseCase rather than calling
 * repository implementations, providers, or HTTP parsers directly.
 */
class SearchReleasesUseCase(
    private val repository: SearchRepository
) {
    suspend operator fun invoke(
        request: SearchRequest
    ): SearchResult {
        return repository.search(request)
    }
}
