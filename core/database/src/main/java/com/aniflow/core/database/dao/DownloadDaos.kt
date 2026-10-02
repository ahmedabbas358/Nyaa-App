package com.aniflow.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.aniflow.core.database.entity.DownloadFileEntity
import com.aniflow.core.database.entity.DownloadHistoryEntity
import com.aniflow.core.database.entity.DownloadPlanEntity
import com.aniflow.core.database.entity.DownloadPlanItemEntity
import com.aniflow.core.database.entity.DownloadSegmentEntity
import com.aniflow.core.database.entity.DownloadTaskEntity
import com.aniflow.core.database.relation.DownloadFileWithSegments
import com.aniflow.core.database.relation.DownloadPlanWithItems
import com.aniflow.core.database.relation.DownloadTaskWithFiles
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadPlanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: DownloadPlanEntity)

    @Query("SELECT * FROM download_plans WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DownloadPlanEntity?

    @Transaction
    @Query("SELECT * FROM download_plans WHERE id = :id")
    suspend fun getPlanWithItems(id: String): DownloadPlanWithItems?

    @Query("UPDATE download_plans SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)
}

@Dao
interface DownloadPlanItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<DownloadPlanItemEntity>)

    @Query("SELECT * FROM download_plan_items WHERE plan_id = :planId")
    suspend fun getItemsForPlan(planId: String): List<DownloadPlanItemEntity>
}

@Dao
interface DownloadTaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: DownloadTaskEntity)

    @Update
    suspend fun update(task: DownloadTaskEntity)

    @Query("UPDATE download_tasks SET state = :state, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateState(id: String, state: String, updatedAt: Long = System.currentTimeMillis())

    @Query("""
        UPDATE download_tasks 
        SET downloaded_bytes = :downloadedBytes, speed_bytes_per_second = :speed, 
            eta_seconds = :eta, updated_at = :updatedAt 
        WHERE id = :id
    """)
    suspend fun updateProgress(
        id: String,
        downloadedBytes: Long,
        speed: Long,
        eta: Long,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("SELECT * FROM download_tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DownloadTaskEntity?

    @Transaction
    @Query("SELECT * FROM download_tasks WHERE id = :id")
    suspend fun getTaskWithFiles(id: String): DownloadTaskWithFiles?

    @Query("SELECT * FROM download_tasks ORDER BY priority DESC, created_at ASC")
    fun observeTasks(): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_tasks WHERE state IN (:states) ORDER BY priority DESC, created_at ASC")
    fun observeTasksByStates(states: List<String>): Flow<List<DownloadTaskEntity>>

    /**
     * Efficient queue retrieval conforming strictly to Clause 46:
     * Does NOT pull the entire queue into RAM.
     */
    @Query("SELECT * FROM download_tasks WHERE state = 'Queued' ORDER BY priority DESC, created_at ASC LIMIT :limit")
    suspend fun getQueuedTasksForExecution(limit: Int): List<DownloadTaskEntity>

    @Query("DELETE FROM download_tasks WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface DownloadFileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(file: DownloadFileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(files: List<DownloadFileEntity>)

    @Query("UPDATE download_files SET state = :state WHERE id = :id")
    suspend fun updateState(id: String, state: String)

    @Query("UPDATE download_files SET downloaded_bytes = :downloadedBytes WHERE id = :id")
    suspend fun updateProgress(id: String, downloadedBytes: Long)

    @Query("SELECT * FROM download_files WHERE task_id = :taskId")
    suspend fun getByTaskId(taskId: String): List<DownloadFileEntity>

    @Transaction
    @Query("SELECT * FROM download_files WHERE id = :id")
    suspend fun getFileWithSegments(id: String): DownloadFileWithSegments?
}

@Dao
interface DownloadSegmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(segments: List<DownloadSegmentEntity>)

    @Query("UPDATE download_segments SET downloaded_bytes = :downloadedBytes, state = :state WHERE id = :id")
    suspend fun updateProgress(id: String, downloadedBytes: Long, state: String)

    @Query("SELECT * FROM download_segments WHERE file_id = :fileId ORDER BY start_offset ASC")
    suspend fun getByFileId(fileId: String): List<DownloadSegmentEntity>
}

@Dao
interface DownloadHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: DownloadHistoryEntity)

    @Query("SELECT * FROM download_history WHERE task_id = :taskId ORDER BY timestamp DESC")
    suspend fun getHistoryForTask(taskId: String): List<DownloadHistoryEntity>

    @Query("DELETE FROM download_history WHERE timestamp < :cutoffMillis")
    suspend fun pruneHistoryOlderThan(cutoffMillis: Long)
}
