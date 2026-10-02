package com.aniflow.domain

import com.aniflow.domain.identity.AnimeIdentity
import com.aniflow.domain.identity.ProviderId
import com.aniflow.domain.identity.ReleaseGroupId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.identity.ReleaseIdentity
import com.aniflow.domain.identity.UploaderId
import com.aniflow.domain.model.aggregate.download.GroupingMode
import com.aniflow.domain.model.aggregate.release.ProviderRef
import com.aniflow.domain.model.aggregate.release.Release
import com.aniflow.domain.model.aggregate.release.ReleaseGroup
import com.aniflow.domain.model.aggregate.release.ReleaseType
import com.aniflow.domain.model.aggregate.release.Uploader
import com.aniflow.domain.service.ReleaseGroupingService
import com.aniflow.domain.service.ReleaseNormalizationService
import com.aniflow.domain.state.ParseState
import com.aniflow.domain.valueobject.EpisodeRange
import com.aniflow.domain.valueobject.ParseInfo
import com.aniflow.domain.valueobject.SeasonNumber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseGroupingTest {

    private val provider = ProviderRef(ProviderId("nyaa"), "Nyaa")

    private fun createRelease(
        id: String,
        title: String,
        uploaderName: String = "SubsPlease",
        groupName: String = "SubsPlease",
        season: Int = 1,
        episode: Int = 1,
        isAmbiguous: Boolean = false
    ): Release {
        val normalized = ReleaseNormalizationService.normalizeTitle(title)
        val animeIdent = AnimeIdentity(
            normalizedTitle = normalized,
            rawTitle = title
        )
        val releaseIdent = ReleaseIdentity(
            providerId = provider.providerId,
            providerReleaseId = id,
            infoHash = null,
            canonicalSourceUrl = null,
            normalizedTitle = normalized,
            episodeRange = EpisodeRange.ofSingle(episode),
            technicalSignature = "1080p-HEVC"
        )
        return Release(
            id = ReleaseId(id),
            provider = provider,
            providerReleaseId = id,
            title = title,
            normalizedTitle = normalized,
            releaseType = ReleaseType.SingleEpisode,
            animeIdentity = animeIdent,
            seasonHint = SeasonNumber.of(season),
            episodeRange = EpisodeRange.ofSingle(episode),
            uploader = Uploader(UploaderId(uploaderName.lowercase()), provider, uploaderName, uploaderName.lowercase()),
            releaseGroup = ReleaseGroup(ReleaseGroupId(groupName.lowercase()), provider, groupName, groupName.lowercase()),
            identity = releaseIdent,
            parseInfo = if (isAmbiguous) ParseInfo.ambiguous("1.0", "Ambiguous title") else ParseInfo.perfect()
        )
    }

    @Test
    fun `Groups releases by anime and season`() {
        val r1 = createRelease("1", "One Piece - 1089", season = 1, episode = 1089)
        val r2 = createRelease("2", "One Piece - 1090", season = 1, episode = 1090)
        val r3 = createRelease("3", "Frieren - 01", season = 1, episode = 1)
        val r4 = createRelease("4", "Frieren - 02", season = 2, episode = 1)

        val result = ReleaseGroupingService.group(listOf(r1, r2, r3, r4), GroupingMode.AnimeSeason)

        assertEquals(3, result.groups.size)
        assertTrue(result.groups.keys.any { it.contains("one piece", ignoreCase = true) })
        assertTrue(result.groups.keys.any { it.contains("frieren", ignoreCase = true) && it.contains("Season 1") })
        assertTrue(result.groups.keys.any { it.contains("frieren", ignoreCase = true) && it.contains("Season 2") })
    }

    @Test
    fun `Groups releases by uploader`() {
        val r1 = createRelease("1", "Spy x Family - 01", uploaderName = "SubsPlease")
        val r2 = createRelease("2", "Spy x Family - 01", uploaderName = "Erai-raws")

        val result = ReleaseGroupingService.group(listOf(r1, r2), GroupingMode.AnimeUploader)

        assertEquals(2, result.groups.size)
        assertTrue(result.groups.keys.any { it.contains("SubsPlease") })
        assertTrue(result.groups.keys.any { it.contains("Erai-raws") })
    }

    @Test
    fun `Flags ambiguous releases without forcing destructive merge`() {
        val r1 = createRelease("1", "Ambiguous Anime - 01", isAmbiguous = true)
        val r2 = createRelease("2", "Clear Anime - 01", isAmbiguous = false)

        val result = ReleaseGroupingService.group(listOf(r1, r2))

        assertEquals(1, result.ambiguousReleases.size)
        assertEquals("1", result.ambiguousReleases.first().id.value)
    }
}
