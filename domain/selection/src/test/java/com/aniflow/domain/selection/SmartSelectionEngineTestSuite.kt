package com.aniflow.domain.selection

import com.aniflow.domain.identity.AnimeId
import com.aniflow.domain.identity.EpisodeId
import com.aniflow.domain.identity.ProviderId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.identity.SeasonId
import com.aniflow.domain.intelligence.model.ReleaseConfidence
import com.aniflow.domain.model.aggregate.organization.DownloadProfile
import com.aniflow.domain.model.aggregate.organization.Rule
import com.aniflow.domain.model.aggregate.release.TechnicalMetadata
import com.aniflow.domain.selection.context.SelectionCandidate
import com.aniflow.domain.selection.context.SelectionContext
import com.aniflow.domain.selection.context.SelectionTarget
import com.aniflow.domain.selection.engine.SmartSelectionEngine
import com.aniflow.domain.selection.model.BatchPreference
import com.aniflow.domain.selection.model.CodecPreference
import com.aniflow.domain.selection.model.HardConstraint
import com.aniflow.domain.selection.model.ResolutionMatchMode
import com.aniflow.domain.selection.model.ResolutionPreference
import com.aniflow.domain.selection.model.SelectionObjective
import com.aniflow.domain.selection.model.SelectionStatus
import com.aniflow.domain.selection.model.SelectionWeights
import com.aniflow.domain.selection.model.SizePolicy
import com.aniflow.domain.selection.model.UploaderPreference
import com.aniflow.domain.selection.model.UserSelectionPreferences
import com.aniflow.domain.selection.optimization.SeasonSelectionContext
import com.aniflow.domain.selection.optimization.SeasonSelectionOptimizer
import com.aniflow.domain.selection.preference.LayeredResolutionPreference
import com.aniflow.domain.selection.preference.PreferenceLayer
import com.aniflow.domain.selection.preference.PreferencePrecedencePolicy
import com.aniflow.domain.valueobject.ByteSize
import com.aniflow.domain.valueobject.EpisodeNumber
import com.aniflow.domain.valueobject.MediaSource
import com.aniflow.domain.valueobject.Resolution
import com.aniflow.domain.valueobject.VideoCodec
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 21 — Smart Selection Engine Comprehensive Test Suite (Section 98-105).
 */
class SmartSelectionEngineTestSuite {

    private val engine = SmartSelectionEngine()
    private val precedencePolicy = PreferencePrecedencePolicy()
    private val seasonOptimizer = SeasonSelectionOptimizer()

    private fun createCandidate(
        id: String,
        title: String,
        resolution: Resolution = Resolution.R1080p,
        codec: VideoCodec = VideoCodec.HEVC,
        uploader: String = "SubsPlease",
        group: String = "SubsPlease",
        sizeBytes: Long = 1024L * 1024 * 1400, // 1.4 GB
        seeders: Int = 50,
        source: MediaSource = MediaSource.WebRip,
        confidence: ReleaseConfidence = ReleaseConfidence.High
    ): SelectionCandidate {
        return SelectionCandidate(
            releaseId = ReleaseId(id),
            rawTitle = title,
            technical = TechnicalMetadata(
                resolution = resolution,
                videoCodec = codec,
                source = source
            ),
            uploader = uploader,
            releaseGroup = group,
            size = ByteSize.ofBytes(sizeBytes),
            seeders = seeders,
            confidence = confidence,
            provider = ProviderId("nyaa")
        )
    }

    // -------------------------------------------------------------------------
    // 1. Eligibility & Hard Constraints Tests (Section 98)
    // -------------------------------------------------------------------------
    @Test
    fun `Hard constraint violates candidate and prevents it from winning regardless of high seeders`() = runBlocking {
        val candA = createCandidate(
            id = "c1",
            title = "Release A (720p with 500 seeds)",
            resolution = Resolution.R720p,
            seeders = 500
        )
        val candB = createCandidate(
            id = "c2",
            title = "Release B (1080p with 10 seeds)",
            resolution = Resolution.R1080p,
            seeders = 10
        )

        val profile = DownloadProfile(
            name = "1080p Required",
            hardConstraints = listOf(HardConstraint.RequireResolution(Resolution.R1080p))
        )

        val context = SelectionContext(
            target = SelectionTarget.EpisodeTarget(EpisodeId("ep1"), AnimeId("a1")),
            candidates = listOf(candA, candB),
            profile = profile,
            preferences = UserSelectionPreferences(
                resolution = ResolutionPreference(mode = ResolutionMatchMode.Exact, target = Resolution.R1080p)
            )
        )

        val result = engine.select(context)

        assertEquals("Candidate violating hard constraint must not be selected", "c2", result.selected?.candidate?.releaseId?.value)
        assertEquals(1, result.rejected.size)
        assertEquals("c1", result.rejected[0].candidate.releaseId.value)
    }

