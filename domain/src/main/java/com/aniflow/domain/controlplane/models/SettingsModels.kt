package com.aniflow.domain.controlplane.models

import com.aniflow.domain.valueobject.ByteSize
import com.aniflow.domain.valueobject.LanguageCode
import com.aniflow.domain.valueobject.Resolution
import com.aniflow.domain.valueobject.VideoCodec

enum class SettingsMode {
    Simple,
    Advanced,
    Developer
}

enum class ThemeMode {
    System,
    Light,
    Dark
}

enum class StartupDestination {
    Home,
    Search,
    Downloads,
    Library
}

data class AppSettings(
    val language: LanguageCode = LanguageCode.ENGLISH,
    val theme: ThemeMode = ThemeMode.Dark,
    val mode: SettingsMode = SettingsMode.Simple,
    val startupDestination: StartupDestination = StartupDestination.Home,
    val confirmDestructiveActions: Boolean = true
)

data class SearchSettings(
    val defaultSearchProviderId: String = "nyaa",
    val defaultSortOption: String = "Date",
    val defaultTrustedOnly: Boolean = false,
    val rememberRecentSearches: Boolean = true,
    val maxRecentSearchesCount: Int = 20,
    val autoSuggestKeywords: Boolean = true
)

data class ProviderSettings(
    val providerId: String,
    val isEnabled: Boolean = true,
    val autoSearchEnabled: Boolean = true,
    val rssPollingIntervalMinutes: Int = 30,
    val requestTimeoutSeconds: Int = 15,
    val maxRetries: Int = 3,
    val respectsRateLimits: Boolean = true
)

data class DownloadSettings(
    val maxConcurrentDownloads: Int = 3,
    val maxHttpSegmentsPerDownload: Int = 4,
    val globalDownloadSpeedLimitBytesPerSec: Long? = null, // null = unlimited
    val autoVerifyChecksumAfterDownload: Boolean = true,
    val autoOrganizeCompletedFiles: Boolean = true,
    val autoRemoveTorrentAfterSeedRatio: Float? = null
)

data class StorageSettings(
    val defaultStorageLocationId: String = "loc-internal",
    val animeStorageLocationId: String = "loc-sd",
    val safetyMarginBytes: Long = 1024 * 1024 * 500L, // 500 MB
    val lowStorageThresholdPercent: Double = 10.0,
    val criticalStorageThresholdPercent: Double = 5.0,
    val autoCleanOrphanPartsOlderThanDays: Int = 3
)

enum class NetworkPolicyType {
    AnyNetwork,
    WiFiOnly,
    WiFiOrEthernet,
    MeteredAllowed,
    RoamingDisallowed
}

data class NetworkSettings(
    val policy: NetworkPolicyType = NetworkPolicyType.WiFiOnly,
    val pauseOnMetered: Boolean = true,
    val pauseOnLowBattery: Boolean = false,
    val lowBatteryThresholdPercent: Int = 15
)

data class NotificationSettings(
    val notifyOnDownloadStarted: Boolean = false,
    val notifyOnDownloadCompleted: Boolean = true,
    val notifyOnDownloadFailed: Boolean = true,
    val notifyOnBatchCompleted: Boolean = true,
    val notifyOnStorageLow: Boolean = true,
    val notifyOnAutomationExecuted: Boolean = false,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStartHour: Int = 22,
    val quietHoursEndHour: Int = 7
)

data class AutomationSettings(
    val isAutoDownloadEnabled: Boolean = false,
    val maxAutomatedDownloadsPerDay: Int = 20,
    val cooldownPeriodMinutes: Int = 15,
    val requireConfirmationForBatchesOverGb: Double = 10.0,
    val allowUpgradesOfExistingFiles: Boolean = false
)

data class PrivacySettings(
    val collectDiagnosticLogs: Boolean = false,
    val allowCrashReports: Boolean = false,
    val clearCacheOnExit: Boolean = false
)

data class DeveloperSettings(
    val showParserDiagnostics: Boolean = false,
    val showHttpWireLogs: Boolean = false,
    val bypassNetworkConstraintsForTesting: Boolean = false,
    val simulateLowStorage: Boolean = false,
    val ruleEvaluationTraceEnabled: Boolean = true
)
