package com.aniflow.domain.automation.safety

import com.aniflow.domain.automation.model.AutomationLimits
import com.aniflow.domain.automation.model.ConfirmationMode
import com.aniflow.domain.automation.model.ConfirmationPolicy
import com.aniflow.domain.automation.model.SafetyBlockReason
import com.aniflow.domain.automation.model.SafetyDecision
import com.aniflow.domain.intelligence.model.NormalizedRelease

data class SafetyEvaluationContext(
    val release: NormalizedRelease,
    val isAmbiguous: Boolean = false,
    val isBatch: Boolean = false,
    val batchEpisodeCount: Int = 1,
    val isDuplicateActiveTask: Boolean = false,
    val isDuplicateLibraryFile: Boolean = false,
    val isUpgrade: Boolean = false,
    val isManualLocked: Boolean = false,
    val availableStorageBytes: Long,
    val requiredSizeBytes: Long,
    val isWiFiConnected: Boolean = true,
    val isNetworkMetered: Boolean = false,
    val downloadsTodayCount: Int = 0,
    val storageUsedTodayBytes: Long = 0L
)

data class SafetyCheckResult(
    val decision: SafetyDecision,
    val blockReason: SafetyBlockReason? = null,
    val explanation: String
)

/**
 * AutomationSafetyGate (Section 29, 30, 31, 32, 33, 34, 35, 82, 118, 119, 120, 121, 170).
 * Prevents unexpected downloads, storage exhaustion, infinite loops, and data loss.
 */
class AutomationSafetyGate(
    private val limits: AutomationLimits = AutomationLimits()
) {

    private val safetyMarginBytes: Long = 1024L * 1024 * 1024 // 1 GB safety margin

    fun evaluate(
        context: SafetyEvaluationContext,
        confirmationPolicy: ConfirmationPolicy = ConfirmationPolicy()
    ): SafetyCheckResult {
        // 1. Global Kill Switch Check (Section 94, 112)
        if (limits.globalKillSwitch) {
            return SafetyCheckResult(
                decision = SafetyDecision.Blocked,
                blockReason = SafetyBlockReason.GlobalKillSwitchActive,
                explanation = "All automation is globally paused by the master switch."
            )
        }

        // 2. Manual Lock Check (Section 122)
        if (context.isManualLocked) {
            return SafetyCheckResult(
                decision = SafetyDecision.Blocked,
                blockReason = SafetyBlockReason.ManualLockActive,
                explanation = "Target episode has an active manual user lock."
            )
        }

        // 3. Duplicate Task Check (Section 35)
        if (context.isDuplicateActiveTask) {
            return SafetyCheckResult(
                decision = SafetyDecision.Blocked,
                blockReason = SafetyBlockReason.DuplicateTaskExists,
                explanation = "An active or queued download task already exists for this release/episode."
            )
        }

        // 4. Ambiguous Release Protection (Section 31)
        if (context.isAmbiguous) {
            return SafetyCheckResult(
                decision = SafetyDecision.RequiresConfirmation,
                blockReason = SafetyBlockReason.AmbiguousParser,
                explanation = "Release title or episode identity is ambiguous. Requires manual review."
            )
        }

        // 5. Daily Downloads Count Limit (Section 119)
        if (context.downloadsTodayCount >= limits.maxAutomaticDownloadsPerDay) {
            return SafetyCheckResult(
                decision = SafetyDecision.Blocked,
                blockReason = SafetyBlockReason.ExceedsDailyLimit,
                explanation = "Daily automatic download limit reached (${limits.maxAutomaticDownloadsPerDay} files/day)."
            )
        }

        // 6. Daily Storage Budget Limit (Section 119, 121)
        if (context.storageUsedTodayBytes + context.requiredSizeBytes > limits.maxAutomaticStoragePerDayBytes) {
            return SafetyCheckResult(
                decision = SafetyDecision.Blocked,
                blockReason = SafetyBlockReason.ExceedsDailyLimit,
                explanation = "Daily automatic storage budget exceeded (Limit: ${limits.maxAutomaticStoragePerDayBytes / (1024 * 1024 * 1024)} GB/day)."
            )
        }

        // 7. Physical Storage Free Space Check (Section 33, 80)
        val neededBytes = context.requiredSizeBytes + safetyMarginBytes
        if (context.availableStorageBytes < neededBytes) {
            return SafetyCheckResult(
                decision = SafetyDecision.Blocked,
                blockReason = SafetyBlockReason.InsufficientStorage,
                explanation = "Available storage is insufficient (Available: ${context.availableStorageBytes / (1024 * 1024)} MB, Required: ${neededBytes / (1024 * 1024)} MB)."
            )
        }

        // 8. Network WiFi / Metered Check (Section 79)
        if (!context.isWiFiConnected && context.isNetworkMetered) {
            return SafetyCheckResult(
                decision = SafetyDecision.Blocked,
                blockReason = SafetyBlockReason.NetworkRestricted,
                explanation = "Automatic downloads require an unmetered Wi-Fi connection."
            )
        }

        // 9. Large Batch Protection (Section 30, 120)
        if (context.isBatch && context.batchEpisodeCount > limits.maxAutomaticBatchSize) {
            return SafetyCheckResult(
                decision = SafetyDecision.RequiresConfirmation,
                blockReason = SafetyBlockReason.LargeBatchUnapproved,
                explanation = "Batch contains ${context.batchEpisodeCount} episodes (Exceeds automatic limit of ${limits.maxAutomaticBatchSize}). Confirmation required."
            )
        }

        // 10. Upgrade Safety Gate (Section 81, 82, 174)
        if (context.isUpgrade && !limits.allowAutomaticUpgrades) {
            return SafetyCheckResult(
                decision = SafetyDecision.RequiresConfirmation,
                explanation = "Better release found, but automatic upgrades are disabled in settings. Requires review."
            )
        }

        // 11. Confirmation Policy Threshold (Section 29)
        if (confirmationPolicy.mode == ConfirmationMode.AlwaysConfirm ||
            (context.requiredSizeBytes > confirmationPolicy.sizeThresholdBytes)
        ) {
            return SafetyCheckResult(
                decision = SafetyDecision.RequiresConfirmation,
                explanation = "File size (${context.requiredSizeBytes / (1024 * 1024 * 1024)} GB) exceeds confirmation threshold."
            )
        }

        if (confirmationPolicy.mode == ConfirmationMode.Notify) {
            return SafetyCheckResult(
                decision = SafetyDecision.AllowedWithWarning,
                explanation = "Safety checks passed. Notification will be dispatched upon queueing."
            )
        }

        return SafetyCheckResult(
            decision = SafetyDecision.Allowed,
            explanation = "All safety gates passed successfully."
        )
    }
}