    @Test
    fun `Blocked uploader constraint rejects candidate unconditionally`() = runBlocking {
        val cand = createCandidate(id = "c_blocked", title = "Blocked Uploader Release", uploader = "BadUploader")
        val profile = DownloadProfile(
            name = "Block Uploader",
            hardConstraints = listOf(HardConstraint.BlockUploader("BadUploader"))
        )

        val context = SelectionContext(
            target = SelectionTarget.EpisodeTarget(EpisodeId("ep1"), AnimeId("a1")),
            candidates = listOf(cand),
            profile = profile,
            preferences = UserSelectionPreferences()
        )

        val result = engine.select(context)

        assertEquals(SelectionStatus.NoEligibleCandidate, result.status)
        assertNull(result.selected)
        assertEquals(1, result.rejected.size)
    }

    // -------------------------------------------------------------------------
    // 2. Preference Precedence Policy Tests (Section 11, 12, 99)
    // -------------------------------------------------------------------------
    @Test
    fun `PreferencePrecedencePolicy prioritizes Season over Anime and Anime over Global`() {
        // Global: 1080p preferred
        // Anime: 720p preferred
        // Season: 1080p required
        val layers = listOf(
            LayeredResolutionPreference(PreferenceLayer.Global, Resolution.R1080p, isRequired = false),
            LayeredResolutionPreference(PreferenceLayer.Anime, Resolution.R720p, isRequired = false),
            LayeredResolutionPreference(PreferenceLayer.Season, Resolution.R1080p, isRequired = true)
        )

        val resolved = precedencePolicy.resolveResolution(layers)

        assertEquals(Resolution.R1080p, resolved.target)
        assertEquals(ResolutionMatchMode.Exact, resolved.mode)
    }

    // -------------------------------------------------------------------------
    // 3. Fallback Tiers & Safety (Section 42-46, 101)
    // -------------------------------------------------------------------------
    @Test
    fun `Fallback relaxes soft preference (Tier 2) when Tier 1 is unavailable`() = runBlocking {
        // Preferred uploader: "SubsPlease"
        // Candidates available: Only "Erai-raws" (matches 1080p and HEVC)
        val cand = createCandidate(
            id = "c_fallback",
            title = "[Erai-raws] One Piece 1050 [1080p][HEVC]",
            resolution = Resolution.R1080p,
            codec = VideoCodec.HEVC,
            uploader = "Erai-raws"
        )

        val context = SelectionContext(
            target = SelectionTarget.EpisodeTarget(EpisodeId("ep1"), AnimeId("a1")),
            candidates = listOf(cand),
            preferences = UserSelectionPreferences(
                resolution = ResolutionPreference(target = Resolution.R1080p),
                codec = CodecPreference(preferredCodecs = listOf(VideoCodec.HEVC)),
                uploader = UploaderPreference(preferred = setOf("SubsPlease"))
            )
        )

        val result = engine.select(context)

        assertEquals("c_fallback", result.selected?.candidate?.releaseId?.value)
        assertEquals(2, result.selected?.fallbackTier)
        assertEquals(SelectionStatus.SelectedWithWarning, result.status)
        assertNotNull(result.explanation.fallback)
        assertEquals(2, result.explanation.fallback?.tier)
    }

    // -------------------------------------------------------------------------
    // 4. Deterministic Tie-Breaking (Section 56, 57)
    // -------------------------------------------------------------------------
    @Test
    fun `Tie-breaker produces identical deterministic output for identical candidate scores`() = runBlocking {
        val candA = createCandidate(id = "rel_aaa", title = "Equal Release A", sizeBytes = 1000L, seeders = 50)
        val candB = createCandidate(id = "rel_bbb", title = "Equal Release B", sizeBytes = 1000L, seeders = 50)

        val context = SelectionContext(
            target = SelectionTarget.EpisodeTarget(EpisodeId("ep1"), AnimeId("a1")),
            candidates = listOf(candB, candA), // Pass in reverse order
            preferences = UserSelectionPreferences()
        )

        val result1 = engine.select(context)
        val result2 = engine.select(context)

        assertEquals("Lexical deterministic tie breaker must select rel_aaa", "rel_aaa", result1.selected?.candidate?.releaseId?.value)
        assertEquals("Identical inputs must produce identical selection (Section 85)", result1.selected?.candidate?.releaseId?.value, result2.selected?.candidate?.releaseId?.value)
    }

