package com.aniflow.core.common.result

/**
 * Domain error taxonomy for explicit error handling and user-facing resolution.
 */
sealed interface ErrorType {
    data object NetworkError : ErrorType
    data object TimeoutError : ErrorType
    data class ProviderUnavailable(val providerId: String, val statusCode: Int? = null) : ErrorType
    data class ParserError(val rawTitle: String, val stage: String) : ErrorType
    data class InvalidRelease(val reason: String) : ErrorType
    data class StorageError(val path: String, val reason: String) : ErrorType
    data class InsufficientStorage(val requiredBytes: Long, val availableBytes: Long) : ErrorType
    data class DownloadError(val taskId: String, val details: String) : ErrorType
    data class TorrentError(val hash: String, val message: String) : ErrorType
    data class PermissionError(val permission: String) : ErrorType
    data class DuplicateError(val identifier: String) : ErrorType
    data class DatabaseError(val message: String) : ErrorType
    data class ValidationError(val reason: String) : ErrorType
    data class UnknownError(val exception: Throwable? = null) : ErrorType
}
