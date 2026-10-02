package com.aniflow.core.database.mapper

import com.aniflow.domain.identity.AnimeId
import com.aniflow.domain.identity.AnimeIdentity
import com.aniflow.domain.identity.CollectionId
import com.aniflow.domain.identity.DownloadFileId
import com.aniflow.domain.identity.DownloadPlanId
import com.aniflow.domain.identity.DownloadProfileId
import com.aniflow.domain.identity.DownloadTaskId
import com.aniflow.domain.identity.EpisodeId
import com.aniflow.domain.identity.FileFingerprint
import com.aniflow.domain.identity.LibraryFileId
import com.aniflow.domain.identity.LibraryItemId
import com.aniflow.domain.identity.MediaIdentity
import com.aniflow.domain.identity.ProviderId
import com.aniflow.domain.identity.ReleaseGroupId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.identity.ReleaseIdentity
import com.aniflow.domain.identity.RuleId
import com.aniflow.domain.identity.SavedSearchId
import com.aniflow.domain.identity.SeasonId
import com.aniflow.domain.identity.UploaderId
import com.aniflow.domain.model.aggregate.download.DownloadFile
import com.aniflow.domain.model.aggregate.download.DownloadFileState
import com.aniflow.domain.model.aggregate.download.DownloadPlan
import com.aniflow.domain.model.aggregate.download.DownloadPriority
import com.aniflow.domain.model.aggregate.download.DownloadSource
import com.aniflow.domain.model.aggregate.download.DownloadTask
import com.aniflow.domain.model.aggregate.library.LibraryFile
import com.aniflow.domain.model.aggregate.library.LibraryItem
import com.aniflow.domain.model.aggregate.media.Anime
import com.aniflow.domain.model.aggregate.media.AnimeStatus
import com.aniflow.domain.model.aggregate.media.AnimeType
import com.aniflow.domain.model.aggregate.media.Episode
import com.aniflow.domain.model.aggregate.media.EpisodeType
import com.aniflow.domain.model.aggregate.media.Season
import com.aniflow.domain.model.aggregate.organization.Collection
import com.aniflow.domain.model.aggregate.organization.CollectionSource
import com.aniflow.domain.model.aggregate.organization.CollectionType
import com.aniflow.domain.model.aggregate.organization.DownloadProfile
import com.aniflow.domain.model.aggregate.organization.Rule
import com.aniflow.domain.model.aggregate.organization.RuleScope
import com.aniflow.domain.model.aggregate.organization.SavedSearch
import com.aniflow.domain.model.aggregate.release.ProviderRef
import com.aniflow.domain.model.aggregate.release.Release
import com.aniflow.domain.model.aggregate.release.ReleaseAvailability
import com.aniflow.domain.model.aggregate.release.ReleaseGroup
import com.aniflow.domain.model.aggregate.release.ReleaseSource
import com.aniflow.domain.model.aggregate.release.ReleaseTechnicalMetadata
import com.aniflow.domain.model.aggregate.release.ReleaseType
import com.aniflow.domain.model.aggregate.release.Uploader
import com.aniflow.domain.state.DownloadState
import com.aniflow.domain.state.LibraryItemState
import com.aniflow.domain.state.ParseState
import com.aniflow.domain.valueobject.ByteSize
import com.aniflow.domain.valueobject.EpisodeNumber
import com.aniflow.domain.valueobject.EpisodeRange
import com.aniflow.domain.valueobject.InfoHash
import com.aniflow.domain.valueobject.ParseInfo
import com.aniflow.domain.valueobject.Resolution
import com.aniflow.domain.valueobject.SearchQuery
import com.aniflow.domain.valueobject.SeasonNumber
import com.aniflow.domain.valueobject.StorageTarget
import com.aniflow.domain.valueobject.UrlValue
import com.aniflow.domain.valueobject.VideoCodec
import com.aniflow.core.database.entity.AnimeEntity
import com.aniflow.core.database.entity.CollectionEntity
import com.aniflow.core.database.entity.DownloadFileEntity
import com.aniflow.core.database.entity.DownloadPlanEntity
import com.aniflow.core.database.entity.DownloadProfileEntity
import com.aniflow.core.database.entity.DownloadTaskEntity
import com.aniflow.core.database.entity.EpisodeEntity
import com.aniflow.core.database.entity.LibraryFileEntity
import com.aniflow.core.database.entity.LibraryItemEntity
import com.aniflow.core.database.entity.ReleaseEntity
import com.aniflow.core.database.entity.ReleaseGroupEntity
import com.aniflow.core.database.entity.RuleEntity
import com.aniflow.core.database.entity.SavedSearchEntity
import com.aniflow.core.database.entity.SeasonEntity
import com.aniflow.core.database.entity.UploaderEntity
import java.time.Instant

