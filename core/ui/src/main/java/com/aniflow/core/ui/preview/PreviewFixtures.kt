package com.aniflow.core.ui.preview

import com.aniflow.core.ui.model.AnimeUiModel
import com.aniflow.core.ui.model.CollectionUiModel
import com.aniflow.core.ui.model.DownloadStatusUi
import com.aniflow.core.ui.model.DownloadUiModel
import com.aniflow.core.ui.model.EpisodeUiModel
import com.aniflow.core.ui.model.LibraryItemUiModel
import com.aniflow.core.ui.model.ReleaseUiModel
import com.aniflow.core.ui.model.StorageUiModel
import com.aniflow.core.ui.model.UiBadge
import com.aniflow.core.ui.theme.AppSemanticColors

/**
 * Preview Fixtures (Section 155).
 * Rich dummy data for Compose previews and unit tests without hitting network or Room DB.
 */
object PreviewFixtures {

    val sampleReleaseOne = ReleaseUiModel(
        id = "rel-1",
        title = "[SubsPlease] One Piece - 1050 (1080p) [16A463B8].mkv",
        animeTitle = "One Piece",
        seasonNumber = 1,
        episodeNumber = 1050.0,
        uploader = "SubsPlease",
        releaseGroup = "SubsPlease",
        resolution = "1080p",
        codec = "H.264",
        audio = "Japanese (2.0)",
        subtitles = "English",
        sizeFormatted = "1.37 GB",
        seeders = 142,
        leechers = 8,
        publishedDateFormatted = "Today",
        isTrusted = true,
        isPreferred = true,
        badges = listOf(
            UiBadge("1080p", AppSemanticColors.Accent.copy(alpha = 0.2f), AppSemanticColors.Accent),
            UiBadge("HEVC", AppSemanticColors.Success.copy(alpha = 0.2f), AppSemanticColors.Success),
            UiBadge("Trusted", AppSemanticColors.Success.copy(alpha = 0.2f), AppSemanticColors.Success)
        ),
        magnetUrl = "magnet:?xt=urn:btih:sample1"
    )

    val sampleReleaseTwo = ReleaseUiModel(
        id = "rel-2",
        title = "[Erai-raws] One Piece - 1050 [720p][Multiple Subtitle].mkv",
        animeTitle = "One Piece",
        seasonNumber = 1,
        episodeNumber = 1050.0,
        uploader = "Erai-raws",
        releaseGroup = "Erai-raws",
        resolution = "720p",
        codec = "H.264",
        audio = "Japanese",
        subtitles = "Multi-Sub",
        sizeFormatted = "750 MB",
        seeders = 48,
        leechers = 3,
        publishedDateFormatted = "Yesterday",
        isTrusted = true,
        isPreferred = false
    )

    val sampleEpisode = EpisodeUiModel(
        id = "ep-3",
        animeId = "anime-op",
        seasonNumber = 1,
        episodeNumber = 3.0,
        title = "Morgan versus Luffy! Who's This Mysterious Pretty Girl?",
        statusText = "Available",
        statusColor = AppSemanticColors.Info,
        isAvailable = true,
        isDownloaded = false,
        preferredRelease = sampleReleaseOne,
        alternativeReleases = listOf(sampleReleaseTwo),
        whySelectedReasons = listOf(
            "Preferred uploader: SubsPlease",
            "Highest resolution: 1080p",
            "HEVC video codec",
            "English subtitles included"
        )
    )

    val sampleDownloadActive = DownloadUiModel(
        taskId = "task-1",
        title = "One Piece — Episode 1050",
        animeTitle = "One Piece",
        episodeNumber = 1050.0,
        status = DownloadStatusUi.Active,
        progressPercent = 68,
        downloadedBytesFormatted = "931 MB",
        totalBytesFormatted = "1.37 GB",
        speedFormatted = "8.4 MB/s",
        etaFormatted = "00:54",
        engineType = "HTTP",
        activeConnections = 8,
        totalSegments = 8
    )

    val sampleDownloadQueued = DownloadUiModel(
        taskId = "task-2",
        title = "Bleach: Thousand-Year Blood War — Episode 12",
        animeTitle = "Bleach",
        episodeNumber = 12.0,
        status = DownloadStatusUi.Queued,
        progressPercent = 0,
        downloadedBytesFormatted = "0 B",
        totalBytesFormatted = "1.42 GB",
        speedFormatted = "0 B/s",
        etaFormatted = "—",
        engineType = "Torrent",
        seeders = 84,
        peers = 12
    )

    val sampleAnime = AnimeUiModel(
        id = "anime-1",
        title = "One Piece",
        coverUrl = null,
        totalSeasons = 1,
        totalEpisodes = 24,
        downloadedEpisodesCount = 18,
        downloadingEpisodesCount = 2,
        totalSizeBytesFormatted = "28.4 GB",
        isCompleted = false
    )

    val sampleLibraryItem = LibraryItemUiModel(
        id = "lib-1",
        animeTitle = "One Piece",
        seasonNumber = 1,
        episodeCount = 18,
        displaySizeFormatted = "28.4 GB",
        storageLocationName = "SD Card (512 GB)",
        isOffline = false,
        lastWatchedEpisode = 16.0
    )

    val sampleStorageInternal = StorageUiModel(
        locationId = "loc-internal",
        displayName = "Internal Storage",
        typeName = "Internal",
        usedBytesFormatted = "72.4 GB",
        freeBytesFormatted = "55.6 GB",
        totalBytesFormatted = "128.0 GB",
        freePercentage = 43,
        statusText = "Healthy",
        statusColor = AppSemanticColors.Success
    )

    val sampleStorageSd = StorageUiModel(
        locationId = "loc-sd",
        displayName = "SanDisk Extreme",
        typeName = "SD Card",
        usedBytesFormatted = "342 GB",
        freeBytesFormatted = "170 GB",
        totalBytesFormatted = "512 GB",
        freePercentage = 33,
        statusText = "Healthy",
        statusColor = AppSemanticColors.Success
    )

    val sampleCollection = CollectionUiModel(
        id = "col-1",
        name = "All 1080p HEVC Anime",
        description = "Smart filter automatically indexing high quality encodes",
        itemCount = 142,
        downloadedCount = 118,
        isSmartCollection = true,
        queryCriteria = "resolution:1080p codec:HEVC"
    )
}
