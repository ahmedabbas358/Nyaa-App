package com.aniflow.data.repository

import com.aniflow.core.common.result.AniFlowResult
import com.aniflow.core.database.dao.ProviderCacheDao
import com.aniflow.core.database.dao.ReleaseDao
import com.aniflow.core.database.entity.ProviderCacheEntity
import com.aniflow.core.database.mapper.EntityMappers.toDomain
import com.aniflow.core.database.mapper.EntityMappers.toEntity
import com.aniflow.data.mapper.ProviderReleaseMapper
import com.aniflow.domain.model.aggregate.release.Release
import com.aniflow.domain.valueobject.PageResult
import com.aniflow.domain.valueobject.SearchQuery
import com.aniflow.domain.valueobject.SortOption
import com.aniflow.provider.core.coordinator.ProviderSearchCoordinator
import com.aniflow.provider.core.model.ProviderCategory
import com.aniflow.provider.core.model.ProviderSearchFilters
import com.aniflow.provider.core.model.ProviderSearchRequest
import com.aniflow.provider.core.model.ProviderSort
import com.aniflow.provider.core.model.ProviderSortField
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.security.MessageDigest

/**
 * Repository orchestrating remote release discovery via ProviderSearchCoordinator,
 * two-tier cache lookup (in-memory/Room), and persistence into the local Room database (Section 57, 58, 59, 129).
 */
class ReleaseDiscoveryRepository(
    private val coordinator: ProviderSearchCoordinator,
    private val releaseDao: ReleaseDao,
    private val cacheDao: ProviderCacheDao,
    private val cacheTtlMillis: Long = 10 * 60 * 1000L // 10 minutes cache TTL
) {

    fun searchRemote(
        query: SearchQuery,
        forceRefresh: Boolean = false
    ): Flow<AniFlowResult<PageResult<Release>>> = flow {
        emit(AniFlowResult.Loading(0.1f))

        val cacheKey = buildCacheKey(query)

        // 1. Check cache unless user explicitly requested a manual refresh (Section 60)
        if (!forceRefresh) {
            val cached = cacheDao.get(cacheKey)
            if (cached != null && cached.expiresAt > System.currentTimeMillis()) {
                val localOffset = (query.pagination.page - 1) * query.pagination.pageSize
                val localResults = if (query.text.isBlank()) {
                    releaseDao.queryProjectionsPaged(limit = query.pagination.pageSize, offset = localOffset)
                        .mapNotNull { releaseDao.getById(it.id)?.toDomain() }
                } else {
                    releaseDao.searchFts(query.text, limit = query.pagination.pageSize, offset = localOffset)
                        .map { it.toDomain() }
                }

                if (localResults.isNotEmpty()) {
                    emit(
                        AniFlowResult.Success(
                            PageResult(
                                items = localResults,
                                page = query.pagination.page,
                                hasNextPage = localResults.size >= query.pagination.pageSize
                            )
                        )
                    )
                    return@flow
                }
            }
        }

        // 2. Fetch from remote providers through Provider Engine
        val request = toProviderRequest(query)
        val searchResult = coordinator.search(request)

        val domainReleases = searchResult.items.map { ProviderReleaseMapper.toDomainRelease(it) }

        // 3. Persist discovered releases into Room cache
        if (domainReleases.isNotEmpty()) {
            releaseDao.insertAll(domainReleases.map { it.toEntity() })
        }

        // 4. Update provider search cache entry
        val now = System.currentTimeMillis()
        cacheDao.insert(
            ProviderCacheEntity(
                cacheKey = cacheKey,
                providerId = "coordinated",
                requestHash = cacheKey,
                responseType = "search",
                payload = "count=${domainReleases.size}",
                fetchedAt = now,
                expiresAt = now + cacheTtlMillis
            )
        )

        emit(
            AniFlowResult.Success(
                PageResult(
                    items = domainReleases,
                    page = searchResult.page,
                    hasNextPage = searchResult.hasNextPage
                )
            )
        )
    }

    private fun buildCacheKey(query: SearchQuery): String {
        val raw = "q=${query.text.trim()}|p=${query.pagination.page}|s=${query.sorting.option}_${query.sorting.direction}|f=${query.filters.trustedOnly}_${query.filters.excludeRemakes}"
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(raw.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun toProviderRequest(query: SearchQuery): ProviderSearchRequest {
        val filter = when {
            query.filters.trustedOnly -> ProviderSearchFilters.TrustedOnly
            query.filters.excludeRemakes -> ProviderSearchFilters.NoRemakes
            else -> ProviderSearchFilters.NoFilter
        }
        val sortField = when (query.sorting.option) {
            SortOption.PublishedDate -> ProviderSortField.Date
            SortOption.Seeders -> ProviderSortField.Seeders
            SortOption.Leechers -> ProviderSortField.Leechers
            SortOption.Downloads -> ProviderSortField.Downloads
            SortOption.Size -> ProviderSortField.Size
            SortOption.Title -> ProviderSortField.Date
        }

        return ProviderSearchRequest(
            query = query.text,
            page = query.pagination.page,
            pageSize = query.pagination.pageSize,
            category = ProviderCategory.All,
            filters = filter,
            sort = ProviderSort(sortField, query.sorting.direction)
        )
    }
}