/**
 * Bi-directional Mappers converting between Room Entities and Domain Models.
 * Strictly guarantees that Room never leaks into Domain, and Domain never dictates relational constraints.
 */
object EntityMappers {

    // --- Anime Mappers ---
    fun AnimeEntity.toDomain(titles: List<String> = emptyList()): Anime = Anime(
        id = AnimeId(id),
        canonicalTitle = canonicalTitle,
        alternateTitles = titles,
        normalizedTitle = normalizedTitle,
        type = try { AnimeType.valueOf(type) } catch (_: Exception) { AnimeType.Series },
        status = try { AnimeStatus.valueOf(status) } catch (_: Exception) { AnimeStatus.Unknown },
        metadataSource = primaryProviderId?.let { ProviderRef(ProviderId(it), it) }
    )

    fun Anime.toEntity(): AnimeEntity = AnimeEntity(
        id = id.value,
        canonicalTitle = canonicalTitle,
        normalizedTitle = normalizedTitle,
        type = type.name,
        status = status.name,
        primaryProviderId = metadataSource?.providerId?.value
    )

    // --- Season Mappers ---
    fun SeasonEntity.toDomain(): Season = Season(
        id = SeasonId(id),
        animeId = AnimeId(animeId),
        number = SeasonNumber.of(number),
        title = title
    )

    fun Season.toEntity(): SeasonEntity = SeasonEntity(
        id = id.value,
        animeId = animeId.value,
        number = number.numericValue ?: 1,
        title = title
    )

    // --- Episode Mappers ---
    fun EpisodeEntity.toDomain(): Episode = Episode(
        id = EpisodeId(id),
        animeId = AnimeId(animeId),
        seasonId = seasonId?.let { SeasonId(it) },
        number = EpisodeNumber(major = majorNumber, minor = minorNumber),
        title = title,
        type = try { EpisodeType.valueOf(type) } catch (_: Exception) { EpisodeType.Main }
    )

    fun Episode.toEntity(): EpisodeEntity = EpisodeEntity(
        id = id.value,
        animeId = animeId.value,
        seasonId = seasonId?.value,
        episodeKey = number.displayString,
        displayNumber = number.displayString,
        majorNumber = number.major,
        minorNumber = number.minor,
        title = title,
        type = type.name
    )

    // --- Uploader & Group Mappers ---
    fun UploaderEntity.toDomain(provider: ProviderRef): Uploader = Uploader(
        id = UploaderId(id),
        provider = provider,
        name = name,
        normalizedName = normalizedName
    )

    fun Uploader.toEntity(): UploaderEntity = UploaderEntity(
        id = id.value,
        providerId = provider.providerId.value,
        providerUploaderId = id.value,
        name = name,
        normalizedName = normalizedName
    )

    fun ReleaseGroupEntity.toDomain(provider: ProviderRef): ReleaseGroup = ReleaseGroup(
        id = ReleaseGroupId(id),
        provider = provider,
        name = name,
        normalizedName = normalizedName
    )

