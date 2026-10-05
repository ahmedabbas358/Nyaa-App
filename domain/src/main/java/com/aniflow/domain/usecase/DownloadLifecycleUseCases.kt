package com.aniflow.domain.usecase

import com.aniflow.domain.identity.DownloadTaskId
import com.aniflow.domain.identity.ReleaseId
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
import com.aniflow.domain.valueobject.UrlValue
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

    suspend fun queueTorrent(
        title: String,
        magnetUri: String,
        releaseId: String? = null,
        destinationFolder: String = "Anime/Downloads"
    ): DownloadTaskId {
        val taskId = DownloadTaskId("task-${UUID.randomUUID().toString().take(8)}")
        val hashRaw = magnetUri.substringAfter("btih:", "").substringBefore("&").ifBlank { "0000000000000000000000000000000000000000" }
        val source = DownloadSource.TorrentSource(
            infoHash = InfoHash(hashRaw),
            magnetUri = UrlValue.MagnetUri(magnetUri),
            name = title
        )
        val task = DownloadTask(
            id = taskId,
            releaseId = releaseId?.let { ReleaseId(it) },
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

    suspend fun queueFromLink(
        link: String,
        customTitle: String? = null,
        releaseId: String? = null,
        destinationFolder: String = "Anime/Downloads"
    ): DownloadTaskId {
        val trimmed = link.trim()
        val title = customTitle?.ifBlank { null } ?: when {
            trimmed.contains("nyaa.si/view/", ignoreCase = true) -> {
                val id = trimmed.substringAfter("nyaa.si/view/").substringBefore("?").substringBefore("/")
                "Nyaa Release #$id"
            }
            trimmed.startsWith("magnet:?", ignoreCase = true) -> {
                val dn = trimmed.substringAfter("dn=", "").substringBefore("&").replace("+", " ")
                if (dn.isNotBlank()) {
                    try {
                        java.net.URLDecoder.decode(dn, "UTF-8")
                    } catch (e: Exception) {
                        dn
                    }
                } else "Torrent Download"
            }
            trimmed.contains("/") -> trimmed.substringAfterLast("/").substringBefore("?")
            else -> "Download Task"
        }

        return if (trimmed.startsWith("magnet:?", ignoreCase = true)) {
            queueTorrent(title = title, magnetUri = trimmed, releaseId = releaseId, destinationFolder = destinationFolder)
        } else if (trimmed.endsWith(".torrent", ignoreCase = true) || trimmed.contains("nyaa.si/download/")) {
            val taskId = DownloadTaskId("task-${UUID.randomUUID().toString().take(8)}")
            val task = DownloadTask(
                id = taskId,
                releaseId = releaseId?.let { ReleaseId(it) },
                source = DownloadSource.TorrentSource(
                    infoHash = InfoHash("0000000000000000000000000000000000000000"),
                    torrentFileUrl = UrlValue.TorrentUrl(trimmed),
                    name = title
                ),
                state = DownloadState.Queued,
                priority = DownloadPriority.Normal,
                destination = StorageTarget(destinationFolder),
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            downloadRepository.saveTask(task)
            taskId
        } else {
            val taskId = DownloadTaskId("task-${UUID.randomUUID().toString().take(8)}")
            val task = DownloadTask(
                id = taskId,
                releaseId = releaseId?.let { ReleaseId(it) },
                source = DownloadSource.DirectSource(
                    url = UrlValue.HttpsUrl(if (trimmed.startsWith("http")) trimmed else "https://$trimmed"),
                    fileName = if (title.contains(".")) title else "$title.mp4"
                ),
                state = DownloadState.Queued,
                priority = DownloadPriority.Normal,
                destination = StorageTarget(destinationFolder),
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            downloadRepository.saveTask(task)
            taskId
        }
    }
}

class QueueBatchDownloadsUseCase(
    private val queueDownloadUseCase: QueueDownloadUseCase
) {
    suspend operator fun invoke(releases: List<Release>, destinationFolder: String = "Anime/Downloads"): List<DownloadTaskId> {
        return releases.map { queueDownloadUseCase(it, destinationFolder) }
    }

    suspend fun queueTorrents(
        items: List<Pair<String, String>>, // title to magnetUri
        destinationFolder: String = "Anime/Downloads"
    ): List<DownloadTaskId> {
        return items.map { (title, magnet) ->
            queueDownloadUseCase.queueTorrent(title = title, magnetUri = magnet, destinationFolder = destinationFolder)
        }
    }
}

interface DownloadController {
    suspend fun pauseTask(taskId: DownloadTaskId)
    suspend fun resumeTask(taskId: DownloadTaskId)
    suspend fun cancelTask(taskId: DownloadTaskId)
    suspend fun pauseAll()
    suspend fun resumeAll()
    suspend fun clearCompleted()
    fun setMaxConcurrency(limit: Int)
}

class PauseDownloadUseCase(
    private val downloadRepository: DownloadRepository,
    private val controller: DownloadController? = null
) {
    suspend operator fun invoke(taskId: DownloadTaskId) {
        if (controller != null) {
            controller.pauseTask(taskId)
        } else {
            val task = downloadRepository.getTaskById(taskId) ?: return
            downloadRepository.saveTask(task.copy(state = DownloadState.Paused, updatedAt = Instant.now()))
        }
    }
}

class ResumeDownloadUseCase(
    private val downloadRepository: DownloadRepository,
    private val controller: DownloadController? = null
) {
    suspend operator fun invoke(taskId: DownloadTaskId) {
        if (controller != null) {
            controller.resumeTask(taskId)
        } else {
            val task = downloadRepository.getTaskById(taskId) ?: return
            downloadRepository.saveTask(task.copy(state = DownloadState.Downloading, updatedAt = Instant.now()))
        }
    }
}

class CancelDownloadUseCase(
    private val downloadRepository: DownloadRepository,
    private val storageManager: StorageManager? = null,
    private val controller: DownloadController? = null
) {
    suspend operator fun invoke(taskId: DownloadTaskId) {
        if (controller != null) {
            controller.cancelTask(taskId)
        } else {
            downloadRepository.deleteTask(taskId)
        }
    }
}

