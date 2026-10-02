package com.aniflow.domain.model.aggregate.download

enum class NetworkPolicy {
    AnyNetwork,
    WifiOnly,
    UnmeteredOnly,
    WifiAndEthernet
}

enum class VerificationPolicy {
    None,
    WhenHashAvailable,
    AlwaysWhenSupported
}

enum class OrganizationPolicy {
    KeepSourceFilename,
    TemplateBased,
    AnimeFolder,
    SeasonFolder,
    Custom
}

data class RetryPolicy(
    val maxRetries: Int = 3,
    val initialBackoffMs: Long = 1000L,
    val maxBackoffMs: Long = 30000L
) {
    init {
        require(maxRetries >= 0) { "maxRetries cannot be negative" }
        require(initialBackoffMs > 0) { "initialBackoffMs must be > 0" }
        require(maxBackoffMs >= initialBackoffMs) { "maxBackoffMs cannot be less than initialBackoffMs" }
    }
}

/**
 * Universal download execution policies (Section 81, 82, 83, 84).
 * Completely framework-agnostic.
 */
data class DownloadPolicy(
    val maxConcurrentTasks: Int = 3,
    val maxSegmentsPerTask: Int = 4,
    val retryPolicy: RetryPolicy = RetryPolicy(),
    val networkPolicy: NetworkPolicy = NetworkPolicy.AnyNetwork,
    val verificationPolicy: VerificationPolicy = VerificationPolicy.WhenHashAvailable,
    val organizationPolicy: OrganizationPolicy = OrganizationPolicy.AnimeFolder
) {
    init {
        require(maxConcurrentTasks in 1..20) { "maxConcurrentTasks must be between 1 and 20" }
        require(maxSegmentsPerTask in 1..16) { "maxSegmentsPerTask must be between 1 and 16" }
    }
}
