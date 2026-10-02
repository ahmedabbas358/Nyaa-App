package com.aniflow.domain.intelligence

import com.aniflow.domain.identity.ProviderId
import com.aniflow.domain.intelligence.parser.ReleaseIntelligenceParser
import com.aniflow.domain.model.aggregate.release.ReleaseType
import com.aniflow.domain.valueobject.Resolution
import com.aniflow.domain.valueobject.VideoCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Golden regression tests for ReleaseIntelligenceParser (Sections 1, 92, 93, 142).
 */
class DetectorsAndParserTest {

    private lateinit var parser: ReleaseIntelligenceParser

    @Before
    fun setUp() {
        parser = ReleaseIntelligenceParser()
    }

    @Test
    fun testGoldenCase1_SingleEpisodeWithGroupAndHevc() {
        val title = "[GROUP-A] One Piece - 01 [1080p][HEVC]"
        val result = parser.parse(title, providerId = ProviderId("nyaa"))

        assertEquals("One Piece", result.animeCandidate)
        assertEquals(1, result.seasonCandidate)
        assertEquals(1, result.episodes.firstOrNull())
        assertEquals(Resolution.R1080p, result.technicalMetadata.resolution)
        assertEquals(VideoCodec.HEVC, result.technicalMetadata.videoCodec)
        assertEquals("GROUP-A", result.groupCandidate)
        assertEquals(ReleaseType.SingleEpisode, result.releaseType)
        assertTrue(result.confidence.isHighConfidence)
        assertTrue(result.conflicts.isEmpty())
    }

    @Test
    fun testGoldenCase2_SxxExxFormatWithAvc() {
        val title = "[GROUP-B] One Piece S01E02 [720p][H264]"
        val result = parser.parse(title, providerId = ProviderId("nyaa"))

        assertEquals("One Piece", result.animeCandidate)
        assertEquals(1, result.seasonCandidate)
        assertEquals(2, result.episodes.firstOrNull())
        assertEquals(Resolution.R720p, result.technicalMetadata.resolution)
        assertEquals(VideoCodec.AVC, result.technicalMetadata.videoCodec)
        assertEquals("GROUP-B", result.groupCandidate)
    }

    @Test
    fun testGoldenCase3_BatchRangeRelease() {
        val title = "[GROUP-C] One Piece 01-12 Batch [1080p]"
        val result = parser.parse(title, providerId = ProviderId("nyaa"))

        assertEquals("One Piece", result.animeCandidate)
        assertEquals(1, result.seasonCandidate)
        assertTrue(result.isBatch)
        assertEquals(ReleaseType.Batch, result.releaseType)
        assertNotNull(result.episodeRange)
        assertEquals(1, result.episodeRange?.start?.value)
        assertEquals(12, result.episodeRange?.end?.value)
        assertEquals(12, result.episodes.size)
    }

    @Test
    fun testPreserveTitleNumericName_86EightySix() {
        val title = "[SubsPlease] 86 - Eighty Six S01E03 [1080p]"
        val result = parser.parse(title, providerId = ProviderId("nyaa"))

        // Should resolve to canonical "86" without turning 86 into episode 86
        assertTrue(result.animeCandidate?.contains("86") == true)
        assertEquals(1, result.seasonCandidate)
        assertEquals(3, result.episodes.firstOrNull())
    }

    @Test
    fun testConflictingCodecsRecordsConflict() {
        val title = "[Test] Anime S01E01 [1080p][H264][HEVC]"
        val result = parser.parse(title, providerId = ProviderId("nyaa"))

        // Must record conflict and NOT pick one silently (Section 38, 61)
        assertTrue(result.conflicts.any { it.field == "VideoCodec" })
        assertEquals(null, result.technicalMetadata.videoCodec)
        assertTrue(result.confidence.needsManualReview)
    }
}
