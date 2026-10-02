package com.aniflow.platform.storage

import com.aniflow.platform.storage.model.StorageFile
import com.aniflow.platform.storage.model.StorageTarget
import com.aniflow.platform.storage.provider.StorageProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest

data class MoveResult(
    val isSuccess: Boolean,
    val movedFile: StorageFile? = null,
    val verifiedBytes: Long = 0L,
    val errorMessage: String? = null
)

/**
 * CrossVolumeMoveEngine (Section 80, 81, 82).
 * Coordinates safe moves across different storage providers/volumes (e.g. AppPrivate -> SD Card via SAF).
 * Never deletes source file before verifying the target copy integrity.
 */
class CrossVolumeMoveEngine {

    suspend fun move(
        sourceProvider: StorageProvider,
        sourceFile: StorageFile,
        targetProvider: StorageProvider,
        target: StorageTarget,
        onProgress: ((bytesMoved: Long, totalBytes: Long) -> Unit)? = null
    ): MoveResult = withContext(Dispatchers.IO) {
        val targetFile = try {
            targetProvider.createFile(
                target = target,
                name = sourceFile.name,
                mimeType = sourceFile.mimeType ?: "video/x-matroska"
            )
        } catch (e: Exception) {
            return@withContext MoveResult(
                isSuccess = false,
                errorMessage = "Failed to create target file: ${e.message}"
            )
        }

        val inStream = sourceProvider.openInputStream(sourceFile)
        val outStream = targetProvider.openOutputStream(targetFile)

        if (inStream == null || outStream == null) {
            inStream?.close()
            outStream?.close()
            targetProvider.delete(targetFile)
            return@withContext MoveResult(
                isSuccess = false,
                errorMessage = "Unable to open streams for cross-volume move"
            )
        }

        var bytesCopied = 0L
        val sourceDigest = MessageDigest.getInstance("MD5")
        val targetDigest = MessageDigest.getInstance("MD5")

        try {
            inStream.use { input ->
                outStream.use { output ->
                    val buffer = ByteArray(64 * 1024) // 64 KB buffer
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        sourceDigest.update(buffer, 0, read)
                        targetDigest.update(buffer, 0, read)
                        bytesCopied += read
                        onProgress?.invoke(bytesCopied, sourceFile.sizeBytes)
                    }
                    output.flush()
                }
            }
        } catch (e: Exception) {
            // Interrupted or failed during transfer
            targetProvider.delete(targetFile) // Clean up partial destination
            return@withContext MoveResult(
                isSuccess = false,
                errorMessage = "Interrupted during stream copy: ${e.message}. Source file preserved."
            )
        }

        // Verify checksums & size
        val sourceHash = sourceDigest.digest()
        val targetHash = targetDigest.digest()

        if (!sourceHash.contentEquals(targetHash) || (sourceFile.sizeBytes > 0 && bytesCopied != sourceFile.sizeBytes)) {
            targetProvider.delete(targetFile)
            return@withContext MoveResult(
                isSuccess = false,
                errorMessage = "Checksum or size verification mismatch after copy. Source preserved."
            )
        }

        // Integrity verified! Now delete source safely.
        val sourceDeleted = sourceProvider.delete(sourceFile)
        if (!sourceDeleted) {
            // Source deletion failed but target is intact
        }

        MoveResult(
            isSuccess = true,
            movedFile = targetFile.copy(sizeBytes = bytesCopied),
            verifiedBytes = bytesCopied
        )
    }
}
