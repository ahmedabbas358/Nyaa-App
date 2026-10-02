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
import com.aniflow.domain.selection.engine.DefaultSelectionEngine
import com.aniflow.domain.selection.lock.SelectionLockManager
import com.aniflow.domain.selection.model.CandidateClassification
import com.aniflow.domain.selection.model.ExistingMediaState
import com.aniflow.domain.selection.model.SelectionContext
import com.aniflow.domain.selection.model.SelectionResultStatus
import com.aniflow.domain.selection.model.UploaderPreference
import com.aniflow.domain.selection.model.UserSelectionPreferences
import com.aniflow.domain.selection.override.ManualOverrideManager
import com.aniflow.domain.valueobject.AudioTrack
import com.aniflow.domain.valueobject.EpisodeNumber
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

class ManualOverrideAndLockTest {

    private val overrideManager = ManualOverrideManager()
    private val lockManager = SelectionLockManager()
    private val engine = DefaultSelectionEngine(
        manualOverrideManager = overrideManager,
        selectionLockManager = lockManager
    )

    private val episode = Episode(
        id = EpisodeId("ep-5"),
        animeId = AnimeId("anime-1"),
        seasonId = null,
        number = EpisodeNumber(5),
        title = "Episode 5",
        type = EpisodeType.Main
    )

    private fun candidate(id: String, uploader: String): ReleaseCandidate = ReleaseCandidate(
        episodeNumber = 5,
        release = NormalizedRelease(
            releaseId = ReleaseId(id),
            rawTitle = "Anime - 05 [$uploader]",
            normalizedTitle = "Anime - 05",
            animeCandidate = "Anime",
            seasonCandidate = 1,
            episodeRange = null,
            episodes = listOf(5),
            releaseType = ReleaseType.SingleEpisode,
            technicalMetadata = ReleaseTechnicalMetadata(
                resolution = Resolution.R1080p,
                videoCodec = VideoCodec.HEVC,
                audioTracks = listOf(AudioTrack(LanguageCode.JAPANESE, null, null, null)),
                subtitles = listOf(SubtitleTrack(LanguageCode.ENGLISH, null, null)),
                source = MediaSource.WebRip,
                bitDepth = null
            ),
            groupCandidate = "SubGroup",
            uploader = uploader,
            source = MediaSource.WebRip,
            confidence = CompositeConfidence(overall = 0.95),
            rawMetadata = mapOf("sizeBytes" to "1400000000", "seeders" to "50")
        ),
        confidence = 0.95
    )

    @Test
    fun manualOverride_supersedesProfilePreference() {
        val candidateA = candidate("rel-A", "UploaderA") // Preferred by profile
        val candidateB = candidate("rel-B", "UploaderB") // User manually chooses this

        val preferences = UserSelectionPreferences(
            uploader = UploaderPreference(preferred = setOf("UploaderA"))
        )

        // 1. Without override, Candidate A is chosen
        val contextWithoutOverride = SelectionContext(
            episode = episode,
            candidates = listOf(candidateA, candidateB),
            profile = null,
            preferences = preferences
        )
        val initialResult = engine.evaluate(contextWithoutOverride)
        assertEquals("Without override, Candidate A should be selected", candidateA.release.id, initialResult.selected?.release?.id)

        // 2. User sets manual override for Candidate B
        overrideManager.setOverride(episode.id, candidateB.release.id, "User specifically picked B")

        val resultWithOverride = engine.evaluate(contextWithoutOverride)
        assertEquals("With override, Candidate B MUST be selected", candidateB.release.id, resultWithOverride.selected?.release?.id)
        assertEquals(SelectionResultStatus.ManualOverrideApplied, resultWithOverride.status)
        assertTrue(resultWithOverride.isManualOverride)

        // 3. User resets override
        overrideManager.removeOverride(episode.id)
        val resultAfterReset = engine.evaluate(contextWithoutOverride)
        assertEquals("After removing override, engine returns to Candidate A", candidateA.release.id, resultAfterReset.selected?.release?.id)
    }

    @Test
    fun selectionLock_protectsSelectionFromAutoReplacement() {
        val candidateA = candidate("rel-A", "UploaderA")
        val candidateB = candidate("rel-B", "UploaderB")

        lockManager.lock(episode.id, candidateA.release.id)

        val context = SelectionContext(
            episode = episode,
            candidates = listOf(candidateA, candidateB),
            profile = null,
            preferences = UserSelectionPreferences()
        )

        val result = engine.evaluate(context)
        assertTrue("Result must reflect locked state", result.isLocked)
        assertEquals(SelectionResultStatus.Locked, result.status)
    }
}
