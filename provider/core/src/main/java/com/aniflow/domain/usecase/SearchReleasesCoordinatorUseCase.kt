package com.aniflow.domain.usecase

import com.aniflow.core.common.result.AniFlowResult
import com.aniflow.domain.event.AniFlowEventBus
import com.aniflow.domain.event.DomainEvent
import com.aniflow.domain.identity.AnimeIdentity
import com.aniflow.domain.identity.ProviderId
import com.aniflow.domain.identity.ReleaseGroupId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.identity.ReleaseIdentity
import com.aniflow.domain.identity.UploaderId
import com.aniflow.domain.model.SearchRequest
import com.aniflow.domain.model.aggregate.release.ProviderRef
import com.aniflow.domain.model.aggregate.release.Release
import com.aniflow.domain.model.aggregate.release.ReleaseAvailability
import com.aniflow.domain.model.aggregate.release.ReleaseGroup
import com.aniflow.domain.model.aggregate.release.ReleaseSource
import com.aniflow.domain.model.aggregate.release.ReleaseType
import com.aniflow.domain.model.aggregate.release.Uploader
import com.aniflow.domain.repository.ReleaseRepository
import com.aniflow.domain.service.ReleaseGroupingService
import com.aniflow.domain.service.ReleaseNormalizationService
import com.aniflow.domain.service.ReleaseParser
import com.aniflow.domain.service.ReleaseParserImpl
import com.aniflow.domain.valueobject.ByteSize
import com.aniflow.domain.valueobject.InfoHash
import com.aniflow.domain.valueobject.PageResult
import com.aniflow.domain.valueobject.ReleaseTechnicalMetadata
import com.aniflow.domain.valueobject.SortOption
import com.aniflow.domain.valueobject.UrlValue
import com.aniflow.provider.core.coordinator.ProviderSearchCoordinator
import com.aniflow.provider.core.model.ProviderCategory
import com.aniflow.provider.core.model.ProviderRelease
import com.aniflow.provider.core.model.ProviderSearchFilters
import com.aniflow.provider.core.model.ProviderSearchRequest
import com.aniflow.provider.core.model.ProviderSort
import com.aniflow.provider.core.model.ProviderSortField
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.util.UUID

/**
 * SearchReleasesCoordinatorUseCase (Sections 6, 7, 8, 10, 11, 12, 13, 14, 15).
 * Executes the complete End-to-End Search Pipeline:
 * UI SearchRequest -> ProviderSearchCoordinator -> Nyaa/Multi-provider ->
 * Parser & Normalization -> Domain Release -> Atomic Cache Persistence -> Grouping.
 */
class SearchReleasesCoordinatorUseCase(
    private val coordinator: ProviderSearchCoordinator,
    private val releaseRepository: ReleaseRepository,
    private val releaseParser: ReleaseParser = ReleaseParserImpl(),
    private val eventBus: AniFlowEventBus? = null
) {

    operator fun invoke(request: SearchRequest): Flow<AniFlowResult<PageResult<Release>>> = flow {
        emit(AniFlowResult.Loading(0.1f))

        try {
            // Compile AST / Query text
            val queryText = when (val root = request.query.root) {
                is com.aniflow.domain.controlplane.models.ComparisonExpression -> root.value
                is com.aniflow.domain.controlplane.models.AndExpression -> root.children.filterIsInstance<com.aniflow.domain.controlplane.models.ComparisonExpression>().joinToString(" ") { it.value }
                else -> request.query.toQueryString()
            }

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
                sort = ProviderSort(sortField, request.sort.direction)
            )

            val mergedResult = coordinator.search(providerRequest)

            // Normalize and parse remote releases into domain aggregates
            val domainReleases = mergedResult.items.map { providerRelease ->
                mapToDomainRelease(providerRelease)
            }

            // Section 13: Atomic Persistence into cache
            if (domainReleases.isNotEmpty()) {
                releaseRepository.saveAll(domainReleases)
            }

            // Emit discovery events for automation / listener tracing (Section 43)
            domainReleases.forEach { release ->
                eventBus?.tryEmit(DomainEvent.ReleaseDiscovered(release))
            }

            emit(
                AniFlowResult.Success(
                    PageResult(
                        items = domainReleases,
                        page = mergedResult.page,
                        hasNextPage = mergedResult.hasNextPage
                    )
                )
            )
        } catch (e: Exception) {
            emit(
                AniFlowResult.Error(
                    error = com.aniflow.core.common.result.ErrorType.NetworkError,
                    message = "Could not fetch search results: ${e.message ?: "Search failed"}",
                    cause = e
                )
            )
        }
    }

    private fun mapToDomainRelease(pr: ProviderRelease): Release {
        val parsed = releaseParser.parse(pr.title)
        val cleanTitle = ReleaseNormalizationService.normalizeTitle(pr.title)
        val animeIdentity = ReleaseNormalizationService.createAnimeIdentity(parsed.animeTitle)

        val infoHash = pr.magnetUri?.let { uri ->
            val match = Regex("""xt=urn:btih:([a-fA-F0-9]{40}|[a-zA-Z2-7]{32})""", RegexOption.IGNORE_CASE).find(uri)
            match?.groupValues?.get(1)?.let { InfoHash(it) }
        } ?: InfoHash("0000000000000000000000000000000000000000")

        val source = when {
            pr.magnetUri != null || pr.torrentUrl != null -> {
                ReleaseSource.Torrent(
                    infoHash = infoHash,
                    torrentUrl = pr.torrentUrl?.let { UrlValue.TorrentUrl(it.rawValue) },
                    magnetUri = pr.magnetUri?.let { UrlValue.MagnetUri(it) }
                )
            }
            else -> ReleaseSource.Unknown
        }

        val uploader = pr.uploader?.let {
            Uploader(
                id = UploaderId(it.displayName),
                provider = ProviderRef.NYAA,
                name = it.displayName,
                normalizedName = it.displayName.lowercase()
            )
        }

        val group = (parsed.releaseGroup ?: pr.releaseGroup)?.let {
            ReleaseGroup(
                id = ReleaseGroupId(it),
                provider = ProviderRef.NYAA,
                name = it,
                normalizedName = it.lowercase()
            )
        }

        val releaseId = ReleaseId("rel-${pr.providerReleaseId ?: UUID.randomUUID().toString().take(8)}")

        return Release(
            id = releaseId,
            provider = ProviderRef.NYAA,
            providerReleaseId = pr.providerReleaseId,
            title = pr.title,
            normalizedTitle = if (cleanTitle.isNotBlank()) cleanTitle else pr.title,
            releaseType = parsed.releaseType,
            animeIdentity = animeIdentity,
            seasonHint = parsed.seasonHint,
            episodeRange = parsed.episodeRange,
            uploader = uploader,
            releaseGroup = group,
            technical = parsed.technical,
            availability = ReleaseAvailability(
                size = pr.sizeBytes?.let { ByteSize.ofBytes(it) },
                seeders = pr.seeders,
                leechers = pr.leechers,
                completedDownloads = pr.downloads
            ),
            source = source,
            publishedAt = pr.publishedAt ?: Instant.now(),
            identity = ReleaseIdentity(
                providerId = ProviderId("nyaa"),
                providerReleaseId = pr.providerReleaseId,
                infoHash = infoHash,
                canonicalSourceUrl = pr.detailsUrl?.rawValue,
                normalizedTitle = if (cleanTitle.isNotBlank()) cleanTitle else pr.title,
                episodeRange = parsed.episodeRange,
                technicalSignature = parsed.technical.resolution?.displayName
            ),
            parseInfo = parsed.parseInfo
        )
    }
}
