package com.aniflow.domain

import com.aniflow.domain.identity.ProviderId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.identity.ReleaseIdentity
import com.aniflow.domain.model.aggregate.release.ProviderRef
import com.aniflow.domain.model.aggregate.release.Release
import com.aniflow.domain.model.aggregate.release.ReleaseAvailability
import com.aniflow.domain.model.aggregate.release.ReleaseType
import com.aniflow.domain.service.CoverageComparisonService
import com.aniflow.domain.service.EpisodeCoverageService
import com.aniflow.domain.valueobject.ByteSize
import com.aniflow.domain.valueobject.CoverageComparison
import com.aniflow.domain.valueobject.EpisodeNumber
import com.aniflow.domain.valueobject.EpisodeRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EpisodeCoverageTest {

    private val provider = ProviderRef(ProviderId("nyaa"), "Nyaa")

    private fun createBatchRelease(start: Int, end: Int, sizeGb: Double): Release {
        val range = EpisodeRange.ofRange(start, end)
        return Release(
            id = ReleaseId("batch_1"),
            provider = provider,
            providerReleaseId = "b1",
            title = "Batch $start-$end",
            normalizedTitle = "batch",
            releaseType = ReleaseType.Batch,
            episodeRange = range,
            availability = ReleaseAvailability(size = ByteSize.ofGigabytes(sizeGb)),
            identity = ReleaseIdentity(provider.providerId, "b1", null, null, "batch", range, null)
        )
    }

    private fun createSingleRelease(ep: Int, sizeGb: Double): Release {
        val range = EpisodeRange.ofSingle(ep)
        return Release(
            id = ReleaseId("single_$ep"),
            provider = provider,
            providerReleaseId = "s$ep",
            title = "Episode $ep",
            normalizedTitle = "single",
            releaseType = ReleaseType.SingleEpisode,
            episodeRange = range,
            availability = ReleaseAvailability(size = ByteSize.ofGigabytes(sizeGb)),
            identity = ReleaseIdentity(provider.providerId, "s$ep", null, null, "single", range, null)
        )
    }

    @Test
    fun `Calculates missing episodes accurately`() {
        val expected = (1..12).map { EpisodeNumber.of(it) }.toSet()
        val available = listOf(
            createSingleRelease(1, 1.0),
            createSingleRelease(2, 1.0),
            createSingleRelease(3, 1.0),
            createSingleRelease(5, 1.0) // episode 4 is missing
        )
        val downloaded = setOf(EpisodeNumber.of(1), EpisodeNumber.of(2))

        val coverage = EpisodeCoverageService.calculateCoverage(
            expectedEpisodes = expected,
            availableReleases = available,
            downloadedEpisodes = downloaded
        )

        assertEquals(12, coverage.totalExpectedCount)
        assertEquals(2, coverage.downloadedCount)
        assertEquals(10, coverage.missingCount)
        assertTrue(coverage.missing.contains(EpisodeNumber.of(4)))
        assertFalse(coverage.isFullyDownloaded)
    }

    @Test
    fun `Recommends individual downloads when user already has most of batch`() {
        val batch = createBatchRelease(1, 12, 12.0)
        val singles = (1..12).map { createSingleRelease(it, 1.1) }
        val downloaded = (1..10).map { EpisodeNumber.of(it) }.toSet() // Already has 10 of 12

        val comparison = CoverageComparisonService.compare(
            batchRelease = batch,
            individualReleases = singles,
            downloadedEpisodes = downloaded
        )

        assertEquals(CoverageComparison.ComparisonRecommendation.PreferIndividuals, comparison.recommendation)
        assertEquals(10, comparison.redundantEpisodes.size)
    }
}
