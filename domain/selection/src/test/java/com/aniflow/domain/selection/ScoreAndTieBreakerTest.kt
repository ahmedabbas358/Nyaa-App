package com.aniflow.domain.selection

import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.intelligence.model.CompositeConfidence
import com.aniflow.domain.intelligence.model.NormalizedRelease
import com.aniflow.domain.intelligence.model.ReleaseCandidate
import com.aniflow.domain.model.aggregate.release.ReleaseType
import com.aniflow.domain.selection.model.CodecPreference
import com.aniflow.domain.selection.model.SelectionWeights
import com.aniflow.domain.selection.model.UploaderPreference
import com.aniflow.domain.selection.model.UserSelectionPreferences
import com.aniflow.domain.selection.scoring.ScoreEngine
import com.aniflow.domain.selection.scoring.TieBreaker
import com.aniflow.domain.valueobject.AudioTrack
import com.aniflow.domain.valueobject.LanguageCode
import com.aniflow.domain.valueobject.MediaSource
import com.aniflow.domain.valueobject.ReleaseTechnicalMetadata
import com.aniflow.domain.valueobject.Resolution
import com.aniflow.domain.valueobject.SubtitleTrack
import com.aniflow.domain.valueobject.VideoCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoreAndTieBreakerTest {

    private val scoreEngine = ScoreEngine()
    private val tieBreaker = TieBreaker()

    private fun candidate(
        id: String,
        uploader: String,
        codec: VideoCodec,
        resolution: Resolution = Resolution.R1080p,
        sizeBytes: Long = 1_400_000_000L,
        seeders: Int = 40
    ): ReleaseCandidate = ReleaseCandidate(
        episodeNumber = 1,
        release = NormalizedRelease(
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
                subtitles = listOf(SubtitleTrack(LanguageCode.ENGLISH, null, null)),
                source = MediaSource.WebRip,
                bitDepth = null
            ),
            groupCandidate = "SubGroup",
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
    fun preferredCodec_scoresHigherThanAlternative() {
        val hevcCandidate = candidate(id = "hevc", uploader = "Neutral", codec = VideoCodec.HEVC)
        val h264Candidate = candidate(id = "h264", uploader = "Neutral", codec = VideoCodec.AVC)

        val prefs = UserSelectionPreferences(
            codec = CodecPreference(
                codecRanks = mapOf(VideoCodec.HEVC to 100, VideoCodec.AVC to 50)
            )
        )
        val weights = SelectionWeights.Default

        val scoreHevc = scoreEngine.score(hevcCandidate, prefs, weights)
        val scoreH264 = scoreEngine.score(h264Candidate, prefs, weights)

        assertTrue(
            "HEVC candidate score (${scoreHevc.total}) should be higher than H264 (${scoreH264.total})",
            scoreHevc.total > scoreH264.total
        )
    }

    @Test
    fun preferredUploader_scoresHigherThanNeutral() {
        val preferredUploaderCand = candidate(id = "pref", uploader = "Erai-raws", codec = VideoCodec.HEVC)
        val neutralUploaderCand = candidate(id = "neutral", uploader = "UnknownUploader", codec = VideoCodec.HEVC)

        val prefs = UserSelectionPreferences(
            uploader = UploaderPreference(preferred = setOf("Erai-raws"))
        )
        val weights = SelectionWeights.Default

        val scorePref = scoreEngine.score(preferredUploaderCand, prefs, weights)
        val scoreNeut = scoreEngine.score(neutralUploaderCand, prefs, weights)

        assertTrue(
            "Preferred uploader (${scorePref.total}) should outscore neutral uploader (${scoreNeut.total})",
            scorePref.total > scoreNeut.total
        )
    }

    @Test
    fun tieBreaker_isDeterministicOver100Runs() {
        val candA = candidate(id = "candA", uploader = "SameUploader", codec = VideoCodec.HEVC, seeders = 50, sizeBytes = 1_000_000_000L)
        val candB = candidate(id = "candB", uploader = "SameUploader", codec = VideoCodec.HEVC, seeders = 50, sizeBytes = 1_000_000_000L)

        val prefs = UserSelectionPreferences()

        val firstResult = tieBreaker.breakTie(candA, candB, prefs)

        // 100 consecutive executions must produce the exact same outcome
        for (i in 1..100) {
            val result = tieBreaker.breakTie(candA, candB, prefs)
            assertEquals("Tie break must be completely deterministic", firstResult, result)
        }
    }
}
