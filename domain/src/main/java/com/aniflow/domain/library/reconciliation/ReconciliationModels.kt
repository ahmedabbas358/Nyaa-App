package com.aniflow.domain.library.reconciliation

import com.aniflow.domain.identity.LibraryFileId
import com.aniflow.domain.identity.StorageId
import com.aniflow.domain.library.model.DuplicateMediaType
import com.aniflow.domain.library.scanner.DiscoveredPhysicalFile
import com.aniflow.domain.storage.model.StorageLocation
import java.time.Instant

/**
 * Individual reconciliation item discovery (Section 42, 43, 44, 45, 85, 87).
 */
sealed interface ReconciliationItemResult {
    data class Consistent(val fileId: LibraryFileId) : ReconciliationItemResult
    data class MissingPhysicalFile(val fileId: LibraryFileId, val lastLocation: StorageLocation) : ReconciliationItemResult
    data class UnindexedFile(val physicalFile: DiscoveredPhysicalFile) : ReconciliationItemResult
    data class MovedFile(val fileId: LibraryFileId, val oldLocation: StorageLocation, val newLocation: StorageLocation) : ReconciliationItemResult
    data class RenamedFile(val fileId: LibraryFileId, val oldName: String, val newName: String) : ReconciliationItemResult
    data class StorageOffline(val fileId: LibraryFileId, val storageId: StorageId) : ReconciliationItemResult
    data class ChangedFile(val fileId: LibraryFileId, val sizeDelta: Long, val newTimestamp: Instant) : ReconciliationItemResult
    data class Duplicate(val primaryFileId: LibraryFileId, val duplicateFileId: LibraryFileId, val duplicateType: DuplicateMediaType) : ReconciliationItemResult
    data class Unknown(val detail: String) : ReconciliationItemResult
}

/**
 * Summary of a full or incremental reconciliation cycle (Section 43).
 */
data class ReconciliationSummary(
    val results: List<ReconciliationItemResult>,
    val consistentCount: Int,
    val missingCount: Int,
    val unindexedCount: Int,
    val movedCount: Int,
    val renamedCount: Int,
    val offlineCount: Int,
    val duplicateCount: Int,
    val changedCount: Int
)
