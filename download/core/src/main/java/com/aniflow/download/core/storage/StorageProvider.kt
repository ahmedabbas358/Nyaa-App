package com.aniflow.download.core.storage

import com.aniflow.download.core.model.StorageTarget
import java.io.File

data class StorageLocation(
    val uriString: String,
    val isDirectory: Boolean = false
) {
    val file: File?
        get() = try {
            File(uriString)
        } catch (e: Exception) {
            null
        }
}

sealed interface StorageOperationResult {
    data class Success(val location: StorageLocation) : StorageOperationResult
    data class Failure(val message: String, val cause: Throwable? = null) : StorageOperationResult

    val isSuccess: Boolean get() = this is Success
}

/**
 * Pure Storage Provider abstraction adhering to Step 22 Section 21.
 * Pure Kotlin contract completely decoupled from android.net.Uri.
 */
interface StorageProvider {
    suspend fun exists(target: StorageTarget): Boolean
    suspend fun freeSpace(target: StorageTarget): Long
    suspend fun createDirectory(target: StorageTarget): StorageLocation
    suspend fun move(from: StorageLocation, to: StorageLocation): StorageOperationResult
    suspend fun copy(from: StorageLocation, to: StorageLocation): StorageOperationResult
    suspend fun delete(target: StorageLocation): StorageOperationResult
}

/**
 * Standard File System implementation for Desktop/Unit Tests/App-Private storage.
 */
class LocalFileStorageProvider : StorageProvider {

    override suspend fun exists(target: StorageTarget): Boolean {
        return File(target.fullPath).exists()
    }

    override suspend fun freeSpace(target: StorageTarget): Long {
        val f = File(target.fullPath)
        val parent = f.parentFile ?: f
        return if (parent.exists()) parent.usableSpace else f.usableSpace
    }

    override suspend fun createDirectory(target: StorageTarget): StorageLocation {
        val dir = File(target.fullPath)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return StorageLocation(dir.absolutePath, isDirectory = true)
    }

    override suspend fun move(from: StorageLocation, to: StorageLocation): StorageOperationResult {
        val src = File(from.uriString)
        val dst = File(to.uriString)

        if (!src.exists()) {
            return StorageOperationResult.Failure("Source file does not exist: ${src.absolutePath}")
        }

        dst.parentFile?.mkdirs()

        // Attempt atomic rename if on same volume
        val renamed = src.renameTo(dst)
        if (renamed) {
            return StorageOperationResult.Success(StorageLocation(dst.absolutePath))
        }

        // Cross-volume fallback: copy + verify + delete
        return try {
            src.copyTo(dst, overwrite = true)
            if (dst.exists() && dst.length() == src.length()) {
                src.delete()
                StorageOperationResult.Success(StorageLocation(dst.absolutePath))
            } else {
                dst.delete()
                StorageOperationResult.Failure("Cross-volume copy verification failed for ${dst.absolutePath}")
            }
        } catch (e: Exception) {
            StorageOperationResult.Failure("Failed moving file from ${src.absolutePath} to ${dst.absolutePath}: ${e.message}", e)
        }
    }

    override suspend fun copy(from: StorageLocation, to: StorageLocation): StorageOperationResult {
        val src = File(from.uriString)
        val dst = File(to.uriString)

        if (!src.exists()) {
            return StorageOperationResult.Failure("Source file does not exist: ${src.absolutePath}")
        }

        dst.parentFile?.mkdirs()
        return try {
            src.copyTo(dst, overwrite = true)
            StorageOperationResult.Success(StorageLocation(dst.absolutePath))
        } catch (e: Exception) {
            StorageOperationResult.Failure("Failed copying file: ${e.message}", e)
        }
    }

    override suspend fun delete(target: StorageLocation): StorageOperationResult {
        val f = File(target.uriString)
        return if (f.exists()) {
            val deleted = f.deleteRecursively()
            if (deleted) StorageOperationResult.Success(target)
            else StorageOperationResult.Failure("Could not delete ${f.absolutePath}")
        } else {
            StorageOperationResult.Success(target) // Idempotent
        }
    }
}
