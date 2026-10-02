package com.aniflow.domain.usecase

import com.aniflow.core.common.result.AniFlowResult
import com.aniflow.domain.controlplane.models.AdvancedRule
import com.aniflow.domain.controlplane.models.AutomationAuditLog
import com.aniflow.domain.controlplane.models.AutomationSafetyLevel
import com.aniflow.domain.controlplane.models.AutomationTrigger
import com.aniflow.domain.controlplane.service.AutomationEngine
import com.aniflow.domain.controlplane.service.CandidateReleaseContext
import com.aniflow.domain.event.AniFlowEventBus
import com.aniflow.domain.event.DomainEvent
import com.aniflow.domain.identity.DownloadPlanId
import com.aniflow.domain.identity.DownloadTaskId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.model.aggregate.download.DownloadPriority
import com.aniflow.domain.model.aggregate.download.DownloadSource
import com.aniflow.domain.model.aggregate.download.DownloadTask
import com.aniflow.domain.repository.DownloadRepository
import com.aniflow.domain.state.DownloadState
import com.aniflow.domain.valueobject.InfoHash
import com.aniflow.domain.valueobject.StorageTarget
import java.time.Instant
import java.util.UUID

data class AutomationWorkflowResult(
    val trigger: AutomationTrigger,
    val tasksQueued: Int,
    val isBlockedBySafety: Boolean,
    val safetyReason: String?,
    val auditLog: AutomationAuditLog
)

/**
 * AutomationWorkflowCoordinator (Sections 41, 42, 45, 46).
 * End-to-end automation orchestrator:
 * Event Trigger -> Rule Evaluation -> Idempotency Check -> Safety Gate ->
 * Plan Creation -> Task Persistence -> Queue Dispatch -> Audit Logging.
 */
class AutomationWorkflowCoordinator(
    private val automationEngine: AutomationEngine = AutomationEngine(),
    private val downloadRepository: DownloadRepository,
    private val eventBus: AniFlowEventBus? = null
) {

    suspend fun handleTrigger(
        trigger: AutomationTrigger,
        candidates: List<CandidateReleaseContext>,
        rules: List<AdvancedRule>,
        storageFreeBytes: Long = Long.MAX_VALUE
    ): AniFlowResult<AutomationWorkflowResult> {
        val existingTasks = downloadRepository.getAllTasks()
        val activeKeys = existingTasks.mapNotNull { it.releaseId?.value }.toSet()

        // 1. Evaluate Trigger through Rule Engine and Safety Gates (Section 42)
        val plan = automationEngine.evaluateTrigger(
            trigger = trigger,
            candidates = candidates,
            rules = rules,
            activeTaskIdentifiers = activeKeys,
            storageFreeBytes = storageFreeBytes
        )

        // 2. Safety Gate Check: block execution if safety says RequireConfirmation or AlwaysConfirm (Section 22)
        if (plan.isBlockedBySafety || plan.safetyLevel == AutomationSafetyLevel.RequireConfirmation || plan.safetyLevel == AutomationSafetyLevel.AlwaysConfirm) {
            return AniFlowResult.Success(
                AutomationWorkflowResult(
                    trigger = trigger,
                    tasksQueued = 0,
                    isBlockedBySafety = true,
                    safetyReason = plan.safetyReason ?: "Safety confirmation required for batch operation",
                    auditLog = plan.auditLog
                )
            )
        }

        // 3. Execution: Create DownloadTask entities and enqueue them
        var queuedCount = 0
        for (releaseTitle in plan.selectedReleases) {
            val candidate = candidates.firstOrNull { it.title == releaseTitle } ?: continue
            val taskId = DownloadTaskId("task-auto-${UUID.randomUUID().toString().take(8)}")

            val task = DownloadTask(
                id = taskId,
                releaseId = ReleaseId("rel-auto-${candidate.title.hashCode()}"),
                source = DownloadSource.TorrentSource(
                    infoHash = InfoHash("0000000000000000000000000000000000000000"),
                    name = candidate.title
                ),
                state = DownloadState.Pending,
                priority = DownloadPriority.Normal,
                destination = StorageTarget.DEFAULT,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )

            // Persist Task before network start (Section 26)
            downloadRepository.saveTask(task)
            eventBus?.tryEmit(DomainEvent.DownloadQueued(taskId))
            queuedCount++
        }

        return AniFlowResult.Success(
            AutomationWorkflowResult(
                trigger = trigger,
                tasksQueued = queuedCount,
                isBlockedBySafety = false,
                safetyReason = null,
                auditLog = plan.auditLog
            )
        )
    }
}
