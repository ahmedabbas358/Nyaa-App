package com.aniflow.domain.service

import com.aniflow.domain.model.aggregate.release.Release
import com.aniflow.domain.valueobject.ByteSize
import com.aniflow.domain.valueobject.CoverageComparison
import com.aniflow.domain.valueobject.EpisodeNumber

/**
 * Compares batch releases vs individual releases against current library downloads (Section 112).
 * Formulates recommendations without forcing decisions on the user.
 */
object CoverageComparisonService {

    fun compare(
        batchRelease: Release,
        individualReleases: List<Release>,
        downloadedEpisodes: Set<EpisodeNumber> = emptySet()
    ): CoverageComparison {
        val batchEpisodes = batchRelease.episodeRange?.toList()?.toSet() ?: emptySet()
        val individualEpisodes = individualReleases.flatMap { it.episodeRange?.toList() ?: emptyList() }.toSet()

        val covered = batchEpisodes.intersect(individualEpisodes)
        val missingFromIndividuals = batchEpisodes - individualEpisodes
        val redundantAlreadyDownloaded = batchEpisodes.intersect(downloadedEpisodes)

        val batchSize = batchRelease.availability.size ?: ByteSize.ZERO
        val individualTotalSize = individualReleases.fold(ByteSize.ZERO) { acc, r ->
            acc + (r.availability.size ?: ByteSize.ZERO)
        }

        val recommendation = when {
            // If already mostly downloaded, individual downloads are preferred to save bandwidth
            redundantAlreadyDownloaded.size > batchEpisodes.size / 2 -> {
                CoverageComparison.ComparisonRecommendation.PreferIndividuals
            }
            // If user has zero episodes and batch covers everything cleanly with higher seeders
            downloadedEpisodes.isEmpty() && batchEpisodes.isNotEmpty() && missingFromIndividuals.isEmpty() -> {
                CoverageComparison.ComparisonRecommendation.PreferBatch
            }
            batchSize > ByteSize.ZERO && individualTotalSize > ByteSize.ZERO && batchSize < individualTotalSize -> {
                CoverageComparison.ComparisonRecommendation.PreferBatch
            }
            else -> CoverageComparison.ComparisonRecommendation.RequiresManualChoice
        }

        val explanation = buildString {
            append("Batch covers ${batchEpisodes.size} episodes (${batchSize.toDisplayString()}). ")
            if (redundantAlreadyDownloaded.isNotEmpty()) {
                append("${redundantAlreadyDownloaded.size} episodes are already present in library. ")
            }
            if (missingFromIndividuals.isNotEmpty()) {
                append("Batch includes ${missingFromIndividuals.size} episodes not found as singles. ")
            }
        }

        return CoverageComparison(
            coveredEpisodes = covered,
            missingEpisodes = missingFromIndividuals,
            redundantEpisodes = redundantAlreadyDownloaded,
            totalBatchSize = batchSize,
            totalIndividualSize = individualTotalSize,
            recommendation = recommendation,
            explanation = explanation
        )
    }
}