    // -------------------------------------------------------------------------
    // 5. Season & Batch Optimization (Section 62-75, 103)
    // -------------------------------------------------------------------------
    @Test
    fun `SeasonSelectionOptimizer prefers Batch package when user preference is PreferBatch`() {
        val batchCand = createCandidate(
            id = "batch_01_12",
            title = "Season Batch 01-12",
            sizeBytes = 1024L * 1024 * 1024 * 12 // 12 GB
        )

        val individualCandidates = (1..12).associateWith { epNum ->
            listOf(createCandidate(id = "ep_$epNum", title = "Episode $epNum", sizeBytes = 1024L * 1024 * 1024))
        }

        val context = SeasonSelectionContext(
            seasonId = SeasonId("s1"),
            expectedEpisodes = (1..12).toList(),
            perEpisodeCandidates = individualCandidates,
            batchCandidates = listOf(batchCand),
            batchPreference = BatchPreference.PreferBatch,
            preferences = UserSelectionPreferences()
        )

        val planResult = seasonOptimizer.optimize(context)

        assertTrue("Winning plan must be a batch plan", planResult.winningPlan.isBatchPlan)
        assertEquals("batch_01_12", planResult.winningPlan.items[0].candidate.releaseId.value)
        assertEquals(1, planResult.winningPlan.downloadCount)
    }

    // -------------------------------------------------------------------------
    // 6. Property-Based Invariant Tests (Section 104)
    // -------------------------------------------------------------------------
    @Test
    fun `Property Test 1 - Forbidden candidate never becomes selected`() = runBlocking {
        val forbidden = createCandidate(
            id = "c_forbidden",
            title = "Forbidden Release",
            codec = VideoCodec.Unknown("CAM")
        )
        val profile = DownloadProfile(
            name = "No CAM",
            hardConstraints = listOf(HardConstraint.RequireCodec(VideoCodec.HEVC))
        )

        val context = SelectionContext(
            target = SelectionTarget.EpisodeTarget(EpisodeId("ep1"), AnimeId("a1")),
            candidates = listOf(forbidden),
            profile = profile,
            preferences = UserSelectionPreferences()
        )

        val result = engine.select(context)
        assertFalse("Forbidden candidate must NEVER be selected", result.hasWinner)
        assertEquals(SelectionStatus.NoEligibleCandidate, result.status)
    }

    @Test
    fun `Property Test 2 - Removing an eligible candidate cannot make an ineligible candidate selected`() = runBlocking {
        val eligible = createCandidate(id = "c_elig", title = "Eligible Release", resolution = Resolution.R1080p)
        val ineligible = createCandidate(id = "c_inelig", title = "Ineligible Release", resolution = Resolution.R720p)

        val profile = DownloadProfile(
            name = "1080p only",
            hardConstraints = listOf(HardConstraint.RequireResolution(Resolution.R1080p))
        )

        // Run 1: Both present -> eligible wins
        val ctx1 = SelectionContext(
            target = SelectionTarget.EpisodeTarget(EpisodeId("ep1"), AnimeId("a1")),
            candidates = listOf(eligible, ineligible),
            profile = profile,
            preferences = UserSelectionPreferences()
        )
        val res1 = engine.select(ctx1)
        assertEquals("c_elig", res1.selected?.candidate?.releaseId?.value)

        // Run 2: Remove eligible candidate -> ineligible must NOT win
        val ctx2 = SelectionContext(
            target = SelectionTarget.EpisodeTarget(EpisodeId("ep1"), AnimeId("a1")),
            candidates = listOf(ineligible),
            profile = profile,
            preferences = UserSelectionPreferences()
        )
        val res2 = engine.select(ctx2)
        assertFalse("Ineligible candidate must not win after eligible removal", res2.hasWinner)
        assertEquals(SelectionStatus.NoEligibleCandidate, res2.status)
    }

    // -------------------------------------------------------------------------
    // 7. Performance & Scalability (Section 105)
    // -------------------------------------------------------------------------
    @Test
    fun `Performance Test - Evaluates 1,000 candidates in under 300 milliseconds`() = runBlocking {
        val candidates = (1..1000).map { i ->
            createCandidate(
                id = "cand_$i",
                title = "Release $i 1080p HEVC",
                resolution = if (i % 2 == 0) Resolution.R1080p else Resolution.R720p,
                sizeBytes = (500L + (i % 500)) * 1024 * 1024,
                seeders = (i % 100) + 1
            )
        }

        val context = SelectionContext(
            target = SelectionTarget.EpisodeTarget(EpisodeId("ep1"), AnimeId("a1")),
            candidates = candidates,
            preferences = UserSelectionPreferences()
        )

        val startTime = System.currentTimeMillis()
        val result = engine.select(context)
        val elapsedMs = System.currentTimeMillis() - startTime

        assertNotNull(result.selected)
        assertTrue("Evaluating 1,000 candidates must complete in under 300ms (took ${elapsedMs}ms)", elapsedMs < 300)
    }
}
