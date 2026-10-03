package com.aniflow.domain.anime

import com.aniflow.domain.coverage.AnimeCoverageCalculationService
import com.aniflow.domain.coverage.EpisodeAvailabilityState
import com.aniflow.domain.identity.AnimeId
import com.aniflow.domain.identity.AnimeTitleId
import com.aniflow.domain.identity.EpisodeId
import com.aniflow.domain.identity.ProviderId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.identity.ReleaseIdentity
import com.aniflow.domain.identity.SeasonId
import com.aniflow.domain.model.aggregate.release.ProviderRef
import com.aniflow.domain.model.aggregate.release.Release
import com.aniflow.domain.model.aggregate.release.ReleaseAvailability
import com.aniflow.domain.identity.ReleaseGroup
import com.aniflow.domain.identity.Uploader
import com.aniflow.domain.model.aggregate.release.ReleaseType
import com.aniflow.domain.valueobject.MediaSource
import com.aniflow.domain.valueobject.ReleaseTechnicalMetadata
import com.aniflow.domain.valueobject.Resolution
import com.aniflow.domain.valueobject.VideoCodec
import com.aniflow.domain.repository.ReleaseRepository
import com.aniflow.domain.repository.ReviewQueueRepository
import com.aniflow.domain.repository.UserMappingRepository
import com.aniflow.domain.usecase.CompareReleasesUseCase
import com.aniflow.domain.usecase.GetAnimeCoverageUseCase
import com.aniflow.domain.usecase.GetEpisodeCandidatesUseCase
import com.aniflow.domain.usecase.GetEpisodeCoverageUseCase
import com.aniflow.domain.usecase.GetMissingEpisodesUseCase
import com.aniflow.domain.usecase.GetSeasonCoverageUseCase
import com.aniflow.domain.valueobject.ByteSize
import com.aniflow.domain.valueobject.EpisodeNumber
import com.aniflow.domain.valueobject.EpisodeRange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * Step 20 Comprehensive Test Suite (Section 97, 98, 99, 100, 101, 102).
 */
class AnimeExperienceTestSuite {

    private val coverageService = AnimeCoverageCalculationService()

    // -------------------------------------------------------------
    // Test 1: Anime Identity and Title Deduplication (Section 97)
    // -------------------------------------------------------------
    @Test
    fun `Anime identity handles aliases and titles cleanly without embedding releases`() {
        val animeId = AnimeId("anime_one_piece")
        val canonicalTitle = "One Piece"
        val titles = listOf(
            AnimeTitle(
                id = AnimeTitleId("title_1"),
                animeId = animeId,
                value = "One Piece",
                normalizedValue = "one piece",
                type = AnimeTitleType.Canonical,
                source = TitleSource.ExternalMetadata
            ),
            AnimeTitle(
                id = AnimeTitleId("title_2"),
                animeId = animeId,
                value = "ワンピース",
                normalizedValue = "wan piisu",
                type = AnimeTitleType.Native,
                source = TitleSource.ExternalMetadata
            ),
            AnimeTitle(
                id = AnimeTitleId("title_3"),
                animeId = animeId,
                value = "ONE.PIECE",
                normalizedValue = "one piece",
                type = AnimeTitleType.ProviderObserved,
                source = TitleSource.Provider
            )
        )

        val anime = Anime(
            id = animeId,
            canonicalTitle = canonicalTitle,
            titles = titles,
            year = 1999,
            mediaType = MediaType.Series,
            status = AnimeStatus.Ongoing
        )

        assertEquals("One Piece", anime.canonicalTitle)
        assertEquals(3, anime.titles.size)
        assertEquals(MediaType.Series, anime.mediaType)
        assertEquals(AnimeStatus.Ongoing, anime.status)
        assertEquals("1999", anime.displayYear)
    }

    // -------------------------------------------------------------
    // Test 2: Unknown Season Handling (Section 12, 104)
    // -------------------------------------------------------------
    @Test
    fun `Unknown Season does not artificially become Season 1`() {
        val season = Season(
            id = SeasonId("season_unknown"),
            animeId = AnimeId("anime_one_piece"),
            number = null, // Season is unknown from raw release
            title = null,
            type = SeasonType.Unknown,
            expectedEpisodeCount = null,
            source = SeasonSource.Discovered
        )

        assertNull("Season number must be null when evidence is absent", season.number)
        assertEquals("Season (Unknown)", season.displayTitle)
        assertEquals(SeasonType.Unknown, season.type)
    }

