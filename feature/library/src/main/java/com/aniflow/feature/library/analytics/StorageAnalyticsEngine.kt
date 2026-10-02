package com.aniflow.feature.library.analytics

import com.aniflow.feature.library.search.SearchableLibraryItem

data class StorageCategoryBreakdown(
    val animeBytes: Long,
    val movieBytes: Long,
    val otherBytes: Long,
    val temporaryBytes: Long,
    val freeBytes: Long
)

data class AnimeStorageUsage(
    val animeTitle: String,
    val totalBytes: Long,
    val fileCount: Int
)

data class LibraryHealthReport(
    val indexedFilesCount: Int,
    val brokenRecordsCount: Int,
    val missingFilesCount: Int,
    val inaccessibleStorageCount: Int,
    val isHealthy: Boolean
)

/**
 * StorageAnalyticsEngine (Sections 100, 101, 102, 103, 104).
 * Calculates real storage metrics, per-series storage footprint, and system health status.
 */
class StorageAnalyticsEngine {

    fun calculateCategoryBreakdown(
        items: List<SearchableLibraryItem>,
        temporaryBytes: Long,
        totalFreeBytes: Long
    ): StorageCategoryBreakdown {
        var animeBytes = 0L
        var movieBytes = 0L
        var otherBytes = 0L

        for (item in items) {
            when {
                item.animeTitle.contains("Movie", ignoreCase = true) -> movieBytes += item.sizeBytes
                item.animeTitle.equals("Unidentified", ignoreCase = true) -> otherBytes += item.sizeBytes
                else -> animeBytes += item.sizeBytes
            }
        }

        return StorageCategoryBreakdown(
            animeBytes = animeBytes,
            movieBytes = movieBytes,
            otherBytes = otherBytes,
            temporaryBytes = temporaryBytes,
            freeBytes = totalFreeBytes
        )
    }

    fun calculateSizeByAnime(items: List<SearchableLibraryItem>): List<AnimeStorageUsage> {
        return items.groupBy { it.animeTitle }
            .map { (title, files) ->
                AnimeStorageUsage(
                    animeTitle = title,
                    totalBytes = files.sumOf { it.sizeBytes },
                    fileCount = files.size
                )
            }
            .sortedByDescending { it.totalBytes }
    }

    fun diagnoseLibraryHealth(
        totalIndexedFiles: Int,
        missingFiles: Int,
        brokenRecords: Int,
        inaccessibleStorages: Int
    ): LibraryHealthReport {
        val isHealthy = missingFiles == 0 && brokenRecords == 0 && inaccessibleStorages == 0
        return LibraryHealthReport(
            indexedFilesCount = totalIndexedFiles,
            brokenRecordsCount = brokenRecords,
            missingFilesCount = missingFiles,
            inaccessibleStorageCount = inaccessibleStorages,
            isHealthy = isHealthy
        )
    }
}
