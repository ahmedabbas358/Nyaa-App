package com.aniflow.provider.nyaa

import com.aniflow.provider.nyaa.parser.NyaaHtmlDetailsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.InputStreamReader

/**
 * Unit tests verifying parsing of release details view pages (Section 38, 122).
 */
class NyaaHtmlDetailsParserTest {

    private lateinit var parser: NyaaHtmlDetailsParser

    @Before
    fun setUp() {
        parser = NyaaHtmlDetailsParser()
    }

    private fun loadFixture(path: String): String {
        val stream = javaClass.classLoader?.getResourceAsStream(path)
            ?: throw IllegalArgumentException("Fixture not found: $path")
        return InputStreamReader(stream).readText()
    }

    @Test
    fun testParseDetailsPage() {
        val html = loadFixture("fixtures/nyaa_details_page.html")
        val details = parser.parse(html, releaseId = "1800001")

        assertEquals("1800001", details.id)
        assertEquals("[SubsPlease] Sousou no Frieren - 28 (1080p) [ABCD1234].mkv", details.title)
        assertEquals("SubsPlease", details.uploaderName)
        assertEquals("https://nyaa.si/user/SubsPlease", details.uploaderUrl)
        assertEquals(142, details.seeders)
        assertEquals(4, details.leechers)
        assertEquals(3520L, details.completedDownloads)
        assertEquals("0123456789abcdef0123456789abcdef01234567", details.infoHash)
        assertEquals("1.4 GiB", details.sizeDisplay)
        assertEquals("https://nyaa.si/download/1800001.torrent", details.torrentUrl)
        assertNotNull(details.magnetUri)
        assertTrue(details.descriptionMarkdown?.contains("Frieren: Beyond Journey's End") == true)
        assertEquals(2, details.commentsCount)
    }
}
