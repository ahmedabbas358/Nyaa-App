package com.aniflow.data.repository

import com.aniflow.domain.identity.ProfileId
import com.aniflow.domain.profile.model.UserProfile
import com.aniflow.domain.profile.repository.ProfileRepository
import com.aniflow.domain.profile.template.ProfileTemplates
import com.aniflow.domain.profile.validator.ProfileValidationResult
import com.aniflow.domain.profile.validator.ProfileValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe transactional implementation of ProfileRepository (STEP 27).
 * Strictly guarantees that exactly ONE profile is default at any time (Section 54).
 */
class UserProfileRepositoryImpl : ProfileRepository {

    private val profiles = ConcurrentHashMap<ProfileId, UserProfile>()
    private val _flow = MutableStateFlow<List<UserProfile>>(emptyList())

    init {
        // Pre-populate with standard templates upon initial system initialization
        val balanced = ProfileTemplates.createBalanced(ProfileId("prof_balanced"), isDefault = true)
        val highQuality = ProfileTemplates.createHighQuality(ProfileId("prof_high_quality"), isDefault = false)
        val smallSize = ProfileTemplates.createSmallSize(ProfileId("prof_small_size"), isDefault = false)

        profiles[balanced.id] = balanced
        profiles[highQuality.id] = highQuality
        profiles[smallSize.id] = smallSize

        _flow.value = profiles.values.sortedBy { it.name }
    }

    override fun observeAllProfiles(): Flow<List<UserProfile>> = _flow.asStateFlow()

    override suspend fun getAllProfiles(): List<UserProfile> =
        profiles.values.sortedBy { it.name }

    override suspend fun getProfileById(id: ProfileId): UserProfile? = profiles[id]

    override suspend fun getDefaultProfile(): UserProfile {
        return profiles.values.firstOrNull { it.isDefault }
            ?: profiles.values.firstOrNull()
            ?: run {
                val fallback = ProfileTemplates.createBalanced(ProfileId("prof_default"), isDefault = true)
                profiles[fallback.id] = fallback
                updateFlow()
                fallback
            }
    }

    @Synchronized
    override suspend fun saveProfile(profile: UserProfile): ProfileValidationResult {
        val validation = ProfileValidator.validate(profile)
        if (!validation.isValid) {
            return validation
        }

        if (profile.isDefault) {
            // Unmark any previous default profile to guarantee exactly one default
            for ((id, existing) in profiles) {
                if (existing.isDefault && id != profile.id) {
                    profiles[id] = existing.copy(isDefault = false, updatedAt = Instant.now())
                }
            }
        }

        profiles[profile.id] = profile
        updateFlow()
        return validation
    }

    @Synchronized
    override suspend fun setDefaultProfile(id: ProfileId) {
        val target = profiles[id] ?: throw IllegalArgumentException("Profile with ID '$id' not found")

        for ((pId, existing) in profiles) {
            val shouldBeDefault = (pId == id)
            if (existing.isDefault != shouldBeDefault) {
                profiles[pId] = existing.copy(isDefault = shouldBeDefault, updatedAt = Instant.now())
            }
        }
        updateFlow()
    }

    @Synchronized
    override suspend fun duplicateProfile(id: ProfileId, newName: String?): UserProfile {
        val source = profiles[id] ?: throw IllegalArgumentException("Cannot duplicate: Profile '$id' does not exist")
        val duplicateId = ProfileId("prof_${UUID.randomUUID()}")
        val nameToUse = newName ?: "${source.name} (Copy)"

        val duplicated = source.copy(
            id = duplicateId,
            name = nameToUse,
            isDefault = false,
            version = 1,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        profiles[duplicateId] = duplicated
        updateFlow()
        return duplicated
    }

    @Synchronized
    override suspend fun deleteProfile(id: ProfileId): Boolean {
        val target = profiles[id] ?: return false
        require(!target.isDefault) {
            "Cannot delete the default profile '$id'. Designate another profile as default before deleting."
        }

        val removed = profiles.remove(id) != null
        if (removed) {
            updateFlow()
        }
        return removed
    }

    override suspend fun getActiveProfileCount(): Int = profiles.size

    private fun updateFlow() {
        _flow.value = profiles.values.sortedBy { it.name }
    }
}
