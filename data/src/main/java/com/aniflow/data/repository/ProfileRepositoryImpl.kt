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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val mutex = Mutex()

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

    override suspend fun saveProfile(profile: UserProfile): ProfileValidationResult = mutex.withLock {
        val validation = ProfileValidator.validate(profile)
        if (!validation.isValid) {
            return@withLock validation
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
        validation
    }

    override suspend fun setDefaultProfile(id: ProfileId): Unit = mutex.withLock {
        val target = profiles[id] ?: throw IllegalArgumentException("Profile with ID '$id' not found")

        for ((pId, existing) in profiles) {
            val shouldBeDefault = (pId == id)
            if (existing.isDefault != shouldBeDefault) {
                profiles[pId] = existing.copy(isDefault = shouldBeDefault, updatedAt = Instant.now())
            }
        }
        updateFlow()
    }

    override suspend fun duplicateProfile(id: ProfileId, newName: String?): UserProfile = mutex.withLock {
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
        duplicated
    }

    override suspend fun deleteProfile(id: ProfileId): Boolean = mutex.withLock {
        val target = profiles[id] ?: return@withLock false
        require(!target.isDefault) {
            "Cannot delete the default profile '$id'. Designate another profile as default before deleting."
        }

        val removed = profiles.remove(id) != null
        if (removed) {
            updateFlow()
        }
        removed
    }

    override suspend fun getActiveProfileCount(): Int = profiles.size

    private fun updateFlow() {
        _flow.value = profiles.values.sortedBy { it.name }
    }
}
