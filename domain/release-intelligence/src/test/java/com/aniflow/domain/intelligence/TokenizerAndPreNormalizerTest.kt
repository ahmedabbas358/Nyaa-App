package com.aniflow.domain.intelligence

import com.aniflow.domain.intelligence.model.TokenType
import com.aniflow.domain.intelligence.normalizer.TitlePreNormalizer
import com.aniflow.domain.intelligence.tokenizer.TitleTokenizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit & Property tests for TitlePreNormalizer and TitleTokenizer (Sections 8, 9, 10, 11, 12, 143).
 */
class TokenizerAndPreNormalizerTest {

    @Test
    fun testPreNormalizerIdempotence() {
        val input = "【SubsPlease】 One.Piece - 1080.5 (1080p) [H.264]〜Test"
        val once = TitlePreNormalizer.preNormalize(input)
        val twice = TitlePreNormalizer.preNormalize(once)

        assertEquals("Pre-normalization must be strictly idempotent", once, twice)
    }

    @Test
    fun testContextAwareDotHandling() {
        val input = "Attack.on.Titan.S01E12.5.H.264.mkv"
        val result = TitlePreNormalizer.preNormalize(input)

        // Dots between words replaced by spaces; dots in decimal "12.5" and "H.264" and ".mkv" preserved
        assertTrue(result.contains("Attack on Titan"))
        assertTrue(result.contains("12.5"))
        assertTrue(result.contains("H.264"))
    }

    @Test
    fun testUnicodeBracketsNormalization() {
        val input = "【Erai-raws】 Sousou no Frieren - 01 ［1080p］"
        val result = TitlePreNormalizer.preNormalize(input)

        assertTrue(result.startsWith("[Erai-raws]"))
        assertTrue(result.contains("[1080p]"))
    }

    @Test
    fun testTokenizationStructuredOutput() {
        val input = "[SubsPlease] One Piece S01E03 [1080p] [HEVC] [Dual Audio]"
        val preNorm = TitlePreNormalizer.preNormalize(input)
        val tokens = TitleTokenizer.tokenize(preNorm)

        assertEquals(6, tokens.size)

        assertEquals("[SubsPlease]", tokens[0].raw)
        assertEquals(TokenType.ReleaseGroup, tokens[0].type)

        assertEquals("One", tokens[1].raw)
        assertEquals("Piece", tokens[2].raw)

        assertEquals("S01E03", tokens[3].raw)
        assertTrue(tokens[3].classifications.contains(TokenType.SeasonPattern))
        assertTrue(tokens[3].classifications.contains(TokenType.EpisodePattern))

        assertEquals("[1080p]", tokens[4].raw)
        assertEquals(TokenType.Resolution, tokens[4].type)

        assertEquals("[HEVC]", tokens[5].raw)
        assertEquals(TokenType.Codec, tokens[5].type)
    }
}
