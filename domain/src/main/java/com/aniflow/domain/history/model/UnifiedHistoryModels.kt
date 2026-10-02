package com.aniflow.domain.history.model

import com.aniflow.domain.identity.AnimeId
import com.aniflow.domain.identity.CollectionId
import com.aniflow.domain.identity.DownloadTaskId
import com.aniflow.domain.identity.EpisodeId
import com.aniflow.domain.identity.LibraryMediaId
import com.aniflow.domain.identity.ReleaseGroupId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.identity.SeasonId
import com.aniflow.domain.identity.UploaderId
import java.time.Instant

/**
 * Universal Favorite Entity (Section 24).
 * Supports pinning and bookmarking across any core domain entity.
 */
sealed interface FavoriteTarget {
    data class AnimeTarget(val animeId: AnimeId, val title: String) : FavoriteTarget
    data class SeasonTarget(val seasonId: SeasonId, val animeTitle: String, val seasonNumber: Int) : FavoriteTarget
    data class EpisodeTarget(val episodeId: EpisodeId, val title: String, val episodeNumber: Double) : FavoriteTarget
    data class ReleaseTarget(val releaseId: ReleaseId, val releaseTitle: String) : FavoriteTarget
    data class UploaderTarget(val uploaderId: UploaderId, val uploaderName: String) : FavoriteTarget
    data class ReleaseGroupTarget(val groupId: ReleaseGroupId, val groupName: String) : FavoriteTarget
    data class CollectionTarget(val collectionId: CollectionId, val name: String) : FavoriteTarget
}

data class FavoriteItem(
    val id: String,
    val target: FavoriteTarget,
    val note: String? = null,
    val createdAt: Instant = Instant.now()
)

/**
 * Unified History Partitioning (Section 26).
 * Maintains separate immutable auditing timelines for distinct subsystem lifecycles.
 */
enum class HistoryCategory {
    Watch,
    Search,
    Download,
    Automation,
    Organization
}

sealed interface HistoryEntry {
    val id: String
    val timestamp: Instant
    val category: HistoryCategory

    data class WatchHistory(
        override val id: String,
        override val timestamp: Instant = Instant.now(),
        val mediaId: LibraryMediaId,
        val animeTitle: String,
        val episodeNumber: Double,
        val watchedDurationMs: Long,
        val totalDurationMs: Long,
        val completed: Boolean
    ) : HistoryEntry {
        override val category = HistoryCategory.Watch
    }

    data class SearchHistory(
        override val id: String,
        override val timestamp: Instant = Instant.now(),
        val rawQuery: String,
        val scopeName: String,
        val resultsCount: Int,
        val wasSuccessful: Boolean
    ) : HistoryEntry {
        override val category = HistoryCategory.Search
    }

    data class DownloadHistory(
        override val id: String,
        override val timestamp: Instant = Instant.now(),
        val taskId: DownloadTaskId,
        val releaseTitle: String,
        val totalSizeBytes: Long,
        val status: String,
        val averageSpeedBytesPerSec: Long
    ) : HistoryEntry {
        override val category = HistoryCategory.Download
    }

    data class AutomationHistory(
        override val id: String,
        override val timestamp: Instant = Instant.now(),
        val ruleName: String,
        val triggerType: String,
        val actionTaken: String,
        val status: String,
        val reason: String?
    ) : HistoryEntry {
        override val category = HistoryCategory.Automation
    }

    data class OrganizationHistory(
        override val id: String,
        override val timestamp: Instant = Instant.now(),
        val sourcePath: String,
        val destinationPath: String,
        val bytesMoved: Long,
        val verifiedIntegrity: Boolean
    ) : HistoryEntry {
        override val category = HistoryCategory.Organization
    }
}
