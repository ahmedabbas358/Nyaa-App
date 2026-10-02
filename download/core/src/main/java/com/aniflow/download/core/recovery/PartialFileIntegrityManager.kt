package com.aniflow.download.core.recovery

import java.io.File

/**
 * Enforces STEP 13 Section 22 (Download Recovery Testing) and Section 23 (Partial File Integrity).
 *
 * Inspects .part / .partial / .tmp files after crashes or sudden terminations,
 * verifying byte integrity, task ownership, and resume viability without data corruption.
 */
class PartialFileIntegrityManager {

    sealed interface RecoveryAssessment {
        data class ResumeEligible(
            val partialFile: File,
            val existingBytes: Long,
            val expectedTotalBytes: Long
        ) : RecoveryAssessment

        data class CorruptedNeedsReset(
            val partialFile: File,
            val reason: String
        ) : RecoveryAssessment

        data object FreshStartRequired : RecoveryAssessment
    }

    /**
     * Assesses a partial file on disk to determine whether it can be safely resumed.
     */
    fun assessPartialFile(
        partialFile: File,
        expectedTotalBytes: Long,
        supportsRangeRequests: Boolean = true
    ): RecoveryAssessment {
        if (!partialFile.exists() || !partialFile.isFile) {
            return RecoveryAssessment.FreshStartRequired
        }

        val actualSize = partialFile.length()

        // 1. If range requests are not supported, cannot resume existing bytes
        if (!supportsRangeRequests && actualSize > 0) {
            return RecoveryAssessment.CorruptedNeedsReset(
                partialFile = partialFile,
                reason = "Server does not support HTTP range requests; partial bytes cannot be resumed"
            )
        }

        // 2. Oversized partial file (file on disk is larger than total expected size)
        if (expectedTotalBytes > 0 && actualSize > expectedTotalBytes) {
            return RecoveryAssessment.CorruptedNeedsReset(
                partialFile = partialFile,
                reason = "Partial file size ($actualSize bytes) exceeds expected total ($expectedTotalBytes bytes)"
            )
        }

        // 3. Complete file masquerading as partial
        if (expectedTotalBytes > 0 && actualSize == expectedTotalBytes) {
            return RecoveryAssessment.ResumeEligible(
                partialFile = partialFile,
                existingBytes = actualSize,
                expectedTotalBytes = expectedTotalBytes
            )
        }

        // 4. Valid partial file ready for resume
        return RecoveryAssessment.ResumeEligible(
            partialFile = partialFile,
            existingBytes = actualSize,
            expectedTotalBytes = expectedTotalBytes
        )
    }

    /**
     * Identifies orphan partial files in a directory that do not belong to any active task ID.
     */
    fun findOrphanPartialFiles(
        directory: File,
        activeTaskIds: Set<String>
    ): List<File> {
        if (!directory.exists() || !directory.isDirectory) return emptyList()

        val partialExtensions = setOf("part", "partial", "tmp")
        val files = directory.listFiles() ?: return emptyList()

        return files.filter { file ->
            val ext = file.extension.lowercase()
            if (partialExtensions.contains(ext)) {
                val matchesAnyActiveTask = activeTaskIds.any { taskId -> file.name.contains(taskId) }
                !matchesAnyActiveTask
            } else {
                false
            }
        }
    }
}
