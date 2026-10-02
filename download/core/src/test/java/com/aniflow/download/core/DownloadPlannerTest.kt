package com.aniflow.download.core

import com.aniflow.domain.identity.AnimeId
import com.aniflow.domain.identity.EpisodeId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.intelligence.model.CompositeConfidence
import com.aniflow.domain.intelligence.model.NormalizedRelease
import com.aniflow.domain.intelligence.model.ReleaseCandidate
import com.aniflow.domain.model.aggregate.download.DownloadPolicy
import com.aniflow.domain.model.aggregate.media.Episode
import com.aniflow.domain.model.aggregate.media.EpisodeType
import com.aniflow.domain.model.aggregate.release.ReleaseType
import com.aniflow.domain.selection.model.CandidateClassification
import com.aniflow.domain.selection.model.CandidateExplanation
import com.aniflow.domain.selection.model.CandidateScore
import com.aniflow.domain.selection.model.EligibilityResult
import com.aniflow.domain.selection.model.EvaluatedCandidate
import com.aniflow.domain.selection.model.SelectionExplanation
import com.aniflow.domain.selection.model.SelectionResult
import com.aniflow.domain.selection.model.SelectionResultStatus
import com.aniflow.domain.valueobject.EpisodeNumber
import com.aniflow.domain.valueobject.MediaSource
import com.aniflow.domain.valueobject.ReleaseTechnicalMetadata
import com.aniflow.domain.valueobject.Resolution
import com.aniflow.domain.valueobject.VideoCodec
import com.aniflow.download.core.model.PlanDecision
import com.aniflow.download.core.model.RuntimeCapabilities
import com.aniflow.download.core.model.StorageTarget
import com.aniflow.download.core.planner.DownloadPlanner
import com.aniflow.download.core.planner.DuplicateDetector
import com.aniflow.download.core.planner.FileExistenceChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadPlannerTest {

    private class MockFileChecker(private val existingPaths: Set<String>) : FileExistenceChecker {
        override fun exists(absolutePath: String): Boolean = existingPaths.contains(absolutePath)
    }

    private val target = StorageTarget(
        locationId = "loc-internal",
        relativePath = "Anime",
        absoluteBasePath = "/storage/emulated/0"
    )

    private fun candidate(id: String, title: String, sizeBytes: Long): ReleaseCandidate = ReleaseCandidate(
        episodeNumber = 1,
        release = NormalizedRelease(
            releaseId = ReleaseId(id),
            rawTitle = title,
            normalizedTitle = title,
            animeCandidate = "One Piece",
            seasonCandidate = 1,
            episodeRange = null,
            episodes = listOf(1),
            releaseType = ReleaseType.SingleEpisode,
            technicalMetadata = ReleaseTechnicalMetadata(
                resolution = Resolution.R1080p,
                videoCodec = VideoCodec.HEVC,
                audioTracks = emptyList(),
                subtitles = emptyList(),
                source = MediaSource.WebRip,
                bitDepth = null
            ),
            groupCandidate = "XYZ",
            uploader = "XYZ",
            source = MediaSource.WebRip,
            confidence = CompositeConfidence(overall = 0.95),
            rawMetadata = mapOf(
                "sizeBytes" to sizeBytes.toString(),
                "magnetUri" to "magnet:?xt=urn:btih:1234567890abcdef1234567890abcdef12345678"
            )
        ),
        confidence = 0.95
    )

    private fun selection(cand: ReleaseCandidate): SelectionResult {
        val eval = EvaluatedCandidate(
            candidate = cand,
            classification = CandidateClassification.Preferred,
            score = CandidateScore.Zero,
            eligibility = EligibilityResult.Eligible,
            explanation = CandidateExplanation("Match")
        )
        return SelectionResult(
            selected = cand,
            rankedCandidates = listOf(eval),
            explanation = SelectionExplanation("Match"),
            status = SelectionResultStatus.Ready
        )
    }

    @Test
    fun createPlan_generatesValidPlanWithEstimatedSize() {
        val cand = candidate("rel-1", "One Piece - 01", 1_400_000_000L)
        val epId = EpisodeId("ep-1")
        val selections = mapOf(epId to selection(cand))

        val planner = DownloadPlanner()
        val capabilities = RuntimeCapabilities(availableStorageBytes = 100_000_000_000L)

        val plan = planner.createPlan(
            selections = selections,
            profile = null,
            policy = DownloadPolicy(),
            destination = target,
            capabilities = capabilities
        )

        assertEquals(1, plan.totalItems)
        assertEquals(1, plan.selectedItems.size)
        assertEquals(1_400_000_000L, plan.estimatedBytes)
        assertEquals(PlanDecision.Selected, plan.items.first().decision)
        assertTrue(plan.items.first().destination.finalFilePath.contains("One Piece"))
    }

    @Test
    fun createPlan_flagsDuplicatesAsSkipped() {
        val cand = candidate("rel-dup", "One Piece - 02", 1_400_000_000L)
        val epId = EpisodeId("ep-2")
        val selections = mapOf(epId to selection(cand))

        // Mock existing file on disk
        val existingFilePath = "/storage/emulated/0/Anime/One Piece/Season 01/One Piece - 02.mkv"
        val planner = DownloadPlanner(
            duplicateDetector = DuplicateDetector(MockFileChecker(setOf(existingFilePath)))
        )

        val plan = planner.createPlan(
            selections = selections,
            profile = null,
            policy = DownloadPolicy(),
            destination = target,
            capabilities = RuntimeCapabilities(availableStorageBytes = 100_000_000_000L)
        )

        assertEquals(PlanDecision.SkippedDuplicate, plan.items.first().decision)
    }

    @Test
    fun createPlan_blocksItemsWhenStorageInsufficient() {
        val cand = candidate("rel-large", "One Piece - 03", 5_000_000_000L) // 5 GB
        val epId = EpisodeId("ep-3")
        val selections = mapOf(epId to selection(cand))

        val planner = DownloadPlanner()
        // Available storage is only 1 GB (less than required 5 GB + safety margin)
        val capabilities = RuntimeCapabilities(availableStorageBytes = 1_000_000_000L)

        val plan = planner.createPlan(
            selections = selections,
            profile = null,
            policy = DownloadPolicy(),
            destination = target,
            capabilities = capabilities
        )

        assertEquals(PlanDecision.BlockedByStorage, plan.items.first().decision)
        assertTrue(plan.warnings.any { it.code == "INSUFFICIENT_STORAGE" && it.isCritical })
    }
}
