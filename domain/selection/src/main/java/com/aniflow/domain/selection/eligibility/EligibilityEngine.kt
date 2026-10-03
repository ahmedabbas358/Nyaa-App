package com.aniflow.domain.selection.eligibility

import com.aniflow.domain.intelligence.model.ReleaseCandidate
import com.aniflow.domain.selection.model.AvailabilityAction
import com.aniflow.domain.selection.model.ConstraintViolation
import com.aniflow.domain.selection.model.DownloadProfile
import com.aniflow.domain.selection.model.EligibilityResult
import com.aniflow.domain.selection.model.ExistingMediaState
import com.aniflow.domain.selection.model.HardConstraint
import com.aniflow.domain.selection.model.PreferenceMode
import com.aniflow.domain.selection.model.ResolutionMatchMode
import com.aniflow.domain.selection.model.SelectionWarning
import com.aniflow.domain.selection.model.UserSelectionPreferences
import com.aniflow.domain.selection.model.WarningSeverity
import com.aniflow.domain.valueobject.LanguageCode
import com.aniflow.domain.valueobject.MediaSource
import com.aniflow.domain.valueobject.Resolution

/**
 * Validates hard constraints on candidate releases before any scoring occurs (Section 8, 36, 37, 38, 39, 48).
 * Hard constraints are absolute: if violated, Candidate is marked Ineligible regardless of potential score.
 */
class EligibilityEngine {

