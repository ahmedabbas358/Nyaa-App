package com.aniflow.feature.search

import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.search.comparison.ReleaseComparisonEngine
import com.aniflow.domain.search.coordinator.LocalSearchDataSource
import com.aniflow.domain.search.coordinator.ProviderSearchDataSource
import com.aniflow.domain.search.coordinator.SearchCoordinator
import com.aniflow.domain.search.deduplication.SearchDeduplicator
import com.aniflow.domain.search.model.ReleaseComparisonItem
import com.aniflow.domain.search.model.SearchContext
import com.aniflow.domain.search.model.SearchExpression
import com.aniflow.domain.search.model.SearchField
import com.aniflow.domain.search.model.SearchQuery
import com.aniflow.domain.search.model.SearchResultItem
import com.aniflow.domain.search.model.SearchSource
import com.aniflow.domain.search.model.SearchSuggestion
import com.aniflow.domain.search.normalizer.SearchArabicNormalizer
import com.aniflow.domain.search.parser.SearchQueryParser
import com.aniflow.domain.search.ranking.SearchRankingPolicy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * UniversalSearchTestSuite (Section 50, 54).
 * Tests all DoD criteria for Step 26:
 * 1. Arabic normalization (Alef, Yeh, Teh Marbuta, diacritics removal, original preservation).
 * 2. Query parsing (tokens, AST compilation, prefixes: uploader, resolution, codec, batch).
 * 3. Search Deduplicator (deterministic, multi-source merging into Hybrid).
 * 4. Search Ranking Policy (exact match, prefix, tokens, context boost, explainability).
 * 5. Release Comparison Engine (difference detection across technical attributes).
 * 6. Search Coordinator (local + provider merging, offline fallback handling).
 */
class UniversalSearchTestSuite {

    // =========================================================================
    // 1. Arabic Normalization Tests (Section 13)
    // =========================================================================

    @Test
    fun testArabicNormalizer_normalizesAlefYehTehAndStripsTashkeel() {
        val input = "نَارُوتُو: شِيبُودِنْ أَنْمِي جَمِيلَةٌ"
        val result = SearchArabicNormalizer.normalize(input)

        // Original preserved
        assertEquals(input, result.original)

        // Tashkeel stripped, Alef normalized, Teh Marbuta to Heh
        assertEquals("ناروتو: شيبودن انمي جميله", result.normalized)
    }

    @Test
    fun testArabicNormalizer_matchesRegardlessOfDiacriticsOrGlyphVariants() {
        val query = "أكاديمية"
        val target = "أَكَادِيمِيَّةُ بَطَلِي"

        assertTrue(SearchArabicNormalizer.matches(query, target))
    }

    // =========================================================================
    // 2. Query Parser & AST Compilation Tests (Section 3, 4)
    // =========================================================================

    @Test
    fun testSearchQueryParser_extractsStructuredAstTokens() {
        val raw = "uploader:SubsPlease One Piece 1080p HEVC batch"
        val query = SearchQueryParser.parse(raw)

        assertNotNull(query.expression)
        val expr = query.expression as SearchExpression.And

        // Verified AST has extracted conditions: Uploader, Resolution, Codec, Batch, Title
        assertEquals(5, expr.children.size)

        val compiled = query.expression!!.toQueryString()
        assertTrue(compiled.contains("Uploader Equals \"SubsPlease\""))
        assertTrue(compiled.contains("Resolution Equals \"1080p\""))
        assertTrue(compiled.contains("Codec Equals \"HEVC\""))
        assertTrue(compiled.contains("Batch Equals true"))
        assertTrue(compiled.contains("Title Contains \"One Piece\""))
    }

    // =========================================================================
    // 3. Deduplication Tests (Section 17, 18)
    // =========================================================================

    @Test
    fun testSearchDeduplicator_mergesLocalAndProviderResultsIntoHybrid() {
        val localRelease = SearchResultItem.ReleaseResult(
            id = "nyaa_123",
            title = "[SubsPlease] One Piece - 1110 (1080p)",
            source = SearchSource.Local,
            resolution = "1080p",
            codec = "HEVC",
            uploader = "SubsPlease",
            isDownloaded = true,
            seeders = 10
        )

        val providerRelease = SearchResultItem.ReleaseResult(
            id = "nyaa_123",
            title = "[SubsPlease] One Piece - 1110 (1080p)",
            source = SearchSource.Provider,
            resolution = "1080p",
            codec = "HEVC",
            uploader = "SubsPlease",
            isDownloaded = false,
            seeders = 250 // Live seeds count
        )

        val deduplicated = SearchDeduplicator.deduplicate(listOf(localRelease, providerRelease))

        assertEquals(1, deduplicated.size)
        val merged = deduplicated.first() as SearchResultItem.ReleaseResult
        assertEquals(SearchSource.Hybrid, merged.source)
        assertTrue(merged.isDownloaded) // Kept local state
        assertEquals(250, merged.seeders) // Kept updated provider seeds
    }

