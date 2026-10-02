package com.aniflow.download.core.planner

import com.aniflow.domain.identity.ReleaseId
import com.aniflow.download.core.model.DownloadPlan
import com.aniflow.download.core.model.DownloadPlanId
import com.aniflow.download.core.model.DownloadSource
import com.aniflow.download.core.model.DownloadTask
import com.aniflow.download.core.model.DownloadTaskId
import com.aniflow.download.core.model.DownloadTaskState
import com.aniflow.download.core.model.DuplicateDecision
import com.aniflow.download.core.model.DuplicateResult
import java.io.File

interface FileExistenceChecker {
    fun exists(absolutePath: String): Boolean
}

class DefaultFileExistenceChecker : FileExistenceChecker {
    override fun exists(absolutePath: String): Boolean = File(absolutePath).exists()
}

/**
 * Download Identity uniquely identifying a release download intent (Section 93).
 */
data class DownloadIdentity(
    val providerReleaseId: String?,
    val infoHash: String?,
    val targetFilename: String
)

/**
 * Multi-layer Duplicate Detection Engine adhering strictly to Step 22 Sections 9, 10, 11, 12, 92, 93.
 *
 * Layers in strict priority:
 * 1. Existing DownloadTask
 * 2. Existing DownloadPlan
 * 3. Provider Release ID
 * 4. InfoHash
 * 5. Download identity
 * 6. Existing physical file
 * 7. Optional fingerprint
 */
class DuplicateDetector(
    private val fileChecker: FileExistenceChecker = DefaultFileExistenceChecker()
) {

    /**
     * Comprehensive multi-layer duplicate check returning strongly-typed DuplicateResult (Section 10).
     */
    fun detectDuplicate(
        releaseId: ReleaseId,
        providerReleaseId: String?,
        source: DownloadSource,
        targetFilePath: String,
        existingTasks: List<DownloadTask> = emptyList(),
        existingPlans: List<DownloadPlan> = emptyList()
    ): DuplicateResult {
        val incomingInfoHash = extractInfoHash(source)

        // 1. Existing DownloadTask layer
        val taskByRelease = existingTasks.firstOrNull { it.releaseId == releaseId }
        if (taskByRelease != null && !taskByRelease.isFinished) {
            return DuplicateResult.ExistingTask(taskByRelease.id, taskByRelease.state)
        }

        // 2. Existing DownloadPlan layer
        val matchingPlan = existingPlans.firstOrNull { plan ->
            plan.items.any { it.releaseId == releaseId }
        }
        if (matchingPlan != null) {
            return DuplicateResult.ExistingPlan(matchingPlan.id)
        }

        // 3. Provider Release ID in existing active/completed tasks
        if (!providerReleaseId.isNullOrBlank()) {
            val matchingProviderTask = existingTasks.firstOrNull {
                it.releaseId?.value == providerReleaseId
            }
            if (matchingProviderTask != null) {
                return DuplicateResult.SameRelease(releaseId)
            }
        }

        // 4. InfoHash layer
        if (incomingInfoHash != null) {
            val matchingHashTask = existingTasks.firstOrNull { task ->
                val taskHash = extractInfoHash(task.source)
                taskHash != null && taskHash.equals(incomingInfoHash, ignoreCase = true)
            }
            if (matchingHashTask != null) {
                return DuplicateResult.SameTorrent(incomingInfoHash)
            }
        }

        // 5. Download identity layer (matching target filename & active task)
        val targetFileName = File(targetFilePath).name
        val matchingFilenameTask = existingTasks.firstOrNull {
            it.destination.filename.equals(targetFileName, ignoreCase = true) && !it.isFinished
        }
        if (matchingFilenameTask != null) {
            return DuplicateResult.ExistingTask(matchingFilenameTask.id, matchingFilenameTask.state)
        }

        // 6. Existing physical file layer
        if (fileChecker.exists(targetFilePath)) {
            return DuplicateResult.ExistingFile(targetFilePath)
        }

        // 7. Partial file layer (possible duplicate in progress)
        val partialPath = "$targetFilePath.aniflow.part"
        if (fileChecker.exists(partialPath)) {
            return DuplicateResult.ExistingFile(partialPath)
        }

        return DuplicateResult.None
    }

    /**
     * Compatibility decision mapper returning legacy DuplicateDecision.
     */
    fun checkDuplicate(
        releaseId: ReleaseId,
        providerReleaseId: String?,
        source: DownloadSource,
        targetFilePath: String,
        existingTasks: List<DownloadTask>
    ): DuplicateDecision {
        val result = detectDuplicate(
            releaseId = releaseId,
            providerReleaseId = providerReleaseId,
            source = source,
            targetFilePath = targetFilePath,
            existingTasks = existingTasks
        )

        return when (result) {
            is DuplicateResult.None -> DuplicateDecision.NotDuplicate
            is DuplicateResult.ExistingTask -> mapTaskStateToDuplicate(result.state)
            is DuplicateResult.ExistingFile -> {
                if (result.filePath.endsWith(".part")) DuplicateDecision.PossibleDuplicate
                else DuplicateDecision.AlreadyDownloaded
            }
            is DuplicateResult.ExistingPlan -> DuplicateDecision.AlreadyQueued
            is DuplicateResult.SameTorrent -> DuplicateDecision.AlreadyDownloading
            is DuplicateResult.SameRelease -> DuplicateDecision.AlreadyQueued
        }
    }

    private fun extractInfoHash(source: DownloadSource): String? = when (source) {
        is DownloadSource.Magnet -> source.infoHash?.hexString ?: source.uri.extractInfoHash?.hexString
        is DownloadSource.TorrentFile -> source.infoHash?.hexString
        is DownloadSource.MagnetSource -> source.infoHash
        is DownloadSource.TorrentFileSource -> source.infoHash
        else -> null
    }

    private fun mapTaskStateToDuplicate(state: DownloadTaskState): DuplicateDecision = when (state) {
        DownloadTaskState.Completed -> DuplicateDecision.AlreadyDownloaded
        DownloadTaskState.Downloading, DownloadTaskState.Starting, DownloadTaskState.Verifying,
        DownloadTaskState.Organizing, DownloadTaskState.Moving ->
            DuplicateDecision.AlreadyDownloading
        DownloadTaskState.Queued, DownloadTaskState.Pending, DownloadTaskState.Waiting,
        DownloadTaskState.WaitingForNetwork, DownloadTaskState.WaitingForStorage, DownloadTaskState.Paused ->
            DuplicateDecision.AlreadyQueued
        DownloadTaskState.Failed, DownloadTaskState.Cancelled, DownloadTaskState.RetryScheduled,
        DownloadTaskState.Retrying, DownloadTaskState.Removed ->
            DuplicateDecision.PossibleDuplicate
    }
}
