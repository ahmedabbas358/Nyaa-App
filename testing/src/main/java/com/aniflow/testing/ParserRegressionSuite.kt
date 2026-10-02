package com.aniflow.testing

import com.aniflow.domain.model.aggregate.release.ReleaseType
import com.aniflow.domain.service.ReleaseParser
import com.aniflow.domain.service.ReleaseParserImpl
import com.aniflow.domain.valueobject.AudioTrack
import com.aniflow.domain.valueobject.EpisodeRange
import com.aniflow.domain.valueobject.Resolution
import com.aniflow.domain.valueobject.SeasonNumber
import com.aniflow.domain.valueobject.SubtitleTrack
import com.aniflow.domain.valueobject.VideoCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Enforces STEP 13 Section 8 (Parser Regression Suite).
 *
 * Exhaustive regression suite testing real-world anime release title formats
 * across major fansub and rip groups (SubsPlease, Judas, ASW, Erai-raws, HorribleSubs, VARYG).
 */
class ParserRegressionSuite {

    private lateinit var parser: ReleaseParser

    @Before
    fun setUp() {
        parser = ReleaseParserImpl()
    }

    @Test
    fun testStandardSingleEpisode_SubsPlease() {
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
    fun testSeasonAndEpisode_S01E03_WebDl() {
        val title = "Frieren.Beyond.Journeys.End.S01E03.1080p.WEB-DL.HEVC.mkv"
        val result = parser.parse(title)

        assertTrue(result.animeTitle.contains("Frieren", ignoreCase = true))
        assertEquals(SeasonNumber.of(1), result.seasonHint)
        assertTrue(result.episodeRange is EpisodeRange.Single)
        assertEquals(3, (result.episodeRange as EpisodeRange.Single).number.major)
        assertEquals(Resolution.R1080p, result.technical.resolution)
        assertEquals(VideoCodec.HEVC, result.technical.videoCodec)
    }

    @Test
    fun testBatchRelease_RangeEpisodes() {
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
    fun testCompleteSeries_Batch() {
        val title = "[Judas] Death Note Complete Series (Seasons 1-2 + Specials) [BD 1080p HEVC DDP]"
        val result = parser.parse(title)

        assertEquals("Judas", result.releaseGroup)
        assertTrue(result.animeTitle.contains("Death Note", ignoreCase = true))
        assertEquals(ReleaseType.Batch, result.releaseType)
        assertEquals(Resolution.R1080p, result.technical.resolution)
        assertEquals(VideoCodec.HEVC, result.technical.videoCodec)
    }

    @Test
    fun testMovieRelease_TheMovie() {
        val title = "[Yameii] Jujutsu Kaisen 0 the Movie (1080p) [BDRip]"
        val result = parser.parse(title)

        assertEquals(ReleaseType.Movie, result.releaseType)
        assertEquals(Resolution.R1080p, result.technical.resolution)
    }

    @Test
    fun testVersion2_SingleEpisode() {
        val title = "[Erai-raws] Chainsaw Man - 04v2 [1080p][Multiple Subtitle].mkv"
        val result = parser.parse(title)

        assertEquals("Erai-raws", result.releaseGroup)
        assertTrue(result.animeTitle.contains("Chainsaw Man", ignoreCase = true))
        assertTrue(result.episodeRange is EpisodeRange.Single)
        val single = result.episodeRange as EpisodeRange.Single
        assertEquals(4, single.number.major)
        assertEquals(2, single.number.version)
    }

    @Test
    fun testDoubleEpisode_01_02() {
        val title = "[HorribleSubs] Re Zero - 01-02 [720p].mkv"
        val result = parser.parse(title)

        assertEquals("HorribleSubs", result.releaseGroup)
        assertTrue(result.animeTitle.contains("Re Zero", ignoreCase = true))
        assertTrue(result.episodeRange is EpisodeRange.Range)
        val range = result.episodeRange as EpisodeRange.Range
        assertEquals(1, range.start.major)
        assertEquals(2, range.end.major)
        assertEquals(Resolution.R720p, result.technical.resolution)
    }

    @Test
    fun testPartRelease_Part2() {
        val title = "[Erai-raws] Bleach Thousand-Year Blood War Part 2 - 01 [1080p]"
        val result = parser.parse(title)

        assertTrue(result.animeTitle.contains("Bleach", ignoreCase = true))
        assertEquals(Resolution.R1080p, result.technical.resolution)
    }

    @Test
    fun testThreeDigitEpisode_001_100() {
        val title = "[AnimeRG] Detective Conan 001-100 [480p] [Batch]"
        val result = parser.parse(title)

        assertEquals(ReleaseType.Batch, result.releaseType)
        assertTrue(result.episodeRange is EpisodeRange.Range)
        val range = result.episodeRange as EpisodeRange.Range
        assertEquals(1, range.start.major)
        assertEquals(100, range.end.major)
    }
}
