package com.aniflow.domain.usecase

import com.aniflow.domain.identity.DownloadTaskId
import com.aniflow.domain.model.aggregate.download.DownloadPriority
import com.aniflow.domain.model.aggregate.download.DownloadSource
import com.aniflow.domain.model.aggregate.download.DownloadTask
import com.aniflow.domain.model.aggregate.release.Release
import com.aniflow.domain.model.aggregate.release.ReleaseSource
import com.aniflow.domain.repository.DownloadRepository
import com.aniflow.domain.service.StorageManager
import com.aniflow.domain.state.DownloadState
import com.aniflow.domain.valueobject.InfoHash
import com.aniflow.domain.valueobject.StorageTarget
import java.time.Instant
import java.util.UUID

/**
 * Download Lifecycle Use Cases:
 * Queue, Pause, Resume, and Cancel real download tasks against DownloadRepository.
 */

class QueueDownloadUseCase(
    private val downloadRepository: DownloadRepository,
    private val storageManager: StorageManager? = null
) {
    suspend operator fun invoke(release: Release, destinationFolder: String = "Anime/Downloads"): DownloadTaskId {
        val taskId = DownloadTaskId("task-${UUID.randomUUID().toString().take(8)}")
        val source = when (val s = release.source) {
            is ReleaseSource.Torrent -> DownloadSource.TorrentSource(
                infoHash = s.infoHash,
                magnetUri = s.magnetUri,
                torrentFileUrl = s.torrentUrl,
                name = release.title
            )
            is ReleaseSource.DirectHttp -> DownloadSource.DirectSource(
                url = s.url,
                fileName = "${release.title}.mp4"
            )
            else -> DownloadSource.TorrentSource(
                infoHash = InfoHash("0000000000000000000000000000000000000000"),
                name = release.title
            )
        }
        val task = DownloadTask(
            id = taskId,
            releaseId = release.id,
            source = source,
            state = DownloadState.Queued,
            priority = DownloadPriority.Normal,
            destination = StorageTarget(destinationFolder),
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        downloadRepository.saveTask(task)
        return taskId
    }
}

class PauseDownloadUseCase(
    private val downloadRepository: DownloadRepository
) {
    suspend operator fun invoke(taskId: DownloadTaskId) {
        val task = downloadRepository.getTaskById(taskId) ?: return
        downloadRepository.saveTask(task.copy(state = DownloadState.Paused, updatedAt = Instant.now()))
    }
}

class ResumeDownloadUseCase(
    private val downloadRepository: DownloadRepository
) {
    suspend operator fun invoke(taskId: DownloadTaskId) {
        val task = downloadRepository.getTaskById(taskId) ?: return
        downloadRepository.saveTask(task.copy(state = DownloadState.Downloading, updatedAt = Instant.now()))
    }
}

class CancelDownloadUseCase(
    private val downloadRepository: DownloadRepository,
    private val storageManager: StorageManager? = null
) {
    suspend operator fun invoke(taskId: DownloadTaskId) {
        downloadRepository.deleteTask(taskId)
    }
}
