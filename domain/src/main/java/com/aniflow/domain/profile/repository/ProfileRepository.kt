package com.aniflow.domain.profile.repository

import com.aniflow.domain.identity.ProfileId
import com.aniflow.domain.profile.model.UserProfile
import com.aniflow.domain.profile.validator.ProfileValidationResult
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for managing UserProfiles (STEP 27).
 * Encapsulates transactional guarantees for the Control Plane.
 */
interface ProfileRepository {
    fun observeAllProfiles(): Flow<List<UserProfile>>
    suspend fun getAllProfiles(): List<UserProfile>
    suspend fun getProfileById(id: ProfileId): UserProfile?
    suspend fun getDefaultProfile(): UserProfile
    suspend fun saveProfile(profile: UserProfile): ProfileValidationResult
    suspend fun setDefaultProfile(id: ProfileId)
    suspend fun duplicateProfile(id: ProfileId, newName: String? = null): UserProfile
    suspend fun deleteProfile(id: ProfileId): Boolean
    suspend fun getActiveProfileCount(): Int
}
