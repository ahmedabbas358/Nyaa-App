package com.aniflow.domain.state

import com.aniflow.domain.identity.ProviderId
import java.time.Instant

/**
 * Health states of external content providers (Section 64).
 */
enum class ProviderHealthState {
    Unknown,
    Healthy,
    Degraded,
    RateLimited,
    Unavailable,
    Maintenance,
    Blocked;

    val isUsable: Boolean get() = this == Healthy || this == Degraded
    val isTemporaryFailure: Boolean get() = this == RateLimited || this == Unavailable || this == Degraded
}

/**
 * Persisted and live telemetry metrics for provider operational health (Section 65).
 */
data class ProviderHealthRecord(
    val providerId: ProviderId,
    val state: ProviderHealthState = ProviderHealthState.Unknown,
    val retryAfter: Instant? = null,
    val lastSuccess: Instant? = null,
    val lastFailure: Instant? = null,
    val consecutiveFailures: Int = 0,
    val message: String? = null
) {
    val isCurrentlyBlockedByCooldown: Boolean
        get() = retryAfter != null && Instant.now().isBefore(retryAfter)
}

/**
 * State machine managing provider health transitions and backoff (Section 65).
 */
object ProviderHealthStateMachine {

    fun onResponseSuccess(current: ProviderHealthRecord): ProviderHealthRecord {
        return current.copy(
            state = ProviderHealthState.Healthy,
            retryAfter = null,
            lastSuccess = Instant.now(),
            consecutiveFailures = 0,
            message = null
        )
    }

    fun onRateLimited(current: ProviderHealthRecord, retryAfter: Instant?, message: String? = null): ProviderHealthRecord {
        return current.copy(
            state = ProviderHealthState.RateLimited,
            retryAfter = retryAfter ?: Instant.now().plusSeconds(60),
            lastFailure = Instant.now(),
            consecutiveFailures = current.consecutiveFailures + 1,
            message = message ?: "Rate limit reached"
        )
    }

    fun onNetworkFailure(current: ProviderHealthRecord, message: String?): ProviderHealthRecord {
        val nextFailures = current.consecutiveFailures + 1
        val nextState = if (nextFailures >= 3) ProviderHealthState.Unavailable else ProviderHealthState.Degraded
        val backoffSeconds = (1L shl (nextFailures.coerceAtMost(6))) * 5 // Exponential backoff: 10s, 20s, 40s...
        return current.copy(
            state = nextState,
            retryAfter = Instant.now().plusSeconds(backoffSeconds),
            lastFailure = Instant.now(),
            consecutiveFailures = nextFailures,
            message = message
        )
    }

    fun onMaintenance(current: ProviderHealthRecord, estimatedReturn: Instant?): ProviderHealthRecord {
        return current.copy(
            state = ProviderHealthState.Maintenance,
            retryAfter = estimatedReturn,
            lastFailure = Instant.now(),
            message = "Provider under scheduled maintenance"
        )
    }
}
