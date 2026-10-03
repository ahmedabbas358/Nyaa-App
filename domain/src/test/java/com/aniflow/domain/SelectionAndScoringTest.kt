package com.aniflow.domain

import com.aniflow.domain.identity.DownloadProfileId
import com.aniflow.domain.identity.ProviderId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.identity.ReleaseIdentity
import com.aniflow.domain.identity.RuleId
import com.aniflow.domain.identity.UploaderId
import com.aniflow.domain.model.aggregate.organization.DownloadPreferences
import com.aniflow.domain.model.aggregate.organization.DownloadProfile
import com.aniflow.domain.model.aggregate.organization.PreferenceRequirement
import com.aniflow.domain.model.aggregate.organization.Rule
import com.aniflow.domain.model.aggregate.organization.RuleAction
import com.aniflow.domain.model.aggregate.organization.RuleCondition
import com.aniflow.domain.model.aggregate.release.ProviderRef
import com.aniflow.domain.model.aggregate.release.Release
import com.aniflow.domain.model.aggregate.release.ReleaseAvailability
import com.aniflow.domain.valueobject.ReleaseTechnicalMetadata
import com.aniflow.domain.model.aggregate.release.Uploader
import com.aniflow.domain.service.SelectionService
import com.aniflow.domain.valueobject.ByteSize
import com.aniflow.domain.valueobject.EpisodeRange
import com.aniflow.domain.valueobject.Resolution
import com.aniflow.domain.valueobject.SelectionState
import com.aniflow.domain.valueobject.VideoCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectionAndScoringTest {

    private val provider = ProviderRef(ProviderId("nyaa"), "Nyaa")

    private fun createSampleRelease(
        resolution: Resolution = Resolution.R1080p,
        codec: VideoCodec = VideoCodec.HEVC,
        seeders: Int = 10,
        size: ByteSize = ByteSize.ofGigabytes(1.2),
        uploaderName: String = "SubsPlease"
    ): Release {
        val uploader = Uploader(UploaderId(uploaderName.lowercase()), provider, uploaderName, uploaderName.lowercase())
        val ident = ReleaseIdentity(
            providerId = provider.providerId,
            providerReleaseId = "123",
            infoHash = null,
            canonicalSourceUrl = null,
            normalizedTitle = "sample anime",
            episodeRange = EpisodeRange.ofSingle(1),
            technicalSignature = "${resolution.displayName}-${codec.displayName}"
        )
        return Release(
            id = ReleaseId("rel_123"),
            provider = provider,
            providerReleaseId = "123",
            title = "Sample Anime - 01",
            normalizedTitle = "sample anime",
            uploader = uploader,
            technical = ReleaseTechnicalMetadata(
                resolution = resolution,
                videoCodec = codec,
                source = null,
                bitDepth = null
            ),
            availability = ReleaseAvailability(
                size = size,
                seeders = seeders
            ),
            identity = ident
        )
    }

    @Test
    fun `Scores release higher when matching preferred quality`() {
        val profile = DownloadProfile(
            id = DownloadProfileId("prof_default"),
            name = "Default",
            preferences = DownloadPreferences(
                resolution = PreferenceRequirement.Preferred(Resolution.R1080p),
                codec = PreferenceRequirement.Preferred(VideoCodec.HEVC)
            )
        )

        val matchingRelease = createSampleRelease(resolution = Resolution.R1080p, codec = VideoCodec.HEVC)
        val lowerRelease = createSampleRelease(resolution = Resolution.R720p, codec = VideoCodec.AVC)

        val result1 = SelectionService.evaluateRelease(matchingRelease, profile)
        val result2 = SelectionService.evaluateRelease(lowerRelease, profile)

        assertTrue(result1.score.totalScore > result2.score.totalScore)
        assertTrue(result1.state == SelectionState.Preferred || result1.state == SelectionState.Selected)
        assertTrue(result1.explanation.positiveReasons.any { it.code == "PREF_RES_MATCH" })
    }

    @Test
    fun `Applies severe penalty or rejection for duplicates`() {
        val profile = DownloadProfile(
            id = DownloadProfileId("prof_default"),
            name = "Default"
        )
        val release = createSampleRelease()

        val result = SelectionService.evaluateRelease(release, profile, isDuplicate = true)

        assertEquals(SelectionState.Rejected, result.state)
        assertTrue(result.score.duplicatePenalty > 0)
        assertTrue(result.explanation.negativeReasons.any { it.code == "DUPLICATE_ITEM" })
    }

    @Test
    fun `Applies rule actions to priority and explanation`() {
        val profile = DownloadProfile(id = DownloadProfileId("prof"), name = "P")
        val release = createSampleRelease(resolution = Resolution.R1080p)

        val bonusRule = Rule(
            id = RuleId("rule_1"),
            name = "Bonus For 1080p",
            conditions = listOf(RuleCondition.ResolutionIs(Resolution.R1080p)),
            actions = listOf(RuleAction.Prefer(scoreBonus = 25)),
            priority = 10
        )

        val result = SelectionService.evaluateRelease(release, profile, rules = listOf(bonusRule))

        assertTrue(result.explanation.positiveReasons.any { it.code == "RULE_PREFER" })
    }
}
