package com.aniflow.domain.model.aggregate.download

import com.aniflow.domain.identity.DownloadFileId
import com.aniflow.domain.identity.DownloadTaskId
import com.aniflow.domain.valueobject.ByteSize

enum class DownloadFileState {
    Pending,
    Downloading,
    Verifying,
    Completed,
    Failed,
    Skipped
}

/**
 * Domain entity representing an individual file inside a download task (Section 54 & 132).
 * Accommodates single-file and multi-file torrent/HTTP downloads.
 */
data class DownloadFile(
    val id: DownloadFileId,
    val taskId: DownloadTaskId,
    val relativePath: String,
    val fileName: String,
    val expectedSize: ByteSize? = null,
    val downloadedSize: ByteSize = ByteSize.ZERO,
    val state: DownloadFileState = DownloadFileState.Pending
) {
    init {
        require(fileName.isNotBlank()) { "DownloadFile fileName cannot be blank" }
    }

    val progress: Float
        get() = if (expectedSize != null && expectedSize.bytes > 0) {
            (downloadedSize.bytes.toFloat() / expectedSize.bytes.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
}
