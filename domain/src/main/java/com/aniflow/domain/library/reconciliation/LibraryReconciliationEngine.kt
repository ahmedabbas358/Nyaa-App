package com.aniflow.domain.library.reconciliation

import com.aniflow.domain.identity.FileFingerprint
import com.aniflow.domain.identity.LibraryFileId
import com.aniflow.domain.identity.StorageId
import com.aniflow.domain.library.model.DuplicateMediaType
import com.aniflow.domain.library.model.LibraryFile
import com.aniflow.domain.library.scanner.DiscoveredPhysicalFile
import com.aniflow.domain.storage.model.StorageAvailability
import com.aniflow.domain.storage.model.StorageLocation
import com.aniflow.domain.storage.model.StorageRoot
import java.time.Instant

/**
 * LibraryReconciliationEngine (Section 41, 42, 43, 44, 45, 85, 86, 87).
 * Compares Room database index against actual physical storage ground truth.
 *
 * Core Principles:
 * - Room is purely an index; physical storage is the source of truth.
 * - If external storage is offline, files are marked Offline, NEVER deleted (Section 85).
 * - If a file is missing at path A but its fingerprint matches path B, detects MovedFile (Section 44).
 * - If a file is missing, marks MissingPhysicalFile; does not silently delete the record (Section 87).
 */
class LibraryReconciliationEngine {

    fun reconcile(
        storageRoots: List<StorageRoot>,
        indexedFiles: List<LibraryFile>,
        physicalFiles: List<DiscoveredPhysicalFile>
    ): ReconciliationSummary {
        val results = mutableListOf<ReconciliationItemResult>()

        val rootsById = storageRoots.associateBy { it.id }
        val physicalByLocation = physicalFiles.associateBy { "${it.storageId.value}:${it.relativePath.trimStart('/')}" }
        val physicalByFingerprint = physicalFiles.groupBy { it.cheapFingerprint }

        val matchedPhysicalKeys = mutableSetOf<String>()

        for (dbFile in indexedFiles) {
            val root = rootsById[dbFile.location.storageId]

            // Section 85: Check if storage is offline / permission required
            if (root == null || root.state == StorageAvailability.Offline || root.state == StorageAvailability.PermissionRequired) {
                results.add(
                    ReconciliationItemResult.StorageOffline(
                        fileId = dbFile.id,
                        storageId = dbFile.location.storageId
                    )
                )
                continue
            }

            val locationKey = "${dbFile.location.storageId.value}:${dbFile.location.normalizedPath}"
            val directPhysicalMatch = physicalByLocation[locationKey]

            if (directPhysicalMatch != null) {
                matchedPhysicalKeys.add(locationKey)

                // Check for size or timestamp discrepancy
                val sizeDelta = directPhysicalMatch.sizeBytes - dbFile.sizeBytes
                if (sizeDelta != 0L) {
                    results.add(
                        ReconciliationItemResult.ChangedFile(
                            fileId = dbFile.id,
                            sizeDelta = sizeDelta,
                            newTimestamp = Instant.ofEpochMilli(directPhysicalMatch.modifiedEpochMillis)
                        )
                    )
                } else {
                    results.add(ReconciliationItemResult.Consistent(dbFile.id))
                }
            } else {
                // File missing at original location. Check for Moved or Renamed file via fingerprint (Section 44, 45)
                val dbFingerprintStr = getFingerprintString(dbFile.fingerprint, dbFile.sizeBytes)
                val candidatePhysicalFiles = physicalByFingerprint[dbFingerprintStr] ?: emptyList()
                val movedCandidate = candidatePhysicalFiles.firstOrNull { candidate ->
                    val key = "${candidate.storageId.value}:${candidate.relativePath.trimStart('/')}"
                    !matchedPhysicalKeys.contains(key)
                }

                if (movedCandidate != null) {
                    val newLocKey = "${movedCandidate.storageId.value}:${movedCandidate.relativePath.trimStart('/')}"
                    matchedPhysicalKeys.add(newLocKey)

                    val newLocation = StorageLocation(movedCandidate.storageId, movedCandidate.relativePath)

                    if (movedCandidate.name != dbFile.displayName && movedCandidate.parentFolder == dbFile.location.parentPath) {
                        results.add(
                            ReconciliationItemResult.RenamedFile(
                                fileId = dbFile.id,
                                oldName = dbFile.displayName,
                                newName = movedCandidate.name
                            )
                        )
                    } else {
                        results.add(
                            ReconciliationItemResult.MovedFile(
                                fileId = dbFile.id,
                                oldLocation = dbFile.location,
                                newLocation = newLocation
                            )
                        )
                    }
                } else {
                    // Physical file is missing (Section 87)
                    results.add(
                        ReconciliationItemResult.MissingPhysicalFile(
                            fileId = dbFile.id,
                            lastLocation = dbFile.location
                        )
                    )
                }
            }
        }

        // Section 42: Find physical files present on storage that are unindexed in Room
        for (physical in physicalFiles) {
            val key = "${physical.storageId.value}:${physical.relativePath.trimStart('/')}"
            if (!matchedPhysicalKeys.contains(key) && physical.isMedia) {
                results.add(ReconciliationItemResult.UnindexedFile(physical))
            }
        }

        return ReconciliationSummary(
            results = results,
            consistentCount = results.count { it is ReconciliationItemResult.Consistent },
            missingCount = results.count { it is ReconciliationItemResult.MissingPhysicalFile },
            unindexedCount = results.count { it is ReconciliationItemResult.UnindexedFile },
            movedCount = results.count { it is ReconciliationItemResult.MovedFile },
            renamedCount = results.count { it is ReconciliationItemResult.RenamedFile },
            offlineCount = results.count { it is ReconciliationItemResult.StorageOffline },
            duplicateCount = results.count { it is ReconciliationItemResult.Duplicate },
            changedCount = results.count { it is ReconciliationItemResult.ChangedFile }
        )
    }

    private fun getFingerprintString(fp: FileFingerprint?, size: Long): String {
        return when (fp) {
            is FileFingerprint.FullHash -> fp.hash
            is FileFingerprint.PartialHash -> fp.hash
            else -> "${size}_known"
        }
    }
}
