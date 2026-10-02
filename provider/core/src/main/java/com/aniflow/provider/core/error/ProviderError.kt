package com.aniflow.provider.core.error

/**
 * Standard typed errors emitted by ReleaseProvider implementations (Step 18 Section 38).
 * Declares explicit retryability for all failure modes to drive exponential backoff.
 */
sealed class ProviderError(
    message: String,
    cause: Throwable? = null,
    val retryable: Boolean = false
) : Exception(message, cause) {

    data class NetworkFailure(val details: String, override val cause: Throwable? = null) :
        ProviderError("Network connection failed: $details", cause, retryable = true)

    data class Timeout(val timeoutMs: Long) :
        ProviderError("Request timed out after ${timeoutMs}ms", retryable = true)

    data class RateLimited(val retryAfterSeconds: Long? = null) :
        ProviderError("Provider rate limit reached (429). Retry after: ${retryAfterSeconds ?: "unknown"}s", retryable = true)

    data class Blocked(val reason: String) :
        ProviderError("Access blocked by provider: $reason", retryable = false)

    data class NotFound(val resourceId: String) :
        ProviderError("Requested resource not found: $resourceId", retryable = false)

    data class InvalidResponse(val statusCode: Int, val details: String) :
        ProviderError("HTTP error $statusCode: $details", retryable = statusCode in 500..599)

    data class ParserFailure(val details: String, override val cause: Throwable? = null) :
        ProviderError("Failed to parse provider response: $details", cause, retryable = false)

    data class ParserStructureChanged(val details: String) :
        ProviderError("Provider HTML structure appears to have changed: $details", retryable = false)

    data class Unavailable(val reason: String) :
        ProviderError("Provider service currently unavailable: $reason", retryable = true)

    data class Unsupported(val feature: String) :
        ProviderError("Provider does not support feature: $feature", retryable = false)

    data class Unknown(val details: String, override val cause: Throwable? = null) :
        ProviderError("Unexpected provider failure: $details", cause, retryable = false)
}

// Section 38 Aliases
typealias ProviderNetworkError = ProviderError.NetworkFailure
typealias ProviderRateLimited = ProviderError.RateLimited
typealias ProviderUnavailable = ProviderError.Unavailable
typealias ProviderBlocked = ProviderError.Blocked
typealias ProviderMalformedResponse = ProviderError.InvalidResponse
typealias ProviderParseError = ProviderError.ParserFailure
typealias ProviderNotFound = ProviderError.NotFound
typealias ProviderUnsupported = ProviderError.Unsupported
