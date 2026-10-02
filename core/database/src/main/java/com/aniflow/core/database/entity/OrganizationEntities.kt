package com.aniflow.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Table: collections
 * User and dynamic smart collection definitions (Section 34 & 35).
 */
@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: String = "Static",
    @ColumnInfo(name = "source_type")
    val sourceType: String = "Manual",
    @ColumnInfo(name = "definition_json")
    val definitionJson: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Table: collection_items
 * Polymorphic item membership in collections (Section 36).
 */
@Entity(
    tableName = "collection_items",
    primaryKeys = ["collection_id", "item_type", "item_id"],
    foreignKeys = [
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collection_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["collection_id"])
    ]
)
data class CollectionItemEntity(
    @ColumnInfo(name = "collection_id")
    val collectionId: String,
    @ColumnInfo(name = "item_type")
    val itemType: String,
    @ColumnInfo(name = "item_id")
    val itemId: String,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,
    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis()
)

/**
 * Table: saved_searches
 * User saved and pinned search queries (Section 37).
 */
@Entity(tableName = "saved_searches")
data class SavedSearchEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    @ColumnInfo(name = "query_text")
    val queryText: String,
    @ColumnInfo(name = "query_config_json")
    val queryConfigJson: String,
    @ColumnInfo(name = "is_pinned")
    val isPinned: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "last_run_at")
    val lastRunAt: Long? = null
)

/**
 * Table: download_profiles
 * User download preferences profile with versioned JSON config (Section 38).
 */
@Entity(tableName = "download_profiles")
data class DownloadProfileEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    @ColumnInfo(name = "config_version")
    val configVersion: Int = 1,
    @ColumnInfo(name = "config_json")
    val configJson: String,
    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Table: rules
 * User and system automation rules (Section 39).
 */
@Entity(
    tableName = "rules",
    indices = [
        Index(value = ["enabled"]),
        Index(value = ["priority"])
    ]
)
data class RuleEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    @ColumnInfo(name = "scope_type")
    val scopeType: String = "Global",
    @ColumnInfo(name = "scope_id")
    val scopeId: String? = null,
    val priority: Int = 0,
    val enabled: Boolean = true,
    @ColumnInfo(name = "conditions_json")
    val conditionsJson: String,
    @ColumnInfo(name = "actions_json")
    val actionsJson: String,
    @ColumnInfo(name = "rule_version")
    val ruleVersion: Int = 1,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Table: favorites
 * Standalone favorites targeting Anime, Uploader, Group, or Release (Section 40).
 */
@Entity(
    tableName = "favorites",
    indices = [
        Index(value = ["target_type", "target_id"], unique = true)
    ]
)
data class FavoriteEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "target_type")
    val targetType: String,
    @ColumnInfo(name = "target_id")
    val targetId: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Table: preferences
 * Key-value configuration store for small application preferences (Section 41).
 */
@Entity(tableName = "preferences")
data class PreferenceEntity(
    @PrimaryKey
    val key: String,
    val value: String,
    @ColumnInfo(name = "value_type")
    val valueType: String = "String",
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
