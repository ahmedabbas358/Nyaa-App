package com.aniflow.feature.library.scanner

import com.aniflow.feature.library.probe.MediaProbe
import com.aniflow.platform.storage.model.StorageFile
import com.aniflow.platform.storage.model.StorageLocation
import com.aniflow.platform.storage.provider.StorageProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

data class ScannedFile(
    val name: String,
    val relativePath: String,
    val sizeBytes: Long,
    val modifiedEpochMillis: Long,
    val cheapFingerprint: String,
    val parentFolder: String? = null,
    val isMediaFile: Boolean = true,
    val isSubtitle: Boolean = false
)

data class ScanProgress(
    val totalDiscovered: Int,
    val processedCount: Int,
    val currentBatch: List<ScannedFile> = emptyList(),
    val isComplete: Boolean = false
)

/**
 * LibraryScanner (Sections 43, 44, 45, 46, 47, 48, 95, 96, 168).
 * Scans directories using incremental cheap fingerprints (size + modifiedAt) and
 * batched pagination (500-2,000 files/batch) to maintain low memory overhead even with 100k+ files.
 */
class LibraryScanner(
    private val mediaProbe: MediaProbe
) {

    fun scanIncrementally(
        files: List<StorageFile>,
        knownFingerprints: Set<String>,
        batchSize: Int = 1000
    ): Flow<ScanProgress> = flow {
        var processed = 0
        val currentBatch = mutableListOf<ScannedFile>()

        for (file in files) {
            val isMedia = mediaProbe.isMediaFile(file.name)
            val isSub = mediaProbe.isSubtitleFile(file.name)

            if (!isMedia && !isSub) continue

            val cheapFingerprint = "${file.sizeBytes}_${file.lastModified.toEpochMilli()}"

            // If incremental and already known with same size & timestamp, skip heavy re-indexing
            if (knownFingerprints.contains(cheapFingerprint)) {
                processed++
                continue
            }

            val parentFolder = file.relativePath.substringBeforeLast('/', "").substringAfterLast('/')
                .ifBlank { null }

            val scanned = ScannedFile(
                name = file.name,
                relativePath = file.relativePath,
                sizeBytes = file.sizeBytes,
                modifiedEpochMillis = file.lastModified.toEpochMilli(),
                cheapFingerprint = cheapFingerprint,
                parentFolder = parentFolder,
                isMediaFile = isMedia,
                isSubtitle = isSub
            )

            currentBatch.add(scanned)
            processed++

            if (currentBatch.size >= batchSize) {
                emit(
                    ScanProgress(
                        totalDiscovered = files.size,
                        processedCount = processed,
                        currentBatch = currentBatch.toList(),
                        isComplete = false
                    )
                )
                currentBatch.clear()
            }
        }

        // Emit final remaining batch
        if (currentBatch.isNotEmpty() || processed == files.size) {
            emit(
                ScanProgress(
                    totalDiscovered = files.size,
                    processedCount = processed,
                    currentBatch = currentBatch.toList(),
                    isComplete = true
                )
            )
        }
    }.flowOn(Dispatchers.IO)
}
