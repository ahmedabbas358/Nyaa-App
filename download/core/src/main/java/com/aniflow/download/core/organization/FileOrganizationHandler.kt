package com.aniflow.download.core.organization

import com.aniflow.download.core.model.PlannedDestination
import java.io.File

data class FinalizationResult(
    val isSuccess: Boolean,
    val finalFile: File?,
    val errorMessage: String? = null
)

/**
 * Handles atomic file moves and finalization from temporary .part files to permanent media paths (Section 110, 111, 118, 119).
 */
class FileOrganizationHandler {

    fun finalizeDownload(destination: PlannedDestination): FinalizationResult {
        val partFile = File(destination.tempFilePath)
        val finalFile = File(destination.finalFilePath)

        if (!partFile.exists()) {
            return FinalizationResult(
                isSuccess = false,
                finalFile = null,
                errorMessage = "Temporary download file does not exist: ${partFile.absolutePath}"
            )
        }

        finalFile.parentFile?.mkdirs()

        // Atomic file move / rename
        val moved = partFile.renameTo(finalFile)

        return if (moved || finalFile.exists()) {
            FinalizationResult(
                isSuccess = true,
                finalFile = finalFile
            )
        } else {
            FinalizationResult(
                isSuccess = false,
                finalFile = null,
                errorMessage = "Failed to move file to final destination: ${finalFile.absolutePath}"
            )
        }
    }
}
