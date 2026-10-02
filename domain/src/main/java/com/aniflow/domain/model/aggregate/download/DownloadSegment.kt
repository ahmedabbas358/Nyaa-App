package com.aniflow.domain.model.aggregate.download

import com.aniflow.domain.identity.DownloadFileId
import com.aniflow.domain.identity.DownloadSegmentId

enum class SegmentState {
    Pending,
    Downloading,
    Completed,
    Failed
}

/**
 * Domain entity representing an HTTP segmented chunk or torrent piece (Section 55 & 133).
 */
data class DownloadSegment(
    val id: DownloadSegmentId,
    val fileId: DownloadFileId,
    val startOffset: Long,
    val endOffset: Long,
    val downloadedBytes: Long = 0L,
    val state: SegmentState = SegmentState.Pending
) {
    init {
        require(startOffset >= 0) { "startOffset cannot be negative" }
        require(endOffset >= startOffset) { "endOffset must be >= startOffset" }
        require(downloadedBytes >= 0) { "downloadedBytes cannot be negative" }
    }

    val totalBytes: Long get() = endOffset - startOffset + 1
    val isComplete: Boolean get() = downloadedBytes >= totalBytes || state == SegmentState.Completed
}
