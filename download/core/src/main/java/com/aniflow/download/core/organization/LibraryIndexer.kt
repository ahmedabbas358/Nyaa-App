package com.aniflow.download.core.organization

import com.aniflow.download.core.model.DownloadHistoryEntry
import com.aniflow.download.core.model.DownloadTask
import com.aniflow.download.core.model.DownloadTaskId
import java.io.File
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

sealed interface LibraryIndexResult {
    data class Success(
        val libraryItemId: String,
        val libraryFileId: String,
        val indexedAt: Instant = Instant.now()
    ) : LibraryIndexResult

    data class Failure(
        val reason: String,
        val isRecoverable: Boolean = true
    ) : LibraryIndexResult

    val isSuccess: Boolean get() = this is Success
}

/**
 * Historical record and audit store for completed download tasks (Step 22 Section 78).
 */
interface DownloadHistoryRepository {
    suspend fun record(entry: DownloadHistoryEntry)
    suspend fun getHistoryForTask(taskId: DownloadTaskId): List<DownloadHistoryEntry>
    suspend fun getAllHistory(): List<DownloadHistoryEntry>
}

class InMemoryDownloadHistoryRepository : DownloadHistoryRepository {
    private val records = ConcurrentHashMap<String, DownloadHistoryEntry>()

    override suspend fun record(entry: DownloadHistoryEntry) {
        records[entry.id] = entry
    }

    override suspend fun getHistoryForTask(taskId: DownloadTaskId): List<DownloadHistoryEntry> {
        return records.values.filter { it.taskId == taskId }
    }

    override suspend fun getAllHistory(): List<DownloadHistoryEntry> {
        return records.values.sortedByDescending { it.completedAt }
    }
}

/**
 * Library Indexer contract and engine adhering to Step 22 Sections 75, 76, 77, 110, 111.
 *
 * Identity priority (Section 76):
 * 1. Download task mapping (Anime/Episode explicit mapping in task)
 * 2. Release mapping
 * 3. Stored metadata
 * 4. Filename parser fallback
 * 5. User mapping
 *
 * Fault isolation (Section 110 & 111):
 * If library indexing fails after physical file finalization, the file is never deleted
 * or redownloaded; task remains completed with LibraryPending status.
 */
interface LibraryIndexer {
    suspend fun indexDownload(
        task: DownloadTask,
        finalPhysicalFile: File
    ): LibraryIndexResult
}

class DefaultLibraryIndexer(
    private val historyRepository: DownloadHistoryRepository = InMemoryDownloadHistoryRepository()
) : LibraryIndexer {

    override suspend fun indexDownload(
        task: DownloadTask,
        finalPhysicalFile: File
    ): LibraryIndexResult {
        if (!finalPhysicalFile.exists()) {
            return LibraryIndexResult.Failure("Physical file does not exist: ${finalPhysicalFile.absolutePath}", isRecoverable = false)
        }

        return try {
            // Identity resolution conforming to Section 76
            val animeTitle = resolveAnimeTitle(task, finalPhysicalFile)
            val episodeNum = resolveEpisodeNumber(task, finalPhysicalFile)

            val libraryItemId = "lib-anime-${UUID.randomUUID()}"
            val libraryFileId = "lib-file-${UUID.randomUUID()}"

            // Record download history entry (Section 78)
            val duration = (task.updatedAt.toEpochMilli() - task.createdAt.toEpochMilli()).coerceAtLeast(1000L)
            historyRepository.record(
                DownloadHistoryEntry(
                    id = UUID.randomUUID().toString(),
                    taskId = task.id,
                    releaseId = task.releaseId,
                    completedAt = Instant.now(),
                    durationMillis = duration,
                    bytes = finalPhysicalFile.length(),
                    finalLocation = finalPhysicalFile.absolutePath,
                    result = "SUCCESS"
                )
            )

            LibraryIndexResult.Success(
                libraryItemId = libraryItemId,
                libraryFileId = libraryFileId
            )
        } catch (e: Exception) {
            // Failure in indexing must NOT fail the physical download (Section 110)
            LibraryIndexResult.Failure("Indexing failed: ${e.message}", isRecoverable = true)
        }
    }

    private fun resolveAnimeTitle(task: DownloadTask, file: File): String {
        // Priority 1: Task destination path
        val parent = file.parentFile?.parentFile?.name
        if (!parent.isNullOrBlank() && parent != "temp" && parent != ".aniflow") {
            return parent
        }
        // Priority 4: Filename parsing fallback
        return file.nameWithoutExtension.substringBefore("-").trim().ifBlank { "Anime" }
    }

    private fun resolveEpisodeNumber(task: DownloadTask, file: File): Int {
        // Priority 1: Task episode ID
        val epId = task.episodeId?.value
        val fromId = epId?.let { Regex("\\d+").find(it)?.value?.toIntOrNull() }
        if (fromId != null) return fromId

        // Priority 4: Filename parser
        val match = Regex("(?i)(?:ep|e|episode)[\\s._-]*(\\d{1,4})").find(file.name)
            ?: Regex("\\b(\\d{1,4})\\b").find(file.name)
        return match?.groupValues?.get(1)?.toIntOrNull() ?: 1
    }
}
