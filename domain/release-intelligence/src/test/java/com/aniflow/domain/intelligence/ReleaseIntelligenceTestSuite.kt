package com.aniflow.domain.intelligence

import com.aniflow.domain.identity.AnimeId
import com.aniflow.domain.identity.ProviderId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.intelligence.adapter.SelectionCompatibilityAdapter
import com.aniflow.domain.intelligence.coverage.EpisodeCoverageService
import com.aniflow.domain.intelligence.coverage.MissingReason
import com.aniflow.domain.intelligence.grouping.GroupingField
import com.aniflow.domain.intelligence.grouping.GroupingRule
import com.aniflow.domain.intelligence.grouping.IntelligenceGroupingMode
import com.aniflow.domain.intelligence.grouping.ReleaseGroupingEngine
import com.aniflow.domain.intelligence.identity.AnimeIdentityResolver
import com.aniflow.domain.intelligence.identity.EpisodeMapper
import com.aniflow.domain.intelligence.identity.UserMappingOverride
import com.aniflow.domain.intelligence.model.BatchType
import com.aniflow.domain.intelligence.model.ConfidenceLevel
import com.aniflow.domain.intelligence.model.EpisodeCoverage
import com.aniflow.domain.intelligence.model.MediaType
import com.aniflow.domain.intelligence.model.ParserState
import com.aniflow.domain.intelligence.model.RawReleaseInput
import com.aniflow.domain.intelligence.model.TechnicalSummary
import com.aniflow.domain.intelligence.model.VideoSource
import com.aniflow.domain.intelligence.parser.ReleaseIntelligenceParser
import com.aniflow.domain.model.aggregate.release.ReleaseEpisodeRelationType
import com.aniflow.domain.valueobject.ByteSize
import com.aniflow.domain.valueobject.Resolution
import com.aniflow.domain.valueobject.VideoCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.InputStreamReader
import java.util.Random

/**
 * Step 19 — Comprehensive Release Intelligence Test Suite (Sections 90, 91, 92, 93, 94, 121).
 * Verifies Golden Fixtures, Property Tests, Regression Tests, Fuzz Tests, and Performance Gates.
 */
class ReleaseIntelligenceTestSuite {

    private lateinit var parser: ReleaseIntelligenceParser
    private lateinit var identityResolver: AnimeIdentityResolver
    private lateinit var groupingEngine: ReleaseGroupingEngine
    private lateinit var coverageService: EpisodeCoverageService
    private lateinit var episodeMapper: EpisodeMapper

    @Before
    fun setUp() {
        identityResolver = AnimeIdentityResolver()
        parser = ReleaseIntelligenceParser(identityResolver = identityResolver)
        groupingEngine = ReleaseGroupingEngine()
        coverageService = EpisodeCoverageService()
        episodeMapper = EpisodeMapper()
    }

    // =========================================================================
    // 1. Golden Fixtures Tests (Section 91)
    // =========================================================================

    @Test
    fun testGoldenFixture_SimpleEpisode() {
        val lines = loadFixture("simple-episode.txt")
        assertTrue("Fixture must have lines", lines.isNotEmpty())

        val r1 = parser.parse(lines[0]) // [SubsPlease] One Piece - 1080 (1080p) [1234ABCD].mkv
        assertEquals("One Piece", r1.animeIdentity?.canonicalTitle)
        assertEquals(1080, (r1.episodes as? EpisodeCoverage.Single)?.episode)
        assertEquals(Resolution.R1080p, r1.technical.resolution)
        assertEquals("SubsPlease", r1.releaseGroup?.name)

        val r2 = parser.parse(lines[1]) // [Erai-raws] Jujutsu Kaisen - 01 [1080p][HEVC].mkv
        assertEquals("Jujutsu Kaisen", r2.animeIdentity?.canonicalTitle)
        assertEquals(1, (r2.episodes as? EpisodeCoverage.Single)?.episode)
        assertEquals(VideoCodec.HEVC, r2.technical.codec)
        assertEquals("Erai-raws", r2.releaseGroup?.name)
    }

