package com.aniflow.domain.storage.model

import com.aniflow.domain.identity.LibraryFileId
import com.aniflow.domain.identity.StorageId

/**
 * StorageLocation represents an abstract, pure domain storage coordinate (Section 7, 8).
 * Completely decoupled from Android android.net.Uri or raw filesystem paths.
 */
data class StorageLocation(
    val storageId: StorageId,
    val relativePath: String
) {
    val normalizedPath: String
        get() = relativePath.trim().replace('\\', '/').trimStart('/')

    val fileName: String
        get() = normalizedPath.substringAfterLast('/')

    val parentPath: String?
        get() {
            val idx = normalizedPath.lastIndexOf('/')
            return if (idx >= 0) normalizedPath.substring(0, idx) else null
        }

    fun resolveChild(childName: String): StorageLocation {
        val sanitizedChild = childName.trim().replace('\\', '/').trimStart('/')
        val joined = if (normalizedPath.isBlank()) sanitizedChild else "$normalizedPath/$sanitizedChild"
        return StorageLocation(storageId, joined)
    }

    override fun toString(): String = "${storageId.value}:$normalizedPath"
}

data class StorageLocationRoot(
    val rawUriOrPath: String
)

enum class StorageType {
    AppPrivate,
    UserSelectedFolder,
    ExternalVolume,
    MediaStore,
    Other
}

enum class StorageAvailability {
    Available,
    Offline,
    PermissionRequired,
    Unavailable,
    ReadOnly,
    Full,
    Unknown
}

/**
 * StorageRoot represents a registered storage root / library volume (Section 9, 10, 11, 12).
 */
data class StorageRoot(
    val id: StorageId,
    val name: String,
    val type: StorageType,
    val state: StorageAvailability,
    val location: StorageLocationRoot,
    val totalSpaceBytes: Long = 0L,
    val freeSpaceBytes: Long = 0L,
    val isDefault: Boolean = false,
    val canRead: Boolean = true,
    val canWrite: Boolean = true,
    val canDelete: Boolean = true
) {
    val usedSpaceBytes: Long get() = (totalSpaceBytes - freeSpaceBytes).coerceAtLeast(0L)
    val usedPercentage: Float get() = if (totalSpaceBytes > 0) (usedSpaceBytes.toFloat() / totalSpaceBytes.toFloat()) * 100f else 0f
}

/**
 * Storage threshold policy (Section 93).
 */
data class StorageThresholds(
    val warningThresholdBytes: Long = 5L * 1024 * 1024 * 1024, // 5 GB
    val criticalThresholdBytes: Long = 1L * 1024 * 1024 * 1024 // 1 GB
)

/**
 * Real-time storage health snapshot (Section 91, 92).
 */
data class StorageHealth(
    val storageId: StorageId,
    val name: String,
    val totalBytes: Long,
    val freeBytes: Long,
    val libraryUsedBytes: Long,
    val downloadTempBytes: Long,
    val orphanedTempBytes: Long = 0L,
    val unidentifiedSizeBytes: Long = 0L,
    val isLowStorage: Boolean,
    val isCriticalStorage: Boolean
)

/**
 * Storage Analytics aggregate representation (Section 51, 52).
 */
data class StorageAnalytics(
    val totalLibrarySizeBytes: Long,
    val animeBreakdown: Map<String, Long> = emptyMap(),
    val seasonBreakdown: Map<String, Long> = emptyMap(),
    val largestFiles: List<StorageFileSummary> = emptyList(),
    val storageRootDistribution: Map<StorageId, Long> = emptyMap(),
    val unidentifiedSizeBytes: Long = 0L,
    val duplicateWastedBytes: Long = 0L
)

data class StorageFileSummary(
    val fileId: LibraryFileId,
    val fileName: String,
    val animeTitle: String?,
    val episodeNumber: Double?,
    val sizeBytes: Long,
    val location: StorageLocation
)