    fun ReleaseGroup.toEntity(): ReleaseGroupEntity = ReleaseGroupEntity(
        id = id.value,
        providerId = provider.providerId.value,
        providerGroupId = id.value,
        name = name,
        normalizedName = normalizedName
    )

    // --- Release Mappers ---
    fun ReleaseEntity.toDomain(
        uploader: Uploader? = null,
        group: ReleaseGroup? = null
    ): Release {
        val prov = ProviderRef(ProviderId(providerId), providerId)
        val src = when (sourceType) {
            "Torrent" -> ReleaseSource.Torrent(
                infoHash = torrentHash?.let { InfoHash(it) } ?: InfoHash("0000000000000000000000000000000000000000"),
                magnetUri = magnetUri?.let { UrlValue.MagnetUri(it) },
                torrentUrl = downloadUri?.let { UrlValue.TorrentUrl(it) }
            )
            "DirectHttp" -> downloadUri?.let { ReleaseSource.DirectHttp(UrlValue.HttpsUrl(it)) } ?: ReleaseSource.Unknown
            else -> ReleaseSource.Unknown
        }

        val epRange = EpisodeRange.parseOrNull(normalizedTitle)
        val identity = ReleaseIdentity(
            providerId = prov.providerId,
            providerReleaseId = providerReleaseId,
            infoHash = torrentHash?.let { InfoHash(it) },
            canonicalSourceUrl = downloadUri ?: magnetUri,
            normalizedTitle = normalizedTitle,
            episodeRange = epRange,
            technicalSignature = "$resolution-$videoCodec"
        )

        return Release(
            id = ReleaseId(id),
            provider = prov,
            providerReleaseId = providerReleaseId,
            title = title,
            normalizedTitle = normalizedTitle,
            releaseType = try { ReleaseType.valueOf(releaseType) } catch (_: Exception) { ReleaseType.SingleEpisode },
            animeIdentity = AnimeIdentity(normalizedTitle = normalizedTitle, rawTitle = title),
            seasonHint = seasonId?.let { SeasonNumber.of(1) },
            episodeRange = epRange,
            uploader = uploader,
            releaseGroup = group,
            technical = ReleaseTechnicalMetadata(
                resolution = Resolution.fromString(resolution),
                videoCodec = VideoCodec.fromString(videoCodec),
                source = null,
                bitDepth = null
            ),
            availability = ReleaseAvailability(
                size = ByteSize.ofBytes(sizeBytes),
                seeders = seeders,
                leechers = leechers,
                completedDownloads = completedDownloads,
                lastCheckedAt = lastUpdatedAt.let { Instant.ofEpochMilli(it) }
            ),
            source = src,
            publishedAt = publishedAt?.let { Instant.ofEpochMilli(it) },
            identity = identity,
            parseInfo = ParseInfo(
                parserVersion = parserVersion,
                confidence = parseConfidence,
                state = try { ParseState.valueOf(parseState) } catch (_: Exception) { ParseState.Parsed }
            ),
            discoveredAt = Instant.ofEpochMilli(discoveredAt),
            lastUpdatedAt = Instant.ofEpochMilli(lastUpdatedAt)
        )
    }