    @Test
    fun testGoldenFixture_Seasonal() {
        val lines = loadFixture("seasonal.txt")
        val r1 = parser.parse(lines[0]) // [Group] Attack on Titan S04E15 [1080p].mkv
        assertEquals("Attack on Titan", r1.animeIdentity?.canonicalTitle)
        assertEquals(4, r1.season?.seasonNumber)
        assertEquals(15, (r1.episodes as? EpisodeCoverage.Single)?.episode)

        val r4 = parser.parse(lines[3]) // [Group] Vinland Saga Season II - 03 [1080p].mkv
        assertTrue(r4.animeIdentity?.canonicalTitle?.contains("Vinland Saga") == true)
        assertEquals(2, r4.season?.seasonNumber) // Roman numeral II recognized as Season 2
        assertEquals(3, (r4.episodes as? EpisodeCoverage.Single)?.episode)
    }

    @Test
    fun testGoldenFixture_Batch() {
        val lines = loadFixture("batch.txt")
        val r1 = parser.parse(lines[0]) // [Group] One Piece 01-12 Batch [1080p].mkv
        assertEquals("One Piece", r1.animeIdentity?.canonicalTitle)
        assertEquals(BatchType.EpisodeRange, r1.batchType)
        assertTrue(r1.episodes is EpisodeCoverage.Range)
        val range = r1.episodes as EpisodeCoverage.Range
        assertEquals(1, range.from)
        assertEquals(12, range.to)

        val r3 = parser.parse(lines[2]) // [Group] Death Note Complete Series [1080p][HEVC].mkv
        assertEquals("Death Note", r3.animeIdentity?.canonicalTitle)
        assertEquals(BatchType.SeriesBatch, r3.batchType)
        assertEquals(EpisodeCoverage.Series, r3.episodes)
    }

    @Test
    fun testGoldenFixture_MultiAudio() {
        val lines = loadFixture("multi-audio.txt")
        val r1 = parser.parse(lines[0]) // [Group] Cowboy Bebop [1080p][Dual Audio][FLAC].mkv
        assertTrue("Dual Audio must flag multiAudio", r1.technical.multiAudio)

        val r2 = parser.parse(lines[1]) // [Group] Cyberpunk Edgerunners [1080p][Japanese + English][Multi Audio].mkv
        assertTrue("Japanese + English must flag multiAudio", r2.technical.multiAudio)
    }

    @Test
    fun testGoldenFixture_Ambiguous() {
        val lines = loadFixture("ambiguous.txt")
        val r1 = parser.parse(lines[0]) // Anime 2024 1080p
        // Ambiguous numbers check (Section 12): 2024 is Year, NOT episode 2024!
        assertNotEquals(2024, (r1.episodes as? EpisodeCoverage.Single)?.episode)
        assertEquals(2024, r1.animeIdentity?.year)

        val r2 = parser.parse(lines[1]) // [Group] Anime S01E01 [1080p][H264][HEVC]
        assertTrue("Conflicting codecs must be recorded", r2.conflicts.any { it.field == "VideoCodec" })
        assertEquals(ParserState.Ambiguous, r2.state)
    }

    @Test
    fun testGoldenFixture_Movie() {
        val lines = loadFixture("movie.txt")
        val r1 = parser.parse(lines[0]) // [Group] Jujutsu Kaisen 0 the Movie [1080p][BluRay].mkv
        assertEquals(VideoSource.BluRay, r1.technical.source)
        assertTrue(r1.legacyReleaseType == com.aniflow.domain.model.aggregate.release.ReleaseType.Movie)
    }

    @Test
    fun testGoldenFixture_Special() {
        val lines = loadFixture("special.txt")
        val r1 = parser.parse(lines[0]) // [Group] One Piece SP01 [1080p].mkv
        assertEquals(1, (r1.episodes as? EpisodeCoverage.Single)?.episode)
        assertEquals(com.aniflow.domain.model.aggregate.release.ReleaseType.Special, r1.legacyReleaseType)

        val r2 = parser.parse(lines[1]) // [Group] Attack on Titan OVA 01 [1080p].mkv
        assertEquals(1, (r2.episodes as? EpisodeCoverage.Single)?.episode)
        assertEquals(com.aniflow.domain.model.aggregate.release.ReleaseType.Special, r2.legacyReleaseType)
    }

