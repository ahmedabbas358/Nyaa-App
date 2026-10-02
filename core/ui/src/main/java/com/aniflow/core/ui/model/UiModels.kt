package com.aniflow.core.ui.model

import androidx.compose.ui.graphics.Color
import com.aniflow.core.ui.theme.AppSemanticColors

/**
 * UI Models (Section 10).
 * Complete decoupling: Domain entities are transformed into immutable UiModels before entering Compose.
 */

data class UiBadge(
    val text: String,
    val backgroundColor: Color = AppSemanticColors.Accent.copy(alpha = 0.15f),
    val textColor: Color = AppSemanticColors.Accent
)

data class ReleaseUiModel(
    val id: String,
    val title: String,
    val animeTitle: String,
    val seasonNumber: Int? = null,
    val episodeNumber: Double? = null,
    val uploader: String,
    val releaseGroup: String? = null,
    val resolution: String = "1080p",
    val codec: String = "HEVC",
    val audio: String = "Japanese",
    val subtitles: String = "English",
    val sizeFormatted: String,
    val seeders: Int,
    val leechers: Int = 0,
    val publishedDateFormatted: String,
    val isTrusted: Boolean = false,
    val isPreferred: Boolean = false,
    val isDownloaded: Boolean = false,
    val isDownloading: Boolean = false,
    val badges: List<UiBadge> = emptyList(),
    val magnetUrl: String? = null
)

data class EpisodeUiModel(
    val id: String,
    val animeId: String,
    val seasonNumber: Int,
    val episodeNumber: Double,
    val title: String,
    val statusText: String,
    val statusColor: Color = AppSemanticColors.Neutral,
    val isAvailable: Boolean = true,
    val isDownloaded: Boolean = false,
    val isDownloading: Boolean = false,
    val preferredRelease: ReleaseUiModel? = null,
    val alternativeReleases: List<ReleaseUiModel> = emptyList(),
    val whySelectedReasons: List<String> = emptyList()
)

data class AnimeUiModel(
    val id: String,
    val title: String,
    val coverUrl: String? = null,
    val totalSeasons: Int = 1,
    val totalEpisodes: Int = 0,
    val downloadedEpisodesCount: Int = 0,
    val downloadingEpisodesCount: Int = 0,
    val totalSizeBytesFormatted: String? = null,
    val isCompleted: Boolean = false
) {
    val progressFraction: Float
        get() = if (totalEpisodes > 0) downloadedEpisodesCount.toFloat() / totalEpisodes.toFloat() else 0f
}

enum class DownloadStatusUi {
    Active,
    Queued,
    Paused,
    Completed,
    Failed
}

data class DownloadUiModel(
    val taskId: String,
    val title: String,
    val animeTitle: String,
    val episodeNumber: Double? = null,
    val status: DownloadStatusUi,
    val progressPercent: Int,
    val downloadedBytesFormatted: String,
    val totalBytesFormatted: String,
    val speedFormatted: String,
    val etaFormatted: String,
    val engineType: String = "HTTP", // "HTTP" or "Torrent"
    val activeConnections: Int = 0,
    val totalSegments: Int = 0,
    val seeders: Int? = null,
    val peers: Int? = null,
    val errorMessage: String? = null
)

data class LibraryItemUiModel(
    val id: String,
    val animeTitle: String,
    val seasonNumber: Int = 1,
    val episodeCount: Int = 0,
    val displaySizeFormatted: String,
    val coverUrl: String? = null,
    val storageLocationName: String,
    val isOffline: Boolean = false,
    val lastWatchedEpisode: Double? = null
)

data class StorageUiModel(
    val locationId: String,
    val displayName: String,
    val typeName: String,
    val usedBytesFormatted: String,
    val freeBytesFormatted: String,
    val totalBytesFormatted: String,
    val freePercentage: Int,
    val isWritable: Boolean = true,
    val isRemovable: Boolean = false,
    val isMounted: Boolean = true,
    val statusText: String = "Healthy",
    val statusColor: Color = AppSemanticColors.Success
)

data class CollectionUiModel(
    val id: String,
    val name: String,
    val description: String? = null,
    val itemCount: Int = 0,
    val downloadedCount: Int = 0,
    val isSmartCollection: Boolean = false,
    val queryCriteria: String? = null
)

/**
 * Standardized UI Error representation (Section 177, 178).
 */
data class UiError(
    val code: String,
    val title: String,
    val message: String,
    val retryAction: (() -> Unit)? = null
)
