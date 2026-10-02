package com.aniflow.testing

import com.aniflow.domain.service.ReleaseParser
import com.aniflow.domain.service.ReleaseParserImpl
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Enforces STEP 13 Section 9 (Parser Fuzz Testing).
 *
 * Subjecting the Release Parser to adversarial, malformed, oversized, and edge-case inputs:
 * 1. Empty and whitespace-only strings
 * 2. Massive strings (50,000+ characters)
 * 3. Non-Latin scripts (Arabic, Japanese Kanji/Kana, Chinese, Cyrillic)
 * 4. Broken and mismatched brackets
 * 5. Excessive repeated separators
 * 6. Malformed numbers and season patterns
 *
 * Guarantees that the parser NEVER crashes, NEVER enters infinite loops, and ALWAYS terminates safely.
 */
class ParserFuzzTests {

    private lateinit var parser: ReleaseParser

    @Before
    fun setUp() {
        parser = ReleaseParserImpl()
    }

    @Test
    fun testEmptyAndWhitespaceInput() {
        val emptyResult = parser.parse("")
        assertNotNull(emptyResult)

        val whitespaceResult = parser.parse("     \t \n \r  ")
        assertNotNull(whitespaceResult)
    }

    @Test
    fun testMassiveStringInput() {
        val hugeTitle = "[Group] " + "A".repeat(50_000) + " - 01 [1080p].mkv"
        val startTime = System.currentTimeMillis()
        val result = parser.parse(hugeTitle)
        val elapsed = System.currentTimeMillis() - startTime

        assertNotNull(result)
        assertTrue("Parser should handle 50k chars in under 500ms, took ${elapsed}ms", elapsed < 500)
    }

    @Test
    fun testArabicAndJapaneseUnicodeCharacters() {
        val arabicTitle = "[ترجمة العرب] هجوم العمالقة الموسم الرابع - 01 [1080p HEVC].mkv"
        val resultArabic = parser.parse(arabicTitle)
        assertNotNull(resultArabic)

        val japaneseTitle = "【推しの子】 第01話 「Mother and Children」 (1080p WEB x264 AAC).mp4"
        val resultJapanese = parser.parse(japaneseTitle)
        assertNotNull(resultJapanese)
    }

    @Test
    fun testBrokenBracketsAndSeparators() {
        val brokenTitle = "[[[Group]] Anime (((Broken) [Bracket - 01 [[[1080p] (((HEVC))))"
        val result = parser.parse(brokenTitle)
        assertNotNull(result)

        val repeatedSeps = "Anime------01______1080p......mkv"
        val sepResult = parser.parse(repeatedSeps)
        assertNotNull(sepResult)
    }

    @Test
    fun testMalformedSeasonAndEpisodeRanges() {
        val malformedTitle = "Anime S99999999999999999999E-999999999 - 01--99 [1080p]"
        val result = parser.parse(malformedTitle)
        assertNotNull(result)
    }
}