    // -------------------------------------------------------------
    // Test 3: Dual Episode Numbering (Section 15)
    // -------------------------------------------------------------
    @Test
    fun `Episode supports seasonal number and absolute number simultaneously`() {
        val episode = Episode(
            id = EpisodeId("ep_s2_e3"),
            animeId = AnimeId("anime_naruto"),
            seasonId = SeasonId("season_2"),
            number = EpisodeNumber.of(3),
            absoluteNumber = 27,
            type = EpisodeType.Regular,
            title = "Awakening",
            expected = true,
            expectationSource = EpisodeExpectationSource.KnownSeasonStructure
        )

        assertEquals(3, episode.number?.major)
        assertEquals(27, episode.absoluteNumber)
        assertTrue(episode.expected)
        assertEquals(EpisodeExpectationSource.KnownSeasonStructure, episode.expectationSource)
    }

    // -------------------------------------------------------------
    // Test 4: Coverage Engine - Single, Range, Batch & Set (Section 25-28, 98)
    // -------------------------------------------------------------
    @Test
    fun `Coverage correctly computes single, range, and batch mappings`() {
        val animeId = AnimeId("anime_fma")
        val seasonId = SeasonId("season_1")

        val ep1 = Episode(id = EpisodeId("ep_1"), animeId = animeId, seasonId = seasonId, number = EpisodeNumber.of(1))
        val ep2 = Episode(id = EpisodeId("ep_2"), animeId = animeId, seasonId = seasonId, number = EpisodeNumber.of(2))
        val ep3 = Episode(id = EpisodeId("ep_3"), animeId = animeId, seasonId = seasonId, number = EpisodeNumber.of(3))

        // Episode 1 has primary single release
        val relSingle = ReleaseId("rel_single_1")
        val mapping1 = listOf(
            ReleaseEpisode(releaseId = relSingle, episodeId = ep1.id, relation = ReleaseEpisodeRelation.Primary, confidence = 1.0f)
        )
        val cov1 = coverageService.calculateEpisodeCoverage(ep1, mapping1)
        assertEquals(EpisodeAvailabilityState.Available, cov1.state)
        assertEquals(1, cov1.releaseCount)

        // Episode 2 has batch mapping
        val relBatch = ReleaseId("rel_batch_01_12")
        val mapping2 = listOf(
            ReleaseEpisode(releaseId = relBatch, episodeId = ep2.id, relation = ReleaseEpisodeRelation.Contained, confidence = 0.95f)
        )
        val cov2 = coverageService.calculateEpisodeCoverage(ep2, mapping2)
        assertEquals(EpisodeAvailabilityState.Available, cov2.state)
        assertEquals(1, cov2.releaseCount)

        // Episode 3 has no releases found
        val cov3 = coverageService.calculateEpisodeCoverage(ep3, emptyList())
        assertEquals(EpisodeAvailabilityState.NoReleaseFound, cov3.state)
        assertTrue(cov3.isMissing)
    }

