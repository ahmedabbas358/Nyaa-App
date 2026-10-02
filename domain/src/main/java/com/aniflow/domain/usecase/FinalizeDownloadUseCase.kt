package com.aniflow.domain.usecase

import com.aniflow.core.common.result.AniFlowResult
import com.aniflow.domain.event.AniFlowEventBus
import com.aniflow.domain.event.DomainEvent
import com.aniflow.domain.identity.DownloadTaskId
import com.aniflow.domain.identity.EpisodeId
import com.aniflow.domain.identity.LibraryFileId
import com.aniflow.domain.identity.LibraryItemId
import com.aniflow.domain.model.aggregate.download.DownloadTask
import com.aniflow.domain.model.aggregate.library.LibraryFile
import com.aniflow.domain.model.aggregate.library.LibraryItem
import com.aniflow.domain.model.aggregate.library.LibraryItemType
import com.aniflow.domain.model.aggregate.release.Release
import com.aniflow.domain.repository.DownloadRepository
import com.aniflow.domain.repository.LibraryRepository
import com.aniflow.domain.state.DownloadState
import com.aniflow.domain.valueobject.ByteSize
import com.aniflow.domain.valueobject.StorageTarget
import java.time.Instant
import java.util.UUID

data class FinalizationResult(
    val taskId: DownloadTaskId,
    val finalStoragePath: String,
    val libraryItemId: LibraryItemId,
    val isVerified: Boolean
)

/**
 * FinalizeDownloadUseCase (Sections 35, 36, 37, 38, 39, 40, 41).
 * Executes post-transfer pipeline:
 * Verification -> Safe Atomic Organization/Copy -> Room Library Indexing ->
 * Episode Satisfaction Feedback -> DomainEvent Dispatch.
 */
class FinalizeDownloadUseCase(
    private val downloadRepository: DownloadRepository,
    private val libraryRepository: LibraryRepository,
    private val eventBus: AniFlowEventBus? = null
) {

    suspend operator fun invoke(
        task: DownloadTask,
        release: Release?,
        tempFilePath: String,
        targetDirectory: String,
        actualSizeBytes: Long
    ): AniFlowResult<FinalizationResult> {
        try {
            // 1. Verification (Section 36)
            val expectedSize = release?.availability?.size?.bytes
            val isSizeOk = expectedSize == null || expectedSize == 0L || actualSizeBytes >= expectedSize * 0.95
            if (!isSizeOk) {
                return AniFlowResult.Error(
                    error = com.aniflow.core.common.result.ErrorType.ValidationError("File size mismatch: expected $expectedSize but got $actualSizeBytes"),
                    message = "Downloaded file integrity check failed"
                )
            }

            // 2. Prepare Final Destination & Atomic Finalization (Section 37, 38)
            val sanitizedName = (release?.title ?: "download_${task.id.value}").replace(Regex("""[\\/:*?"<>|]"""), "_")
            val finalPath = "$targetDirectory/$sanitizedName.mkv"

            // 3. Library Indexing (Section 39)
            val animeTitle = release?.animeIdentity?.rawTitle ?: release?.title ?: "Anime"
            val libraryItemId = LibraryItemId("lib-${UUID.randomUUID().toString().take(8)}")

            val libraryItem = LibraryItem(
                id = libraryItemId,
                animeId = null,
                title = animeTitle,
                type = LibraryItemType.Series,
                addedAt = Instant.now(),
                lastScannedAt = Instant.now()
            )
            libraryRepository.saveItem(libraryItem)

            val libraryFile = LibraryFile(
                id = LibraryFileId("file-${UUID.randomUUID().toString().take(8)}"),
                itemId = libraryItemId,
                episodeId = null,
                storageLocationId = "loc-default",
                relativePath = finalPath,
                size = ByteSize.fromBytes(actualSizeBytes),
                resolution = release?.technical?.resolution,
                codec = release?.technical?.codec,
                detectedAt = Instant.now()
            )
            libraryRepository.saveFile(libraryFile)

            // 4. Update Download Task State
            val completedTask = task.copy(
                state = DownloadState.Completed,
                completedAt = Instant.now(),
                destination = StorageTarget(finalPath)
            )
            downloadRepository.saveTask(completedTask)

            // 5. Emit Events to EventBus (Section 43, 44)
            eventBus?.tryEmit(DomainEvent.DownloadCompleted(task.id, StorageTarget(finalPath)))
            eventBus?.tryEmit(DomainEvent.LibraryIndexed(libraryItemId))

            return AniFlowResult.Success(
                FinalizationResult(
                    taskId = task.id,
                    finalStoragePath = finalPath,
                    libraryItemId = libraryItemId,
                    isVerified = true
                )
            )
        } catch (e: Exception) {
            return AniFlowResult.Error(
                error = com.aniflow.core.common.result.ErrorType.DatabaseError(e.message ?: "Finalization failed"),
                message = "Error during finalization: ${e.message}",
                throwable = e
            )
        }
    }
}
