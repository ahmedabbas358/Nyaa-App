package com.aniflow.domain.repository

import com.aniflow.domain.identity.StorageId
import com.aniflow.domain.storage.model.StorageRoot
import kotlinx.coroutines.flow.Flow

/**
 * StorageRepository contract (Section 12, 13).
 * Manages registered library storage roots / locations across internal, external, and SAF drives.
 */
interface StorageRepository {
    suspend fun getRootById(id: StorageId): StorageRoot?
    suspend fun getDefaultRoot(): StorageRoot
    fun observeRoots(): Flow<List<StorageRoot>>
    suspend fun saveRoot(root: StorageRoot)
    suspend fun deleteRoot(id: StorageId)
    suspend fun setDefaultRoot(id: StorageId)
}