    fun evaluate(
        candidate: ReleaseCandidate,
        preferences: UserSelectionPreferences,
        profile: DownloadProfile?,
        existingState: ExistingMediaState = ExistingMediaState.Missing
    ): EligibilityResult {
        val violations = mutableListOf<ConstraintViolation>()
        val warnings = mutableListOf<SelectionWarning>()
        val release = candidate.release
        val technical = release.technicalMetadata

        // 1. Evaluate explicit hard constraints from profile
        profile?.hardConstraints?.forEach { constraint ->
            when (constraint) {
                is HardConstraint.RequireResolution -> {
                    if (technical.resolution != constraint.resolution) {
                        violations += ConstraintViolation.WrongResolution(technical.resolution, constraint.resolution)
                    }
                }
                is HardConstraint.RequireAtLeastResolution -> {
                    val actualHeight = technical.resolution?.height ?: 0
                    if (actualHeight < constraint.minResolution.height) {
                        violations += ConstraintViolation.ResolutionTooLow(technical.resolution, constraint.minResolution)
                    }
                }
                is HardConstraint.RequireAudioLanguage -> {
                    val hasAudio = technical.audioTracks.any { it.language == constraint.language }
                    if (!hasAudio) {
                        violations += ConstraintViolation.MissingAudio(constraint.language)
                    }
                }
                is HardConstraint.RequireSubtitleLanguage -> {
                    val hasSubtitle = technical.subtitles.any { it.language == constraint.language }
                    if (!hasSubtitle) {
                        violations += ConstraintViolation.MissingSubtitle(constraint.language)
                    }
                }
                is HardConstraint.BlockUploader -> {
                    if (release.uploaderName?.equals(constraint.uploader, ignoreCase = true) == true) {
                        violations += ConstraintViolation.BlockedUploader(constraint.uploader)
                    }
                }
                is HardConstraint.BlockReleaseGroup -> {
                    if (release.groupCandidate?.equals(constraint.group, ignoreCase = true) == true) {
                        violations += ConstraintViolation.BlockedReleaseGroup(constraint.group)
                    }
                }
                is HardConstraint.MaxFileSize -> {
                    val actualBytes = release.rawMetadata["sizeBytes"]?.toLongOrNull()
                    if (actualBytes != null && actualBytes > constraint.maxSize.bytes) {
                        violations += ConstraintViolation.SizeExceeded(actualBytes, constraint.maxSize.bytes)
                    }
                }
                is HardConstraint.MinSeeders -> {
                    val seeders = release.rawMetadata["seeders"]?.toIntOrNull() ?: 0
                    if (seeders < constraint.minSeeders) {
                        violations += ConstraintViolation.InsufficientSeeders(seeders, constraint.minSeeders)
                    }
                }
                is HardConstraint.AllowSourcesOnly -> {
                    if (release.source != null && !constraint.allowedSources.contains(release.source)) {
                        violations += ConstraintViolation.UnsupportedSource(release.source, constraint.allowedSources)
                    }
                }
                is HardConstraint.RequireCodec -> {
                    if (technical.videoCodec != constraint.codec) {
                        violations += ConstraintViolation.WrongCodec(technical.videoCodec, constraint.codec)
                    }
                }
                is HardConstraint.ForbidDuplicates -> {
                    if (existingState == ExistingMediaState.Downloaded || existingState == ExistingMediaState.Downloading) {
                        violations += ConstraintViolation.Duplicate("Episode is already in local state: $existingState")
                    }
                }
            }
        }

        // 2. Resolution policy check (Exact match mode acts as hard requirement)
        if (preferences.resolution.mode == ResolutionMatchMode.Exact) {
            if (technical.resolution != preferences.resolution.target) {
                violations += ConstraintViolation.WrongResolution(technical.resolution, preferences.resolution.target)
            }
        } else if (preferences.resolution.mode == ResolutionMatchMode.AtLeast) {
            val actualHeight = technical.resolution?.height ?: 0
            if (actualHeight < preferences.resolution.target.height) {
                violations += ConstraintViolation.ResolutionTooLow(technical.resolution, preferences.resolution.target)
            }
        }

        // 3. Blocked uploader & release group checks from preferences
        if (preferences.uploader.getDisposition(release.uploaderName) == PreferenceMode.Forbidden) {
            violations += ConstraintViolation.BlockedUploader(release.uploaderName ?: "Unknown")
        }
        if (preferences.releaseGroup.getDisposition(release.groupCandidate) == PreferenceMode.Forbidden) {
            violations += ConstraintViolation.BlockedReleaseGroup(release.groupCandidate ?: "Unknown")
        }

        // 4. Subtitle policy check (Required subtitles)
        preferences.subtitles.requiredLanguages.forEach { reqLang ->
            val hasLang = technical.subtitles.any { it.language == reqLang }
            if (!hasLang) {
                violations += ConstraintViolation.MissingSubtitle(reqLang)
            }
        }

        // 5. Audio language policy check (Required audio)
        if (preferences.audioLanguage.isRequired) {
            preferences.audioLanguage.primary.forEach { reqAudio ->
                val hasAudio = technical.audioTracks.any { it.language == reqAudio }
                if (!hasAudio) {
                    violations += ConstraintViolation.MissingAudio(reqAudio)
                }
            }
        }

        // 6. Hard file size limit check
        val actualBytes = release.rawMetadata["sizeBytes"]?.toLongOrNull()
        preferences.sizePolicy.hardMaxBytes?.let { maxBytes ->
            if (actualBytes != null && actualBytes > maxBytes) {
                violations += ConstraintViolation.SizeExceeded(actualBytes, maxBytes)
            }
        }

        // 7. Seeder & availability policy check
        val seeders = release.rawMetadata["seeders"]?.toIntOrNull() ?: 0
        if (seeders == 0 && preferences.seederPolicy.onZeroSeeders == AvailabilityAction.Reject) {
            violations += ConstraintViolation.InsufficientSeeders(0, preferences.seederPolicy.minSeeders)
        } else if (seeders < preferences.seederPolicy.minSeeders && preferences.seederPolicy.onBelowMinimum == AvailabilityAction.Reject) {
            violations += ConstraintViolation.InsufficientSeeders(seeders, preferences.seederPolicy.minSeeders)
        } else if (seeders < preferences.seederPolicy.minSeeders && preferences.seederPolicy.onBelowMinimum == AvailabilityAction.Warn) {
            warnings += SelectionWarning(
                code = "LOW_SEEDERS",
                message = "Only $seeders seeders currently available (preferred ${preferences.seederPolicy.preferredSeeders}+)",
                severity = WarningSeverity.High
            )
        }

        // 8. Forbidden sources check
        if (release.source != null && preferences.source.forbidden.contains(release.source)) {
            violations += ConstraintViolation.UnsupportedSource(release.source, preferences.source.allowed)
        }

        // 9. Warnings evaluation
        if (candidate.confidence < 0.70) {
            warnings += SelectionWarning(
                code = "LOW_CONFIDENCE",
                message = "Episode parsing confidence is ${(candidate.confidence * 100).toInt()}%",
                severity = WarningSeverity.Medium
            )
        }

        preferences.sizePolicy.preferredMaxBytes?.let { prefMax ->
            if (actualBytes != null && actualBytes > prefMax) {
                warnings += SelectionWarning(
                    code = "EXCEEDS_PREFERRED_SIZE",
                    message = "File size (${actualBytes / (1024 * 1024)} MB) exceeds preferred target (${prefMax / (1024 * 1024)} MB)",
                    severity = WarningSeverity.Low
                )
            }
        }

        return EligibilityResult(
            eligible = violations.isEmpty(),
            violations = violations,
            warnings = warnings
        )
    }

