package com.aniflow.domain.library.model

import com.aniflow.domain.identity.AnimeId
import com.aniflow.domain.identity.EpisodeId
import com.aniflow.domain.identity.LibraryFileId
import com.aniflow.domain.identity.LibraryMediaId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.identity.SeasonId
import java.time.Instant

/**
 * WatchProgress domain model (Section 10, 11).
 * Tracks detailed playback position independently of filesystem paths or filenames.
 */
data class WatchProgress(
    val mediaId: LibraryMediaId,
    val animeId: AnimeId? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Double? = null,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Instant = Instant.now(),
    val completed: Boolean = false
) {
    val progressFraction: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val remainingMs: Long
        get() = (durationMs - positionMs).coerceAtLeast(0L)

    val remainingMinutes: Long
        get() = remainingMs / 60000L
}

/**
 * Configurable watch completion policy (Section 11).
 * Default rule: >= 90% is Completed, < 90% is In Progress.
 * Merely opening a file never counts as Watched.
 */
data class WatchCompletionPolicy(
    val completionThresholdFraction: Float = 0.90f
) {
    fun evaluate(positionMs: Long, durationMs: Long): Boolean {
        if (durationMs <= 0) return false
        val fraction = positionMs.toFloat() / durationMs.toFloat()
        return fraction >= completionThresholdFraction
    }
}

/**
 * High-level presentation projection for personal dashboard (Section 12).
 */
data class ContinueWatchingItem(
    val mediaId: LibraryMediaId,
    val animeId: AnimeId,
    val animeTitle: String,
    val seasonNumber: Int,
    val episodeNumber: Double,
    val episodeTitle: String?,
    val progress: WatchProgress,
    val thumbnailUri: String? = null
)

/**
 * Persistent Media Identity entity (Section 21, 22).
 * Decouples logical media and watch state from physical disk paths and filenames.
 * Guarantees that File Replacement (quality upgrade) preserves WatchProgress without resetting to 0%.
 */
data class LibraryMedia(
    val id: LibraryMediaId,
    val animeId: AnimeId,
    val seasonId: SeasonId?,
    val episodeId: EpisodeId?,
    val releaseId: ReleaseId?,
    val physicalFileId: LibraryFileId,
    val physicalFilePath: String,
    val isMissing: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    /**
     * Executes safe upgrade/replacement preserving media identity and history (Section 22).
     */
    fun upgradeFile(newFileId: LibraryFileId, newFilePath: String, newReleaseId: ReleaseId? = null): LibraryMedia {
        return copy(
            physicalFileId = newFileId,
            physicalFilePath = newFilePath,
            releaseId = newReleaseId ?: this.releaseId,
            isMissing = false,
            updatedAt = Instant.now()
        )
    }

    /**
     * Flags file as missing without deleting library entity or watch history (Section 32).
     */
    fun markMissing(): LibraryMedia {
        return copy(isMissing = true, updatedAt = Instant.now())
    }
}