    @Test
    fun testGoldenFixture_DualNumbering() {
        val lines = loadFixture("dual-numbering.txt")
        val r1 = parser.parse(lines[0]) // [Group] Bleach S02E03 - 27 [1080p].mkv
        assertEquals("Bleach", r1.animeIdentity?.canonicalTitle)
        assertEquals(2, r1.season?.seasonNumber)
        assertEquals(3, (r1.episodes as? EpisodeCoverage.Single)?.episode)
    }

    @Test
    fun testGoldenFixture_UnicodeTitle() {
        val lines = loadFixture("unicode-title.txt")
        val r1 = parser.parse(lines[0]) // 【Group】One Piece - 01［1080p］（HEVC）
        assertEquals("One Piece", r1.animeIdentity?.canonicalTitle)
        assertEquals(1, (r1.episodes as? EpisodeCoverage.Single)?.episode)
        assertEquals(Resolution.R1080p, r1.technical.resolution)
        assertEquals(VideoCodec.HEVC, r1.technical.codec)
        assertEquals("Group", r1.releaseGroup?.name)

        val r2 = parser.parse(lines[1]) // 〔Group〕進撃の巨人〜01〜［720p］
        // Japanese alias進撃の巨人 resolves to Attack on Titan (Section 39)
        assertEquals("Attack on Titan", r2.animeIdentity?.canonicalTitle)
    }

    // =========================================================================
    // 2. Property Tests (Section 92)
    // =========================================================================

    @Test
    fun testProperty_Idempotence() {
        val raw = "[Group] One Piece S01E03 [1080p][HEVC]"
        val res1 = parser.parse(raw)
        val res2 = parser.parse(raw)

        assertEquals("Same input must produce identical anime", res1.animeIdentity?.canonicalTitle, res2.animeIdentity?.canonicalTitle)
        assertEquals("Same input must produce identical season", res1.season?.seasonNumber, res2.season?.seasonNumber)
        assertEquals("Same input must produce identical episodes", res1.episodes, res2.episodes)
        assertEquals("Same input must produce identical technical metadata", res1.technical, res2.technical)
        assertEquals("Same input must produce identical confidence", res1.confidence, res2.confidence)
    }

    @Test
    fun testProperty_TechnicalTokensNeverBecomeTitle() {
        val raw = "[Group] 1080p HEVC WEB-DL S01E01 [AAC]"
        val res = parser.parse(raw)

        val title = res.animeIdentity?.canonicalTitle ?: ""
        assertFalse("Title cannot contain 1080p", title.contains("1080p"))
        assertFalse("Title cannot contain HEVC", title.contains("HEVC", ignoreCase = true))
        assertFalse("Title cannot contain WEB-DL", title.contains("WEB-DL", ignoreCase = true))
    }

    @Test
    fun testProperty_BatchCoverageInternallyConsistent() {
        val raw = "[Group] One Piece 01-24 Batch [1080p]"
        val res = parser.parse(raw)

        assertTrue(res.isBatch)
        assertEquals(24, res.episodes.allEpisodes.size)
        assertTrue(res.episodes.covers(1))
        assertTrue(res.episodes.covers(12))
        assertTrue(res.episodes.covers(24))
        assertFalse(res.episodes.covers(25))
    }

    // =========================================================================
    // 3. Regression Tests (Section 93)
    // =========================================================================

    @Test
    fun testRegression_NumericTitleEightySix() {
        val raw = "[SubsPlease] 86 - Eighty Six - 04 [1080p].mkv"
        val res = parser.parse(raw)

        assertEquals("86", res.animeIdentity?.canonicalTitle)
        assertEquals(4, (res.episodes as? EpisodeCoverage.Single)?.episode)
    }

