package com.aniflow.domain.profile.usecase

import com.aniflow.domain.controlplane.models.AdvancedRule
import com.aniflow.domain.controlplane.service.CandidateReleaseContext
import com.aniflow.domain.identity.ProfileId
import com.aniflow.domain.profile.model.UserProfile
import com.aniflow.domain.profile.repository.ProfileRepository
import com.aniflow.domain.profile.resolver.ProfileResolutionContext
import com.aniflow.domain.profile.resolver.ProfileResolver
import com.aniflow.domain.profile.resolver.ResolvedProfileResult
import com.aniflow.domain.profile.validator.ProfileValidationResult
import com.aniflow.domain.profile.validator.ProfileValidator
import com.aniflow.domain.rules.conflict.RuleConflict
import com.aniflow.domain.rules.conflict.RuleConflictDetector
import com.aniflow.domain.rules.simulation.RuleSimulationReport
import com.aniflow.domain.rules.simulation.RuleSimulator
import kotlinx.coroutines.flow.Flow
import java.time.Instant

class GetProfilesUseCase(
    private val repository: ProfileRepository
) {
    fun observe(): Flow<List<UserProfile>> = repository.observeAllProfiles()
    suspend operator fun invoke(): List<UserProfile> = repository.getAllProfiles()
}

class GetDefaultProfileUseCase(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(): UserProfile = repository.getDefaultProfile()
}

class SaveProfileUseCase(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(profile: UserProfile): ProfileValidationResult {
        // Enforce contradiction and logic validation (Section 9, 46)
        ProfileValidator.validateOrThrow(profile)

        val existing = repository.getProfileById(profile.id)
        val profileToSave = if (existing != null) {
            // Version incrementing on meaningful mutation (Section 34)
            profile.copy(
                version = existing.version + 1,
                updatedAt = Instant.now()
            )
        } else {
            profile.copy(
                version = 1,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        }

        return repository.saveProfile(profileToSave)
    }
}

class SetDefaultProfileUseCase(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(profileId: ProfileId) {
        repository.setDefaultProfile(profileId)
    }
}

class DuplicateProfileUseCase(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(profileId: ProfileId, customName: String? = null): UserProfile {
        return repository.duplicateProfile(profileId, customName)
    }
}

class DeleteProfileUseCase(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(profileId: ProfileId): Boolean {
        val target = repository.getProfileById(profileId)
            ?: return false

        require(!target.isDefault) {
            "Cannot delete the default profile. Please mark another profile as default first."
        }

        return repository.deleteProfile(profileId)
    }
}

class ResolveProfileUseCase(
    private val resolver: ProfileResolver = ProfileResolver()
) {
    operator fun invoke(context: ProfileResolutionContext): ResolvedProfileResult {
        return resolver.resolve(context)
    }
}

class SimulateRuleUseCase(
    private val simulator: RuleSimulator = RuleSimulator()
) {
    operator fun invoke(
        rule: AdvancedRule,
        candidate: CandidateReleaseContext,
        associatedProfile: UserProfile? = null
    ): RuleSimulationReport {
        return simulator.simulate(rule, candidate, associatedProfile)
    }
}

class DetectRuleConflictsUseCase(
    private val detector: RuleConflictDetector = RuleConflictDetector()
) {
    operator fun invoke(rules: List<AdvancedRule>): List<RuleConflict> {
        return detector.detectConflicts(rules)
    }
}
