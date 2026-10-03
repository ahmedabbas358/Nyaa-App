package com.aniflow.domain.usecase

import com.aniflow.core.common.result.AniFlowResult
import com.aniflow.core.common.result.ErrorType
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.model.aggregate.release.Release
import com.aniflow.domain.repository.ReleaseRepository

/**
 * Lazy, cache-first Release Details retrieval UseCase (Step 18 Sections 32, 33, 34, 51, 91).
 * Inspects Room cache first via ReleaseRepository.
 */
class GetReleaseDetailsUseCase(
    private val releaseRepository: ReleaseRepository
) {

    suspend operator fun invoke(
        releaseId: ReleaseId,
        forceRefresh: Boolean = false
    ): AniFlowResult<Release> {
        val localRelease = releaseRepository.getById(releaseId)
        return if (localRelease != null) {
            AniFlowResult.Success(localRelease)
        } else {
            AniFlowResult.Error(
                error = ErrorType.DatabaseError("Release not found with ID: ${releaseId.value}"),
                message = "Release not found with ID: ${releaseId.value}"
            )
        }
    }
}
