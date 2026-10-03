package com.aniflow.data.repository

import com.aniflow.core.database.dao.ProviderCacheDao
import com.aniflow.core.database.dao.ReleaseDao
import com.aniflow.core.database.entity.ProviderCacheEntity
import com.aniflow.core.database.mapper.EntityMappers.toDomain
import com.aniflow.core.database.mapper.EntityMappers.toEntity
import com.aniflow.data.mapper.ProviderReleaseMapper
import com.aniflow.domain.model.SearchRequest
import com.aniflow.domain.model.SearchResult
import com.aniflow.domain.repository.SearchRepository
import com.aniflow.domain.valueobject.SortOption
import com.aniflow.provider.core.coordinator.ProviderSearchCoordinator
import com.aniflow.provider.core.model.ProviderCategory
import com.aniflow.provider.core.model.ProviderSearchFilters
import com.aniflow.provider.core.model.ProviderSearchRequest
import com.aniflow.provider.core.model.ProviderSort
import com.aniflow.provider.core.model.ProviderSortField
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SearchRepository (Sections 51, 52).
 * Orchestrates:
 * 1. Cache inspection in Room database
 * 2. Remote search via ProviderSearchCoordinator (Nyaa / multi-provider)
 * 3. Normalization into Domain Release models
 * 4. Room persistence for offline caching & fast local retrieval
 * 5. Returns clean SearchResult to UseCase
 */
@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val coordinator: ProviderSearchCoordinator,
    private val releaseDao: ReleaseDao,
    private val cacheDao: ProviderCacheDao
) : SearchRepository {

    private val cacheTtlMillis = 10 * 60 * 1000L // 10 minutes

    override suspend fun search(request: SearchRequest): SearchResult {
        val queryText = when (val root = request.query.root) {
            is com.aniflow.domain.controlplane.models.ComparisonExpression -> root.value
            is com.aniflow.domain.controlplane.models.AndExpression -> root.children.filterIsInstance<com.aniflow.domain.controlplane.models.ComparisonExpression>().joinToString(" ") { it.value }
            else -> request.query.toQueryString()
        }
        val cacheKey = buildCacheKey(queryText, request.page, request.filters.trustedOnly)

        try {
            // 1. Convert domain request to provider search request
            val filter = when {
                request.filters.trustedOnly -> ProviderSearchFilters.TrustedOnly
                request.filters.excludeRemakes -> ProviderSearchFilters.NoRemakes
                else -> ProviderSearchFilters.NoFilter
            }
            val sortField = when (request.sort.option) {
                SortOption.PublishedDate -> ProviderSortField.Date
                SortOption.Seeders -> ProviderSortField.Seeders
                SortOption.Leechers -> ProviderSortField.Leechers
                SortOption.Downloads -> ProviderSortField.Downloads
                SortOption.Size -> ProviderSortField.Size
                SortOption.Title -> ProviderSortField.Date
            }

            val providerRequest = ProviderSearchRequest(
                query = queryText,
                page = request.page,
                pageSize = 50,
                category = ProviderCategory.All,
                filters = filter,
                sort = ProviderSort(sortField, request.sort.direction),
                trustedOnly = request.filters.trustedOnly
            )

            // 2. Execute search through provider coordinator (Nyaa HTTP + Parser)
            val providerPage = coordinator.search(providerRequest)

            // 3. Map to domain releases
            val domainReleases = providerPage.items.map { ProviderReleaseMapper.toDomainRelease(it) }

            // 4. Persist to Room database (Section 59)
            if (domainReleases.isNotEmpty()) {
                releaseDao.insertAll(domainReleases.map { it.toEntity() })
            }

            // 5. Update cache metadata
            val now = System.currentTimeMillis()
            cacheDao.insert(
                ProviderCacheEntity(
                    cacheKey = cacheKey,
                    providerId = "nyaa",
                    queryHash = cacheKey,
                    cachedAt = now,
                    expiresAt = now + cacheTtlMillis,
                    responsePayload = "count=${domainReleases.size}"
                )
            )

            return SearchResult(
                items = domainReleases,
                page = providerPage.page,
                hasNextPage = providerPage.hasNextPage,
                totalItems = null,
                isFromCache = false
            )
        } catch (e: Exception) {
            // Fallback to Room local cache if remote fails (Sections 60, 61)
            val localOffset = (request.page - 1) * 50
            val cachedReleases = if (queryText.isBlank()) {
                releaseDao.queryProjectionsPaged(limit = 50, offset = localOffset)
                    .mapNotNull { releaseDao.getById(it.id)?.toDomain() }
            } else {
                releaseDao.searchFts(queryText, limit = 50, offset = localOffset)
                    .map { it.toDomain() }
            }

            if (cachedReleases.isNotEmpty()) {
                return SearchResult(
                    items = cachedReleases,
                    page = request.page,
                    hasNextPage = cachedReleases.size >= 50,
                    totalItems = null,
                    isFromCache = true,
                    error = "Displaying offline cached results (${e.message ?: "Network unavailable"})"
                )
            }

            return SearchResult(
                items = emptyList(),
                page = request.page,
                hasNextPage = false,
                isFromCache = false,
                error = e.message ?: "Failed to fetch releases"
            )
        }
    }

    private fun buildCacheKey(query: String, page: Int, trustedOnly: Boolean): String {
        val raw = "q=${query.trim()}|p=$page|t=$trustedOnly"
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(raw.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
