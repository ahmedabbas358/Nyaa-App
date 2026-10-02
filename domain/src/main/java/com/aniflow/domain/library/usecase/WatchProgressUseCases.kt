package com.aniflow.domain.library.usecase

import com.aniflow.domain.identity.LibraryMediaId
import com.aniflow.domain.library.model.ContinueWatchingItem
import com.aniflow.domain.library.model.WatchCompletionPolicy
import com.aniflow.domain.library.model.WatchProgress
import com.aniflow.domain.library.repository.WatchProgressRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant

class SaveWatchProgressUseCase(
    private val repository: WatchProgressRepository,
    private val completionPolicy: WatchCompletionPolicy = WatchCompletionPolicy()
) {
    suspend operator fun invoke(
        mediaId: LibraryMediaId,
        positionMs: Long,
        durationMs: Long,
        animeId: com.aniflow.domain.identity.AnimeId? = null,
        seasonNumber: Int? = null,
        episodeNumber: Double? = null
    ) {
        val isCompleted = completionPolicy.evaluate(positionMs, durationMs)
        val progress = WatchProgress(
            mediaId = mediaId,
            animeId = animeId,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            positionMs = positionMs,
            durationMs = durationMs,
            updatedAt = Instant.now(),
            completed = isCompleted
        )
        repository.saveWatchProgress(progress)
    }
}

class GetContinueWatchingUseCase(
    private val repository: WatchProgressRepository
) {
    fun observe(): Flow<List<ContinueWatchingItem>> = repository.observeContinueWatching()
    suspend operator fun invoke(): List<ContinueWatchingItem> = repository.getContinueWatching()
}

class MarkEpisodeCompletedUseCase(
    private val repository: WatchProgressRepository
) {
    suspend operator fun invoke(mediaId: LibraryMediaId) {
        repository.markCompleted(mediaId)
    }
}
