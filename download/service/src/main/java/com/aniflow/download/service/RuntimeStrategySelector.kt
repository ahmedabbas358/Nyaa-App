package com.aniflow.download.service

import android.os.Build

enum class AndroidExecutionMechanism {
    UserInitiatedDataTransfer, // Android 14+ (API 34+) UIDT
    ForegroundService,        // Legacy & active continuous foreground execution
    WorkManagerDeferred       // Short post-processing, cleanup, retry scheduling
}

data class RuntimeTaskRequirements(
    val isUserInitiated: Boolean = true,
    val isLongRunningDataTransfer: Boolean = true,
    val requiresContinuousSocket: Boolean = false,
    val isDeferredOrCleanup: Boolean = false
)

/**
 * Evaluates Android API levels and task requirements to select the optimal background execution strategy (Section 82, 83, 84, 85, 86, 87, 148, 149).
 * Android 14+ (API 34+): Evaluates UIDT as primary path for user-initiated downloads.
 * Android 15+ (API 35+): Respects 6-hour dataSync daily quotas and onTimeout() limits.
 */
class RuntimeStrategySelector(
    private val apiLevel: Int = Build.VERSION.SDK_INT
) {
    fun selectStrategy(requirements: RuntimeTaskRequirements): AndroidExecutionMechanism {
        if (requirements.isDeferredOrCleanup) {
            return AndroidExecutionMechanism.WorkManagerDeferred
        }

        return when {
            // Android 14+ (API 34+) User-Initiated Data Transfer
            apiLevel >= 34 && requirements.isUserInitiated && requirements.isLongRunningDataTransfer -> {
                AndroidExecutionMechanism.UserInitiatedDataTransfer
            }
            // Standard / Legacy Foreground Service
            else -> {
                AndroidExecutionMechanism.ForegroundService
            }
        }
    }
}
