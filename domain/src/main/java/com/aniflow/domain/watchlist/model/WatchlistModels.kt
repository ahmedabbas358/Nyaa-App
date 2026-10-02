package com.aniflow.domain.watchlist.model

import com.aniflow.domain.identity.WatchlistId
import com.aniflow.domain.identity.WatchlistItemId
import java.time.Instant

enum class WatchlistTargetType {
    Anime,
    Season,
    Episode,
    Release,
    Uploader,
    Collection
}

enum class WatchlistState {
    Watching,
    Paused,
    Completed,
    Disabled,
    Expired
}

/**
 * Watchlist policy controlling automation aggressiveness (Section 7, 72, 113).
 * Default rule: Notify only! Never assume auto-download consent.
 */
data class WatchlistPolicy(
    val notifyOnly: Boolean = true,
    val autoSelect: Boolean = false,
    val autoDownload: Boolean = false,
    val autoUpgrade: Boolean = false,
    val stopOnCompleted: Boolean = true,
    val preferredQuality: String? = "1080p",
    val preferredCodec: String? = "HEVC"
)

data class WatchlistItem(
    val id: WatchlistItemId,
    val watchlistId: WatchlistId,
    val targetType: WatchlistTargetType,
    val targetId: String,
    val title: String,
    val state: WatchlistState = WatchlistState.Watching,
    val lastCheckedAt: Instant? = null,
    val lastKnownEpisode: Double? = null,
    val matchedReleaseCount: Int = 0,
    val createdAt: Instant = Instant.now()
)

/**
 * Watchlist Domain Model (Section 7, 8, 9).
 * Represents "Watch this target" (Anime, Episode, etc.), decoupled from "Run this query" (SavedSearch).
 */
data class Watchlist(
    val id: WatchlistId,
    val name: String,
    val enabled: Boolean = true,
    val policy: WatchlistPolicy = WatchlistPolicy(),
    val items: List<WatchlistItem> = emptyList(),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)
