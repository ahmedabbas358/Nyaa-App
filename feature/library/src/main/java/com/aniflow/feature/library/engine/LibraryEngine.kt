package com.aniflow.feature.library.engine

import com.aniflow.domain.identity.LibraryFileId
import com.aniflow.domain.identity.LibraryItemId
import com.aniflow.domain.identity.MediaIdentity
import com.aniflow.domain.model.aggregate.library.LibraryFile
import com.aniflow.domain.model.aggregate.library.LibraryItem
import com.aniflow.domain.repository.LibraryRepository
import com.aniflow.domain.state.LibraryItemState
import com.aniflow.domain.valueobject.ByteSize
import com.aniflow.domain.valueobject.StorageTarget
import com.aniflow.feature.library.analytics.LibraryHealthReport
import com.aniflow.feature.library.analytics.StorageAnalyticsEngine
import com.aniflow.feature.library.analytics.StorageCategoryBreakdown
import com.aniflow.feature.library.cleanup.StorageCleanupEngine
import com.aniflow.feature.library.cleanup.StorageCleanupReport
import com.aniflow.feature.library.model.MediaProbeResult
import com.aniflow.feature.library.parser.MediaFilenameParser
import com.aniflow.feature.library.probe.DefaultMediaProbe
import com.aniflow.feature.library.probe.MediaProbe
import com.aniflow.feature.library.queue.IndexPriority
import com.aniflow.feature.library.queue.IndexTask
import com.aniflow.feature.library.queue.LibraryIndexQueue
import com.aniflow.feature.library.reconciliation.DatabaseFileRecord
import com.aniflow.feature.library.reconciliation.LibraryReconciliationEngine
import com.aniflow.feature.library.reconciliation.ReconciliationReport
import com.aniflow.feature.library.resolver.LibraryIdentityResolver
import com.aniflow.feature.library.resolver.ResolvedMediaIdentity
import com.aniflow.feature.library.scanner.LibraryScanner
import com.aniflow.feature.library.scanner.ScanProgress
import com.aniflow.feature.library.scanner.ScannedFile
import com.aniflow.feature.library.search.LibraryFilterCriteria
import com.aniflow.feature.library.search.LibrarySearchEngine
import com.aniflow.feature.library.search.LibrarySortBy
import com.aniflow.feature.library.search.SearchableLibraryItem
import com.aniflow.feature.library.search.SortDirection
import com.aniflow.platform.storage.model.StorageFile
import com.aniflow.platform.storage.model.StorageLocation
import com.aniflow.platform.storage.provider.StorageProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * LibraryEngine (Section 43, 44, 175, 178).
 * Central coordinator responsible for Indexing, Scanning, Identifying, Grouping,
 * Reconciling, Searching, and Storage Analytics.
 * Room DB acts purely as the Index; physical storage remains the true source of truth.
 */
class LibraryEngine(
    private val libraryRepository: LibraryRepository? = null,
    private val mediaProbe: MediaProbe = DefaultMediaProbe(),
    private val scanner: LibraryScanner = LibraryScanner(mediaProbe),
    private val resolver: LibraryIdentityResolver = LibraryIdentityResolver(MediaFilenameParser()),
    private val reconciliationEngine: LibraryReconciliationEngine = LibraryReconciliationEngine(),
    private val searchEngine: LibrarySearchEngine = LibrarySearchEngine(),
    private val analyticsEngine: StorageAnalyticsEngine = StorageAnalyticsEngine(),
    private val cleanupEngine: StorageCleanupEngine = StorageCleanupEngine(),
    private val indexQueue: LibraryIndexQueue = LibraryIndexQueue()
) {

    suspend fun indexSingleFile(
        file: StorageFile,
        rootStorageId: String,
        priority: IndexPriority = IndexPriority.High,
        customMapping: ResolvedMediaIdentity? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val scanned = ScannedFile(
            name = file.name,
            relativePath = file.relativePath,
            sizeBytes = file.sizeBytes,
            modifiedEpochMillis = file.lastModified.toEpochMilli(),
            cheapFingerprint = "${file.sizeBytes}_${file.lastModified.toEpochMilli()}",
            parentFolder = file.relativePath.substringBeforeLast('/', "").substringAfterLast('/').ifBlank { null }
        )

        val resolved = customMapping ?: resolver.resolve(scanned)

        val itemId = LibraryItemId("item-${resolved.animeTitle.hashCode()}-${resolved.seasonNumber}")
        if (libraryRepository != null) {
            val existingItem = libraryRepository.getItemById(itemId)

            if (existingItem == null) {
                val newItem = LibraryItem(
                    id = itemId,
                    mediaIdentity = MediaIdentity(
                        animeId = null,
                        seasonNumber = null,
                        episodeRange = null,
                        canonicalTitle = resolved.animeTitle
                    ),
                    state = LibraryItemState.Indexed,
                    location = StorageTarget(rootStorageId)
                )
                libraryRepository.saveItem(newItem)
            }

            // Run non-blocking probe
            val probeResult: MediaProbeResult = mediaProbe.probe(file)

            val fileRecord = LibraryFile(
                id = LibraryFileId(UUID.randomUUID().toString()),
                libraryItemId = itemId,
                path = file.relativePath,
                fileName = file.name,
                size = ByteSize(file.sizeBytes)
            )

            libraryRepository.saveFile(fileRecord)
        }
        true
    }

    suspend fun reconcile(
        scannedFiles: List<ScannedFile>
    ): ReconciliationReport = withContext(Dispatchers.IO) {
        // Build db records
        val allItems = mutableListOf<DatabaseFileRecord>()
        // In actual repository, would fetch all files from libraryDao
        reconciliationEngine.reconcile(allItems, scannedFiles)
    }

    fun search(
        items: List<SearchableLibraryItem>,
        criteria: LibraryFilterCriteria,
        sortBy: LibrarySortBy = LibrarySortBy.Name,
        direction: SortDirection = SortDirection.Ascending
    ): List<SearchableLibraryItem> {
        return searchEngine.filterAndSort(items, criteria, sortBy, direction)
    }

    fun getCategoryAnalytics(
        items: List<SearchableLibraryItem>,
        tempBytes: Long,
        freeBytes: Long
    ): StorageCategoryBreakdown {
        return analyticsEngine.calculateCategoryBreakdown(items, tempBytes, freeBytes)
    }

    fun auditHealth(
        totalFiles: Int,
        missing: Int,
        broken: Int,
        inaccessible: Int
    ): LibraryHealthReport {
        return analyticsEngine.diagnoseLibraryHealth(totalFiles, missing, broken, inaccessible)
    }

    fun checkOrphans(tempDirectory: File, activePaths: Set<String>): StorageCleanupReport {
        return cleanupEngine.scanOrphanFiles(tempDirectory, activePaths)
    }
}
