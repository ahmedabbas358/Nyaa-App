package com.aniflow.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Table: download_plans
 * Pre-download blueprint execution record (Section 42).
 */
@Entity(tableName = "download_plans")
data class DownloadPlanEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    @ColumnInfo(name = "collection_id")
    val collectionId: String? = null,
    @ColumnInfo(name = "policy_json")
    val policyJson: String,
    val status: String = "Created",
    @ColumnInfo(name = "total_items")
    val totalItems: Int = 0,
    @ColumnInfo(name = "selected_items")
    val selectedItems: Int = 0,
    @ColumnInfo(name = "estimated_bytes")
    val estimatedBytes: Long = 0L,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "started_at")
    val startedAt: Long? = null,
    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null
)

/**
 * Table: download_plan_items
 * Individual items selected within a download plan (Section 43).
 */
@Entity(
    tableName = "download_plan_items",
    primaryKeys = ["plan_id", "release_id", "episode_id"],
    foreignKeys = [
        ForeignKey(
            entity = DownloadPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["plan_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["plan_id"])
    ]
)
data class DownloadPlanItemEntity(
    @ColumnInfo(name = "plan_id")
    val planId: String,
    @ColumnInfo(name = "release_id")
    val releaseId: String,
    @ColumnInfo(name = "episode_id")
    val episodeId: String,
    @ColumnInfo(name = "selection_reason")
    val selectionReason: String = "Manual",
    @ColumnInfo(name = "manual_override")
    val manualOverride: Boolean = false,
    val state: String = "Pending",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Table: download_tasks
 * Persisted state of an active, queued, or completed download task (Section 44, 45, 46).
 */
@Entity(
    tableName = "download_tasks",
    foreignKeys = [
        ForeignKey(
            entity = DownloadPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["plan_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = ReleaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["release_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["state"]),
        Index(value = ["priority"]),
        Index(value = ["state", "priority", "created_at"]),
        Index(value = ["release_id"]),
        Index(value = ["plan_id"])
    ]
)
data class DownloadTaskEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "plan_id")
    val planId: String? = null,
    @ColumnInfo(name = "release_id")
    val releaseId: String? = null,
    @ColumnInfo(name = "episode_id")
    val episodeId: String? = null,
    @ColumnInfo(name = "source_type")
    val sourceType: String = "Torrent",
    @ColumnInfo(name = "source_uri")
    val sourceUri: String,
    @ColumnInfo(name = "torrent_hash")
    val torrentHash: String? = null,
    val state: String = "Pending",
    val priority: Int = 3, // Normal
    @ColumnInfo(name = "destination_id")
    val destinationId: String = "default_storage",
    @ColumnInfo(name = "destination_relative_path")
    val destinationRelativePath: String = "",
    @ColumnInfo(name = "total_bytes")
    val totalBytes: Long = 0L,
    @ColumnInfo(name = "downloaded_bytes")
    val downloadedBytes: Long = 0L,
    @ColumnInfo(name = "speed_bytes_per_second")
    val speedBytesPerSecond: Long = 0L,
    @ColumnInfo(name = "eta_seconds")
    val etaSeconds: Long = 0L,
    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0,
    @ColumnInfo(name = "next_retry_at")
    val nextRetryAt: Long? = null,
    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "started_at")
    val startedAt: Long? = null,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null,
    @ColumnInfo(name = "failed_at")
    val failedAt: Long? = null
)

/**
 * Table: download_files
 * Individual files belonging to a download task (Section 47).
 */
@Entity(
    tableName = "download_files",
    foreignKeys = [
        ForeignKey(
            entity = DownloadTaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["task_id"]),
        Index(value = ["task_id", "state"])
    ]
)
data class DownloadFileEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "task_id")
    val taskId: String,
    @ColumnInfo(name = "relative_path")
    val relativePath: String,
    @ColumnInfo(name = "file_name")
    val fileName: String,
    @ColumnInfo(name = "expected_bytes")
    val expectedBytes: Long? = null,
    @ColumnInfo(name = "downloaded_bytes")
    val downloadedBytes: Long = 0L,
    val state: String = "Pending",
    @ColumnInfo(name = "temp_path")
    val tempPath: String? = null,
    @ColumnInfo(name = "final_path")
    val finalPath: String? = null,
    @ColumnInfo(name = "hash_type")
    val hashType: String? = null,
    @ColumnInfo(name = "hash_value")
    val hashValue: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null
)

/**
 * Table: download_segments
 * HTTP chunk or byte-range segment state for segmented downloading (Section 48).
 */
@Entity(
    tableName = "download_segments",
    foreignKeys = [
        ForeignKey(
            entity = DownloadFileEntity::class,
            parentColumns = ["id"],
            childColumns = ["file_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["file_id"]),
        Index(value = ["file_id", "state"])
    ]
)
data class DownloadSegmentEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "file_id")
    val fileId: String,
    @ColumnInfo(name = "start_offset")
    val startOffset: Long,
    @ColumnInfo(name = "end_offset")
    val endOffset: Long,
    @ColumnInfo(name = "downloaded_bytes")
    val downloadedBytes: Long = 0L,
    val state: String = "Pending",
    val etag: String? = null,
    @ColumnInfo(name = "last_modified")
    val lastModified: String? = null
)

/**
 * Table: download_history
 * Historical event audit log for task execution (Section 49).
 */
@Entity(
    tableName = "download_history",
    indices = [
        Index(value = ["task_id"]),
        Index(value = ["timestamp"])
    ]
)
data class DownloadHistoryEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "task_id")
    val taskId: String,
    @ColumnInfo(name = "event_type")
    val eventType: String,
    val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "details_json")
    val detailsJson: String? = null
)
