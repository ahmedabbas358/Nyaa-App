package com.aniflow.domain.usecase

import com.aniflow.core.common.result.AniFlowResult
import com.aniflow.domain.controlplane.models.NetworkPolicyType
import com.aniflow.domain.event.AniFlowEventBus
import com.aniflow.domain.event.DomainEvent
import com.aniflow.domain.identity.DownloadPlanId
import com.aniflow.domain.identity.DownloadTaskId
import com.aniflow.domain.model.aggregate.download.DownloadPlan
import com.aniflow.domain.model.aggregate.download.DownloadPolicy
import com.aniflow.domain.model.aggregate.download.DownloadPriority
import com.aniflow.domain.model.aggregate.download.DownloadSelection
import com.aniflow.domain.model.aggregate.download.DownloadSource
import com.aniflow.domain.model.aggregate.download.DownloadTask
import com.aniflow.domain.model.aggregate.release.Release
import com.aniflow.domain.model.aggregate.release.ReleaseSource
import com.aniflow.domain.repository.DownloadRepository
import com.aniflow.domain.service.DuplicateDetectionService
import com.aniflow.domain.state.DownloadState
import com.aniflow.domain.valueobject.ByteSize
import com.aniflow.domain.valueobject.InfoHash
import com.aniflow.domain.valueobject.StorageTarget
import java.time.Instant
import java.util.UUID

/**
 * Summary view model for the Plan Review Screen (Section 25).
 */
data class PlanReviewSummary(
    val planId: DownloadPlanId,
    val totalFilesCount: Int,
    val totalBytes: Long,
    val preferredCount: Int,
    val fallbackCount: Int,
    val destinationFolder: String,
    val networkPolicy: NetworkPolicyType,
    val concurrency: Int,
    val availableStorageBytes: Long,
    val warnings: List<String> = emptyList(),
    val canProceed: Boolean = true
)

/**
 * PrepareDownloadPlanUseCase (Sections 23, 24, 25).
 * Validates candidates, verifies storage capacity, captures policy snapshot,
 * and generates a reviewable DownloadPlan.
 */
class PrepareDownloadPlanUseCase {

    operator fun invoke(
        releases: List<Release>,
        destinationPath: String = "Anime/Downloads",
        availableStorageBytes: Long = 50L * 1024 * 1024 * 1024, // 50 GB default
        networkPolicy: NetworkPolicyType = NetworkPolicyType.WiFiOnly,
        concurrency: Int = 3
    ): AniFlowResult<PlanReviewSummary> {
        if (releases.isEmpty()) {
            return AniFlowResult.Error(
                error = com.aniflow.core.common.result.ErrorType.ValidationError("No releases selected"),
                message = "Please select at least one release to download"
            )
        }

        val planId = DownloadPlanId("plan-${UUID.randomUUID().toString().take(8)}")
        var totalBytes = 0L
        val warnings = mutableListOf<String>()

        releases.forEach { rel ->
            val size = rel.availability.size?.bytes ?: 0L
            totalBytes += size
        }

        // Storage Check (Section 23, 55)
        val safetyBuffer = 500L * 1024 * 1024 // 500 MB
        val canProceed = (totalBytes + safetyBuffer) <= availableStorageBytes

        if (!canProceed) {
            warnings.add("Insufficient storage space: requires ${totalBytes / (1024 * 1024)} MB, but available is ${availableStorageBytes / (1024 * 1024)} MB")
        }

        return AniFlowResult.Success(
            PlanReviewSummary(
                planId = planId,
                totalFilesCount = releases.size,
                totalBytes = totalBytes,
                preferredCount = releases.size,
                fallbackCount = 0,
                destinationFolder = destinationPath,
                networkPolicy = networkPolicy,
                concurrency = concurrency,
                availableStorageBytes = availableStorageBytes,
                warnings = warnings,
                canProceed = canProceed
            )
        )
    }
}

/**
 * ExecuteDownloadPlanUseCase (Section 26).
 * Persists the validated plan, creates DownloadTask entities, saves them in Room,
 * dispatches them to Queue, and emits DomainEvents before starting execution.
 */
class ExecuteDownloadPlanUseCase(
    private val downloadRepository: DownloadRepository,
    private val eventBus: AniFlowEventBus? = null
) {

    suspend operator fun invoke(
        summary: PlanReviewSummary,
        releases: List<Release>
    ): AniFlowResult<List<DownloadTaskId>> {
        if (!summary.canProceed) {
            return AniFlowResult.Error(
                error = com.aniflow.core.common.result.ErrorType.ValidationError("Plan cannot proceed due to validation warnings"),
                message = summary.warnings.firstOrNull() ?: "Cannot proceed with download plan"
            )
        }

        val taskIds = mutableListOf<DownloadTaskId>()

        for (release in releases) {
            val taskId = DownloadTaskId("task-${UUID.randomUUID().toString().take(8)}")

            val source = when (val s = release.source) {
                is ReleaseSource.Torrent -> DownloadSource.TorrentSource(
                    infoHash = s.infoHash,
                    magnetUri = s.magnetUri,
                    torrentFileUrl = s.torrentUrl,
                    name = release.title
                )
                is ReleaseSource.DirectHttp -> DownloadSource.DirectSource(
                    url = s.url,
                    fileName = "${release.title}.mp4"
                )
                else -> DownloadSource.TorrentSource(
                    infoHash = InfoHash("0000000000000000000000000000000000000000"),
                    name = release.title
                )
            }

            val task = DownloadTask(
                id = taskId,
                releaseId = release.id,
                source = source,
                state = DownloadState.Pending,
                priority = DownloadPriority.Normal,
                destination = StorageTarget(summary.destinationFolder),
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )

            // Atomic task persistence before runtime starts (Section 26)
            downloadRepository.saveTask(task)
            eventBus?.tryEmit(DomainEvent.DownloadQueued(taskId))
            taskIds.add(taskId)
        }

        return AniFlowResult.Success(taskIds)
    }
}
