package com.aniflow.provider.nyaa

import com.aniflow.provider.core.error.ProviderError
import com.aniflow.provider.nyaa.parser.NyaaHtmlSearchParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.InputStreamReader

/**
 * Regression tests for Nyaa HTML search table parser using golden fixtures (Sections 81, 82, 121).
 */
class NyaaHtmlSearchParserTest {

    private lateinit var parser: NyaaHtmlSearchParser

    @Before
    fun setUp() {
        parser = NyaaHtmlSearchParser()
    }

    private fun loadFixture(path: String): String {
        val stream = javaClass.classLoader?.getResourceAsStream(path)
            ?: throw IllegalArgumentException("Fixture not found: $path")
        return InputStreamReader(stream).readText()
    }

    @Test
    fun testParseStandardSearchPage() {
        val html = loadFixture("fixtures/nyaa_search_page.html")
        val page = parser.parse(html, requestedPage = 1)

        assertEquals(1, page.currentPage)
        assertTrue(page.hasNextPage)
        assertEquals(3, page.releases.size)

        // Verify Release 1 (Trusted SubsPlease 1080p)
        val r1 = page.releases[0]
        assertEquals("1800001", r1.id)
        assertEquals("[SubsPlease] Sousou no Frieren - 28 (1080p) [ABCD1234].mkv", r1.title)
        assertTrue(r1.isTrusted)
        assertFalse(r1.isRemake)
        assertEquals(142, r1.seeders)
        assertEquals(4, r1.leechers)
        assertEquals(3520L, r1.completedDownloads)
        assertEquals("1.4 GiB", r1.sizeDisplay)
        assertEquals(1503238553L, r1.sizeBytes) // 1.4 * 1024^3 = 1503238553L
        assertEquals("https://nyaa.si/download/1800001.torrent", r1.torrentUrl)
        assertNotNull(r1.magnetUri)
        assertEquals("0123456789abcdef0123456789abcdef01234567", r1.infoHash)

        // Verify Release 2 (Remake)
        val r2 = page.releases[1]
        assertEquals("1800002", r2.id)
        assertFalse(r2.isTrusted)
        assertTrue(r2.isRemake)
        assertEquals(25, r2.seeders)

        // Verify Release 3 (Raw Normal)
        val r3 = page.releases[2]
        assertEquals("1800003", r3.id)
        assertFalse(r3.isTrusted)
        assertFalse(r3.isRemake)
        assertEquals(15, r3.seeders)
    }

    @Test
    fun testParseEmptyResultsPage() {
        val html = loadFixture("fixtures/nyaa_search_empty.html")
        val page = parser.parse(html, requestedPage = 1)

        assertEquals(0, page.releases.size)
        assertFalse(page.hasNextPage)
        assertEquals(1, page.currentPage)
    }

    @Test(expected = ProviderError.ParserStructureChanged::class)
    fun testCorruptedHtmlThrowsParserStructureChanged() {
        val corruptedHtml = "<html><body><div>Unknown layout without table</div></body></html>"
        parser.parse(corruptedHtml, requestedPage = 1)
    }
}
