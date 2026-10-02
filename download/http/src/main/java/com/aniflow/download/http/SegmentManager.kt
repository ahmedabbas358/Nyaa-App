package com.aniflow.download.http

import com.aniflow.download.core.model.HttpSegment
import com.aniflow.download.core.model.SegmentState

/**
 * Manages segment division and byte ranges for multi-connection downloads (Section 55, 56, 57, 58).
 */
class SegmentManager {

    fun divideIntoSegments(totalBytes: Long, targetSegments: Int): List<HttpSegment> {
        val segmentCount = targetSegments.coerceIn(1, 16)
        if (totalBytes <= 0L || segmentCount == 1) {
            return listOf(
                HttpSegment(
                    index = 0,
                    startOffset = 0L,
                    endOffset = (totalBytes - 1).coerceAtLeast(0L),
                    downloadedBytes = 0L,
                    state = SegmentState.Pending
                )
            )
        }

        val segmentSize = totalBytes / segmentCount
        val segments = mutableListOf<HttpSegment>()

        for (i in 0 until segmentCount) {
            val start = i * segmentSize
            val end = if (i == segmentCount - 1) totalBytes - 1 else (start + segmentSize - 1)
            segments += HttpSegment(
                index = i,
                startOffset = start,
                endOffset = end,
                downloadedBytes = 0L,
                state = SegmentState.Pending
            )
        }

        return segments
    }
}