    @Test
    fun testRegression_VolumeDoesNotCollideWithEpisode() {
        val raw = "[Group] Manga Anime Volume 01 - 05 [1080p]"
        val res = parser.parse(raw)

        // Episode detector should detect episode 5, not volume 1
        assertEquals(5, (res.episodes as? EpisodeCoverage.Single)?.episode)
    }

    @Test
    fun testRegression_FractionalEpisode() {
        val raw = "[Group] Anime - 12.5 [1080p].mkv"
        val res = parser.parse(raw)

        assertEquals(12, (res.episodes as? EpisodeCoverage.Single)?.episode)
    }

    @Test
    fun testRegression_UploaderSeparatedFromReleaseGroup() {
        val input = RawReleaseInput(
            providerId = ProviderId("nyaa"),
            providerReleaseId = "12345",
            title = "[GroupX] One Piece - 01 [1080p]",
            uploader = "EraiProviderUploader"
        )
        val result = parser.parse(input)

        // Section 27: Uploader is from provider, Group is from title parsing
        assertEquals("EraiProviderUploader", result.release.uploader?.name)
        assertEquals("GroupX", result.release.releaseGroup?.name)
        assertNotEquals(result.release.uploader?.name, result.release.releaseGroup?.name)
    }

    // =========================================================================
    // 4. Fuzz Tests (Section 94)
    // =========================================================================

    @Test
    fun testFuzz_EngineNeverCrashesOnMalformedInput() {
        val random = Random(42)
        val malformedStrings = listOf(
            "",
            "   ",
            "[[[[[[[[[[[[[[",
            "]]]]]]]]]]]]]]",
            "((((((({{{{[[]",
            "~-–—_.~",
            "................",
            "Anime 999999999999999999999999999999999999",
            "Anime - - - - - - - -",
            "Anime \u0000\u0001\u0002 [1080p]",
            "A".repeat(5000), // Very long title
            "【】［］（）",
            "Anime S999E999 [9999p]",
            "混合テキストとアラビア語 العربية mixed with English 1080p",
            "[Group] S01E01E02E03 [1080p]"
        )

        for (malformed in malformedStrings) {
            val result = parser.parse(malformed)
            assertNotNull("Parser must safely return NormalizedRelease", result)
        }

        // Generate 100 randomized fuzzy strings
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789[](){}-_.~!@#$%^&* \u3000\u4e00\u4e8c"
        for (i in 0 until 100) {
            val len = random.nextInt(100) + 1
            val fuzzStr = (1..len).map { chars[random.nextInt(chars.length)] }.joinToString("")
            val result = parser.parse(fuzzStr)
            assertNotNull(result)
        }
    }

    // =========================================================================
    // 5. Large Dataset Performance Test (Section 90)
    // =========================================================================

    @Test
    fun testPerformance_ProcessThousandsOfReleasesEfficiently() {
        val titles = listOf(
            "[GroupA] One Piece - 01 [1080p][HEVC]",
            "[GroupB] One Piece S01E02 [720p][H264]",
            "[GroupA] One Piece - 03 [1080p][HEVC]",
            "[GroupC] One Piece 01-12 Batch [1080p]",
            "[SubsPlease] Sousou no Frieren - 01 [1080p]",
            "[Erai-raws] Jujutsu Kaisen 2nd Season - 01 [1080p]",
            "[Group] Attack on Titan S04E15 [1080p]",
            "[Group] Bleach S02E03 - 27 [1080p]"
        )

        // Generate 1,000 synthetic releases
        val count = 1000
        val inputs = (0 until count).map { i ->
            RawReleaseInput(
                providerId = ProviderId("nyaa"),
                providerReleaseId = "rel_$i",
                title = titles[i % titles.size],
                uploader = "Uploader_${i % 5}",
                size = ByteSize.fromMegabytes(500 + (i % 500))
            )
        }

        val startTime = System.currentTimeMillis()
        val normalizedResults = inputs.map { parser.parse(it).release }
        val grouping = groupingEngine.group(normalizedResults, IntelligenceGroupingMode.AnimeSeason)
        val elapsed = System.currentTimeMillis() - startTime

        assertEquals(count, normalizedResults.size)
        assertTrue("Grouping must cluster thousands into discrete anime season groups", grouping.groups.isNotEmpty())
        assertTrue("Processing 1,000 releases must finish in under 3,000ms", elapsed < 3000)
    }

