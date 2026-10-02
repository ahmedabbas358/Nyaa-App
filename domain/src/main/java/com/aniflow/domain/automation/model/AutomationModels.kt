package com.aniflow.domain.automation.model

import com.aniflow.domain.controlplane.models.RuleNode
import com.aniflow.domain.identity.AnimeId
import com.aniflow.domain.identity.AutomationExecutionId
import com.aniflow.domain.identity.AutomationRuleId
import com.aniflow.domain.identity.CollectionId
import com.aniflow.domain.identity.DownloadProfileId
import com.aniflow.domain.identity.DownloadTaskId
import com.aniflow.domain.identity.EpisodeId
import com.aniflow.domain.identity.ProviderId
import com.aniflow.domain.identity.ReviewItemId
import com.aniflow.domain.identity.SavedSearchId
import com.aniflow.domain.identity.SearchScheduleId
import com.aniflow.domain.identity.StorageId
import java.time.Instant

/**
 * AutomationTrigger (Section 13, 14, 89).
 * Decoupled from rule execution so multiple rules can listen to the same trigger event.
 */
sealed interface AutomationTrigger {
    data object Manual : AutomationTrigger
    data class Schedule(val scheduleId: SearchScheduleId) : AutomationTrigger
    data class NewRelease(val releaseIdentity: String) : AutomationTrigger
    data class EpisodeMissing(val animeId: AnimeId, val episodeNumber: Double) : AutomationTrigger
    data class EpisodeAvailable(val animeId: AnimeId, val episodeNumber: Double) : AutomationTrigger
    data class DownloadCompleted(val taskId: DownloadTaskId) : AutomationTrigger
    data class DownloadFailed(val taskId: DownloadTaskId) : AutomationTrigger
    data object LibraryChanged : AutomationTrigger
    data class UpgradeAvailable(val episodeId: EpisodeId, val currentRelease: String, val candidateRelease: String) : AutomationTrigger
    data class ProviderRecovered(val providerId: ProviderId) : AutomationTrigger
    data class StorageRecovered(val storageId: StorageId) : AutomationTrigger
    data object NetworkAvailable : AutomationTrigger
}

/**
 * AutomationAction (Section 25, 26).
 * Actions must NOT call download engines directly; they create plans and queue tasks.
 */
sealed interface AutomationAction {
    data object MarkSeen : AutomationAction
    data object Favorite : AutomationAction
    data class AddToCollection(val collectionId: CollectionId) : AutomationAction
    data class SelectProfile(val profileId: DownloadProfileId) : AutomationAction
    data object CreateDownloadPlan : AutomationAction
    data object QueueDownload : AutomationAction
    data class Notify(val message: String, val priority: String = "Normal") : AutomationAction
    data object PauseAutomation : AutomationAction
    data class StopWatching(val targetId: String) : AutomationAction
    data class CreateReviewItem(val reason: String) : AutomationAction
    data class TriggerSearch(val query: String) : AutomationAction
    data class MarkUpgrade(val episodeId: EpisodeId) : AutomationAction
}

enum class CooldownScope {
    PerRule,
    PerAnime,
    PerEpisode,
    PerRelease,
    Global
}

data class CooldownPolicy(
    val durationSeconds: Long = 3600L, // 1 hour default
    val scope: CooldownScope = CooldownScope.PerAnime
)

enum class ConfirmationMode {
    Silent,
    Notify,
    Confirm,
    AlwaysConfirm
}

data class ConfirmationPolicy(
    val mode: ConfirmationMode = ConfirmationMode.Notify,
    val sizeThresholdBytes: Long = 10L * 1024 * 1024 * 1024 // 10 GB asks confirmation
)

enum class SafetyDecision {
    Allowed,
    AllowedWithWarning,
    RequiresConfirmation,
    Blocked
}

enum class SafetyBlockReason {
    InsufficientStorage,
    NetworkRestricted,
    DuplicateTaskExists,
    LowConfidence,
    ExceedsDailyLimit,
    ManualLockActive,
    AmbiguousParser,
    LargeBatchUnapproved,
    GlobalKillSwitchActive
}

/**
 * AutomationRule (Section 23, 24, 138, 139).
 * Reuses existing RuleNode AST from Step 11 without creating a secondary rule language.
 */