    /**
     * Overload for SelectionCandidate (Step 21 Domain Model).
     */
    fun evaluateCandidate(
        candidate: com.aniflow.domain.selection.context.SelectionCandidate,
        preferences: UserSelectionPreferences,
        profile: DownloadProfile?,
        existingState: com.aniflow.domain.selection.model.ExistingMediaState = com.aniflow.domain.selection.model.ExistingMediaState.Missing
    ): EligibilityResult {
        val violations = mutableListOf<ConstraintViolation>()
        val warnings = mutableListOf<SelectionWarning>()
        val technical = candidate.technical

        // 1. Evaluate explicit hard constraints from profile
        profile?.hardConstraints?.forEach { constraint ->
            when (constraint) {
                is HardConstraint.RequireResolution -> {
                    if (technical.resolution != constraint.resolution) {
                        violations += ConstraintViolation.WrongResolution(technical.resolution, constraint.resolution)
                    }
                }
                is HardConstraint.RequireAtLeastResolution -> {
                    val actualHeight = technical.resolution?.height ?: 0
                    if (actualHeight < constraint.minResolution.height) {
                        violations += ConstraintViolation.ResolutionTooLow(technical.resolution, constraint.minResolution)
                    }
                }
                is HardConstraint.RequireAudioLanguage -> {
                    val hasAudio = technical.audioTracks.any { it.language == constraint.language }
                    if (!hasAudio) {
                        violations += ConstraintViolation.MissingAudio(constraint.language)
                    }
                }
                is HardConstraint.RequireSubtitleLanguage -> {
                    val hasSubtitle = technical.subtitles.any { it.language == constraint.language }
                    if (!hasSubtitle) {
                        violations += ConstraintViolation.MissingSubtitle(constraint.language)
                    }
                }
                is HardConstraint.BlockUploader -> {
                    if (candidate.uploader?.equals(constraint.uploader, ignoreCase = true) == true) {
                        violations += ConstraintViolation.BlockedUploader(constraint.uploader)
                    }
                }
                is HardConstraint.BlockReleaseGroup -> {
                    if (candidate.releaseGroup?.equals(constraint.group, ignoreCase = true) == true) {
                        violations += ConstraintViolation.BlockedReleaseGroup(constraint.group)
                    }
                }
                is HardConstraint.MaxFileSize -> {
                    val actualBytes = candidate.size?.bytes
                    if (actualBytes != null && actualBytes > constraint.maxSize.bytes) {
                        violations += ConstraintViolation.SizeExceeded(actualBytes, constraint.maxSize.bytes)
                    }
                }
                is HardConstraint.MinSeeders -> {
                    val seeders = candidate.seeders ?: 0
                    if (seeders < constraint.minSeeders) {
                        violations += ConstraintViolation.InsufficientSeeders(seeders, constraint.minSeeders)
                    }
                }
                is HardConstraint.AllowSourcesOnly -> {
                    if (technical.source != null && !constraint.allowedSources.contains(technical.source)) {
                        violations += ConstraintViolation.UnsupportedSource(technical.source, constraint.allowedSources)
                    }
                }
                is HardConstraint.RequireCodec -> {
                    if (technical.videoCodec != constraint.codec) {
                        violations += ConstraintViolation.WrongCodec(technical.videoCodec, constraint.codec)
                    }
                }
                is HardConstraint.ForbidDuplicates -> {
                    if (existingState is com.aniflow.domain.selection.model.ExistingMediaState.Satisfied) {
                        violations += ConstraintViolation.Duplicate("Episode is already satisfied in library")
                    }
                }
            }
        }

        // 2. Resolution policy
        if (preferences.resolution.mode == ResolutionMatchMode.Exact) {
            if (technical.resolution != preferences.resolution.target) {
                violations += ConstraintViolation.WrongResolution(technical.resolution, preferences.resolution.target)
            }
        } else if (preferences.resolution.mode == ResolutionMatchMode.AtLeast) {
            val actualHeight = technical.resolution?.height ?: 0
            if (actualHeight < preferences.resolution.target.height) {
                violations += ConstraintViolation.ResolutionTooLow(technical.resolution, preferences.resolution.target)
            }
        }

        // 3. Blocked uploader & group
        if (preferences.uploader.getDisposition(candidate.uploader) == PreferenceMode.Forbidden) {
            violations += ConstraintViolation.BlockedUploader(candidate.uploader ?: "Unknown")
        }
        if (preferences.releaseGroup.getDisposition(candidate.releaseGroup) == PreferenceMode.Forbidden) {
            violations += ConstraintViolation.BlockedReleaseGroup(candidate.releaseGroup ?: "Unknown")
        }

        // 4. Subtitles
        preferences.subtitles.requiredLanguages.forEach { reqLang ->
            if (!technical.subtitles.any { it.language == reqLang }) {
                violations += ConstraintViolation.MissingSubtitle(reqLang)
            }
        }

        // 5. Audio
        if (preferences.audioLanguage.isRequired) {
            preferences.audioLanguage.primary.forEach { reqAudio ->
                if (!technical.audioTracks.any { it.language == reqAudio }) {
                    violations += ConstraintViolation.MissingAudio(reqAudio)
                }
            }
        }

        // 6. Max size
        val actualBytes = candidate.size?.bytes
        preferences.sizePolicy.hardMaxBytes?.let { maxBytes ->
            if (actualBytes != null && actualBytes > maxBytes) {
                violations += ConstraintViolation.SizeExceeded(actualBytes, maxBytes)
            }
        }

        // 7. Seeders
        val seeders = candidate.seeders ?: 0
        if (seeders == 0 && preferences.seederPolicy.onZeroSeeders == AvailabilityAction.Reject) {
            violations += ConstraintViolation.InsufficientSeeders(0, preferences.seederPolicy.minSeeders)
        } else if (seeders < preferences.seederPolicy.minSeeders && preferences.seederPolicy.onBelowMinimum == AvailabilityAction.Reject) {
            violations += ConstraintViolation.InsufficientSeeders(seeders, preferences.seederPolicy.minSeeders)
        } else if (seeders < preferences.seederPolicy.minSeeders && preferences.seederPolicy.onBelowMinimum == AvailabilityAction.Warn) {
            warnings += SelectionWarning(
                code = "LOW_SEEDERS",
                message = "Only $seeders seeders currently available",
                severity = WarningSeverity.High
            )
        }

        // 8. Forbidden sources
        if (technical.source != null && preferences.source.forbidden.contains(technical.source)) {
            violations += ConstraintViolation.UnsupportedSource(technical.source, preferences.source.allowed)
        }

        return EligibilityResult(
            eligible = violations.isEmpty(),
            violations = violations,
            warnings = warnings
        )
    }
}