    // =========================================================================
    // 6. Contracts & Integration Verifications (Section 3, 4, 36, 100, 107)
    // =========================================================================

    @Test
    fun testSection3And4_ContractsEndToEnd() {
        val rawInput = RawReleaseInput(
            providerId = ProviderId("nyaa"),
            providerReleaseId = "998877",
            title = "[SubsPlease] One Piece S01E03 [1080p][HEVC] [Dual Audio]",
            uploader = "EraiUploader",
            size = ByteSize.fromGigabytes(1.4)
        )

        val normalizationResult = parser.parse(rawInput)
        val rel = normalizationResult.release

        // Output Contract checks (Section 4)
        assertEquals("One Piece", rel.animeIdentity?.canonicalTitle)
        assertEquals(1, rel.season?.seasonNumber)
        assertEquals(3, (rel.episodes as? EpisodeCoverage.Single)?.episode)
        assertEquals(Resolution.R1080p, rel.technical.resolution)
        assertEquals(VideoCodec.HEVC, rel.technical.codec)
        assertTrue(rel.technical.multiAudio)
        assertEquals("SubsPlease", rel.releaseGroup?.name)
        assertEquals("EraiUploader", rel.uploader?.name)
        assertEquals(ConfidenceLevel.Confirmed, rel.confidence.level)
        assertTrue(rel.confidence.isHighConfidence)

        // Technical Summary formatter check (Section 61)
        val summary = TechnicalSummary.format(rel.technical)
        assertTrue(summary.contains("1080p"))
        assertTrue(summary.contains("HEVC"))
        assertTrue(summary.contains("Dual Audio"))

        // Selection Compatibility Contract check (Section 107)
        val candidates = SelectionCompatibilityAdapter.toReleaseCandidates(rel)
        assertEquals(1, candidates.size)
        assertEquals(3, candidates.first().episodeNumber)
        assertEquals(rel, candidates.first().release)

        // EpisodeMapper relation check (Section 100)
        val relations = episodeMapper.mapEpisodes(rel, AnimeId("anime_one_piece"))
        assertEquals(1, relations.size)
        assertEquals(ReleaseEpisodeRelationType.Primary, relations.first().relationType)
    }

    @Test
    fun testCoverageService_MissingDistinction() {
        val releases = listOf(
            parser.parse("[Group] Anime S01E01 [1080p]"),
            parser.parse("[Group] Anime S01E02 [1080p]")
            // Episode 3 missing
        )

        val coverage = coverageService.computeCoverage(
            animeTitle = "Anime",
            seasonNumber = 1,
            expectedRange = 1..3,
            availableReleases = releases,
            defaultMissingReason = MissingReason.NoResult
        )

        assertFalse(coverage.isComplete)
        assertEquals(setOf(3), coverage.missingEpisodes)
        assertEquals(MissingReason.NoResult, coverage.missingReasons[3])
    }

    @Test
    fun testCustomGrouping_ByResolutionAndCodec() {
        val releases = listOf(
            parser.parse("[Group] One Piece S01E01 [1080p][HEVC]"),
            parser.parse("[Group] One Piece S01E01 [720p][AVC]"),
            parser.parse("[Group] One Piece S01E02 [1080p][HEVC]")
        )

        val rule = GroupingRule(fields = listOf(GroupingField.Anime, GroupingField.Resolution, GroupingField.Codec))
        val grouped = groupingEngine.group(releases, mode = IntelligenceGroupingMode.Custom, customRule = rule)

        // Should form 2 groups: 1080p HEVC (2 releases) and 720p AVC (1 release)
        assertEquals(2, grouped.groups.size)
    }

    private fun loadFixture(filename: String): List<String> {
        val stream = javaClass.classLoader?.getResourceAsStream(filename)
            ?: error("Fixture not found on classpath: $filename")
        return InputStreamReader(stream, Charsets.UTF_8).readLines().map { it.trim() }.filter { it.isNotBlank() }
    }
}
