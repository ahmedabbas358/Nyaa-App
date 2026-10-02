package com.aniflow.domain.intelligence

import com.aniflow.domain.identity.AnimeId
import com.aniflow.domain.intelligence.coverage.CoverageEngine
import com.aniflow.domain.intelligence.grouping.GroupingEngine
import com.aniflow.domain.intelligence.grouping.IntelligenceGroupingMode
import com.aniflow.domain.intelligence.identity.AnimeIdentityResolver
import com.aniflow.domain.intelligence.identity.UserMappingOverride
import com.aniflow.domain.intelligence.parser.ReleaseIntelligenceParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for GroupingEngine, Candidate Sets, CoverageEngine, and User Overrides (Sections 1, 67, 72, 73, 145, 146).
 */
class GroupingAndCoverageEngineTest {

    private lateinit var parser: ReleaseIntelligenceParser
    private lateinit var groupingEngine: GroupingEngine
    private lateinit var coverageEngine: CoverageEngine
    private lateinit var identityResolver: AnimeIdentityResolver

    @Before
    fun setUp() {
        identityResolver = AnimeIdentityResolver()
        parser = ReleaseIntelligenceParser(identityResolver = identityResolver)
        groupingEngine = GroupingEngine()
        coverageEngine = CoverageEngine()
    }

    @Test
    fun testCompleteScenarioFromUserPrompt() {
        // Inputs from Section 1:
        // [GROUP-A] One Piece - 01 [1080p][HEVC]
        // [GROUP-B] One Piece S01E02 [720p][H264]
        // [GROUP-A] One Piece - 03 [1080p][HEVC]
        // [GROUP-C] One Piece 01-12 Batch [1080p]
        val titles = listOf(
            "[GROUP-A] One Piece - 01 [1080p][HEVC]",
            "[GROUP-B] One Piece S01E02 [720p][H264]",
            "[GROUP-A] One Piece - 03 [1080p][HEVC]",
            "[GROUP-C] One Piece 01-12 Batch [1080p]"
        )

        val parsedReleases = titles.map { parser.parse(it) }

        // Execute grouping
        val groupResult = groupingEngine.group(parsedReleases, IntelligenceGroupingMode.AnimeSeason)

        assertEquals(1, groupResult.groups.size)
        val group = groupResult.groups.first()

        assertEquals("One Piece", group.animeTitle)
        assertEquals(1, group.seasonNumber)

        // Episode 01 must have candidates: Release A AND Batch C
        val ep1Group = group.episodeGroups.firstOrNull { it.episodeNumber == 1 }
        assertNotNull(ep1Group)
        assertEquals(2, ep1Group?.candidates?.size) // Individual 01 + Batch 01-12

        // Episode 02 must have candidates: Release B AND Batch C
        val ep2Group = group.episodeGroups.firstOrNull { it.episodeNumber == 2 }
        assertNotNull(ep2Group)
        assertEquals(2, ep2Group?.candidates?.size) // Individual 02 + Batch 01-12

        // Episode 03 must have candidates: Release A (ep3) AND Batch C
        val ep3Group = group.episodeGroups.firstOrNull { it.episodeNumber == 3 }
        assertNotNull(ep3Group)
        assertEquals(2, ep3Group?.candidates?.size)

        // Coverage computation across expected range 1..12
        val coverage = coverageEngine.computeCoverage(
            animeTitle = "One Piece",
            seasonNumber = 1,
            expectedRange = 1..12,
            availableReleases = parsedReleases
        )

        // Because of Batch 01-12, all episodes 1..12 are available!
        assertTrue("Batch 01-12 covers 1..12 completely", coverage.isComplete)
        assertEquals(12, coverage.availableEpisodes.size)
        assertTrue(coverage.missingEpisodes.isEmpty())
    }

    @Test
    fun testMissingEpisodeDetection() {
        val titles = listOf(
            "[GROUP] Anime S01E01 [1080p]",
            "[GROUP] Anime S01E02 [1080p]",
            "[GROUP] Anime S01E03 [1080p]",
            // Episode 4 is missing
            "[GROUP] Anime S01E05 [1080p]"
        )

        val parsed = titles.map { parser.parse(it) }

        val coverage = coverageEngine.computeCoverage(
            animeTitle = "Anime",
            seasonNumber = 1,
            expectedRange = 1..5,
            availableReleases = parsed
        )

        assertFalse(coverage.isComplete)
        assertEquals(setOf(4), coverage.missingEpisodes)
    }

    @Test
    fun testUserMappingOverridePrecedence() {
        // User overrides "Custom Weird Signature" to "One Piece"
        identityResolver.addOverride(
            UserMappingOverride(
                titleSignature = "Custom Weird Title 01",
                canonicalAnimeId = AnimeId("anime_one_piece"),
                canonicalAnimeTitle = "One Piece"
            )
        )

        val result = parser.parse("Custom Weird Title 01 [1080p]")

        // User override must take top priority (Section 55, 135)
        assertEquals("One Piece", result.animeCandidate)
        assertTrue(result.confidence.titleConfidence >= 0.99)
    }
}
