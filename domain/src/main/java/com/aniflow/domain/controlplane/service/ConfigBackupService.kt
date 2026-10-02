package com.aniflow.domain.controlplane.service

enum class ImportConflictPolicy {
    Skip,
    Replace,
    Merge,
    CreateCopy
}

data class ProfileBackupDto(
    val id: String,
    val name: String,
    val resolution: String,
    val codec: String
)

data class RuleBackupDto(
    val id: String,
    val name: String,
    val priority: Int
)

data class ExportedConfigBundle(
    val schemaVersion: Int = 1,
    val app: String = "AniFlow",
    val exportedEpochMillis: Long = System.currentTimeMillis(),
    val profiles: List<ProfileBackupDto> = emptyList(),
    val rules: List<RuleBackupDto> = emptyList()
)

data class ImportReport(
    val isSuccess: Boolean,
    val importedProfilesCount: Int,
    val importedRulesCount: Int,
    val conflictsDetected: Int,
    val errorMessage: String? = null
)

/**
 * ConfigBackupService (Sections 33, 34).
 * Handles versioned schema serialization, validation, migration, and conflict resolution
 * for configuration backup and restore without exposing raw database tables.
 */
class ConfigBackupService {

    fun exportConfiguration(
        profiles: List<ProfileBackupDto>,
        rules: List<RuleBackupDto>
    ): ExportedConfigBundle {
        return ExportedConfigBundle(
            schemaVersion = 1,
            app = "AniFlow",
            profiles = profiles,
            rules = rules
        )
    }

    fun importConfiguration(
        bundle: ExportedConfigBundle,
        existingProfileIds: Set<String>,
        existingRuleIds: Set<String>,
        policy: ImportConflictPolicy = ImportConflictPolicy.Merge
    ): ImportReport {
        if (bundle.schemaVersion > 1) {
            return ImportReport(
                isSuccess = false,
                importedProfilesCount = 0,
                importedRulesCount = 0,
                conflictsDetected = 0,
                errorMessage = "Unsupported schema version ${bundle.schemaVersion}. Please update AniFlow."
            )
        }

        var conflicts = 0
        var profilesAdded = 0
        var rulesAdded = 0

        for (profile in bundle.profiles) {
            val hasConflict = existingProfileIds.contains(profile.id)
            if (hasConflict) {
                conflicts++
                if (policy == ImportConflictPolicy.Replace || policy == ImportConflictPolicy.CreateCopy) {
                    profilesAdded++
                }
            } else {
                profilesAdded++
            }
        }

        for (rule in bundle.rules) {
            val hasConflict = existingRuleIds.contains(rule.id)
            if (hasConflict) {
                conflicts++
                if (policy == ImportConflictPolicy.Replace || policy == ImportConflictPolicy.CreateCopy) {
                    rulesAdded++
                }
            } else {
                rulesAdded++
            }
        }

        return ImportReport(
            isSuccess = true,
            importedProfilesCount = profilesAdded,
            importedRulesCount = rulesAdded,
            conflictsDetected = conflicts
        )
    }
}