    // =========================================================================
    // 4. Search Ranking Policy Tests (Section 19, 20, 21)
    // =========================================================================

    @Test
    fun testSearchRankingPolicy_prioritizesExactMatchAndContext() {
        val items = listOf(
            SearchResultItem.ReleaseResult(
                id = "r1",
                title = "Naruto - Episode 01 (720p)",
                source = SearchSource.Provider,
                uploader = "Erai"
            ),
            SearchResultItem.AnimeResult(
                id = "a1",
                title = "Naruto",
                source = SearchSource.Local
            ),
            SearchResultItem.ReleaseResult(
                id = "r2",
                title = "Boruto: Naruto Next Generations - 01",
                source = SearchSource.Provider,
                uploader = "SubsPlease"
            )
        )

        val ranked = SearchRankingPolicy.rank(
            items = items,
            rawQuery = "Naruto",
            context = SearchContext(animeTitle = "Naruto")
        )

        // Exact match anime entity should be ranked #1
        val top = ranked.first()
        assertTrue(top is SearchResultItem.AnimeResult)
        assertEquals("Naruto", top.title)
        assertTrue(top.matchedReasons.contains("Exact title match"))
        assertTrue(top.matchedReasons.contains("Matches active anime context ('Naruto')"))
    }

    // =========================================================================
    // 5. Release Comparison Engine Tests (Section 32)
    // =========================================================================

    @Test
    fun testReleaseComparisonEngine_detectsAttributeDifferences() {
        val items = listOf(
            ReleaseComparisonItem(
                releaseId = ReleaseId("r1"),
                title = "[SubsPlease] One Piece - 1110 (1080p)",
                animeTitle = "One Piece",
                resolution = "1080p",
                codec = "HEVC",
                audio = "Japanese",
                subtitles = "English",
                source = "WEB-DL",
                sizeBytes = 1400_000_000L,
                sizeFormatted = "1.4 GB",
                seeds = 250,
                peers = 15,
                uploader = "SubsPlease",
                releaseGroup = "SubsPlease",
                isBatch = false,
                provider = "Nyaa"
            ),
            ReleaseComparisonItem(
                releaseId = ReleaseId("r2"),
                title = "[Erai-raws] One Piece - 1110 [720p]",
                animeTitle = "One Piece",
                resolution = "720p",
                codec = "HEVC",
                audio = "Japanese",
                subtitles = "Multi-Sub",
                source = "WEB-DL",
                sizeBytes = 850_000_000L,
                sizeFormatted = "850 MB",
                seeds = 120,
                peers = 8,
                uploader = "Erai-raws",
                releaseGroup = "Erai-raws",
                isBatch = false,
                provider = "Nyaa"
            )
        )

        val result = ReleaseComparisonEngine.compare(items)

        assertEquals(2, result.items.size)

        // Check Resolution diff
        val resDiff = result.differences.first { it.attributeName == "Resolution" }
        assertTrue(resDiff.hasDifference)
        assertEquals("1080p", resDiff.valuesByReleaseId["r1"])
        assertEquals("720p", resDiff.valuesByReleaseId["r2"])

        // Check Audio (same, no diff)
        val audioDiff = result.differences.first { it.attributeName == "Audio Language" }
        assertFalse(audioDiff.hasDifference)
    }

    // =========================================================================
    // 6. Search Coordinator Offline Handling Tests (Section 28)
    // =========================================================================

    @Test
    fun testSearchCoordinator_offlineModeContinuesWithLocalResults() = runBlocking {
        val localMock = object : LocalSearchDataSource {
            override suspend fun searchLocal(query: SearchQuery): List<SearchResultItem> {
                return listOf(
                    SearchResultItem.AnimeResult("a1", "One Piece", SearchSource.Local)
                )
            }
            override suspend fun getSuggestions(prefix: String): List<SearchSuggestion> = emptyList()
        }

        val providerMock = object : ProviderSearchDataSource {
            override suspend fun searchProvider(query: SearchQuery, page: Int): List<SearchResultItem.ReleaseResult> {
                throw IllegalStateException("Provider should not be called in offline mode!")
            }
        }

        val coordinator = SearchCoordinator(localMock, providerMock)

        val response = coordinator.search(rawText = "One Piece", isOffline = true)

        assertEquals(1, response.results.size)
        assertEquals("One Piece", response.results.first().title)

        // Provider status marked as unavailable
        val provState = response.sourceStates.first { it.source == SearchSource.Provider }
        assertFalse(provState.isAvailable)
        assertTrue(provState.errorMessage!!.contains("offline"))
    }
}
