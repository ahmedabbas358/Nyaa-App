package com.aniflow.domain.library.duplicates

import com.aniflow.domain.identity.FileFingerprint
import com.aniflow.domain.library.model.DuplicateComparison
import com.aniflow.domain.library.model.DuplicateMediaType
import com.aniflow.domain.library.model.DuplicateResolutionRecommendation
import com.aniflow.domain.library.model.LibraryFile
import kotlin.math.abs

/**
 * DuplicateMediaDetector (Section 48, 49, 50).
 * Detects exact duplicates, technical duplicates (alternative quality/codec), and different releases.
 * Rule: NEVER delete duplicates silently!
 */
class DuplicateMediaDetector {

    fun compareFiles(
        fileA: LibraryFile,
        fileB: LibraryFile
    ): DuplicateComparison? {
        if (fileA.id == fileB.id) return null

        val isSameSize = fileA.sizeBytes == fileB.sizeBytes
        val isSameFingerprint = fingerprintsMatch(fileA.fingerprint, fileB.fingerprint)

        // 1. Exact Duplicate: Same fingerprint or same exact size + display name
        if (isSameFingerprint && isSameFingerprint != null || (isSameSize && fileA.displayName.equals(fileB.displayName, ignoreCase = true))) {
            return DuplicateComparison(
                fileA = fileA,
                fileB = fileB,
                type = DuplicateMediaType.ExactDuplicate,
                recommendation = DuplicateResolutionRecommendation.KeepFileA // Can remove duplicate file B after user confirmation
            )
        }

        // 2. Check if both files belong to the same logical episode/item
        val isSameItem = fileA.libraryItemId != null && fileA.libraryItemId == fileB.libraryItemId
        if (isSameItem) {
            val metaA = fileA.mediaMetadata
            val metaB = fileB.mediaMetadata

            val isSameResolution = metaA?.height != null && metaA.height == metaB?.height
            val isDifferentCodec = metaA?.videoCodec != null && metaA.videoCodec != metaB?.videoCodec

            return if (isSameResolution && isDifferentCodec) {
                // Technical Duplicate (e.g. 1080p HEVC vs 1080p H.264) -> Alternative version
                DuplicateComparison(
                    fileA = fileA,
                    fileB = fileB,
                    type = DuplicateMediaType.TechnicalDuplicate,
                    recommendation = DuplicateResolutionRecommendation.KeepBoth
                )
            } else {
                // Different release of the same episode
                DuplicateComparison(
                    fileA = fileA,
                    fileB = fileB,
                    type = DuplicateMediaType.DifferentRelease,
                    recommendation = DuplicateResolutionRecommendation.AskUser
                )
            }
        }

        // 3. Fallback: If sizes are within 0.1% and names are very similar
        val sizeDelta = abs(fileA.sizeBytes - fileB.sizeBytes)
        if (sizeDelta < 1024 * 1024 && fileA.displayName.equals(fileB.displayName, ignoreCase = true)) {
            return DuplicateComparison(
                fileA = fileA,
                fileB = fileB,
                type = DuplicateMediaType.ExactDuplicate,
                recommendation = DuplicateResolutionRecommendation.AskUser
            )
        }

        return null
    }

    private fun fingerprintsMatch(fpA: FileFingerprint?, fpB: FileFingerprint?): Boolean? {
        if (fpA == null || fpB == null) return null
        return when {
            fpA is FileFingerprint.FullHash && fpB is FileFingerprint.FullHash ->
                fpA.algorithm.equals(fpB.algorithm, ignoreCase = true) && fpA.hash.equals(fpB.hash, ignoreCase = true)
            fpA is FileFingerprint.PartialHash && fpB is FileFingerprint.PartialHash ->
                fpA.algorithm.equals(fpB.algorithm, ignoreCase = true) && fpA.hash.equals(fpB.hash, ignoreCase = true)
            else -> null
        }
    }
}
