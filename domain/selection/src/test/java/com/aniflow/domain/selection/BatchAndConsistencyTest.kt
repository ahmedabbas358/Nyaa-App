package com.aniflow.domain.selection

import com.aniflow.domain.identity.AnimeId
import com.aniflow.domain.identity.EpisodeId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.intelligence.model.CompositeConfidence
import com.aniflow.domain.intelligence.model.NormalizedRelease
import com.aniflow.domain.intelligence.model.ReleaseCandidate
import com.aniflow.domain.model.aggregate.media.Episode
import com.aniflow.domain.model.aggregate.media.EpisodeType
import com.aniflow.domain.model.aggregate.release.ReleaseType
import com.aniflow.domain.selection.batch.CoverageOptimizationEngine
import com.aniflow.domain.selection.consistency.SeasonSelectionOptimizer
import com.aniflow.domain.selection.model.ConsistencyPolicy
import com.aniflow.domain.selection.model.EpisodeCandidateSet
import com.aniflow.domain.selection.model.ExistingMediaState
import com.aniflow.domain.selection.model.UserSelectionPreferences
import com.aniflow.domain.valueobject.EpisodeNumber
import com.aniflow.domain.valueobject.EpisodeRange
import com.aniflow.domain.valueobject.LanguageCode
import com.aniflow.domain.valueobject.MediaSource
import com.aniflow.domain.valueobject.ReleaseTechnicalMetadata
import com.aniflow.domain.valueobject.Resolution
import com.aniflow.domain.valueobject.SubtitleTrack
import com.aniflow.domain.valueobject.VideoCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BatchAndConsistencyTest {

    private val batchEngine = CoverageOptimizationEngine()
    private val seasonOptimizer = SeasonSelectionOptimizer()

    private fun ep(num: Int) = Episode(
        id = EpisodeId("ep-$num"),
        animeId = AnimeId("anime-1"),
        seasonId = null,
        number = EpisodeNumber(num),
        title = "Episode $num",
        type = EpisodeType.Main
    )

    private fun candidate(epNum: Int, uploader: String): ReleaseCandidate = ReleaseCandidate(
        episodeNumber = epNum,
        release = NormalizedRelease(
            releaseId = ReleaseId("rel-$epNum-$uploader"),
            rawTitle = "Anime - $epNum [$uploader]",
            normalizedTitle = "Anime - $epNum",
            animeCandidate = "Anime",
            seasonCandidate = 1,
            episodeRange = null,
            episodes = listOf(epNum),
            releaseType = ReleaseType.SingleEpisode,
            technicalMetadata = ReleaseTechnicalMetadata(
                resolution = Resolution.R1080p,
                videoCodec = VideoCodec.HEVC,
                audioTracks = emptyList(),
                subtitles = listOf(SubtitleTrack(LanguageCode.ENGLISH, null, null)),
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
    fun batchOptimization_detectsCoverageForMissingEpisodes() {
        val episodes = (1..12).map { num ->
            EpisodeCandidateSet(
                episode = ep(num),
                candidates = listOf(candidate(num, "XYZ")),
                existingState = if (num <= 5) ExistingMediaState.Downloaded else ExistingMediaState.Missing
            )
        }

        val batchRelease = NormalizedRelease(
            releaseId = ReleaseId("batch-1-12"),
            rawTitle = "[XYZ] Anime - Batch 01-12 [1080p][HEVC]",
            normalizedTitle = "Anime - Batch 01-12",
            animeCandidate = "Anime",
            seasonCandidate = 1,
            episodeRange = EpisodeRange(1, 12),
            episodes = (1..12).toList(),
            releaseType = ReleaseType.Batch,
            technicalMetadata = ReleaseTechnicalMetadata(
                resolution = Resolution.R1080p,
                videoCodec = VideoCodec.HEVC,
                audioTracks = emptyList(),
                subtitles = listOf(SubtitleTrack(LanguageCode.ENGLISH, null, null)),
                source = MediaSource.WebRip,
                bitDepth = null
            ),
            groupCandidate = "XYZ",
            uploader = "XYZ",
            source = MediaSource.WebRip,
            confidence = CompositeConfidence(overall = 0.95),
            rawMetadata = mapOf("sizeBytes" to "15000000000", "seeders" to "60")
        )

        val suggestion = batchEngine.analyzeBatchCoverage(batchRelease, episodes)

        assertNotNull("Batch suggestion should be created", suggestion)
        assertEquals(12, suggestion?.coveredEpisodeNumbers?.size)
        assertEquals(7, suggestion?.missingEpisodeNumbers?.size) // Episodes 6..12
        assertEquals(5, suggestion?.existingEpisodeNumbers?.size) // Episodes 1..5
        assertTrue(suggestion!!.explanation.contains("Partial batch"))
    }

    @Test
    fun seasonSelectionOptimizer_identifiesDominantUploader() {
        val episodes = (1..10).map { num ->
            val uploader = if (num <= 8) "DominantXYZ" else "AlternativeABC"
            EpisodeCandidateSet(
                episode = ep(num),
                candidates = listOf(candidate(num, uploader)),
                existingState = ExistingMediaState.Missing
            )
        }

        val dominant = seasonOptimizer.determineDominantUploader(episodes, UserSelectionPreferences())
        assertEquals("DominantXYZ", dominant)
    }
}
