package com.aniflow.core.common.error

/**
 * Unified application error hierarchy for explicit error propagation across boundaries.
 * Decoupled from Android Framework classes.
 */
sealed interface AppError {

    data class NetworkError(
        val message: String,
        val statusCode: Int? = null,
        val cause: Throwable? = null
    ) : AppError

    data class DatabaseError(
        val message: String,
        val cause: Throwable? = null
    ) : AppError

    data class ProviderError(
        val providerId: String,
        val message: String,
        val cause: Throwable? = null
    ) : AppError

    data class ValidationError(
        val field: String,
        val message: String
    ) : AppError

    data class StorageError(
        val path: String,
        val message: String,
        val cause: Throwable? = null
    ) : AppError

    data class UnknownError(
        val message: String,
        val cause: Throwable? = null
    ) : AppError
}
