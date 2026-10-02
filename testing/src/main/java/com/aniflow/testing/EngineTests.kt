package com.aniflow.testing

import com.aniflow.domain.identity.DownloadProfileId
import com.aniflow.domain.identity.ProviderId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.identity.ReleaseIdentity
import com.aniflow.domain.identity.UploaderId
import com.aniflow.domain.model.aggregate.download.GroupingMode
import com.aniflow.domain.model.aggregate.organization.DownloadPreferences
import com.aniflow.domain.model.aggregate.organization.DownloadProfile
import com.aniflow.domain.model.aggregate.organization.PreferenceRequirement
import com.aniflow.domain.model.aggregate.release.ProviderRef
import com.aniflow.domain.model.aggregate.release.Release
import com.aniflow.domain.model.aggregate.release.ReleaseAvailability
import com.aniflow.domain.model.aggregate.release.ReleaseTechnicalMetadata
import com.aniflow.domain.model.aggregate.release.ReleaseType
import com.aniflow.domain.model.aggregate.release.Uploader
import com.aniflow.domain.service.EpisodeCoverageService
import com.aniflow.domain.service.ReleaseGroupingService
import com.aniflow.domain.service.ReleaseNormalizationService
import com.aniflow.domain.service.SelectionService
import com.aniflow.domain.state.DownloadState
import com.aniflow.domain.state.DownloadStateMachine
import com.aniflow.domain.valueobject.ByteSize
import com.aniflow.domain.valueobject.EpisodeNumber
import com.aniflow.domain.valueobject.EpisodeRange
import com.aniflow.domain.valueobject.Resolution
import com.aniflow.domain.valueobject.SeasonNumber
import com.aniflow.domain.valueobject.VideoCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineTests {

    private val provider = ProviderRef(ProviderId("nyaa"), "Nyaa")

    @Test
    fun testDownloadStateMachineValidTransitions() {
        assertTrue(DownloadStateMachine.canTransition(DownloadState.Pending, DownloadState.Queued))
        assertTrue(DownloadStateMachine.canTransition(DownloadState.Queued, DownloadState.Starting))
        assertTrue(DownloadStateMachine.canTransition(DownloadState.Starting, DownloadState.Downloading))
        assertTrue(DownloadStateMachine.canTransition(DownloadState.Downloading, DownloadState.Verifying))
        assertTrue(DownloadStateMachine.canTransition(DownloadState.Verifying, DownloadState.Moving))
        assertTrue(DownloadStateMachine.canTransition(DownloadState.Moving, DownloadState.Completed))
    }

    @Test
    fun testDownloadStateMachineInvalidTransitions() {
        assertFalse(DownloadStateMachine.canTransition(DownloadState.Pending, DownloadState.Completed))
        assertFalse(DownloadStateMachine.canTransition(DownloadState.Queued, DownloadState.Completed))
        assertFalse(DownloadStateMachine.canTransition(DownloadState.Completed, DownloadState.Downloading))
    }

    @Test
    fun testMissingEpisodesDetection() {
        val expected = (1..6).map { EpisodeNumber.of(it) }.toSet()
        val availableReleases = listOf(1, 2, 3, 5, 6).map { ep ->
            createTestRelease(ep.toString(), "Anime - $ep", 1, ep, 100)
        }

        val coverage = EpisodeCoverageService.calculateCoverage(
            expectedEpisodes = expected,
            availableReleases = availableReleases
        )

        assertEquals(listOf(EpisodeNumber.of(4)), coverage.missing.toList())
    }

    @Test
    fun testSmartGroupingClustering() {
        val r1 = createTestRelease("1", "One Piece - 01", 1, 1, 100)
        val r2 = createTestRelease("2", "One Piece - 02", 1, 2, 80)
        val r3 = createTestRelease("3", "Bleach - 01", 1, 1, 50)

        val result = ReleaseGroupingService.group(listOf(r1, r2, r3), GroupingMode.Anime)
        assertEquals(2, result.groups.size)
    }

    @Test
    fun testSelectionScoringPrefersResolution() {
        val highQuality = createTestRelease("1", "One Piece - 01", 1, 1, 100, Resolution.R1080p, VideoCodec.HEVC)
        val lowQuality = createTestRelease("2", "One Piece - 01", 1, 1, 100, Resolution.R480p, VideoCodec.AVC)

        val profile = DownloadProfile(
            id = DownloadProfileId("prof"),
            name = "HD",
            preferences = DownloadPreferences(
                resolution = PreferenceRequirement.Preferred(Resolution.R1080p),
                codec = PreferenceRequirement.Preferred(VideoCodec.HEVC)
            )
        )

        val evalHigh = SelectionService.evaluateRelease(highQuality, profile)
        val evalLow = SelectionService.evaluateRelease(lowQuality, profile)

        assertTrue(evalHigh.score.totalScore > evalLow.score.totalScore)
    }

    private fun createTestRelease(
        id: String,
        title: String,
        season: Int,
        episode: Int,
        seeders: Int,
        resolution: Resolution = Resolution.R1080p,
        codec: VideoCodec = VideoCodec.HEVC
    ): Release {
        val normalized = ReleaseNormalizationService.normalizeTitle(title)
        val range = EpisodeRange.ofSingle(episode)
        return Release(
            id = ReleaseId(id),
            provider = provider,
            providerReleaseId = id,
            title = title,
            normalizedTitle = normalized,
            releaseType = ReleaseType.SingleEpisode,
            seasonHint = SeasonNumber.of(season),
            episodeRange = range,
            uploader = Uploader(UploaderId("sub"), provider, "Sub", "sub"),
            technical = ReleaseTechnicalMetadata(resolution, codec, emptyList(), emptyList(), null, null),
            availability = ReleaseAvailability(size = ByteSize.ofGigabytes(1.4), seeders = seeders),
            identity = ReleaseIdentity(provider.providerId, id, null, null, normalized, range, null)
        )
    }
}
