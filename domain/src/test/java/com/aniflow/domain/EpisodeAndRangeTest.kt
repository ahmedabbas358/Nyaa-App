package com.aniflow.domain

import com.aniflow.domain.valueobject.EpisodeNumber
import com.aniflow.domain.valueobject.EpisodeRange
import com.aniflow.domain.valueobject.SeasonNumber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EpisodeAndRangeTest {

    @Test
    fun `Parses integer episodes`() {
        val ep1 = EpisodeNumber.parseOrNull("01")
        assertNotNull(ep1)
        assertEquals(1, ep1?.major)
        assertEquals("01", ep1?.displayString)

        val ep12 = EpisodeNumber.parseOrNull("12")
        assertNotNull(ep12)
        assertEquals(12, ep12?.major)
        assertEquals("12", ep12?.displayString)
    }

    @Test
    fun `Parses decimal and recap episodes`() {
        val ep = EpisodeNumber.parseOrNull("12.5")
        assertNotNull(ep)
        assertEquals(12, ep?.major)
        assertEquals(5, ep?.minor)
        assertTrue(ep?.isDecimal == true)
        assertEquals("12.5", ep?.displayString)
    }

    @Test
    fun `Parses special episodes`() {
        val sp = EpisodeNumber.parseOrNull("SP01")
        assertNotNull(sp)
        assertEquals("SP", sp?.specialTag)
        assertEquals(1, sp?.major)
        assertTrue(sp?.isSpecial == true)
        assertEquals("SP01", sp?.displayString)

        val ova = EpisodeNumber.parseOrNull("OVA2")
        assertNotNull(ova)
        assertEquals("OVA", ova?.specialTag)
        assertEquals(2, ova?.major)
    }

    @Test
    fun `Parses contiguous episode batches`() {
        val batch = EpisodeRange.parseOrNull("01-12")
        assertNotNull(batch)
        assertTrue(batch is EpisodeRange.Range)
        val range = batch as EpisodeRange.Range
        assertEquals(1, range.start.major)
        assertEquals(12, range.end.major)
        assertEquals(12, range.count)

        val list = range.toList()
        assertEquals(12, list.size)
        assertEquals(1, list.first().major)
        assertEquals(12, list.last().major)
    }

    @Test
    fun `Parses season numbers correctly`() {
        val s1 = SeasonNumber.parseOrNull("S01")
        assertNotNull(s1)
        assertTrue(s1 is SeasonNumber.Main)
        assertEquals(1, (s1 as SeasonNumber.Main).number)

        val sSpecials = SeasonNumber.parseOrNull("Specials")
        assertEquals(SeasonNumber.Specials, sSpecials)

        val s00 = SeasonNumber.parseOrNull("S00")
        assertEquals(SeasonNumber.Specials, s00)
    }

    @Test
    fun `EpisodeQueryExpander expands zero-padded and unpadded queries`() {
        // Query containing "02" expands to include unpadded "2", "Episode 02", etc.
        val candidates1 = com.aniflow.domain.search.query.EpisodeQueryExpander.expand("One Piece 02")
        assertTrue("Expected 'One Piece 02', got $candidates1", candidates1.contains("One Piece 02"))
        assertTrue("Expected 'One Piece 2', got $candidates1", candidates1.contains("One Piece 2"))
        assertTrue("Expected 'One Piece Episode 02' or 'One Piece Episode 2', got $candidates1", candidates1.contains("One Piece Episode 2") || candidates1.contains("One Piece Episode 02"))

        // Query containing "2" expands to include zero-padded "02"
        val candidates2 = com.aniflow.domain.search.query.EpisodeQueryExpander.expand("One Piece 2")
        assertTrue(candidates2.contains("One Piece 2"))
        assertTrue(candidates2.contains("One Piece 02"))

        // Explicit episode and season expansion
        val candidates3 = com.aniflow.domain.search.query.EpisodeQueryExpander.expand(
            baseQuery = "Frieren",
            season = 1,
            episode = 4
        )
        assertTrue(candidates3.contains("Frieren S01E04"))
        assertTrue(candidates3.contains("Frieren 04"))
        assertTrue(candidates3.contains("Frieren 4"))
    }
}

