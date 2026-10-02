package com.aniflow.provider.nyaa

import com.aniflow.provider.nyaa.parser.NyaaRssParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.io.InputStreamReader

/**
 * Unit tests verifying RSS 2.0 XML parsing for Nyaa fallback (Section 26, 27).
 */
class NyaaRssParserTest {

    private lateinit var parser: NyaaRssParser

    @Before
    fun setUp() {
        parser = NyaaRssParser()
    }

    private fun loadFixture(path: String): String {
        val stream = javaClass.classLoader?.getResourceAsStream(path)
            ?: throw IllegalArgumentException("Fixture not found: $path")
        return InputStreamReader(stream).readText()
    }

    @Test
    fun testParseRssXml() {
        val xml = loadFixture("fixtures/nyaa_rss.xml")
        val page = parser.parse(xml)

        assertEquals(2, page.releases.size)

        val r1 = page.releases[0]
        assertEquals("1800001", r1.id)
        assertEquals("[SubsPlease] Sousou no Frieren - 28 (1080p) [ABCD1234].mkv", r1.title)
        assertEquals(142, r1.seeders)
        assertEquals(4, r1.leechers)
        assertEquals(3520L, r1.completedDownloads)
        assertEquals("0123456789abcdef0123456789abcdef01234567", r1.infoHash)
        assertNotNull(r1.magnetUri)

        val r2 = page.releases[1]
        assertEquals("1800002", r2.id)
        assertEquals("abcdefabcdefabcdefabcdefabcdefabcdefabcd", r2.infoHash)
        assertEquals(25, r2.seeders)
    }
}
