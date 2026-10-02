package com.aniflow.domain.selection.relaxation

import com.aniflow.domain.selection.model.ConstraintViolation
import com.aniflow.domain.selection.model.EligibilityResult
import com.aniflow.domain.selection.model.UserSelectionPreferences

data class RelaxationSuggestion(
    val title: String,
    val description: String,
    val targetField: String,
    val proposedAction: String,
    val priority: Int
)

/**
 * Generates smart relaxation suggestions when no eligible candidate is found (Section 100, 101, 102, 103).
 * Never relaxes a hard rule automatically; presents suggestions for user confirmation.
 */
class RelaxationEngine {

    fun generateSuggestions(
        eligibilityResults: List<EligibilityResult>,
        preferences: UserSelectionPreferences
    ): List<RelaxationSuggestion> {
        val suggestions = mutableListOf<RelaxationSuggestion>()
        val allViolations = eligibilityResults.flatMap { it.violations }

        // 1. Uploader restriction
        if (allViolations.any { it is ConstraintViolation.BlockedUploader } || preferences.uploader.preferred.isNotEmpty()) {
            suggestions += RelaxationSuggestion(
                title = "Allow other uploaders",
                description = "Expand candidate search to any trusted uploader instead of restricting to preferred uploaders.",
                targetField = "uploader",
                proposedAction = "CLEAR_UPLOADER_RESTRICTION",
                priority = 1
            )
        }

        // 2. Codec restriction
        if (allViolations.any { it is ConstraintViolation.WrongCodec }) {
            suggestions += RelaxationSuggestion(
                title = "Allow alternative video codecs",
                description = "Permit AVC / H.264 or AV1 alongside HEVC.",
                targetField = "codec",
                proposedAction = "ALLOW_ANY_CODEC",
                priority = 2
            )
        }

        // 3. Size limit exceeded
        if (allViolations.any { it is ConstraintViolation.SizeExceeded }) {
            suggestions += RelaxationSuggestion(
                title = "Increase file size limit",
                description = "Raise maximum allowable size to accommodate higher bitrate releases.",
                targetField = "size",
                proposedAction = "INCREASE_MAX_SIZE",
                priority = 3
            )
        }

        // 4. Resolution constraint
        if (allViolations.any { it is ConstraintViolation.WrongResolution || it is ConstraintViolation.ResolutionTooLow }) {
            suggestions += RelaxationSuggestion(
                title = "Allow 720p fallback resolution",
                description = "Enable 720p quality when 1080p is unavailable.",
                targetField = "resolution",
                proposedAction = "ALLOW_720P_FALLBACK",
                priority = 4
            )
        }

        // 5. Seeders constraint
        if (allViolations.any { it is ConstraintViolation.InsufficientSeeders }) {
            suggestions += RelaxationSuggestion(
                title = "Lower minimum seeder requirement",
                description = "Allow releases with fewer seeds (e.g. 1-2 seeds) to be evaluated.",
                targetField = "seeders",
                proposedAction = "LOWER_MIN_SEEDERS",
                priority = 5
            )
        }

        return suggestions.sortedBy { it.priority }
    }
}
