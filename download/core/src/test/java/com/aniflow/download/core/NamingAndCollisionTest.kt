package com.aniflow.download.core

import com.aniflow.download.core.organization.CollisionPolicy
import com.aniflow.download.core.organization.FileCollisionEngine
import com.aniflow.download.core.organization.FileMediaAttributes
import com.aniflow.download.core.organization.MediaNamingContext
import com.aniflow.download.core.organization.NamingTemplateEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NamingAndCollisionTest {

    @Test
    fun testNamingTemplateEngineFormatsCleanFilename() {
        val engine = NamingTemplateEngine()
        val context = MediaNamingContext(
            anime = "One Piece",
            season = 1,
            episode = 3.0,
            episodeTitle = "Enter Zoro",
            resolution = "1080p",
            codec = "HEVC",
            group = "Erai-raws",
            extension = "mkv"
        )

        val formatted = engine.formatFileName(context)
        assertEquals("S01E03 - Enter Zoro [1080p].mkv", formatted)

        val customFormatted = engine.formatFileName(
            context,
            template = "[{group}] {anime} - S{season}E{episode} [{resolution}][{codec}]"
        )
        assertEquals("[Erai-raws] One Piece - S01E03 [1080p][HEVC].mkv", customFormatted)
    }

    @Test
    fun testNamingTemplateEngineSanitizesIllegalCharacters() {
        val engine = NamingTemplateEngine()
        val context = MediaNamingContext(
            anime = "Fate/stay night: Heaven's Feel *Special?*",
            season = 1,
            episode = 1.0,
            episodeTitle = "What <is> \"Fate\" | Part 1",
            extension = "mkv"
        )

        val formatted = engine.formatFileName(context)
        assertFalse(formatted.contains('/'))
        assertFalse(formatted.contains(':'))
        assertFalse(formatted.contains('*'))
        assertFalse(formatted.contains('?'))
        assertFalse(formatted.contains('"'))
        assertFalse(formatted.contains('<'))
        assertFalse(formatted.contains('>'))
        assertFalse(formatted.contains('|'))
    }

    @Test
    fun testDirectoryTemplateEngineNormalizesDots() {
        val engine = NamingTemplateEngine()
        val context = MediaNamingContext(
            anime = "One.Piece.Special",
            season = 2
        )

        val dir = engine.formatDirectoryPath(context)
        assertEquals("One Piece Special/Season 02", dir)
    }

    @Test
    fun testCollisionEngineAttributeComparison() {
        val engine = FileCollisionEngine()
        val existing = FileMediaAttributes(
            fileName = "Episode 01.mkv",
            sizeBytes = 500_000_000L,
            modifiedEpochMillis = 0L,
            resolution = "720p",
            codec = "H.264"
        )
        val incoming = FileMediaAttributes(
            fileName = "Episode 01.mkv",
            sizeBytes = 1_400_000_000L,
            modifiedEpochMillis = 0L,
            resolution = "1080p",
            codec = "HEVC"
        )

        val comparison = engine.compareAttributes(existing, incoming)
        assertTrue(comparison.hasHigherResolution)
        assertTrue(comparison.hasBetterCodec)
        assertEquals(CollisionPolicy.Overwrite, comparison.recommendedAction)
    }
}
