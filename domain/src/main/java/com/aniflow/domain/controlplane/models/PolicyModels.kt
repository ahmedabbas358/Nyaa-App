package com.aniflow.domain.controlplane.models

import java.time.DayOfWeek

enum class DriveType {
    InternalFast,
    RemovableSd,
    ExternalUsb,
    NetworkShare
}

enum class ContentCategory {
    AnimeSeries,
    Movies,
    OvaSpecials,
    GeneralDownloads,
    Archive
}

data class AdvancedStorageRoutingRule(
    val id: String,
    val name: String,
    val category: ContentCategory? = null,
    val animeTitlePattern: String? = null,
    val minFreeSpaceBytes: Long? = null,
    val preferredDriveType: DriveType = DriveType.InternalFast,
    val targetStorageLocationId: String,
    val subfolderTemplate: String = "{anime}/Season {season}",
    val priority: Int = 0
)

data class BandwidthScheduleWindow(
    val id: String,
    val name: String,
    val startHour: Int, // 0-23
    val endHour: Int,   // 0-23
    val days: Set<DayOfWeek> = DayOfWeek.values().toSet(),
    val maxDownloadSpeedBytesPerSec: Long? = null, // null = unlimited
    val isEnabled: Boolean = true
)

enum class DuplicatePolicyType {
    Skip,
    Ask,
    Compare,
    KeepBoth,
    Replace,
    UpgradeExisting
}

data class UpgradePolicyRule(
    val allowUpgradeToHigherResolution: Boolean = true,
    val allowUpgradeToBetterCodec: Boolean = true,
    val minSizeSavingsPercent: Double? = null,
    val keepOldFileUntilNewVerified: Boolean = true,
    val respectManualLock: Boolean = true
)

enum class NotificationEvent {
    DownloadStarted,
    DownloadCompleted,
    DownloadFailed,
    BatchCompleted,
    NewReleaseFound,
    AutomationExecuted,
    RuleConflict,
    StorageLow,
    ProviderError
}

data class QuietHoursPolicy(
    val enabled: Boolean = false,
    val startHour: Int = 22,
    val endHour: Int = 7,
    val suppressNonCritical: Boolean = true,
    val allowErrorAlerts: Boolean = true,
    val allowDownloadCompleted: Boolean = false
)

enum class ProviderCapability {
    Search,
    Pagination,
    Sorting,
    UploaderSearch,
    Details,
    TorrentFile,
    MagnetLink,
    RssFeed
}

data class ProviderConfiguration(
    val providerId: String,
    val displayName: String,
    val isEnabled: Boolean = true,
    val priorityOrder: Int = 1,
    val capabilities: Set<ProviderCapability> = setOf(
        ProviderCapability.Search,
        ProviderCapability.Pagination,
        ProviderCapability.Sorting,
        ProviderCapability.TorrentFile,
        ProviderCapability.MagnetLink
    ),
    val rateLimitRequestsPerMinute: Int = 30,
    val isHealthCheckOk: Boolean = true,
    val lastSyncEpochMillis: Long = System.currentTimeMillis()
)
