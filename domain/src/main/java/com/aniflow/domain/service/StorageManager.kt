package com.aniflow.domain.service

/**
 * Domain interface for storage filesystem operations and pre-flight checks (Section 78).
 */
interface StorageManager {
    suspend fun getAvailableSpaceBytes(directoryPath: String): Long
    suspend fun hasWriteAccess(directoryPath: String): Boolean
    suspend fun createTempFile(prefix: String, suffix: String): String
    suspend fun moveToFinal(tempPath: String, finalPath: String): Boolean
    suspend fun deleteFile(path: String): Boolean
    suspend fun fileExists(path: String): Boolean
    suspend fun getFileSize(path: String): Long
}