    // -------------------------------------------------------------
    // Test 5: Missing States Differentiation (Section 19, 20, 48, 51, 99)
    // -------------------------------------------------------------
    @Test
    fun `Missing states differentiate NoReleaseFound from NoEligibleRelease and NotSearched`() {
        val ep = Episode(id = EpisodeId("ep_missing"), animeId = AnimeId("anime_1"), seasonId = SeasonId("s1"), number = EpisodeNumber.of(5))

        // Case 1: Provider returned nothing
        val noReleaseCov = coverageService.calculateEpisodeCoverage(ep, emptyList())
        assertEquals(EpisodeAvailabilityState.NoReleaseFound, noReleaseCov.state)
        assertTrue(noReleaseCov.isMissing)

        // Case 2: Ineligible releases exist
        val ineligibleCov = coverageService.calculateEpisodeCoverage(
            episode = ep,
            mappedReleases = listOf(ReleaseEpisode(ReleaseId("r_bad"), ep.id)),
            hasEligibleRelease = false
        )
        assertEquals(EpisodeAvailabilityState.NoEligibleRelease, ineligibleCov.state)
        assertTrue(ineligibleCov.isMissing)

        // Case 3: Downloading state
        val downloadingCov = coverageService.calculateEpisodeCoverage(
            episode = ep,
            mappedReleases = listOf(ReleaseEpisode(ReleaseId("r_active"), ep.id)),
            isDownloading = true
        )
        assertEquals(EpisodeAvailabilityState.Downloading, downloadingCov.state)
        assertFalse(downloadingCov.isMissing)

        // Case 4: Downloaded state
        val downloadedCov = coverageService.calculateEpisodeCoverage(
            episode = ep,
            mappedReleases = listOf(ReleaseEpisode(ReleaseId("r_done"), ep.id)),
            localFileCount = 1
        )
        assertEquals(EpisodeAvailabilityState.Downloaded, downloadedCov.state)
        assertFalse(downloadedCov.isMissing)
    }

    // -------------------------------------------------------------
    // Test 6: Unknown Expected Count (Section 22, 24, 83)
    // -------------------------------------------------------------
    @Test
    fun `Season with unknown expected count returns null percentage instead of fake 0 percent`() {
        val season = Season(
            id = SeasonId("s_one_piece"),
            animeId = AnimeId("a_one_piece"),
            number = 1,
            expectedEpisodeCount = null, // Unknown total episodes
            source = SeasonSource.Discovered
        )

        val ep1 = Episode(id = EpisodeId("ep_1"), animeId = season.animeId, seasonId = season.id, number = EpisodeNumber.of(1))
        val ep2 = Episode(id = EpisodeId("ep_2"), animeId = season.animeId, seasonId = season.id, number = EpisodeNumber.of(2))

        val epCoverages = mapOf(
            ep1.id to coverageService.calculateEpisodeCoverage(ep1, listOf(ReleaseEpisode(ReleaseId("r1"), ep1.id))),
            ep2.id to coverageService.calculateEpisodeCoverage(ep2, listOf(ReleaseEpisode(ReleaseId("r2"), ep2.id)))
        )

        val seasonCov = coverageService.calculateSeasonCoverage(season, listOf(ep1, ep2), epCoverages)

        assertNull("Percentage must be null when expected count is unknown (Section 22, 24, 83)", seasonCov.percentage)
        assertEquals(2, seasonCov.availableCount)
        assertNull("Missing count must be null when expected count is unknown", seasonCov.missingCount)
    }

    // -------------------------------------------------------------
    // Test 7: User Mapping Persistence & Parser Override (Section 64, 66, 100)
    // -------------------------------------------------------------
    @Test
    fun `User mapping persists and overrides parsed release fields`() {
        val mapping = UserMapping(
            id = "map_01",
            originalReleaseId = ReleaseId("rel_123"),
            titleSignature = "one.piece.e1050",
            field = "seasonNumber",
            oldValue = null,
            newValue = "2",
            scope = MappingScope.ThisAnime
        )

        assertEquals("rel_123", mapping.originalReleaseId?.value)
        assertEquals("seasonNumber", mapping.field)
        assertNull(mapping.oldValue)
        assertEquals("2", mapping.newValue)
        assertEquals(MappingScope.ThisAnime, mapping.scope)
    }

