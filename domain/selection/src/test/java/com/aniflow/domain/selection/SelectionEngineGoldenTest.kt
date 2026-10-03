package com.aniflow.domain.selection

import com.aniflow.domain.identity.AnimeId
import com.aniflow.domain.identity.DownloadProfileId
import com.aniflow.domain.identity.EpisodeId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.intelligence.model.CompositeConfidence
import com.aniflow.domain.intelligence.model.NormalizedRelease
import com.aniflow.domain.intelligence.model.ReleaseCandidate
import com.aniflow.domain.model.aggregate.media.Episode
import com.aniflow.domain.model.aggregate.media.EpisodeType
import com.aniflow.domain.model.aggregate.release.ReleaseType
import com.aniflow.domain.selection.engine.DefaultSelectionEngine
import com.aniflow.domain.selection.model.CandidateClassification
import com.aniflow.domain.selection.model.CodecPreference
import com.aniflow.domain.selection.model.DownloadProfile
import com.aniflow.domain.selection.model.HardConstraint
import com.aniflow.domain.selection.model.ResolutionMatchMode
import com.aniflow.domain.selection.model.ResolutionPreference
import com.aniflow.domain.selection.model.SeederPolicy
import com.aniflow.domain.selection.model.SelectionContext
import com.aniflow.domain.selection.model.SelectionPolicyType
import com.aniflow.domain.selection.model.SelectionStrategy
import com.aniflow.domain.selection.model.SelectionWeights
import com.aniflow.domain.selection.model.SizePolicy
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectionEngineGoldenTest {

    private val engine = DefaultSelectionEngine()

    private val episode05 = Episode(
        id = EpisodeId("anime-s1-ep05"),
        animeId = AnimeId("anime-one-piece"),
        seasonId = null,
        number = EpisodeNumber(5),
        title = "Episode 05",
        type = EpisodeType.Main
    )

    private fun makeCandidate(
        id: String,
        resolution: Resolution,
        codec: VideoCodec,
        uploader: String,
        subtitles: List<SubtitleTrack>,
        sizeBytes: Long,
        seeders: Int
    ): ReleaseCandidate = ReleaseCandidate(
        episodeNumber = 5,
        release = NormalizedRelease(
            releaseId = ReleaseId(id),
            rawTitle = "[$uploader] Anime - 05 [$resolution][$codec]",
            normalizedTitle = "Anime - 05",
            animeCandidate = "Anime",
            seasonCandidate = 1,
            episodeRange = null,
            episodes = listOf(5),
            releaseType = ReleaseType.SingleEpisode,
            technicalMetadata = ReleaseTechnicalMetadata(
                resolution = resolution,
                videoCodec = codec,
                audioTracks = listOf(AudioTrack(LanguageCode.JAPANESE, null, null, null)),
                subtitles = subtitles,
                source = MediaSource.WebRip,
                bitDepth = null
            ),
            groupCandidate = uploader,
            uploader = uploader,
            source = MediaSource.WebRip,
            confidence = CompositeConfidence(overall = 0.95),
            rawMetadata = mapOf(
                "sizeBytes" to sizeBytes.toString(),
                "seeders" to seeders.toString()
            )
        ),
        confidence = 0.95
    )

    @Test
    fun primaryObjective_evaluatesEpisode05ReleasesCorrectly() {
        // --- 1. Candidate Set (Section 1) ---
        // Release A: 1080p, HEVC, Uploader XYZ, English Subs, 1.4 GB, 40 Seeds
        val candidateA = makeCandidate(
            id = "Release-A",
            resolution = Resolution.R1080p,
            codec = VideoCodec.HEVC,
            uploader = "XYZ",
            subtitles = listOf(SubtitleTrack(LanguageCode.ENGLISH, null, null)),
            sizeBytes = (1.4 * 1024 * 1024 * 1024).toLong(),
            seeders = 40
        )

        // Release B: 1080p, H264, Uploader ABC, English Subs, 2.4 GB, 80 Seeds
        val candidateB = makeCandidate(
            id = "Release-B",
            resolution = Resolution.R1080p,
            codec = VideoCodec.AVC,
            uploader = "ABC",
            subtitles = listOf(SubtitleTrack(LanguageCode.ENGLISH, null, null)),
            sizeBytes = (2.4 * 1024 * 1024 * 1024).toLong(),
            seeders = 80
        )

        // Release C: 720p, H264, Uploader XYZ, English Subs, 800 MB, 100 Seeds
        val candidateC = makeCandidate(
            id = "Release-C",
            resolution = Resolution.R720p,
            codec = VideoCodec.AVC,
            uploader = "XYZ",
            subtitles = listOf(SubtitleTrack(LanguageCode.ENGLISH, null, null)),
            sizeBytes = (800 * 1024 * 1024).toLong(),
            seeders = 100
        )

        // Release D: 1080p, AV1, Uploader XYZ, English Subs, 1.1 GB, 15 Seeds
        val candidateD = makeCandidate(
            id = "Release-D",
            resolution = Resolution.R1080p,
            codec = VideoCodec.AV1,
            uploader = "XYZ",
            subtitles = listOf(SubtitleTrack(LanguageCode.ENGLISH, null, null)),
            sizeBytes = (1.1 * 1024 * 1024 * 1024).toLong(),
            seeders = 15
        )

        // --- 2. User Profile Setup (Section 1) ---
        // Resolution: 1080p preferred
        // Codec: HEVC preferred (HEVC > AV1 > H264)
        // Uploader: XYZ preferred
        // Subtitle: English required
        // Max size: 2 GB
        // Minimum seeders: 5
        val preferences = UserSelectionPreferences(
            resolution = ResolutionPreference(
                mode = ResolutionMatchMode.Prefer,
                target = Resolution.R1080p,
                fallbackResolutions = listOf(Resolution.R720p)
            ),
            codec = CodecPreference(
                preferredCodecs = listOf(VideoCodec.HEVC, VideoCodec.AV1, VideoCodec.AVC),
                codecRanks = mapOf(
                    VideoCodec.HEVC to 100,
                    VideoCodec.AV1 to 80,
                    VideoCodec.AVC to 50
                )
            ),
            uploader = UploaderPreference(
                preferred = setOf("XYZ"),
                neutral = setOf("ABC")
            ),
            subtitles = SubtitlePolicy(
                requiredLanguages = setOf(LanguageCode.ENGLISH),
                preferredLanguages = setOf(LanguageCode.ENGLISH)
            ),
            sizePolicy = SizePolicy.ofGigabytes(hardMaxGb = 2.0, preferredMaxGb = 1.5),
            seederPolicy = SeederPolicy(minSeeders = 5, preferredSeeders = 20)
        )

        val profile = DownloadProfile(
            id = DownloadProfileId("profile-anime-1080p"),
            name = "Anime 1080p User Profile",
            hardConstraints = listOf(
                HardConstraint.RequireSubtitleLanguage(LanguageCode.ENGLISH),
                HardConstraint.MaxFileSize(ByteSize.fromGigabytes(2.0)),
                HardConstraint.MinSeeders(5)
            ),
            preferences = preferences,
            weights = SelectionWeights.Default,
            policy = SelectionPolicyType.BestCompatible,
            strategy = SelectionStrategy.Balanced
        )

        val context = SelectionContext(
            episode = episode05,
            candidates = listOf(candidateA, candidateB, candidateC, candidateD),
            profile = profile,
            preferences = preferences
        )

        // --- 3. Run Evaluation ---
        val result = engine.evaluate(context)

        // --- 4. Assert Expected Golden Outcomes ---
        // Selected must be Candidate A (Preferred)
        assertNotNull("System must select a candidate", result.selected)
        assertEquals("Candidate A should be selected", "Release-A", result.selected?.release?.id?.value)

        val evalA = result.rankedCandidates.first { it.candidate.release.id.value == "Release-A" }
        val evalB = result.rankedCandidates.first { it.candidate.release.id.value == "Release-B" }
        val evalC = result.rankedCandidates.first { it.candidate.release.id.value == "Release-C" }
        val evalD = result.rankedCandidates.first { it.candidate.release.id.value == "Release-D" }

        // Candidate A: Preferred
        assertEquals(CandidateClassification.Preferred, evalA.classification)

        // Candidate D: Strong alternative / Compatible (Tier 1 resolution & uploader, AV1 codec)
        assertTrue(
            "Candidate D should be Compatible or Alternative",
            evalD.classification == CandidateClassification.Compatible || evalD.classification == CandidateClassification.Preferred
        )

        // Candidate B: Rejected / Ineligible because size (2.4 GB) exceeds hard limit (2.0 GB)
        assertEquals(CandidateClassification.Ineligible, evalB.classification)
        assertFalse(evalB.eligibility.eligible)

        // Candidate C: Eligible fallback (720p fallback tier)
        assertTrue(evalC.eligibility.eligible)
        assertEquals(4, evalC.fallbackTier) // Tier 4 due to 720p fallback

        // --- 5. Verify User-Facing Transparent Explanation ---
        val reasons = result.explanation.positiveReasons.map { it.description }
        println("Selection reasons:")
        reasons.forEach { println(it) }

        assertTrue(reasons.any { it.contains("Preferred uploader: XYZ") })
        assertTrue(reasons.any { it.contains("1080p") })
        assertTrue(reasons.any { it.contains("HEVC") })
        assertTrue(reasons.any { it.contains("English") })
        assertTrue(reasons.any { it.contains("seeds") })
    }
}
