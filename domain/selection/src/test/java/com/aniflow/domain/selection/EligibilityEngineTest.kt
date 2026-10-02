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
import com.aniflow.domain.selection.eligibility.EligibilityEngine
import com.aniflow.domain.selection.model.ConstraintViolation
import com.aniflow.domain.selection.model.HardConstraint
import com.aniflow.domain.selection.model.ResolutionMatchMode
import com.aniflow.domain.selection.model.ResolutionPreference
import com.aniflow.domain.selection.model.SubtitlePolicy
import com.aniflow.domain.selection.model.UploaderPreference
import com.aniflow.domain.selection.model.UserSelectionPreferences
import com.aniflow.domain.valueobject.AudioTrack
import com.aniflow.domain.valueobject.ByteSize
import com.aniflow.domain.valueobject.EpisodeNumber
import com.aniflow.domain.valueobject.LanguageCode
import com.aniflow.domain.valueobject.MediaSource
import com.aniflow.domain.valueobject.ReleaseTechnicalMetadata
import com.aniflow.domain.valueobject.Resolution
import com.aniflow.domain.valueobject.SubtitleTrack
import com.aniflow.domain.valueobject.VideoCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EligibilityEngineTest {

    private val engine = EligibilityEngine()

    private fun createTestCandidate(
        id: String,
        resolution: Resolution = Resolution.R1080p,
        codec: VideoCodec = VideoCodec.HEVC,
        subtitles: List<SubtitleTrack> = listOf(SubtitleTrack(LanguageCode.ENGLISH, null, null)),
        uploader: String = "TestUploader",
        sizeBytes: Long = 1_500_000_000L,
        seeders: Int = 20
    ): ReleaseCandidate {
        val normalized = NormalizedRelease(
            releaseId = ReleaseId(id),
            rawTitle = "Anime - 01 [$id]",
            normalizedTitle = "Anime - 01",
            animeCandidate = "Anime",
            seasonCandidate = 1,
            episodeRange = null,
            episodes = listOf(1),
            releaseType = ReleaseType.SingleEpisode,
            technicalMetadata = ReleaseTechnicalMetadata(
                resolution = resolution,
                videoCodec = codec,
                audioTracks = listOf(AudioTrack(LanguageCode.JAPANESE, null, null, null)),
                subtitles = subtitles,
                source = MediaSource.WebRip,
                bitDepth = null
            ),
            groupCandidate = "TestGroup",
            uploader = uploader,
            source = MediaSource.WebRip,
            confidence = CompositeConfidence(overall = 0.95),
            rawMetadata = mapOf(
                "sizeBytes" to sizeBytes.toString(),
                "seeders" to seeders.toString()
            )
        )
        return ReleaseCandidate(
            episodeNumber = 1,
            release = normalized,
            confidence = 0.95
        )
    }

    @Test
    fun requiredSubtitleMissing_rejectsCandidateImmediately() {
        // Given candidate lacking English subtitles
        val candidateWithoutEnglish = createTestCandidate(
            id = "rel-no-sub",
            subtitles = listOf(SubtitleTrack(LanguageCode.FRENCH, null, null))
        )
        val preferences = UserSelectionPreferences(
            subtitles = SubtitlePolicy(requiredLanguages = setOf(LanguageCode.ENGLISH))
        )

        // When evaluating eligibility
        val result = engine.evaluate(candidateWithoutEnglish, preferences, null)

        // Then candidate must be ineligible
        assertFalse("Candidate lacking required English subtitle must be ineligible", result.eligible)
        assertTrue(result.violations.any { it is ConstraintViolation.MissingSubtitle })
    }

    @Test
    fun blockedUploader_rejectsCandidateImmediately() {
        val candidate = createTestCandidate(id = "rel-blocked", uploader = "SpamUploader")
        val preferences = UserSelectionPreferences(
            uploader = UploaderPreference(blocked = setOf("SpamUploader"))
        )

        val result = engine.evaluate(candidate, preferences, null)

        assertFalse("Candidate with blocked uploader must be ineligible", result.eligible)
        assertTrue(result.violations.any { it is ConstraintViolation.BlockedUploader })
    }

    @Test
    fun sizeExceeded_rejectsCandidateImmediately() {
        // 2.5 GB candidate when max is 2.0 GB
        val candidate = createTestCandidate(
            id = "rel-oversize",
            sizeBytes = 2_500_000_000L
        )
        val preferences = UserSelectionPreferences(
            sizePolicy = com.aniflow.domain.selection.model.SizePolicy.ofGigabytes(hardMaxGb = 2.0)
        )

        val result = engine.evaluate(candidate, preferences, null)

        assertFalse("Candidate exceeding hard max size must be ineligible", result.eligible)
        assertTrue(result.violations.any { it is ConstraintViolation.SizeExceeded })
    }

    @Test
    fun insufficientSeeders_rejectsWhenPolicySaysReject() {
        val candidate = createTestCandidate(id = "rel-dead", seeders = 0)
        val preferences = UserSelectionPreferences(
            seederPolicy = com.aniflow.domain.selection.model.SeederPolicy(
                minSeeders = 3,
                onZeroSeeders = com.aniflow.domain.selection.model.AvailabilityAction.Reject
            )
        )

        val result = engine.evaluate(candidate, preferences, null)

        assertFalse("Candidate with zero seeders must be rejected", result.eligible)
        assertTrue(result.violations.any { it is ConstraintViolation.InsufficientSeeders })
    }

    @Test
    fun exactResolutionRequired_rejectsDifferentResolution() {
        val candidate720p = createTestCandidate(id = "rel-720p", resolution = Resolution.R720p)
        val preferences = UserSelectionPreferences(
            resolution = ResolutionPreference(
                mode = ResolutionMatchMode.Exact,
                target = Resolution.R1080p
            )
        )

        val result = engine.evaluate(candidate720p, preferences, null)

        assertFalse("Candidate with 720p must be rejected when 1080p is Exact requirement", result.eligible)
        assertTrue(result.violations.any { it is ConstraintViolation.WrongResolution })
    }
}
