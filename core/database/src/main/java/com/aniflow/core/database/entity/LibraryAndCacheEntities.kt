package com.aniflow.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Table: library_items
 * Local library index record (Section 50).
 */
@Entity(
    tableName = "library_items",
    indices = [
        Index(value = ["anime_id"]),
        Index(value = ["state"]),
        Index(value = ["indexed_at"])
    ]
)
data class LibraryItemEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "anime_id")
    val animeId: String? = null,
    @ColumnInfo(name = "season_id")
    val seasonId: String? = null,
    @ColumnInfo(name = "episode_id")
    val episodeId: String? = null,
    @ColumnInfo(name = "item_type")
    val itemType: String = "Anime",
    @ColumnInfo(name = "display_title")
    val displayTitle: String,
    val state: String = "Indexed",
    @ColumnInfo(name = "root_storage_id")
    val rootStorageId: String = "default_storage",
    @ColumnInfo(name = "relative_path")
    val relativePath: String,
    @ColumnInfo(name = "indexed_at")
    val indexedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Table: library_files
 * Physical/virtual media file records with verified fingerprints (Section 51 & 52).
 */
@Entity(
    tableName = "library_files",
    foreignKeys = [
        ForeignKey(
            entity = LibraryItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["library_item_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["library_item_id"]),
        Index(value = ["fingerprint_value"]),
        Index(value = ["path"])
    ]
)
data class LibraryFileEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "library_item_id")
    val libraryItemId: String,
    val path: String,
    @ColumnInfo(name = "file_name")
    val fileName: String,
    @ColumnInfo(name = "size_bytes")
    val sizeBytes: Long,
    @ColumnInfo(name = "modified_at")
    val modifiedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "fingerprint_type")
    val fingerprintType: String? = null,
    @ColumnInfo(name = "fingerprint_value")
    val fingerprintValue: String? = null,
    @ColumnInfo(name = "media_metadata_json")
    val mediaMetadataJson: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Table: provider_cache
 * Transient raw or structured cache for provider responses with TTL (Section 54, 55, 56).
 */
@Entity(
    tableName = "provider_cache",
    indices = [
        Index(value = ["provider_id"]),
        Index(value = ["expires_at"])
    ]
)
data class ProviderCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "cache_key")
    val cacheKey: String,
    @ColumnInfo(name = "provider_id")
    val providerId: String,
    @ColumnInfo(name = "request_hash")
    val requestHash: String,
    @ColumnInfo(name = "response_type")
    val responseType: String,
    val payload: String,
    @ColumnInfo(name = "fetched_at")
    val fetchedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "expires_at")
    val expiresAt: Long,
    val etag: String? = null,
    @ColumnInfo(name = "last_modified")
    val lastModified: String? = null
)

/**
 * Table: provider_requests
 * Operational diagnostics for provider request logs (Section 57).
 */
@Entity(
    tableName = "provider_requests",
    indices = [
        Index(value = ["provider_id"]),
        Index(value = ["started_at"])
    ]
)
data class ProviderRequestEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "provider_id")
    val providerId: String,
    @ColumnInfo(name = "request_type")
    val requestType: String,
    @ColumnInfo(name = "started_at")
    val startedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null,
    val status: String,
    @ColumnInfo(name = "http_code")
    val httpCode: Int? = null,
    @ColumnInfo(name = "error_type")
    val errorType: String? = null
)
