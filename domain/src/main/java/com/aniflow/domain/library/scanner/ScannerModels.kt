package com.aniflow.domain.library.scanner

import com.aniflow.domain.identity.StorageId
import java.time.Duration

enum class ScanMode {
    FullScan,
    IncrementalScan,
    FolderScan,
    ChangedFilesScan,
    RepairScan
}

data class DiscoveredPhysicalFile(
    val storageId: StorageId,
    val relativePath: String,
    val name: String,
    val sizeBytes: Long,
    val modifiedEpochMillis: Long,
    val cheapFingerprint: String,
    val parentFolder: String? = null,
    val isMedia: Boolean = true,
    val isSidecar: Boolean = false
)

data class ScanResult(
    val discovered: Int,
    val added: Int,
    val updated: Int,
    val moved: Int,
    val missing: Int,
    val unknown: Int,
    val failed: Int,
    val duration: Duration
)

data class ScanProgress(
    val isRunning: Boolean = false,
    val currentRoot: String? = null,
    val currentFolder: String? = null,
    val totalDiscovered: Int = 0,
    val processedCount: Int = 0,
    val addedCount: Int = 0,
    val movedCount: Int = 0,
    val unknownCount: Int = 0,
    val errorCount: Int = 0,
    val isComplete: Boolean = false
) {
    val percent: Float?
        get() = if (totalDiscovered > 0) (processedCount.toFloat() / totalDiscovered.toFloat()) * 100f else null
}
