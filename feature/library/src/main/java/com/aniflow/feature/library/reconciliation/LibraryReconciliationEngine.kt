package com.aniflow.feature.library.reconciliation

import com.aniflow.feature.library.scanner.ScannedFile

enum class ReconciliationState {
    Synced,
    StaleMissing,          // DB says file exists, but filesystem file is missing
    Unindexed,             // Filesystem has file, but DB has no record
    ModifiedExternally,    // File size or timestamp modified outside AniFlow
    MovedCandidate         // File was likely moved to a new path (same size & fingerprint)
}

data class ReconciledItem(
    val id: String,
    val currentPath: String,
    val state: ReconciliationState,
    val detectedScannedFile: ScannedFile? = null,
    val suggestedRelinkPath: String? = null
)

data class DatabaseFileRecord(
    val id: String,
    val libraryItemId: String,
    val path: String,
    val fileName: String,
    val sizeBytes: Long,
    val cheapFingerprint: String
)

data class ReconciliationReport(
    val totalChecked: Int,
    val syncedCount: Int,
    val staleCount: Int,
    val unindexedCount: Int,
    val modifiedCount: Int,
    val movedCandidatesCount: Int,
    val items: List<ReconciledItem>
)

/**
 * LibraryReconciliationEngine (Sections 62, 63, 64, 65, 66).
 * Periodically reconciles the Room Database index with actual filesystem / MediaStore state.
 * Detects missing files, unindexed files, external changes, and candidate moved files for relinking.
 */
class LibraryReconciliationEngine {

    fun reconcile(
        dbRecords: List<DatabaseFileRecord>,
        scannedFiles: List<ScannedFile>
    ): ReconciliationReport {
        val scannedByPath = scannedFiles.associateBy { it.relativePath }
        val scannedByFingerprint = scannedFiles.groupBy { it.cheapFingerprint }

        val reconciled = mutableListOf<ReconciledItem>()
        val matchedScannedPaths = mutableSetOf<String>()

        var synced = 0
        var stale = 0
        var modified = 0
        var moved = 0

        for (db in dbRecords) {
            val exactPathMatch = scannedByPath[db.path]

            if (exactPathMatch != null) {
                matchedScannedPaths.add(exactPathMatch.relativePath)
                if (exactPathMatch.cheapFingerprint == db.cheapFingerprint) {
                    reconciled.add(ReconciledItem(db.id, db.path, ReconciliationState.Synced, exactPathMatch))
                    synced++
                } else {
                    reconciled.add(ReconciledItem(db.id, db.path, ReconciliationState.ModifiedExternally, exactPathMatch))
                    modified++
                }
            } else {
                // Not found at exact path! Check if moved (same size/fingerprint elsewhere)
                val candidates = scannedByFingerprint[db.cheapFingerprint]?.filterNot { matchedScannedPaths.contains(it.relativePath) }
                val bestMovedCandidate = candidates?.firstOrNull()

                if (bestMovedCandidate != null) {
                    matchedScannedPaths.add(bestMovedCandidate.relativePath)
                    reconciled.add(
                        ReconciledItem(
                            id = db.id,
                            currentPath = db.path,
                            state = ReconciliationState.MovedCandidate,
                            detectedScannedFile = bestMovedCandidate,
                            suggestedRelinkPath = bestMovedCandidate.relativePath
                        )
                    )
                    moved++
                } else {
                    reconciled.add(ReconciledItem(db.id, db.path, ReconciliationState.StaleMissing))
                    stale++
                }
            }
        }

        // Any scanned file not matched in DB is Unindexed
        val unindexed = scannedFiles.filterNot { matchedScannedPaths.contains(it.relativePath) }
            .map { scanned ->
                ReconciledItem(
                    id = "unindexed-${scanned.cheapFingerprint}",
                    currentPath = scanned.relativePath,
                    state = ReconciliationState.Unindexed,
                    detectedScannedFile = scanned
                )
            }

        reconciled.addAll(unindexed)

        return ReconciliationReport(
            totalChecked = dbRecords.size + unindexed.size,
            syncedCount = synced,
            staleCount = stale,
            unindexedCount = unindexed.size,
            modifiedCount = modified,
            movedCandidatesCount = moved,
            items = reconciled
        )
    }
}
