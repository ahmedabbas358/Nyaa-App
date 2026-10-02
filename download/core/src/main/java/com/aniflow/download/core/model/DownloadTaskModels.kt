package com.aniflow.download.core.model

import com.aniflow.domain.identity.EpisodeId
import com.aniflow.domain.identity.ReleaseId
import java.time.Instant

/**
 * 13 Discrete states defined by Section 25.
 */
enum class DownloadTaskState {
    Pending,
    Queued,
    Starting,
    Downloading,
    Paused,
    Waiting,
    Retrying,
    Verifying,
    Moving,
    Completed,
    Failed,
    Cancelled,
    Removed,

    // Aliases for compatibility
    Organizing,
    RetryScheduled,
    WaitingForNetwork,
    WaitingForStorage;

    val isTerminal: Boolean
        get() = this == Completed || this == Failed || this == Cancelled || this == Removed
}

/**
 * Waiting reasons explaining the Waiting state (Section 86).
 */
sealed interface WaitingReason {
    data object WaitingForNetwork : WaitingReason
    data object WaitingForStorage : WaitingReason
    data object WaitingForSlot : WaitingReason
    data object WaitingForProvider : WaitingReason
    data object WaitingForUser : WaitingReason
    data object WaitingForRuntime : WaitingReason
}

/**
 * Task block reasons (Section 87).
 */
sealed interface TaskBlockReason {
    data object NetworkRestricted : TaskBlockReason
    data object InsufficientStorage : TaskBlockReason
    data object NoDownloadSlot : TaskBlockReason
    data object ProviderUnavailable : TaskBlockReason
    data object PermissionRequired : TaskBlockReason
    data object InvalidDestination : TaskBlockReason
    data object UserPaused : TaskBlockReason
}

/**
 * DownloadProgress model (Section 57).
 */
data class DownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long?,
    val speedBytesPerSecond: Long,
    val etaSeconds: Long?,
    val percent: Float? = if (totalBytes != null && totalBytes > 0) {
        (downloadedBytes.toFloat() / totalBytes * 100f).coerceIn(0f, 100f)
    } else null
)

/**
 * Verification result (Section 71).
 */
sealed interface VerificationResult {
    data object Passed : VerificationResult
    data class Failed(val reason: String, val details: String? = null) : VerificationResult
    data class Warning(val reason: String, val details: String? = null) : VerificationResult
}

/**
 * Download history entry (Section 78).
 */
data class DownloadHistoryEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val taskId: DownloadTaskId,
    val releaseId: ReleaseId?,
    val completedAt: Instant = Instant.now(),
    val durationMillis: Long,
    val bytes: Long,
    val finalLocation: String,
    val result: String = "SUCCESS"
)

/**
 * Queue lifecycle events (Section 80).
 */
sealed interface QueueEvent {
    data class TaskQueued(val task: DownloadTask) : QueueEvent
    data class TaskStarted(val taskId: DownloadTaskId) : QueueEvent
    data class TaskPaused(val taskId: DownloadTaskId) : QueueEvent
    data class TaskResumed(val taskId: DownloadTaskId) : QueueEvent
    data class TaskRetried(val taskId: DownloadTaskId) : QueueEvent
    data class TaskFailed(val taskId: DownloadTaskId, val error: DownloadError) : QueueEvent
    data class TaskCompleted(val taskId: DownloadTaskId, val finalLocation: String) : QueueEvent
    data class TaskCancelled(val taskId: DownloadTaskId) : QueueEvent
}

enum class DuplicateDecision {
    AlreadyDownloaded,
    AlreadyDownloading,
    AlreadyQueued,
    PossibleDuplicate,
    NotDuplicate
}

