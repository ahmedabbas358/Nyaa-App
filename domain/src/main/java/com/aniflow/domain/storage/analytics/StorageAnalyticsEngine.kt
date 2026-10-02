package com.aniflow.domain.storage.analytics

import com.aniflow.domain.identity.StorageId
import com.aniflow.domain.library.model.DuplicateComparison
import com.aniflow.domain.library.model.DuplicateMediaType
import com.aniflow.domain.library.model.LibraryFile
import com.aniflow.domain.library.model.LibraryItem
import com.aniflow.domain.library.model.LibraryItemType
import com.aniflow.domain.storage.model.StorageAnalytics
import com.aniflow.domain.storage.model.StorageFileSummary
import com.aniflow.domain.storage.model.StorageHealth
import com.aniflow.domain.storage.model.StorageRoot
import com.aniflow.domain.storage.model.StorageThresholds

/**
 * StorageAnalyticsEngine (Section 51, 52, 53, 91, 92, 93, 94).
 * Generates aggregated library size metrics, breakdowns by anime and storage root,
 * large files overview, and storage health thresholds.
 */
class StorageAnalyticsEngine(
    private val thresholds: StorageThresholds = StorageThresholds()
) {

    fun computeAnalytics(
        items: List<LibraryItem>,
        files: List<LibraryFile>,
        duplicates: List<DuplicateComparison> = emptyList()
    ): StorageAnalytics {
        val totalBytes = files.sumOf { it.sizeBytes }
        val filesByItem = files.groupBy { it.libraryItemId }

        val animeBreakdown = mutableMapOf<String, Long>()
        val seasonBreakdown = mutableMapOf<String, Long>()
        var unidentifiedBytes = 0L

        for (item in items) {
            val itemFiles = filesByItem[item.id] ?: emptyList()
            val itemBytes = itemFiles.sumOf { it.sizeBytes }

            if (item.type == LibraryItemType.Unidentified) {
                unidentifiedBytes += itemBytes
            } else {
                val title = item.title ?: "Unknown"
                animeBreakdown[title] = (animeBreakdown[title] ?: 0L) + itemBytes

                val seasonKey = "$title (Season ${item.seasonId?.value ?: "1"})"
                seasonBreakdown[seasonKey] = (seasonBreakdown[seasonKey] ?: 0L) + itemBytes
            }
        }

        val storageDist = files.groupBy { it.location.storageId }
            .mapValues { (_, rootFiles) -> rootFiles.sumOf { it.sizeBytes } }

        val largest = files.sortedByDescending { it.sizeBytes }
            .take(20)
            .map { file ->
                val parentItem = items.firstOrNull { it.id == file.libraryItemId }
                StorageFileSummary(
                    fileId = file.id,
                    fileName = file.displayName,
                    animeTitle = parentItem?.title,
                    episodeNumber = null,
                    sizeBytes = file.sizeBytes,
                    location = file.location
                )
            }

        // Section 95: Calculate duplicate wasted bytes
        val duplicateWasted = duplicates
            .filter { it.type == DuplicateMediaType.ExactDuplicate }
            .sumOf { it.fileB.sizeBytes }

        return StorageAnalytics(
            totalLibrarySizeBytes = totalBytes,
            animeBreakdown = animeBreakdown,
            seasonBreakdown = seasonBreakdown,
            largestFiles = largest,
            storageRootDistribution = storageDist,
            unidentifiedSizeBytes = unidentifiedBytes,
            duplicateWastedBytes = duplicateWasted
        )
    }

    fun computeStorageHealth(
        root: StorageRoot,
        libraryFilesForRoot: List<LibraryFile>,
        downloadTempBytes: Long = 0L,
        orphanedTempBytes: Long = 0L
    ): StorageHealth {
        val libraryUsedBytes = libraryFilesForRoot.sumOf { it.sizeBytes }
        val freeBytes = root.freeSpaceBytes

        val isLow = freeBytes < thresholds.warningThresholdBytes
        val isCritical = freeBytes < thresholds.criticalThresholdBytes

        return StorageHealth(
            storageId = root.id,
            name = root.name,
            totalBytes = root.totalSpaceBytes,
            freeBytes = freeBytes,
            libraryUsedBytes = libraryUsedBytes,
            downloadTempBytes = downloadTempBytes,
            orphanedTempBytes = orphanedTempBytes,
            isLowStorage = isLow,
            isCriticalStorage = isCritical
        )
    }
}
