package com.aniflow.domain.backup.model

import java.time.Instant

const val BACKUP_SCHEMA_VERSION = 1
const val CURRENT_APP_VERSION = "1.0.0"

/**
 * Structured Portable Backup Bundle (Section 41, 42).
 * Contains JSON serialized entities, checksum verification, and application metadata.
 */
data class AniFlowBackupBundle(
    val schemaVersion: Int = BACKUP_SCHEMA_VERSION,
    val appVersion: String = CURRENT_APP_VERSION,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val checksum: String,
    val profilesCount: Int = 0,
    val rulesCount: Int = 0,
    val watchProgressCount: Int = 0,
    val collectionsCount: Int = 0,
    val payloadJson: String
)

/**
 * Non-destructive preview report generated prior to database restoration (Section 42).
 */
data class BackupValidationReport(
    val isValid: Boolean,
    val schemaVersion: Int,
    val appVersion: String,
    val createdAt: Instant,
    val profilesCount: Int,
    val rulesCount: Int,
    val watchProgressCount: Int,
    val collectionsCount: Int,
    val warnings: List<String> = emptyList(),
    val errorMessage: String? = null
)
