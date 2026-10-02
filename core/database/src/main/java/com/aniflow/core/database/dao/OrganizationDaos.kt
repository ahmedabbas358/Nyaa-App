package com.aniflow.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.aniflow.core.database.entity.CollectionEntity
import com.aniflow.core.database.entity.CollectionItemEntity
import com.aniflow.core.database.entity.DownloadProfileEntity
import com.aniflow.core.database.entity.FavoriteEntity
import com.aniflow.core.database.entity.PreferenceEntity
import com.aniflow.core.database.entity.RuleEntity
import com.aniflow.core.database.entity.SavedSearchEntity
import com.aniflow.core.database.relation.CollectionWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(collection: CollectionEntity)

    @Update
    suspend fun update(collection: CollectionEntity)

    @Query("SELECT * FROM collections WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CollectionEntity?

    @Query("SELECT * FROM collections ORDER BY updated_at DESC")
    fun observeAll(): Flow<List<CollectionEntity>>

    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun delete(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: CollectionItemEntity)

    @Query("DELETE FROM collection_items WHERE collection_id = :collectionId AND item_type = :itemType AND item_id = :itemId")
    suspend fun removeItem(collectionId: String, itemType: String, itemId: String)

    @Query("SELECT * FROM collection_items WHERE collection_id = :collectionId ORDER BY sort_order ASC")
    suspend fun getItems(collectionId: String): List<CollectionItemEntity>

    @Query("SELECT * FROM collection_items WHERE collection_id = :collectionId ORDER BY sort_order ASC")
    fun observeItems(collectionId: String): Flow<List<CollectionItemEntity>>

    @Transaction
    @Query("SELECT * FROM collections WHERE id = :id")
    suspend fun getCollectionWithItems(id: String): CollectionWithItems?
}

@Dao
interface SavedSearchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(search: SavedSearchEntity)

    @Query("SELECT * FROM saved_searches WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SavedSearchEntity?

    @Query("SELECT * FROM saved_searches ORDER BY is_pinned DESC, updated_at DESC")
    fun observeAll(): Flow<List<SavedSearchEntity>>

    @Query("DELETE FROM saved_searches WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface ProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: DownloadProfileEntity)

    @Query("SELECT * FROM download_profiles WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DownloadProfileEntity?

    @Query("SELECT * FROM download_profiles WHERE is_default = 1 LIMIT 1")
    suspend fun getDefault(): DownloadProfileEntity?

    @Query("SELECT * FROM download_profiles ORDER BY name ASC")
    fun observeAll(): Flow<List<DownloadProfileEntity>>

    @Query("DELETE FROM download_profiles WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface RuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: RuleEntity)

    @Query("SELECT * FROM rules WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): RuleEntity?

    @Query("SELECT * FROM rules WHERE enabled = 1 ORDER BY priority DESC")
    suspend fun getActiveRules(): List<RuleEntity>

    @Query("SELECT * FROM rules ORDER BY priority DESC")
    fun observeAll(): Flow<List<RuleEntity>>

    @Query("DELETE FROM rules WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE target_type = :targetType AND target_id = :targetId)")
    suspend fun isFavorite(targetType: String, targetId: String): Boolean

    @Query("SELECT * FROM favorites ORDER BY created_at DESC")
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Query("DELETE FROM favorites WHERE target_type = :targetType AND target_id = :targetId")
    suspend fun delete(targetType: String, targetId: String)
}

@Dao
interface PreferenceDao {
    @Query("SELECT * FROM preferences WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): PreferenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(preference: PreferenceEntity)

    @Query("DELETE FROM preferences WHERE `key` = :key")
    suspend fun delete(key: String)
}
