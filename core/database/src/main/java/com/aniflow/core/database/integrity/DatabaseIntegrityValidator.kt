package com.aniflow.core.database.integrity

/**
 * Enforces STEP 13 Section 16 (Database Integrity) and Section 78 (Data Corruption Tests).
 *
 * Runs non-destructive integrity diagnostics over entities, foreign key relationships,
 * orphan records, and boundary constraints.
 */
data class IntegrityViolation(
    val entityType: String,
    val identifier: String,
    val issue: String,
    val severity: Severity = Severity.Error
) {
    enum class Severity { Warning, Error, Critical }
}

data class DatabaseIntegrityReport(
    val isHealthy: Boolean,
    val totalChecked: Int,
    val violations: List<IntegrityViolation>
)

class DatabaseIntegrityValidator {

    fun validateTaskAndFiles(
        tasks: List<com.aniflow.domain.model.aggregate.download.DownloadTask>,
        files: List<com.aniflow.domain.model.aggregate.download.DownloadFile>
    ): DatabaseIntegrityReport {
        val violations = mutableListOf<IntegrityViolation>()
        val taskIds = tasks.map { it.id }.toSet()

        // 1. Check for orphan files without parent tasks
        for (file in files) {
            if (!taskIds.contains(file.taskId)) {
                violations.add(
                    IntegrityViolation(
                        entityType = "DownloadFile",
                        identifier = file.id.value,
                        issue = "Orphan download file with no parent task: ${file.taskId.value}",
                        severity = IntegrityViolation.Severity.Error
                    )
                )
            }

            // 2. Check for impossible/negative byte sizes
            if (file.totalBytes.bytes < 0) {
                violations.add(
                    IntegrityViolation(
                        entityType = "DownloadFile",
                        identifier = file.id.value,
                        issue = "Negative file size detected: ${file.totalBytes.bytes}",
                        severity = IntegrityViolation.Severity.Critical
                    )
                )
            }
        }

        // 3. Check for task state consistency
        for (task in tasks) {
            if (task.downloadedBytes.bytes > task.totalBytes.bytes && task.totalBytes.bytes > 0) {
                violations.add(
                    IntegrityViolation(
                        entityType = "DownloadTask",
                        identifier = task.id.value,
                        issue = "Downloaded bytes (${task.downloadedBytes.bytes}) exceeds total size (${task.totalBytes.bytes})",
                        severity = IntegrityViolation.Severity.Warning
                    )
                )
            }
        }

        return DatabaseIntegrityReport(
            isHealthy = violations.isEmpty(),
            totalChecked = tasks.size + files.size,
            violations = violations
        )
    }
}
