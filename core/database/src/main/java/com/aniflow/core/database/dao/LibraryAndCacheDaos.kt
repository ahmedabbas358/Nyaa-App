package com.aniflow.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.aniflow.core.database.entity.LibraryFileEntity
import com.aniflow.core.database.entity.LibraryItemEntity
import com.aniflow.core.database.entity.ProviderCacheEntity
import com.aniflow.core.database.entity.ProviderRequestEntity
import com.aniflow.core.database.relation.LibraryItemWithFiles
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: LibraryItemEntity)

    @Update
    suspend fun updateItem(item: LibraryItemEntity)

    @Query("SELECT * FROM library_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: String): LibraryItemEntity?

    @Transaction
    @Query("SELECT * FROM library_items WHERE id = :id")
    suspend fun getLibraryItemWithFiles(id: String): LibraryItemWithFiles?

    @Query("SELECT * FROM library_items ORDER BY display_title ASC")
    fun observeItems(): Flow<List<LibraryItemEntity>>

    @Query("SELECT * FROM library_items ORDER BY display_title ASC")
    suspend fun getAllItems(): List<LibraryItemEntity>

    @Query("DELETE FROM library_items WHERE id = :id")
    suspend fun deleteItem(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: LibraryFileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<LibraryFileEntity>)

    @Query("SELECT * FROM library_files WHERE library_item_id = :libraryItemId")
    suspend fun getFilesForItem(libraryItemId: String): List<LibraryFileEntity>

    @Query("SELECT * FROM library_files WHERE fingerprint_value = :fingerprintValue LIMIT 1")
    suspend fun getFileByFingerprint(fingerprintValue: String): LibraryFileEntity?

    @Query("DELETE FROM library_files WHERE id = :id")
    suspend fun deleteFile(id: String)
}

@Dao
interface ProviderCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cache: ProviderCacheEntity)

    @Query("SELECT * FROM provider_cache WHERE cache_key = :cacheKey LIMIT 1")
    suspend fun get(cacheKey: String): ProviderCacheEntity?

    @Query("DELETE FROM provider_cache WHERE expires_at < :nowMillis")
    suspend fun deleteExpired(nowMillis: Long = System.currentTimeMillis())

    @Query("DELETE FROM provider_cache")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM provider_cache")
    suspend fun getCacheCount(): Int
}

@Dao
interface ProviderRequestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(request: ProviderRequestEntity)

    @Query("SELECT * FROM provider_requests ORDER BY started_at DESC LIMIT :limit")
    suspend fun getRecentDiagnostics(limit: Int = 100): List<ProviderRequestEntity>

    @Query("DELETE FROM provider_requests WHERE started_at < :cutoffMillis")
    suspend fun pruneOlderThan(cutoffMillis: Long)
}
