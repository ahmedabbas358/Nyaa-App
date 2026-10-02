package com.aniflow.data.mapper

import com.aniflow.domain.identity.ProviderId
import com.aniflow.domain.identity.ReleaseGroupId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.identity.ReleaseIdentity
import com.aniflow.domain.identity.UploaderId
import com.aniflow.domain.model.aggregate.release.ProviderRef
import com.aniflow.domain.model.aggregate.release.Release
import com.aniflow.domain.model.aggregate.release.ReleaseAvailability
import com.aniflow.domain.model.aggregate.release.ReleaseGroup
import com.aniflow.domain.model.aggregate.release.ReleaseSource
import com.aniflow.domain.model.aggregate.release.ReleaseType
import com.aniflow.domain.model.aggregate.release.Uploader
import com.aniflow.domain.valueobject.ByteSize
import com.aniflow.domain.valueobject.InfoHash
import com.aniflow.domain.valueobject.ParseInfo
import com.aniflow.domain.valueobject.ReleaseTechnicalMetadata
import com.aniflow.domain.valueobject.UrlValue
import com.aniflow.provider.core.model.ProviderRelease
import java.time.Instant
import java.util.UUID

/**
 * Maps Provider transport model (ProviderRelease) to clean Domain Release entity (Section 48, 49, 129).
 */
object ProviderReleaseMapper {

    fun toDomainRelease(
        providerRelease: ProviderRelease,
        providerId: ProviderId = ProviderId("nyaa"),
        providerName: String = "Nyaa.si"
    ): Release {
        val infoHash = providerRelease.magnetUri?.let { uri ->
            val match = Regex("""xt=urn:btih:([a-fA-F0-9]{40}|[a-zA-Z2-7]{32})""", RegexOption.IGNORE_CASE).find(uri)
            match?.groupValues?.get(1)?.lowercase()?.let { InfoHash(it) }
        }

        val releaseId = ReleaseId(
            if (infoHash != null) "rel_${infoHash.hexString.take(16)}"
            else "rel_${providerId.value}_${providerRelease.providerReleaseId ?: UUID.randomUUID().toString().take(8)}"
        )

        val releaseSource = when {
            infoHash != null -> ReleaseSource.Torrent(
                infoHash = infoHash,
                torrentUrl = providerRelease.torrentUrl?.let { UrlValue.TorrentUrl(it.rawValue) },
                magnetUri = providerRelease.magnetUri?.let { UrlValue.MagnetUri(it) }
            )
            providerRelease.torrentUrl != null -> ReleaseSource.Torrent(
                infoHash = InfoHash("0000000000000000000000000000000000000000"),
                torrentUrl = UrlValue.TorrentUrl(providerRelease.torrentUrl.rawValue),
                magnetUri = null
            )
            else -> ReleaseSource.Unknown
        }

        val uploader = providerRelease.uploader?.let {
            Uploader(
                id = UploaderId(it.displayName.lowercase().replace(" ", "_")),
                name = it.displayName,
                providerId = providerId
            )
        }

        val releaseGroup = providerRelease.releaseGroup?.let {
            ReleaseGroup(
                id = ReleaseGroupId(it.lowercase().replace(" ", "_")),
                name = it,
                providerId = providerId
            )
        }

        val availability = ReleaseAvailability(
            size = providerRelease.sizeBytes?.let { ByteSize.fromBytes(it) },
            seeders = providerRelease.seeders,
            leechers = providerRelease.leechers,
            completedDownloads = providerRelease.downloads,
            lastCheckedAt = Instant.now()
        )

        val identity = when {
            infoHash != null -> ReleaseIdentity.TorrentHash(infoHash)
            providerRelease.providerReleaseId != null -> ReleaseIdentity.ProviderSpecific(
                providerId = providerId,
                externalId = providerRelease.providerReleaseId
            )
            else -> ReleaseIdentity.Custom(releaseId.value)
        }

        return Release(
            id = releaseId,
            provider = ProviderRef(id = providerId, name = providerName),
            providerReleaseId = providerRelease.providerReleaseId,
            title = providerRelease.title,
            normalizedTitle = providerRelease.title.trim(),
            releaseType = ReleaseType.SingleEpisode,
            animeIdentity = null,
            seasonHint = null,
            episodeRange = null,
            uploader = uploader,
            releaseGroup = releaseGroup,
            technical = ReleaseTechnicalMetadata(
                resolution = null,
                videoCodec = null,
                audioTracks = emptyList(),
                subtitles = emptyList(),
                source = null,
                bitDepth = null
            ),
            availability = availability,
            source = releaseSource,
            publishedAt = providerRelease.publishedAt,
            identity = identity,
            parseInfo = ParseInfo.perfect(),
            discoveredAt = Instant.now(),
            lastUpdatedAt = Instant.now()
        )
    }
}