    fun Release.toEntity(): ReleaseEntity = ReleaseEntity(
        id = id.value,
        providerId = provider.providerId.value,
        providerReleaseId = providerReleaseId,
        title = title,
        normalizedTitle = normalizedTitle,
        releaseType = releaseType.name,
        animeId = animeIdentity?.canonicalId?.value,
        seasonId = seasonHint?.numericValue?.toString(),
        uploaderId = uploader?.id?.value,
        releaseGroupId = releaseGroup?.id?.value,
        resolution = technical.resolution?.displayName,
        videoCodec = technical.videoCodec?.displayName,
        sourceType = when (source) {
            is ReleaseSource.Torrent -> "Torrent"
            is ReleaseSource.DirectHttp -> "DirectHttp"
            is ReleaseSource.HttpFile -> "HttpFile"
            is ReleaseSource.Unknown -> "Unknown"
        },
        sizeBytes = availability.size?.bytes ?: 0L,
        seeders = availability.seeders ?: 0,
        leechers = availability.leechers ?: 0,
        completedDownloads = availability.completedDownloads ?: 0L,
        publishedAt = publishedAt?.toEpochMilli(),
        discoveredAt = discoveredAt.toEpochMilli(),
        lastUpdatedAt = lastUpdatedAt.toEpochMilli(),
        torrentHash = (source as? ReleaseSource.Torrent)?.infoHash?.hexString,
        magnetUri = (source as? ReleaseSource.Torrent)?.magnetUri?.rawValue,
        downloadUri = (source as? ReleaseSource.Torrent)?.torrentUrl?.rawValue,
        trusted = false,
        remake = false,
        parserVersion = parseInfo.parserVersion,
        parseConfidence = parseInfo.confidence,
        parseState = parseInfo.state.name
    )

    // --- DownloadTask Mappers ---
    fun DownloadTaskEntity.toDomain(): DownloadTask {
        val src = when (sourceType) {
            "Torrent" -> DownloadSource.TorrentSource(
                infoHash = torrentHash?.let { InfoHash(it) } ?: InfoHash("0000000000000000000000000000000000000000"),
                magnetUri = if (sourceUri.startsWith("magnet:?")) UrlValue.MagnetUri(sourceUri) else null,
                name = destinationRelativePath
            )
            "Http" -> DownloadSource.HttpSource(
                url = UrlValue.HttpsUrl(sourceUri),
                fileName = destinationRelativePath
            )
            else -> DownloadSource.DirectSource(
                url = UrlValue.HttpsUrl(sourceUri),
                fileName = destinationRelativePath
            )
        }

        return DownloadTask(
            id = DownloadTaskId(id),
            releaseId = releaseId?.let { ReleaseId(it) },
            source = src,
            state = try { DownloadState.valueOf(state) } catch (_: Exception) { DownloadState.Pending },
            priority = when (priority) {
                5 -> DownloadPriority.Highest
                4 -> DownloadPriority.High
                2 -> DownloadPriority.Low
                1 -> DownloadPriority.Lowest
                else -> DownloadPriority.Normal
            },
            destination = StorageTarget(destinationId, destinationId),
            createdAt = Instant.ofEpochMilli(createdAt),
            updatedAt = Instant.ofEpochMilli(updatedAt),
            startedAt = startedAt?.let { Instant.ofEpochMilli(it) },
            completedAt = completedAt?.let { Instant.ofEpochMilli(it) },
            errorMessage = errorMessage
        )
    }

    fun DownloadTask.toEntity(planId: String? = null): DownloadTaskEntity = DownloadTaskEntity(
        id = id.value,
        planId = planId,
        releaseId = releaseId?.value,
        sourceType = when (source) {
            is DownloadSource.TorrentSource -> "Torrent"
            is DownloadSource.HttpSource -> "Http"
            is DownloadSource.DirectSource -> "Direct"
        },
        sourceUri = when (val s = source) {
            is DownloadSource.TorrentSource -> s.magnetUri?.rawValue ?: s.infoHash.hexString
            is DownloadSource.HttpSource -> s.url.rawValue
            is DownloadSource.DirectSource -> s.url.rawValue
        },
        torrentHash = (source as? DownloadSource.TorrentSource)?.infoHash?.hexString,
        state = state.name,
        priority = priority.weight,
        destinationId = destination.identifier,
        destinationRelativePath = "",
        totalBytes = 0L,
        downloadedBytes = 0L,
        createdAt = createdAt.toEpochMilli(),
        startedAt = startedAt?.toEpochMilli(),
        updatedAt = updatedAt.toEpochMilli(),
        completedAt = completedAt?.toEpochMilli(),
        errorMessage = errorMessage
    )

