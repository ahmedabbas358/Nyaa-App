package com.aniflow.data.repository

import com.aniflow.domain.identity.StorageId
import com.aniflow.domain.repository.StorageRepository
import com.aniflow.domain.storage.model.StorageAvailability
import com.aniflow.domain.storage.model.StorageLocationRoot
import com.aniflow.domain.storage.model.StorageRoot
import com.aniflow.domain.storage.model.StorageType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * StorageRepositoryImpl (Section 12, 13).
 * Manages registered library storage roots / locations across internal, external, and SAF drives.
 */
class StorageRepositoryImpl : StorageRepository {

    private val defaultRootId = StorageId("default_storage")
    private val rootsMap = ConcurrentHashMap<StorageId, StorageRoot>()
    private val _rootsFlow = MutableStateFlow<List<StorageRoot>>(emptyList())

    init {
        // Initialize with default internal storage root
        val defaultRoot = StorageRoot(
            id = defaultRootId,
            name = "Default Internal Library",
            type = StorageType.AppPrivate,
            state = StorageAvailability.Available,
            location = StorageLocationRoot("app_internal_library"),
            totalSpaceBytes = 64L * 1024 * 1024 * 1024, // 64 GB sample
            freeSpaceBytes = 28L * 1024 * 1024 * 1024,  // 28 GB sample
            isDefault = true,
            canRead = true,
            canWrite = true,
            canDelete = true
        )
        rootsMap[defaultRootId] = defaultRoot
        _rootsFlow.value = rootsMap.values.toList()
    }

    override suspend fun getRootById(id: StorageId): StorageRoot? = rootsMap[id]

    override suspend fun getDefaultRoot(): StorageRoot =
        rootsMap.values.firstOrNull { it.isDefault } ?: rootsMap.values.first()

    override fun observeRoots(): Flow<List<StorageRoot>> = _rootsFlow.asStateFlow()

    override suspend fun saveRoot(root: StorageRoot) {
        rootsMap[root.id] = root
        _rootsFlow.value = rootsMap.values.toList()
    }

    override suspend fun deleteRoot(id: StorageId) {
        if (id != defaultRootId) {
            rootsMap.remove(id)
            _rootsFlow.value = rootsMap.values.toList()
        }
    }

    override suspend fun setDefaultRoot(id: StorageId) {
        val current = rootsMap.values.toList()
        for (r in current) {
            rootsMap[r.id] = r.copy(isDefault = r.id == id)
        }
        _rootsFlow.value = rootsMap.values.toList()
    }
}
