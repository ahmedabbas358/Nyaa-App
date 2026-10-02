package com.aniflow.domain.controlplane.models

import java.time.Instant

sealed interface AutomationTrigger {
    data object Manual : AutomationTrigger
    data class SearchCompleted(val query: String) : AutomationTrigger
    data class NewReleaseDetected(val animeTitle: String) : AutomationTrigger
    data class EpisodeMissing(val animeTitle: String, val episodeNumber: Double) : AutomationTrigger
    data class DownloadCompleted(val taskId: String) : AutomationTrigger
    data class StorageChanged(val availableBytes: Long) : AutomationTrigger
    data class NetworkChanged(val policy: NetworkPolicyType) : AutomationTrigger
    data class ScheduledInterval(val minutes: Int) : AutomationTrigger
}

enum class AutomationSafetyLevel {
    Silent,              // Automatic execution for small/standard tasks
    Notify,              // Execute and send informational notification
    RequireConfirmation, // Block execution until user approves in UI
    AlwaysConfirm        // Destructive actions like file replace/delete
}

data class AutomationSimulationResult(
    val matchedReleases: List<String>,
    val rejectedReleases: List<String>,
    val selectedReleases: List<String>,
    val rulesApplied: List<String>,
    val fallbackTierName: String?,
    val estimatedSizeBytes: Long,
    val destinationPath: String,
    val networkPolicy: NetworkPolicyType,
    val potentialDuplicates: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val requiresConfirmation: Boolean = false
)

data class AutomationAuditLog(
    val id: String,
    val timestamp: Instant = Instant.now(),
    val triggerDescription: String,
    val winningRuleName: String?,
    val actionsTaken: List<String>,
    val outcome: String,
    val wasBlockedBySafety: Boolean = false,
    val explanation: String
)