data class StorageEstimate(
    val contentBytes: Long,
    val temporaryBytes: Long,
    val verificationBytes: Long,
    val safetyMarginBytes: Long,
    val totalRequiredBytes: Long,
    val availableBytes: Long,
    val remainingBytes: Long,
    val isSufficient: Boolean
) {
    companion object {
        fun calculate(contentBytes: Long, availableBytes: Long, safetyMarginMb: Long = 500L): StorageEstimate {
            val tempOverhead = (contentBytes * 0.05).toLong() // 5% temp overhead
            val verifyOverhead = (contentBytes * 0.02).toLong() // 2% verify overhead
            val safetyBytes = safetyMarginMb * 1024 * 1024
            val totalRequired = contentBytes + tempOverhead + verifyOverhead + safetyBytes
            val remaining = availableBytes - totalRequired
            return StorageEstimate(
                contentBytes = contentBytes,
                temporaryBytes = tempOverhead,
                verificationBytes = verifyOverhead,
                safetyMarginBytes = safetyBytes,
                totalRequiredBytes = totalRequired,
                availableBytes = availableBytes,
                remainingBytes = remaining,
                isSufficient = remaining >= 0
            )
        }
    }
}

enum class CollisionPolicy {
    Skip,
    Overwrite,
    Rename,
    Compare,
    Ask
}

enum class ErrorTaxonomy {
    Network,
    Http,
    Torrent,
    Storage,
    Permission,
    Verification,
    Engine,
    Provider,
    Policy,
    Cancelled,
    Unknown
}

data class DownloadError(
    val code: String,
    val taxonomy: ErrorTaxonomy,
    val message: String,
    val isRetryable: Boolean = true,
    val timestamp: Instant = Instant.now()
)

data class HttpSegment(
    val index: Int,
    val startOffset: Long,
    val endOffset: Long,
    val downloadedBytes: Long,
    val state: SegmentState = SegmentState.Pending
) {
    val totalBytes: Long get() = (endOffset - startOffset) + 1
    val isCompleted: Boolean get() = downloadedBytes >= totalBytes && state == SegmentState.Completed
}

enum class SegmentState {
    Pending,
    Downloading,
    Completed,
    Failed
}

enum class DownloadFileState {
    Pending,
    Downloading,
    Verifying,
    Completed,
    Failed,
    Skipped
}

/**
 * File model for multi-file downloads (Section 62).
 */
data class DownloadFile(
    val id: String,
    val taskId: DownloadTaskId,
    val filename: String,
    val expectedSize: Long?,
    val downloadedSize: Long = 0L,
    val temporaryLocation: String,
    val finalLocation: String? = null,
    val state: DownloadFileState = DownloadFileState.Pending
)

data class DownloadTask(
    val id: DownloadTaskId,
    val planId: DownloadPlanId? = null,
    val planItemId: DownloadPlanItemId = DownloadPlanItemId("item-${id.value}"),
    val releaseId: ReleaseId? = null,
    val episodeId: EpisodeId? = null,
    val source: DownloadSource,
    val engineType: DownloadEngineType,
    val destination: PlannedDestination,
    val priority: DownloadPriority = DownloadPriority.Normal,
    val state: DownloadTaskState = DownloadTaskState.Pending,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long? = null,
    val speedBytesPerSecond: Long = 0L,
    val etaSeconds: Long? = null,
    val segments: List<HttpSegment> = emptyList(),
    val files: List<DownloadFile> = emptyList(),
    val retryCount: Int = 0,
    val nextRetryAt: Instant? = null,
    val lastError: DownloadError? = null,
    val waitingReason: WaitingReason? = null,
    val blockReason: TaskBlockReason? = null,
    val isLocked: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    val isFinished: Boolean
        get() = state.isTerminal

    val isActive: Boolean
        get() = state == DownloadTaskState.Downloading ||
            state == DownloadTaskState.Starting ||
            state == DownloadTaskState.Verifying ||
            state == DownloadTaskState.Organizing ||
            state == DownloadTaskState.Moving

    val progressPercent: Int
        get() {
            val total = totalBytes ?: return 0
            if (total <= 0L) return 0
            return ((downloadedBytes.toDouble() / total) * 100).toInt().coerceIn(0, 100)
        }
}