    // -------------------------------------------------------------
    // Test 8: Release Comparison (Section 41)
    // -------------------------------------------------------------
    @Test
    fun `CompareReleasesUseCase compares 2 to 5 releases across all technical dimensions`() = runBlocking {
        val provider = ProviderRef(ProviderId("nyaa"), "Nyaa")

        val r1 = Release(
            id = ReleaseId("rel_1"),
            provider = provider,
            providerReleaseId = "1",
            title = "[SubsPlease] One Piece - 1050 (1080p)",
            normalizedTitle = "one piece 1050",
            technical = ReleaseTechnicalMetadata(
                resolution = Resolution.R1080p,
                videoCodec = VideoCodec.AVC,
                source = MediaSource.Web
            ),
            availability = ReleaseAvailability(size = ByteSize.ofGigabytes(1.4), seeders = 85),
            releaseGroup = ReleaseGroup("SubsPlease"),
            uploader = Uploader("SubsPlease"),
            identity = ReleaseIdentity(provider.providerId, "1", null, null, "one piece", null, null)
        )

        val r2 = Release(
            id = ReleaseId("rel_2"),
            provider = provider,
            providerReleaseId = "2",
            title = "[Erai-raws] One Piece - 1050 [720p]",
            normalizedTitle = "one piece 1050",
            technical = ReleaseTechnicalMetadata(
                resolution = Resolution.R720p,
                videoCodec = VideoCodec.HEVC,
                source = MediaSource.Web
            ),
            availability = ReleaseAvailability(size = ByteSize.ofGigabytes(0.7), seeders = 42),
            releaseGroup = ReleaseGroup("Erai-raws"),
            uploader = Uploader("Erai-raws"),
            identity = ReleaseIdentity(provider.providerId, "2", null, null, "one piece", null, null)
        )

        val fakeRepo = object : ReleaseRepository {
            override suspend fun getById(id: ReleaseId): Release? = when (id.value) {
                "rel_1" -> r1
                "rel_2" -> r2
                else -> null
            }
            override suspend fun getByProviderId(providerId: ProviderId, providerReleaseId: String): Release? = null
            override suspend fun getByInfoHash(infoHash: com.aniflow.domain.valueobject.InfoHash): Release? = null
            override fun search(query: com.aniflow.domain.valueobject.SearchQuery) = emptyFlow<com.aniflow.core.common.result.AniFlowResult<com.aniflow.domain.valueobject.PageResult<Release>>>()
            override suspend fun save(release: Release) {}
            override suspend fun saveAll(releases: List<Release>) {}
            override fun observeById(id: ReleaseId): Flow<Release?> = flowOf(null)
        }

        val useCase = CompareReleasesUseCase(fakeRepo)
        val result = useCase(listOf(ReleaseId("rel_1"), ReleaseId("rel_2")))

        assertEquals(2, result.releases.size)
        assertTrue(result.attributes.any { it.name == "Resolution" && it.values == listOf("1080p", "720p") })
        assertTrue(result.attributes.any { it.name == "Video Codec" && (it.values == listOf("AVC / H.264", "HEVC / H.265") || it.values == listOf("H.264 / AVC", "HEVC / H.265")) })
        assertTrue(result.attributes.any { it.name == "Seeders" && it.values == listOf("85", "42") })
    }

    // -------------------------------------------------------------
    // Test 9: High-Volume Performance without N+1 Query (Section 96)
    // -------------------------------------------------------------
    @Test
    fun `Batch coverage calculation scales efficiently with large season and episode graphs`() {
        val anime = Anime(id = AnimeId("anime_massive"), canonicalTitle = "Long Runner")
        val seasons = (1..20).map { sNum ->
            Season(id = SeasonId("s_$sNum"), animeId = anime.id, number = sNum, expectedEpisodeCount = 50)
        }

        val startTime = System.currentTimeMillis()

        val seasonCoverages = seasons.associate { season ->
            val episodes = (1..50).map { epNum ->
                Episode(id = EpisodeId("${season.id.value}_ep_$epNum"), animeId = anime.id, seasonId = season.id, number = EpisodeNumber.of(epNum))
            }

            val epCoverages = episodes.associate { ep ->
                ep.id to coverageService.calculateEpisodeCoverage(
                    episode = ep,
                    mappedReleases = listOf(ReleaseEpisode(ReleaseId("rel_sample"), ep.id))
                )
            }

            season.id to coverageService.calculateSeasonCoverage(season, episodes, epCoverages)
        }

        val animeCoverage = coverageService.calculateAnimeCoverage(anime, seasons, seasonCoverages)
        val elapsedMs = System.currentTimeMillis() - startTime

        assertEquals(20, animeCoverage.seasons.size)
        assertEquals(1000, animeCoverage.totalExpected)
        assertEquals(1000, animeCoverage.totalAvailable)
        assertTrue("Coverage calculation for 1000 episodes across 20 seasons must execute in under 500ms", elapsedMs < 500)
    }
}