data class AutomationRule(
    val id: AutomationRuleId,
    val name: String,
    val enabled: Boolean = true,
    val trigger: AutomationTrigger,
    val conditions: RuleNode? = null,
    val actions: List<AutomationAction> = listOf(AutomationAction.QueueDownload),
    val priority: Int = 100,
    val cooldown: CooldownPolicy = CooldownPolicy(),
    val confirmation: ConfirmationPolicy = ConfirmationPolicy(),
    val version: Int = 1,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

/**
 * Idempotency key guaranteeing exactly-once automation action execution (Section 36, 144, 145).
 */
data class AutomationExecutionKey(
    val ruleId: AutomationRuleId,
    val triggerIdentity: String,
    val targetIdentity: String
)

enum class AutomationExecutionState {
    Pending,
    Running,
    Waiting,
    Completed,
    Skipped,
    Blocked,
    Failed,
    Cancelled
}

sealed interface AutomationExecutionResult {
    data object NothingFound : AutomationExecutionResult
    data class AlreadySatisfied(val detail: String) : AutomationExecutionResult
    data class AlreadyQueued(val taskId: String) : AutomationExecutionResult
    data class ReviewRequired(val reviewItemId: ReviewItemId, val reason: String) : AutomationExecutionResult
    data class DownloadPlanned(val planId: String) : AutomationExecutionResult
    data class DownloadQueued(val planId: String, val taskIds: List<String>) : AutomationExecutionResult
    data class NotificationSent(val message: String) : AutomationExecutionResult
    data class UpgradeDetected(val currentRelease: String, val candidateRelease: String) : AutomationExecutionResult
    data class Blocked(val reason: SafetyBlockReason, val detail: String) : AutomationExecutionResult
    data class Failed(val error: String) : AutomationExecutionResult
}

/**
 * Execution record with full explainability trace (Section 47, 51, 52, 53, 105, 149).
 */
data class AutomationExecution(
    val id: AutomationExecutionId,
    val ruleId: AutomationRuleId,
    val trigger: AutomationTrigger,
    val targetIdentity: String,
    val state: AutomationExecutionState,
    val startedAt: Instant = Instant.now(),
    val completedAt: Instant? = null,
    val decision: SafetyDecision? = null,
    val result: AutomationExecutionResult? = null,
    val explainabilityLog: List<String> = emptyList(),
    val ruleVersionSnapshot: Int = 1
)

enum class ReviewItemState {
    Pending,
    Approved,
    Rejected,
    Expired
}

/**
 * Review Queue item for ambiguous releases or high-impact actions (Section 54, 55, 56).
 */
data class ReviewItem(
    val id: ReviewItemId,
    val executionId: AutomationExecutionId?,
    val issue: String,
    val candidateReleaseTitle: String,
    val reason: String,
    val recommendedAction: String,
    val state: ReviewItemState = ReviewItemState.Pending,
    val createdAt: Instant = Instant.now(),
    val expiresAt: Instant? = null
)

sealed interface ScheduleDefinition {
    data class IntervalMinutes(val minutes: Int) : ScheduleDefinition
    data class DailyAtHour(val hour: Int, val minute: Int = 0) : ScheduleDefinition
    data class WeeklyAtDayHour(val dayOfWeek: Int, val hour: Int) : ScheduleDefinition
    data class CronExpression(val cron: String) : ScheduleDefinition
}

/**
 * Scheduled Search specification (Section 40, 41, 42, 43, 101).
 */
data class SearchSchedule(
    val id: SearchScheduleId,
    val savedSearchId: SavedSearchId,
    val enabled: Boolean = true,
    val schedule: ScheduleDefinition = ScheduleDefinition.DailyAtHour(hour = 2),
    val networkPolicy: String = "WiFiOnly",
    val nextRunAt: Instant = Instant.now(),
    val lastRunAt: Instant? = null
)

/**
 * Global Safety Gates & Rate Limits (Section 118, 119, 120, 121, 170).
 */
data class AutomationLimits(
    val maxAutomaticDownloadsPerDay: Int = 20,
    val maxAutomaticStoragePerDayBytes: Long = 50L * 1024 * 1024 * 1024, // 50 GB
    val maxAutomaticBatchSize: Int = 3,
    val allowAutomaticUpgrades: Boolean = false,
    val allowAutomaticFileReplacement: Boolean = false,
    val globalKillSwitch: Boolean = false
)

/**
 * Dry Run simulation output (Section 66, 67, 68, 104, 127).
 */
data class AutomationDryRunResult(
    val matchesCount: Int,
    val wouldSelectCount: Int,
    val wouldDownloadCount: Int,
    val wouldReviewCount: Int,
    val wouldSkipCount: Int,
    val estimatedSizeBytes: Long,
    val actionsPreview: List<String> = emptyList()
)
