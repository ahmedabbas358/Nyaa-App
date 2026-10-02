package com.aniflow.domain.selection

import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.intelligence.model.CompositeConfidence
import com.aniflow.domain.intelligence.model.NormalizedRelease
import com.aniflow.domain.intelligence.model.ReleaseCandidate
import com.aniflow.domain.model.aggregate.release.ReleaseType
import com.aniflow.domain.selection.fallback.FallbackTierEngine
import com.aniflow.domain.selection.model.ConstraintViolation
import com.aniflow.domain.selection.model.EligibilityResult
import com.aniflow.domain.selection.model.ResolutionMatchMode
import com.aniflow.domain.selection.model.ResolutionPreference
import com.aniflow.domain.selection.model.UploaderPreference
import com.aniflow.domain.selection.model.UserSelectionPreferences
import com.aniflow.domain.selection.relaxation.RelaxationEngine
import com.aniflow.domain.valueobject.MediaSource
import com.aniflow.domain.valueobject.ReleaseTechnicalMetadata
import com.aniflow.domain.valueobject.Resolution
import com.aniflow.domain.valueobject.VideoCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FallbackAndRelaxationTest {

    private val tierEngine = FallbackTierEngine()
    private val relaxationEngine = RelaxationEngine()

    private fun candidate(resolution: Resolution, codec: VideoCodec, uploader: String): ReleaseCandidate = ReleaseCandidate(
        episodeNumber = 1,
        release = NormalizedRelease(
            releaseId = ReleaseId("rel-${resolution.displayName}-$codec-$uploader"),
            rawTitle = "Anime - 01",
            normalizedTitle = "Anime - 01",
            animeCandidate = "Anime",
            seasonCandidate = 1,
            episodeRange = null,
            episodes = listOf(1),
            releaseType = ReleaseType.SingleEpisode,
            technicalMetadata = ReleaseTechnicalMetadata(
                resolution = resolution,
                videoCodec = codec,
                audioTracks = emptyList(),
                subtitles = emptyList(),
                source = MediaSource.WebRip,
                bitDepth = null
            ),
            groupCandidate = uploader,
            uploader = uploader,
            source = MediaSource.WebRip,
            confidence = CompositeConfidence(overall = 0.95),
            rawMetadata = mapOf("sizeBytes" to "1400000000", "seeders" to "50")
        ),
        confidence = 0.95
    )

    @Test
    fun fallbackTierEngine_assignsTier1ToIdealComboAndTier2ToAlternative() {
        val prefs = UserSelectionPreferences(
            resolution = ResolutionPreference(ResolutionMatchMode.Prefer, Resolution.R1080p, listOf(Resolution.R720p)),
            uploader = UploaderPreference(preferred = setOf("XYZ"))
        )
        val tiers = tierEngine.buildDefaultTiers(prefs)

        val idealCand = candidate(Resolution.R1080p, VideoCodec.HEVC, "XYZ")
        val altUploaderCand = candidate(Resolution.R1080p, VideoCodec.HEVC, "Other")
        val fallbackResCand = candidate(Resolution.R720p, VideoCodec.HEVC, "XYZ")

        val tierIdeal = tierEngine.assignTier(idealCand, tiers, prefs)
        val tierAltUploader = tierEngine.assignTier(altUploaderCand, tiers, prefs)
        val tierFallbackRes = tierEngine.assignTier(fallbackResCand, tiers, prefs)

        assertEquals("Ideal match must be Tier 1", 1, tierIdeal)
        assertEquals("Alternative uploader must be Tier 2", 2, tierAltUploader)
        assertTrue("Fallback resolution must be Tier >= 4", tierFallbackRes >= 4)
    }

    @Test
    fun relaxationEngine_suggestsValidAlternativesOnConstraintFailures() {
        val eligibilityResults = listOf(
            EligibilityResult.rejected(
                listOf(
                    ConstraintViolation.BlockedUploader("Restricted"),
                    ConstraintViolation.SizeExceeded(2_500_000_000L, 2_000_000_000L),
                    ConstraintViolation.WrongResolution(Resolution.R720p, Resolution.R1080p)
                )
            )
        )

        val suggestions = relaxationEngine.generateSuggestions(eligibilityResults, UserSelectionPreferences())

        assertTrue(suggestions.isNotEmpty())
        assertTrue("Should suggest relaxing uploader restriction", suggestions.any { it.proposedAction == "CLEAR_UPLOADER_RESTRICTION" })
        assertTrue("Should suggest increasing max size", suggestions.any { it.proposedAction == "INCREASE_MAX_SIZE" })
        assertTrue("Should suggest allowing 720p fallback", suggestions.any { it.proposedAction == "ALLOW_720P_FALLBACK" })
    }
}
