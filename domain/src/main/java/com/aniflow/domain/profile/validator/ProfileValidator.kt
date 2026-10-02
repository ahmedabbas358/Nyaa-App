package com.aniflow.domain.profile.validator

import com.aniflow.domain.profile.model.ProfilePreferences
import com.aniflow.domain.profile.model.UserProfile

class InvalidProfileConfigurationException(
    val violations: List<ProfileValidationViolation>
) : IllegalArgumentException("Invalid profile configuration: ${violations.joinToString("; ") { it.message }}")

data class ProfileValidationViolation(
    val field: String,
    val message: String,
    val isFatal: Boolean = true
)

data class ProfileValidationResult(
    val isValid: Boolean,
    val violations: List<ProfileValidationViolation>
)

object ProfileValidator {

    fun validateOrThrow(profile: UserProfile) {
        val result = validate(profile)
        if (!result.isValid) {
            throw InvalidProfileConfigurationException(result.violations)
        }
    }

    fun validate(profile: UserProfile): ProfileValidationResult {
        val violations = mutableListOf<ProfileValidationViolation>()

        // 1. Profile metadata
        if (profile.name.isBlank()) {
            violations.add(ProfileValidationViolation("name", "Profile name cannot be blank"))
        }

        // 2. Preferences validation
        validatePreferences(profile.preferences, violations)

        // 3. Limits & Policies validation
        if (profile.limits.maxConcurrentDownloads < 1) {
            violations.add(ProfileValidationViolation("limits.maxConcurrentDownloads", "Maximum concurrent downloads must be at least 1"))
        }
        if (profile.downloadPolicy.maxConcurrentTasks < 1) {
            violations.add(ProfileValidationViolation("downloadPolicy.maxConcurrentTasks", "Maximum concurrent download tasks must be at least 1"))
        }
        if (profile.downloadPolicy.maxConcurrentPerProfile != null && profile.downloadPolicy.maxConcurrentPerProfile < 1) {
            violations.add(ProfileValidationViolation("downloadPolicy.maxConcurrentPerProfile", "Max concurrent tasks per profile must be at least 1"))
        }

        return ProfileValidationResult(
            isValid = violations.none { it.isFatal },
            violations = violations
        )
    }

    fun validatePreferences(preferences: ProfilePreferences, violations: MutableList<ProfileValidationViolation> = mutableListOf()): ProfileValidationResult {
        // Resolution contradictions
        val res = preferences.resolution
        if (res.required != null && res.forbidden.contains(res.required)) {
            violations.add(ProfileValidationViolation("resolution", "Resolution ${res.required.displayName} cannot be both Required and Forbidden"))
        }
        if (res.preferred != null && res.forbidden.contains(res.preferred)) {
            violations.add(ProfileValidationViolation("resolution", "Resolution ${res.preferred.displayName} cannot be both Preferred and Forbidden"))
        }
        if (res.required != null && res.allowed.isNotEmpty() && !res.allowed.contains(res.required)) {
            violations.add(ProfileValidationViolation("resolution", "Required resolution ${res.required.displayName} must be included in allowed set"))
        }

        // Codec contradictions
        val codec = preferences.codec
        if (codec.required != null && codec.forbidden.contains(codec.required)) {
            violations.add(ProfileValidationViolation("codec", "Codec ${codec.required.displayName} cannot be both Required and Forbidden"))
        }
        if (codec.preferred != null && codec.forbidden.contains(codec.preferred)) {
            violations.add(ProfileValidationViolation("codec", "Codec ${codec.preferred.displayName} cannot be both Preferred and Forbidden"))
        }

        // Audio contradictions
        val audio = preferences.audio
        if (audio.requiredLanguage != null && audio.forbiddenLanguages.contains(audio.requiredLanguage)) {
            violations.add(ProfileValidationViolation("audio", "Audio language ${audio.requiredLanguage.code} cannot be both Required and Forbidden"))
        }

        // Source contradictions
        val source = preferences.source
        if (source.required != null && source.forbidden.contains(source.required)) {
            violations.add(ProfileValidationViolation("source", "Source ${source.required.displayName} cannot be both Required and Forbidden"))
        }
        if (source.preferred != null && source.forbidden.contains(source.preferred)) {
            violations.add(ProfileValidationViolation("source", "Source ${source.preferred.displayName} cannot be both Preferred and Forbidden"))
        }

        // Size contradictions
        val size = preferences.size
        if (size.minBytes != null && size.maxBytes != null && size.minBytes > size.maxBytes) {
            violations.add(ProfileValidationViolation("size", "Minimum size (${size.minBytes} bytes) cannot exceed maximum size (${size.maxBytes} bytes)"))
        }

        // Entity contradictions
        val groupForbiddenPreferred = preferences.releaseGroup.forbidden.intersect(preferences.releaseGroup.preferred.toSet())
        if (groupForbiddenPreferred.isNotEmpty()) {
            violations.add(ProfileValidationViolation("releaseGroup", "Release groups $groupForbiddenPreferred cannot be both Preferred and Forbidden"))
        }

        val uploaderForbiddenPreferred = preferences.uploader.forbidden.intersect(preferences.uploader.preferred.toSet())
        if (uploaderForbiddenPreferred.isNotEmpty()) {
            violations.add(ProfileValidationViolation("uploader", "Uploaders $uploaderForbiddenPreferred cannot be both Preferred and Forbidden"))
        }

        return ProfileValidationResult(
            isValid = violations.none { it.isFatal },
            violations = violations
        )
    }
}
