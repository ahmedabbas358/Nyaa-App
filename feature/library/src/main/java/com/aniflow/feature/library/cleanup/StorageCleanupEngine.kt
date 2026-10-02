package com.aniflow.feature.library.cleanup

import java.io.File

data class OrphanCleanupCandidate(
    val filePath: String,
    val sizeBytes: Long,
    val modifiedEpochMillis: Long,
    val reason: String,
    val isSafeToAutoClean: Boolean
)

data class StorageCleanupReport(
    val orphanPartFiles: List<OrphanCleanupCandidate>,
    val emptyTempDirs: List<String>,
    val totalReclaimableBytes: Long
)

/**
 * StorageCleanupEngine (Sections 24, 67).
 * Identifies abandoned .part files, orphaned segmented temp directories, and stale caches.
 * Never auto-deletes permanent user media files without explicit user review.
 */
class StorageCleanupEngine {

    fun scanOrphanFiles(
        tempDir: File,
        activeTaskTempPaths: Set<String>,
        olderThanMillis: Long = 1000 * 60 * 60 * 24 // 24 hours threshold for orphan check
    ): StorageCleanupReport {
        if (!tempDir.exists() || !tempDir.isDirectory) {
            return StorageCleanupReport(emptyList(), emptyList(), 0L)
        }

        val orphanCandidates = mutableListOf<OrphanCleanupCandidate>()
        val emptyDirs = mutableListOf<String>()
        val now = System.currentTimeMillis()

        tempDir.walkTopDown().forEach { file ->
            if (file.isDirectory) {
                val children = file.list()
                if (children != null && children.isEmpty() && file.absolutePath != tempDir.absolutePath) {
                    emptyDirs.add(file.absolutePath)
                }
            } else {
                val isPartFile = file.name.endsWith(".part") || file.name.contains(".segment")
                val isTrackedActive = activeTaskTempPaths.contains(file.absolutePath)

                if (isPartFile && !isTrackedActive) {
                    val age = now - file.lastModified()
                    val isOldEnough = age > olderThanMillis

                    orphanCandidates.add(
                        OrphanCleanupCandidate(
                            filePath = file.absolutePath,
                            sizeBytes = file.length(),
                            modifiedEpochMillis = file.lastModified(),
                            reason = if (isOldEnough) "Abandoned download part older than 24h" else "Unlinked download part",
                            isSafeToAutoClean = isOldEnough
                        )
                    )
                }
            }
        }

        val reclaimableBytes = orphanCandidates.sumOf { it.sizeBytes }

        return StorageCleanupReport(
            orphanPartFiles = orphanCandidates,
            emptyTempDirs = emptyDirs,
            totalReclaimableBytes = reclaimableBytes
        )
    }

    fun purgeConfirmedOrphans(candidates: List<OrphanCleanupCandidate>): Int {
        var deletedCount = 0
        for (candidate in candidates) {
            if (candidate.isSafeToAutoClean) {
                val file = File(candidate.filePath)
                if (file.exists() && file.delete()) {
                    deletedCount++
                }
            }
        }
        return deletedCount
    }
}
