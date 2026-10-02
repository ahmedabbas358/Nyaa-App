package com.aniflow.domain.backup.service

import com.aniflow.domain.backup.model.AniFlowBackupBundle
import com.aniflow.domain.backup.model.BACKUP_SCHEMA_VERSION
import com.aniflow.domain.backup.model.BackupValidationReport
import com.aniflow.domain.backup.model.CURRENT_APP_VERSION
import java.security.MessageDigest
import java.time.Instant

/**
 * BackupRestoreService (Sections 41, 42, 43).
 * Validates integrity checksums, checks schema versions, and generates pre-flight previews
 * while strictly prohibiting blind restoration of active runtime downloads or stale URI permissions.
 */
class BackupRestoreService {

    fun validateBackup(bundle: AniFlowBackupBundle): BackupValidationReport {
        // 1. Schema version compatibility check
        if (bundle.schemaVersion > BACKUP_SCHEMA_VERSION) {
            return BackupValidationReport(
                isValid = false,
                schemaVersion = bundle.schemaVersion,
                appVersion = bundle.appVersion,
                createdAt = Instant.ofEpochMilli(bundle.createdAtEpochMs),
                profilesCount = bundle.profilesCount,
                rulesCount = bundle.rulesCount,
                watchProgressCount = bundle.watchProgressCount,
                collectionsCount = bundle.collectionsCount,
                errorMessage = "Incompatible backup schema version ${bundle.schemaVersion}. Current AniFlow app supports version $BACKUP_SCHEMA_VERSION."
            )
        }

        // 2. Checksum integrity validation
        val computedChecksum = computeSha256(bundle.payloadJson)
        if (computedChecksum != bundle.checksum) {
            return BackupValidationReport(
                isValid = false,
                schemaVersion = bundle.schemaVersion,
                appVersion = bundle.appVersion,
                createdAt = Instant.ofEpochMilli(bundle.createdAtEpochMs),
                profilesCount = bundle.profilesCount,
                rulesCount = bundle.rulesCount,
                watchProgressCount = bundle.watchProgressCount,
                collectionsCount = bundle.collectionsCount,
                errorMessage = "Checksum mismatch: Backup payload has been altered or corrupted."
            )
        }

        // 3. Safety checks (Section 43: What Backup Must NOT Blindly Restore)
        val warnings = mutableListOf<String>()
        if (bundle.payloadJson.contains("\"activeDownloadState\"") || bundle.payloadJson.contains("\"partialFiles\"")) {
            warnings.add("Active download tasks and temporary files in backup will be safely skipped during restoration.")
        }
        if (bundle.payloadJson.contains("\"storageUriPermission\"")) {
            warnings.add("Storage URI permissions must be re-authorized manually after restoration.")
        }

        return BackupValidationReport(
            isValid = true,
            schemaVersion = bundle.schemaVersion,
            appVersion = bundle.appVersion,
            createdAt = Instant.ofEpochMilli(bundle.createdAtEpochMs),
            profilesCount = bundle.profilesCount,
            rulesCount = bundle.rulesCount,
            watchProgressCount = bundle.watchProgressCount,
            collectionsCount = bundle.collectionsCount,
            warnings = warnings
        )
    }

    fun createBackupBundle(
        payloadJson: String,
        profilesCount: Int,
        rulesCount: Int,
        watchProgressCount: Int,
        collectionsCount: Int
    ): AniFlowBackupBundle {
        val checksum = computeSha256(payloadJson)
        return AniFlowBackupBundle(
            schemaVersion = BACKUP_SCHEMA_VERSION,
            appVersion = CURRENT_APP_VERSION,
            createdAtEpochMs = System.currentTimeMillis(),
            checksum = checksum,
            profilesCount = profilesCount,
            rulesCount = rulesCount,
            watchProgressCount = watchProgressCount,
            collectionsCount = collectionsCount,
            payloadJson = payloadJson
        )
    }

    private fun computeSha256(data: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(data.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
