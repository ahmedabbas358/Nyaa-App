package com.aniflow.domain.model.aggregate.download

import com.aniflow.domain.identity.DownloadTaskId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.state.DownloadState
import com.aniflow.domain.valueobject.InfoHash
import com.aniflow.domain.valueobject.StorageTarget
import com.aniflow.domain.valueobject.UrlValue
import java.time.Instant

/**
 * Priority levels for downloads (Section 79 & 80).
 */
enum class DownloadPriority(val weight: Int) : Comparable<DownloadPriority> {
    Lowest(1),
    Low(2),
    Normal(3),
    High(4),
    Highest(5)
}

/**
 * Concrete download source specification (Section 53).
 */
sealed interface DownloadSource {
    data class TorrentSource(
        val infoHash: InfoHash,
        val magnetUri: UrlValue.MagnetUri? = null,
        val torrentFileUrl: UrlValue.TorrentUrl? = null,
        val name: String
    ) : DownloadSource

    data class HttpSource(
        val url: UrlValue.HttpsUrl,
        val fileName: String,
        val headers: Map<String, String> = emptyMap()
    ) : DownloadSource

    data class DirectSource(
        val url: UrlValue.HttpsUrl,
        val fileName: String
    ) : DownloadSource
}

/**
 * Domain entity representing an active or persisted download task (Section 52).
 * Strictly immutable: state changes are made via copy() and validated by state machines.
 */
data class DownloadTask(
    val id: DownloadTaskId,
    val releaseId: ReleaseId?,
    val source: DownloadSource,
    val state: DownloadState = DownloadState.Pending,
    val priority: DownloadPriority = DownloadPriority.Normal,
    val destination: StorageTarget = StorageTarget.DEFAULT,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val startedAt: Instant? = null,
    val completedAt: Instant? = null,
    val errorMessage: String? = null
)
