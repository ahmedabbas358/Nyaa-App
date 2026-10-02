package com.aniflow.domain.selection.model

import com.aniflow.domain.valueobject.ByteSize
import com.aniflow.domain.valueobject.LanguageCode
import com.aniflow.domain.valueobject.MediaSource
import com.aniflow.domain.valueobject.Resolution
import com.aniflow.domain.valueobject.VideoCodec

/**
 * Hard constraints that MUST be satisfied for a release candidate to be eligible.
 * Failure of any hard constraint immediately disqualifies the candidate.
 */
sealed interface HardConstraint {
    data class RequireResolution(val resolution: Resolution) : HardConstraint
    data class RequireAtLeastResolution(val minResolution: Resolution) : HardConstraint
    data class RequireAudioLanguage(val language: LanguageCode) : HardConstraint
    data class RequireSubtitleLanguage(val language: LanguageCode) : HardConstraint
    data class BlockUploader(val uploader: String) : HardConstraint
    data class BlockReleaseGroup(val group: String) : HardConstraint
    data class MaxFileSize(val maxSize: ByteSize) : HardConstraint
    data class MinSeeders(val minSeeders: Int) : HardConstraint
    data class AllowSourcesOnly(val allowedSources: Set<MediaSource>) : HardConstraint
    data class RequireCodec(val codec: VideoCodec) : HardConstraint
    data object ForbidDuplicates : HardConstraint
}

/**
 * Granular violation description when a candidate fails a hard constraint.
 */
sealed interface ConstraintViolation {
    val description: String

    data class WrongResolution(val actual: Resolution?, val expected: Resolution) : ConstraintViolation {
        override val description = "Resolution ${actual?.displayName ?: "Unknown"} does not match required ${expected.displayName}"
    }

    data class ResolutionTooLow(val actual: Resolution?, val minimum: Resolution) : ConstraintViolation {
        override val description = "Resolution ${actual?.displayName ?: "Unknown"} is below required minimum ${minimum.displayName}"
    }

    data class MissingSubtitle(val requiredLanguage: LanguageCode) : ConstraintViolation {
        override val description = "Missing required subtitle language: ${requiredLanguage.code}"
    }

    data class MissingAudio(val requiredLanguage: LanguageCode) : ConstraintViolation {
        override val description = "Missing required audio language: ${requiredLanguage.code}"
    }

    data class BlockedUploader(val uploader: String) : ConstraintViolation {
        override val description = "Uploader '$uploader' is blocked by user policy"
    }

    data class BlockedReleaseGroup(val group: String) : ConstraintViolation {
        override val description = "Release group '$group' is blocked by user policy"
    }

    data class SizeExceeded(val actualBytes: Long, val maxBytes: Long) : ConstraintViolation {
        override val description = "File size (${actualBytes / (1024 * 1024)} MB) exceeds maximum limit (${maxBytes / (1024 * 1024)} MB)"
    }

    data class InsufficientSeeders(val actual: Int, val minimum: Int) : ConstraintViolation {
        override val description = "Available seeders ($actual) below required minimum ($minimum)"
    }

    data class UnsupportedSource(val actual: MediaSource?, val allowed: Set<MediaSource>) : ConstraintViolation {
        override val description = "Source '${actual?.displayName ?: "Unknown"}' is not among allowed sources: ${allowed.map { it.displayName }}"
    }

    data class WrongCodec(val actual: VideoCodec?, val expected: VideoCodec) : ConstraintViolation {
        override val description = "Video codec ${actual?.displayName ?: "Unknown"} does not match required ${expected.displayName}"
    }

    data class Duplicate(val reason: String) : ConstraintViolation {
        override val description = "Candidate is a duplicate: $reason"
    }

    data class InvalidRelease(val reason: String) : ConstraintViolation {
        override val description = "Release candidate is invalid: $reason"
    }

    data class RuleViolation(val ruleName: String, val reason: String) : ConstraintViolation {
        override val description = "Violated rule '$ruleName': $reason"
    }
}

/**
 * Non-fatal warnings about candidate properties that do not disqualify it.
 */
data class SelectionWarning(
    val code: String,
    val message: String,
    val severity: WarningSeverity = WarningSeverity.Medium
)

enum class WarningSeverity {
    Low,
    Medium,
    High
}

/**
 * Result of evaluating candidate eligibility through the Eligibility Engine.
 */
data class EligibilityResult(
    val eligible: Boolean,
    val violations: List<ConstraintViolation> = emptyList(),
    val warnings: List<SelectionWarning> = emptyList()
) {
    companion object {
        val Eligible = EligibilityResult(eligible = true)

        fun rejected(violations: List<ConstraintViolation>, warnings: List<SelectionWarning> = emptyList()) =
            EligibilityResult(
                eligible = false,
                violations = violations,
                warnings = warnings
            )
    }
}

/**
 * Traceable evaluation record for user-facing transparency.
 */
data class ConstraintEvaluation(
    val name: String,
    val satisfied: Boolean,
    val isHardConstraint: Boolean,
    val message: String
)

/**
 * The media state of the episode within local storage / download engine.
 */
enum class ExistingMediaState {
    Missing,
    Queued,
    Downloading,
    Downloaded,
    Failed
}
