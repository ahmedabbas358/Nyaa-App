package com.aniflow.download.core.verification

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

data class VerificationResult(
    val isVerified: Boolean,
    val failureReason: String? = null
)

/**
 * Validates downloaded media integrity before finalization (Section 110, 111, 112, 113).
 */
class IntegrityVerificationEngine {

    fun verifyFileSize(file: File, expectedBytes: Long?): VerificationResult {
        if (!file.exists()) {
            return VerificationResult(false, "File does not exist: ${file.absolutePath}")
        }
        if (file.length() == 0L) {
            return VerificationResult(false, "File is zero bytes (empty download)")
        }
        if (expectedBytes != null && expectedBytes > 0L) {
            if (file.length() != expectedBytes) {
                return VerificationResult(
                    false,
                    "File size mismatch: actual ${file.length()} bytes != expected $expectedBytes bytes"
                )
            }
        }
        return VerificationResult(true)
    }

    fun verifyChecksum(file: File, expectedHash: String, algorithm: String = "SHA-256"): VerificationResult {
        if (!file.exists()) {
            return VerificationResult(false, "File does not exist: ${file.absolutePath}")
        }

        return try {
            val digest = MessageDigest.getInstance(algorithm)
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            val calculatedHash = digest.digest().joinToString("") { "%02x".format(it) }
            val matched = calculatedHash.equals(expectedHash.trim(), ignoreCase = true)
            if (matched) {
                VerificationResult(true)
            } else {
                VerificationResult(false, "Checksum mismatch: calculated $calculatedHash != expected $expectedHash")
            }
        } catch (e: Exception) {
            VerificationResult(false, "Checksum verification error: ${e.message}")
        }
    }
}
