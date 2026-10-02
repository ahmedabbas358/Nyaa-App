package com.aniflow.download.core.organization

import com.aniflow.platform.storage.CrossVolumeMoveEngine
import com.aniflow.platform.storage.MoveResult
import com.aniflow.platform.storage.model.StorageFile
import com.aniflow.platform.storage.model.StorageLocationId
import com.aniflow.platform.storage.model.StorageTarget
import com.aniflow.platform.storage.provider.StorageProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant

enum class OrganizationItemStatus {
    Pending,
    Moving,
    Verified,
    Completed,
    Failed,
    Skipped
}

data class OrganizationJobItem(
    val id: String,
    val sourceFile: StorageFile,
    val sourceProvider: StorageProvider,
    val namingContext: MediaNamingContext,
    val targetLocationId: StorageLocationId,
    val targetProvider: StorageProvider,
    val status: OrganizationItemStatus = OrganizationItemStatus.Pending,
    val resultFile: StorageFile? = null,
    val errorMessage: String? = null
)

data class OrganizationJob(
    val jobId: String,
    val items: List<OrganizationJobItem>,
    val createdAt: Instant = Instant.now(),
    val completedAt: Instant? = null
) {
    val totalItems: Int get() = items.size
    val completedCount: Int get() = items.count { it.status == OrganizationItemStatus.Completed }
    val failedCount: Int get() = items.count { it.status == OrganizationItemStatus.Failed }
    val progress: Float get() = if (totalItems > 0) completedCount.toFloat() / totalItems.toFloat() else 0f
}

/**
 * FileOrganizationEngine (Sections 27, 28, 134, 135, 136, 137).
 * Coordinates directory/file naming resolution, collision checks, cross-volume finalization,
 * and bulk library organization jobs.
 */
class FileOrganizationEngine(
    private val templateEngine: NamingTemplateEngine = NamingTemplateEngine(),
    private val collisionEngine: FileCollisionEngine = FileCollisionEngine(),
    private val crossVolumeMoveEngine: CrossVolumeMoveEngine = CrossVolumeMoveEngine()
) {

    fun planTarget(
        context: MediaNamingContext,
        targetLocationId: StorageLocationId,
        directoryTemplate: String? = null,
        fileTemplate: String? = null
    ): Pair<StorageTarget, String> {
        val dir = templateEngine.formatDirectoryPath(
            context = context,
            template = directoryTemplate ?: templateEngine.defaultDirectoryTemplate
        )
        val fileName = templateEngine.formatFileName(
            context = context,
            template = fileTemplate ?: templateEngine.defaultFileTemplate
        )
        return StorageTarget(locationId = targetLocationId, relativePath = dir) to fileName
    }

    suspend fun finalizeFile(
        sourceProvider: StorageProvider,
        sourceFile: StorageFile,
        targetLocationId: StorageLocationId,
        targetProvider: StorageProvider,
        namingContext: MediaNamingContext,
        collisionPolicy: CollisionPolicy = CollisionPolicy.KeepBoth,
        directoryTemplate: String? = null,
        fileTemplate: String? = null
    ): MoveResult {
        val (targetDir, targetFileName) = planTarget(
            context = namingContext,
            targetLocationId = targetLocationId,
            directoryTemplate = directoryTemplate,
            fileTemplate = fileTemplate
        )

        // Check for collision
        val targetExists = targetProvider.exists(StorageTarget(targetLocationId, "${targetDir.relativePath}/$targetFileName"))
        val existingFile: StorageFile? = if (targetExists) {
            StorageFile(
                name = targetFileName,
                relativePath = "${targetDir.relativePath}/$targetFileName",
                persistentUri = "",
                sizeBytes = 0L
            )
        } else null

        val incomingAttributes = FileMediaAttributes(
            fileName = targetFileName,
            sizeBytes = sourceFile.sizeBytes,
            modifiedEpochMillis = sourceFile.lastModified.toEpochMilli(),
            resolution = namingContext.resolution,
            codec = namingContext.codec
        )

        val resolution = collisionEngine.resolveCollision(
            incomingName = targetFileName,
            existingFile = existingFile,
            existingAttributes = null,
            incomingAttributes = incomingAttributes,
            preferredPolicy = collisionPolicy
        )

        if (!resolution.proceedWithTransfer) {
            return MoveResult(
                isSuccess = true,
                movedFile = existingFile,
                errorMessage = "Transfer skipped by collision policy ${resolution.policy}"
            )
        }

        val finalTarget = StorageTarget(
            locationId = targetLocationId,
            relativePath = targetDir.relativePath
        )

        return crossVolumeMoveEngine.move(
            sourceProvider = sourceProvider,
            sourceFile = sourceFile.copy(name = resolution.resolvedFileName),
            targetProvider = targetProvider,
            target = finalTarget
        )
    }

    suspend fun executeBulkJob(
        job: OrganizationJob,
        onProgress: ((progress: Float, currentItem: OrganizationJobItem) -> Unit)? = null
    ): OrganizationJob {
        val updatedItems = mutableListOf<OrganizationJobItem>()

        for (item in job.items) {
            if (item.status == OrganizationItemStatus.Completed) {
                updatedItems.add(item)
                continue
            }

            val inProgressItem = item.copy(status = OrganizationItemStatus.Moving)
            onProgress?.invoke(
                (updatedItems.size.toFloat() / job.totalItems.toFloat()),
                inProgressItem
            )

            val moveResult = finalizeFile(
                sourceProvider = item.sourceProvider,
                sourceFile = item.sourceFile,
                targetLocationId = item.targetLocationId,
                targetProvider = item.targetProvider,
                namingContext = item.namingContext
            )

            val finishedItem = if (moveResult.isSuccess) {
                inProgressItem.copy(
                    status = OrganizationItemStatus.Completed,
                    resultFile = moveResult.movedFile
                )
            } else {
                inProgressItem.copy(
                    status = OrganizationItemStatus.Failed,
                    errorMessage = moveResult.errorMessage
                )
            }

            updatedItems.add(finishedItem)
        }

        return job.copy(
            items = updatedItems,
            completedAt = Instant.now()
        )
    }
}
