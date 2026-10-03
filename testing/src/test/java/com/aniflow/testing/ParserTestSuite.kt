package com.aniflow.testing

import com.aniflow.domain.model.aggregate.release.ReleaseType
import com.aniflow.domain.service.ReleaseParser
import com.aniflow.domain.service.ReleaseParserImpl
import com.aniflow.domain.valueobject.EpisodeRange
import com.aniflow.domain.valueobject.Resolution
import com.aniflow.domain.valueobject.SeasonNumber
import com.aniflow.domain.valueobject.VideoCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Parser Test Suite adhering to Section 117.
 * Tests diverse anime release naming schemes from real-world release groups.
 */
class ParserTestSuite {

    private lateinit var parser: ReleaseParser

    @Before
    fun setUp() {
        parser = ReleaseParserImpl()
    }

    @Test
    fun testStandardSingleEpisode() {
        val title = "[SubsPlease] One Piece - 1089 (1080p) [A1B2C3D4].mkv"
        val result = parser.parse(title)

        assertEquals("SubsPlease", result.releaseGroup)
        assertTrue(result.animeTitle.contains("One Piece", ignoreCase = true))
        assertTrue(result.episodeRange is EpisodeRange.Single)
        assertEquals(1089, (result.episodeRange as EpisodeRange.Single).number.major)
        assertEquals(Resolution.R1080p, result.technical.resolution)
        assertEquals(ReleaseType.SingleEpisode, result.releaseType)
        assertTrue(result.parseInfo.isHighConfidence)
    }

    @Test
    fun testSeasonAndEpisodeFormat() {
        val title = "[Judas] Attack on Titan S04E12 [1080p][HEVC][Dual Audio].mkv"
        val result = parser.parse(title)

        assertEquals("Judas", result.releaseGroup)
        assertTrue(result.animeTitle.contains("Attack on Titan", ignoreCase = true))
        assertEquals(SeasonNumber.of(4), result.seasonHint)
        assertEquals(12, (result.episodeRange as EpisodeRange.Single).number.major)
        assertEquals(Resolution.R1080p, result.technical.resolution)
        assertEquals(VideoCodec.HEVC, result.technical.videoCodec)
        assertEquals(2, result.technical.audioTracks.size)
    }

    @Test
    fun testBatchReleaseFormat() {
        val title = "[ASW] Spy x Family - 01-12 [1080p] (Batch)"
        val result = parser.parse(title)

        assertEquals("ASW", result.releaseGroup)
        assertTrue(result.animeTitle.contains("Spy x Family", ignoreCase = true))
        assertEquals(ReleaseType.Batch, result.releaseType)
        assertTrue(result.episodeRange is EpisodeRange.Range)
        val range = result.episodeRange as EpisodeRange.Range
        assertEquals(1, range.start.major)
        assertEquals(12, range.end.major)
    }

    @Test
    fun testMovieRelease() {
        val title = "[Yameii] Jujutsu Kaisen 0 the Movie (1080p) [BDRip]"
        val result = parser.parse(title)

        assertEquals(ReleaseType.Movie, result.releaseType)
        assertEquals(Resolution.R1080p, result.technical.resolution)
    }
}
