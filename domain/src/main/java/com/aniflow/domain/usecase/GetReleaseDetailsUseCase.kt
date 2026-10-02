package com.aniflow.domain.usecase

import com.aniflow.core.common.result.AniFlowResult
import com.aniflow.core.common.result.ErrorType
import com.aniflow.domain.identity.ProviderId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.model.aggregate.release.ProviderRef
import com.aniflow.domain.model.aggregate.release.Release
import com.aniflow.domain.model.aggregate.release.ReleaseAvailability
import com.aniflow.domain.model.aggregate.release.ReleaseSource
import com.aniflow.domain.model.aggregate.release.ReleaseType
import com.aniflow.domain.model.aggregate.release.Uploader
import com.aniflow.domain.repository.ReleaseRepository
import com.aniflow.domain.valueobject.ByteSize
import com.aniflow.domain.valueobject.ReleaseTechnicalMetadata
import com.aniflow.provider.core.registry.ProviderRegistry
import java.time.Instant

/**
 * Lazy, cache-first Release Details retrieval UseCase (Step 18 Sections 32, 33, 34, 51, 91).
 * Inspects Room cache first; on demand or forced refresh, queries provider for rich metadata.
 */
class GetReleaseDetailsUseCase(
    private val releaseRepository: ReleaseRepository,
    private val providerRegistry: ProviderRegistry
) {

    suspend operator fun invoke(
        releaseId: ReleaseId,
        forceRefresh: Boolean = false
    ): AniFlowResult<Release> {
        val localRelease = releaseRepository.getById(releaseId)

        // Return cached release if already rich and refresh is not forced (Section 34)
        if (localRelease != null && !forceRefresh && localRelease.provider.detailsUrl != null) {
            return AniFlowResult.Success(localRelease)
        }

        val providerReleaseId = localRelease?.providerReleaseId ?: localRelease?.provider?.externalId
        val providerId = localRelease?.provider?.providerId ?: ProviderId("nyaa")

        if (providerReleaseId == null) {
            return if (localRelease != null) {
                AniFlowResult.Success(localRelease)
            } else {
                AniFlowResult.Error(
                    error = ErrorType.DatabaseError("Release not found with ID: ${releaseId.value}"),
                    message = "Release not found"
                )
            }
        }

        val provider = providerRegistry.get(providerId)
            ?: return if (localRelease != null) {
                AniFlowResult.Success(localRelease)
            } else {
                AniFlowResult.Error(
                    error = ErrorType.ProviderUnavailable(providerId.value, 404),
                    message = "Provider ${providerId.value} is not registered"
                )
            }

        return when (val remoteResult = provider.getRelease(providerReleaseId)) {
            is AniFlowResult.Success -> {
                val pr = remoteResult.data
                val updatedRelease = (localRelease ?: Release(
                    id = releaseId,
                    provider = ProviderRef(
                        providerId = providerId,
                        providerName = provider.descriptor.name,
                        externalId = providerReleaseId,
                        detailsUrl = pr.detailsUrl
                    ),
                    providerReleaseId = providerReleaseId,
                    title = pr.title,
                    normalizedTitle = pr.title,
                    releaseType = ReleaseType.SingleEpisode,
                    animeIdentity = null,
                    seasonHint = null,
                    episodeRange = null,
                    uploader = pr.uploader?.let { Uploader(com.aniflow.domain.identity.UploaderId(it.displayName), it.displayName, pr.isTrusted) },
                    releaseGroup = null,
                    technical = ReleaseTechnicalMetadata(),
                    availability = ReleaseAvailability(
                        size = pr.sizeBytes?.let { ByteSize.fromBytes(it) },
                        seeders = pr.seeders,
                        leechers = pr.leechers,
                        completedDownloads = pr.downloads
                    ),
                    source = ReleaseSource.Unknown,
                    publishedAt = pr.publishedAt ?: Instant.now()
                )).copy(
                    availability = (localRelease?.availability ?: ReleaseAvailability()).copy(
                        seeders = pr.seeders ?: localRelease?.availability?.seeders,
                        leechers = pr.leechers ?: localRelease?.availability?.leechers,
                        completedDownloads = pr.downloads ?: localRelease?.availability?.completedDownloads
                    )
                )

                // Persist enriched details to Room cache (Section 34, 59)
                releaseRepository.save(updatedRelease)
                AniFlowResult.Success(updatedRelease)
            }
            is AniFlowResult.Error -> {
                // Return offline cached data with error warning if available (Section 61)
                if (localRelease != null) {
                    AniFlowResult.Success(localRelease)
                } else {
                    remoteResult
                }
            }
            is AniFlowResult.Loading -> AniFlowResult.Loading()
        }
    }
}
