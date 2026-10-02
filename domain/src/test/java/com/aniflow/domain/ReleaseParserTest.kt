package com.aniflow.domain

import com.aniflow.domain.model.aggregate.release.ReleaseType
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

class ReleaseParserTest {

    private lateinit var parser: ReleaseParserImpl

    @Before
    fun setUp() {
        parser = ReleaseParserImpl()
    }

    @Test
    fun `Parses standard single episode release`() {
        val title = "[SubsPlease] One Piece - 1089 (1080p) [A1B2C3D4].mkv"
        val parsed = parser.parse(title)

        assertEquals("SubsPlease", parsed.releaseGroup)
        assertTrue(parsed.animeTitle.contains("One Piece", ignoreCase = true))
        assertNotNull(parsed.episodeRange)
        assertTrue(parsed.episodeRange is EpisodeRange.Single)
        assertEquals(1089, (parsed.episodeRange as EpisodeRange.Single).number.major)
        assertEquals(Resolution.R1080p, parsed.technical.resolution)
        assertEquals(ReleaseType.SingleEpisode, parsed.releaseType)
        assertTrue(parsed.parseInfo.isHighConfidence)
    }

    @Test
    fun `Parses season and episode combo with HEVC and Dual Audio`() {
        val title = "[Judas] Attack on Titan S04E12 [1080p][HEVC][Dual Audio].mkv"
        val parsed = parser.parse(title)

        assertEquals("Judas", parsed.releaseGroup)
        assertTrue(parsed.animeTitle.contains("Attack on Titan", ignoreCase = true))
        assertEquals(SeasonNumber.of(4), parsed.seasonHint)
        assertEquals(12, (parsed.episodeRange as EpisodeRange.Single).number.major)
        assertEquals(Resolution.R1080p, parsed.technical.resolution)
        assertEquals(VideoCodec.HEVC, parsed.technical.videoCodec)
        assertEquals(2, parsed.technical.audioTracks.size)
    }

    @Test
    fun `Parses batch release with range 01-12`() {
        val title = "[ASW] Spy x Family - 01-12 [1080p] (Batch)"
        val parsed = parser.parse(title)

        assertEquals("ASW", parsed.releaseGroup)
        assertTrue(parsed.animeTitle.contains("Spy x Family", ignoreCase = true))
        assertEquals(ReleaseType.Batch, parsed.releaseType)
        assertNotNull(parsed.episodeRange)
        assertTrue(parsed.episodeRange is EpisodeRange.Range)
        val range = parsed.episodeRange as EpisodeRange.Range
        assertEquals(1, range.start.major)
        assertEquals(12, range.end.major)
        assertEquals(12, range.count)
    }

    @Test
    fun `Parses movie release`() {
        val title = "[Yameii] Jujutsu Kaisen 0 the Movie (1080p) [BDRip]"
        val parsed = parser.parse(title)

        assertEquals(ReleaseType.Movie, parsed.releaseType)
        assertEquals(Resolution.R1080p, parsed.technical.resolution)
    }
}