    // --- DownloadFile Mappers ---
    fun DownloadFileEntity.toDomain(): DownloadFile = DownloadFile(
        id = DownloadFileId(id),
        taskId = DownloadTaskId(taskId),
        relativePath = relativePath,
        fileName = fileName,
        expectedSize = expectedBytes?.let { ByteSize.ofBytes(it) },
        downloadedSize = ByteSize.ofBytes(downloadedBytes),
        state = try { DownloadFileState.valueOf(state) } catch (_: Exception) { DownloadFileState.Pending }
    )

    fun DownloadFile.toEntity(): DownloadFileEntity = DownloadFileEntity(
        id = id.value,
        taskId = taskId.value,
        relativePath = relativePath,
        fileName = fileName,
        expectedBytes = expectedSize?.bytes,
        downloadedBytes = downloadedSize.bytes,
        state = state.name
    )

    // --- LibraryItem & LibraryFile Mappers ---
    fun LibraryItemEntity.toDomain(): LibraryItem = LibraryItem(
        id = LibraryItemId(id),
        mediaIdentity = MediaIdentity(
            animeId = animeId?.let { AnimeId(it) },
            seasonNumber = seasonId?.let { SeasonNumber.of(1) },
            episodeRange = episodeId?.let { EpisodeRange.ofSingle(1) },
            canonicalTitle = displayTitle
        ),
        state = try { LibraryItemState.valueOf(state) } catch (_: Exception) { LibraryItemState.Indexed },
        location = StorageTarget(rootStorageId, rootStorageId),
        indexedAt = Instant.ofEpochMilli(indexedAt),
        updatedAt = Instant.ofEpochMilli(updatedAt)
    )

    fun LibraryItem.toEntity(): LibraryItemEntity = LibraryItemEntity(
        id = id.value,
        animeId = mediaIdentity.animeId?.value,
        seasonId = mediaIdentity.seasonNumber?.numericValue?.toString(),
        displayTitle = mediaIdentity.canonicalTitle,
        state = state.name,
        rootStorageId = location.identifier,
        relativePath = "",
        indexedAt = indexedAt.toEpochMilli(),
        updatedAt = updatedAt.toEpochMilli()
    )

    fun LibraryFileEntity.toDomain(): LibraryFile = LibraryFile(
        id = LibraryFileId(id),
        libraryItemId = LibraryItemId(libraryItemId),
        path = path,
        fileName = fileName,
        size = ByteSize.ofBytes(sizeBytes),
        modifiedAt = Instant.ofEpochMilli(modifiedAt),
        fingerprint = fingerprintValue?.let { FileFingerprint.FullHash("SHA-256", it) }
    )

    fun LibraryFile.toEntity(): LibraryFileEntity = LibraryFileEntity(
        id = id.value,
        libraryItemId = libraryItemId.value,
        path = path,
        fileName = fileName,
        sizeBytes = size.bytes,
        modifiedAt = modifiedAt.toEpochMilli(),
        fingerprintType = fingerprint?.let { "SHA-256" },
        fingerprintValue = (fingerprint as? FileFingerprint.FullHash)?.hash
    )

    // --- Collection Mappers ---
    fun CollectionEntity.toDomain(): Collection = Collection(
        id = CollectionId(id),
        name = name,
        type = try { CollectionType.valueOf(type) } catch (_: Exception) { CollectionType.Static },
        source = try { CollectionSource.valueOf(sourceType) } catch (_: Exception) { CollectionSource.Manual },
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt)
    )

    fun Collection.toEntity(): CollectionEntity = CollectionEntity(
        id = id.value,
        name = name,
        type = type.name,
        sourceType = source.name,
        createdAt = createdAt.toEpochMilli(),
        updatedAt = updatedAt.toEpochMilli()
    )
}
