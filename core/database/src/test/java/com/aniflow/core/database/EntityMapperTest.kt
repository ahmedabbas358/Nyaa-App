package com.aniflow.core.database

import com.aniflow.core.database.entity.AnimeEntity
import com.aniflow.core.database.entity.DownloadFileEntity
import com.aniflow.core.database.entity.DownloadTaskEntity
import com.aniflow.core.database.entity.EpisodeEntity
import com.aniflow.core.database.entity.LibraryFileEntity
import com.aniflow.core.database.entity.LibraryItemEntity
import com.aniflow.core.database.entity.ReleaseEntity
import com.aniflow.core.database.mapper.EntityMappers.toDomain
import com.aniflow.core.database.mapper.EntityMappers.toEntity
import com.aniflow.domain.identity.AnimeId
import com.aniflow.domain.identity.DownloadTaskId
import com.aniflow.domain.identity.EpisodeId
import com.aniflow.domain.identity.LibraryItemId
import com.aniflow.domain.identity.MediaIdentity
import com.aniflow.domain.identity.ProviderId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.identity.ReleaseIdentity
import com.aniflow.domain.model.aggregate.download.DownloadPriority
import com.aniflow.domain.model.aggregate.download.DownloadSource
import com.aniflow.domain.model.aggregate.download.DownloadTask
import com.aniflow.domain.model.aggregate.library.LibraryItem
import com.aniflow.domain.model.aggregate.media.Anime
import com.aniflow.domain.model.aggregate.media.AnimeStatus
import com.aniflow.domain.model.aggregate.media.AnimeType
import com.aniflow.domain.model.aggregate.media.Episode
import com.aniflow.domain.model.aggregate.media.EpisodeType
import com.aniflow.domain.model.aggregate.release.ProviderRef
import com.aniflow.domain.model.aggregate.release.Release
import com.aniflow.domain.model.aggregate.release.ReleaseSource
import com.aniflow.domain.model.aggregate.release.ReleaseType
import com.aniflow.domain.state.DownloadState
import com.aniflow.domain.state.LibraryItemState
import com.aniflow.domain.valueobject.ByteSize
import com.aniflow.domain.valueobject.EpisodeNumber
import com.aniflow.domain.valueobject.EpisodeRange
import com.aniflow.domain.valueobject.InfoHash
import com.aniflow.domain.valueobject.Resolution
import com.aniflow.domain.valueobject.SeasonNumber
import com.aniflow.domain.valueobject.StorageTarget
import com.aniflow.domain.valueobject.UrlValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * Validates lossless conversion between Room Entities and Domain Models (Section 129 & 130).
 */
class EntityMapperTest {

    @Test
    fun testAnimeEntityMapping() {
        val domainAnime = Anime(
            id = AnimeId("anime_1"),
            canonicalTitle = "Attack on Titan",
            alternateTitles = listOf("Shingeki no Kyojin", "AOT"),
            normalizedTitle = "attack on titan",
            type = AnimeType.Series,
            status = AnimeStatus.Completed,
            metadataSource = ProviderRef(ProviderId("nyaa"), "Nyaa")
        )

        val entity = domainAnime.toEntity()
        assertEquals("anime_1", entity.id)
        assertEquals("Attack on Titan", entity.canonicalTitle)
        assertEquals("attack on titan", entity.normalizedTitle)
        assertEquals("Series", entity.type)

        val reconstructed = entity.toDomain(listOf("Shingeki no Kyojin", "AOT"))
        assertEquals(domainAnime.id, reconstructed.id)
        assertEquals(domainAnime.canonicalTitle, reconstructed.canonicalTitle)
        assertEquals(domainAnime.type, reconstructed.type)
        assertEquals(domainAnime.status, reconstructed.status)
        assertEquals(2, reconstructed.alternateTitles.size)
    }

    @Test
    fun testEpisodeEntityMapping() {
        val domainEpisode = Episode(
            id = EpisodeId("ep_12"),
            animeId = AnimeId("anime_1"),
            seasonId = null,
            number = EpisodeNumber(major = 12, minor = 5),
            title = "Special Recap 12.5",
            type = EpisodeType.Recap
        )

        val entity = domainEpisode.toEntity()
        assertEquals("ep_12", entity.id)
        assertEquals(12, entity.majorNumber)
        assertEquals(5, entity.minorNumber)
        assertEquals("12.5", entity.displayNumber)

        val reconstructed = entity.toDomain()
        assertEquals(domainEpisode.id, reconstructed.id)
        assertEquals(12, reconstructed.number.major)
        assertEquals(5, reconstructed.number.minor)
        assertEquals(EpisodeType.Recap, reconstructed.type)
    }

    @Test
    fun testDownloadTaskEntityMapping() {
        val hash = InfoHash("0123456789abcdef0123456789abcdef01234567")
        val domainTask = DownloadTask(
            id = DownloadTaskId("task_42"),
            releaseId = ReleaseId("rel_10"),
            source = DownloadSource.TorrentSource(
                infoHash = hash,
                magnetUri = UrlValue.MagnetUri("magnet:?xt=urn:btih:0123456789abcdef0123456789abcdef01234567&dn=Anime"),
                name = "Anime Episode 01.mkv"
            ),
            state = DownloadState.Downloading,
            priority = DownloadPriority.High,
            destination = StorageTarget("sdcard_storage", "External SD Card"),
            createdAt = Instant.ofEpochMilli(1700000000000L),
            updatedAt = Instant.ofEpochMilli(1700000500000L)
        )

        val entity = domainTask.toEntity(planId = "plan_1")
        assertEquals("task_42", entity.id)
        assertEquals("plan_1", entity.planId)
        assertEquals("rel_10", entity.releaseId)
        assertEquals("Torrent", entity.sourceType)
        assertEquals(4, entity.priority)
        assertEquals("Downloading", entity.state)

        val reconstructed = entity.toDomain()
        assertEquals(domainTask.id, reconstructed.id)
        assertEquals(DownloadState.Downloading, reconstructed.state)
        assertEquals(DownloadPriority.High, reconstructed.priority)
        assertTrue(reconstructed.source is DownloadSource.TorrentSource)
        assertEquals(hash, (reconstructed.source as DownloadSource.TorrentSource).infoHash)
    }

    @Test
    fun testLibraryItemEntityMapping() {
        val domainItem = LibraryItem(
            id = LibraryItemId("lib_1"),
            mediaIdentity = MediaIdentity(
                animeId = AnimeId("anime_1"),
                seasonNumber = SeasonNumber.of(1),
                episodeRange = EpisodeRange.ofSingle(1),
                canonicalTitle = "Frieren Episode 1"
            ),
            state = LibraryItemState.Available,
            location = StorageTarget.DEFAULT
        )

        val entity = domainItem.toEntity()
        assertEquals("lib_1", entity.id)
        assertEquals("anime_1", entity.animeId)
        assertEquals("Available", entity.state)

        val reconstructed = entity.toDomain()
        assertEquals(domainItem.id, reconstructed.id)
        assertEquals(LibraryItemState.Available, reconstructed.state)
        assertEquals("Frieren Episode 1", reconstructed.mediaIdentity.canonicalTitle)
    }
}
