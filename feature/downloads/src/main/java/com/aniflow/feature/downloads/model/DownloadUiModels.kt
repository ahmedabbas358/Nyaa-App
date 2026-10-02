package com.aniflow.feature.downloads.model

import com.aniflow.domain.identity.AnimeId
import com.aniflow.domain.identity.DownloadTaskId
import com.aniflow.domain.identity.EpisodeId
import com.aniflow.domain.identity.ReleaseId
import java.time.Instant

enum class DownloadStateUi(val displayName: String) {
    Starting("Starting"),
    Downloading("Downloading"),
    Queued("Queued"),
    Paused("Paused"),
    Waiting("Waiting"),
    Retrying("Retrying"),
    Verifying("Verifying"),
    Moving("Moving to Library"),
    Completed("Completed"),
    Failed("Failed"),
    Cancelled("Cancelled");

    val isActive: Boolean
        get() = this == Downloading || this == Starting || this == Retrying || this == Verifying || this == Moving

    val isTerminal: Boolean
        get() = this == Completed || this == Failed || this == Cancelled
}

enum class WaitingReasonUi(val displayName: String) {
    WaitingForNetwork("Waiting for Wi-Fi connection"),
    WaitingForStorage("Waiting for storage space to clear"),
    WaitingForSlot("Waiting for an open download slot"),
    WaitingForPermission("Waiting for storage permission"),
    WaitingForProvider("Waiting for provider to respond"),
    PausedByUser("Paused by user")
}

enum class DownloadPriorityUi(val displayName: String, val level: Int) {
    Highest("Highest", 4),
    High("High", 3),
    Normal("Normal", 2),
    Low("Low", 1)
}

enum class DownloadEngineBadge(val displayName: String) {
    HTTP("HTTP"),
    Torrent("Torrent")
}

enum class DownloadFilterCategory(val displayName: String) {
    All("All"),
    Active("Active"),
    Queued("Queued"),
    Paused("Paused"),
    Waiting("Waiting"),
    Failed("Failed"),
    Completed("Completed")
}

enum class DownloadSortOption(val displayName: String) {
    CreatedDate("Date Added"),
    Priority("Priority"),
    Progress("Progress"),
    Speed("Speed"),
    Size("Size"),
    ETA("Time Remaining"),
    Name("Name")
}

enum class DownloadGroupingMode(val displayName: String) {
    Flat("List (Flat)"),
    ByAnime("Group by Anime"),
    ByPlan("Group by Plan")
}

data class DownloadFileUiModel(
    val id: String,
    val filename: String,
    val totalSizeBytes: Long,
    val downloadedBytes: Long,
    val progressPercent: Int,
    val state: String = "Pending", // Pending, Downloading, Completed, Skipped
    val isSelected: Boolean = true
)

data class HttpSegmentUiModel(
    val index: Int,
    val startOffset: Long,
    val endOffset: Long,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val speedFormatted: String,
    val isCompleted: Boolean
) {
    val progressPercent: Int
        get() = if (totalBytes > 0) ((downloadedBytes.toFloat() / totalBytes) * 100).toInt().coerceIn(0, 100) else 0
}

data class TorrentSwarmUiModel(
    val seeders: Int,
    val leechers: Int,
    val connectedPeers: Int,
    val shareRatio: Float,
    val uploadSpeedFormatted: String,
    val totalUploadedBytes: Long
)

/**
 * Task UI representation (Section 6, 8, 9, 10, 11, 12, 76, 108).
 */
data class DownloadTaskUiModel(
    val id: DownloadTaskId,
    val title: String,
    val animeTitle: String,
    val animeId: AnimeId? = null,
    val episodeNumber: Double? = null,
    val episodeId: EpisodeId? = null,
    val releaseId: ReleaseId? = null,
    val planId: String? = null,
    val state: DownloadStateUi,
    val engineBadge: DownloadEngineBadge,
    val priority: DownloadPriorityUi = DownloadPriorityUi.Normal,
    val queuePosition: Int? = null,
    val waitingReason: WaitingReasonUi? = null,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long? = null,
    val progressPercent: Int = 0,
    val speedFormatted: String = "0 KB/s",
    val speedBytesPerSec: Long = 0L,
    val etaFormatted: String = "Estimating…",
    val etaSeconds: Long? = null,
    val destinationPath: String = "Internal / Anime",
    val sourceUrlOrMagnet: String? = null,
    val errorMessage: String? = null,
    val whyCreatedReason: String = "Manual selection (1080p HEVC preferred)",
    val isBatch: Boolean = false,
    val files: List<DownloadFileUiModel> = emptyList(),
    val segments: List<HttpSegmentUiModel> = emptyList(),
    val swarm: TorrentSwarmUiModel? = null,
    val createdAt: Instant = Instant.now(),
    val isSelectedForBulk: Boolean = false
) {
    val downloadedBytesFormatted: String
        get() = com.aniflow.feature.downloads.util.ByteSizeFormatter.format(downloadedBytes)

    val totalBytesFormatted: String
        get() = totalBytes?.let { com.aniflow.feature.downloads.util.ByteSizeFormatter.format(it) } ?: "Unknown size"
}

/**
 * Group representation for Anime or Plans (Section 40, 41, 42, 43, 123, 126).
 */
data class DownloadGroupUiModel(
    val groupKey: String,
    val groupTitle: String,
    val tasks: List<DownloadTaskUiModel>,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val progressPercent: Int,
    val totalSpeedFormatted: String,
    val completedCount: Int,
    val totalCount: Int,
    val isExpanded: Boolean = true
)

/**
 * History record for completed or failed tasks (Section 60, 61).
 */
data class DownloadHistoryItemUiModel(
    val id: String,
    val taskId: String,
    val title: String,
    val animeTitle: String,
    val episodeNumber: Double?,
    val sizeFormatted: String,
    val durationFormatted: String,
    val completedAtFormatted: String,
    val finalLocation: String,
    val statusText: String = "Success",
    val isSuccessful: Boolean = true
)

/**
 * High level statistics (Section 54, 55, 59).
 */
data class DownloadStatisticsUiModel(
    val downloadedTodayFormatted: String = "0 B",
    val downloadedThisWeekFormatted: String = "0 B",
    val downloadedAllTimeFormatted: String = "0 B",
    val averageSpeedFormatted: String = "0 KB/s",
    val completedTasksCount: Int = 0,
    val failedTasksCount: Int = 0,
    val cancelledTasksCount: Int = 0,
    val topFailureReasons: List<Pair<String, Int>> = emptyList()
)
