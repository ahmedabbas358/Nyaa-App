package com.aniflow.domain.library.repository

import com.aniflow.domain.identity.LibraryMediaId
import com.aniflow.domain.library.model.ContinueWatchingItem
import com.aniflow.domain.library.model.WatchProgress
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for persisting and observing playback state (Section 10, 12, 20).
 */
interface WatchProgressRepository {
    fun observeWatchProgress(mediaId: LibraryMediaId): Flow<WatchProgress?>
    suspend fun getWatchProgress(mediaId: LibraryMediaId): WatchProgress?
    suspend fun saveWatchProgress(progress: WatchProgress)
    fun observeContinueWatching(): Flow<List<ContinueWatchingItem>>
    suspend fun getContinueWatching(): List<ContinueWatchingItem>
    suspend fun markCompleted(mediaId: LibraryMediaId)
    suspend fun clearWatchProgress(mediaId: LibraryMediaId)
}
