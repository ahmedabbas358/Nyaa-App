package com.aniflow.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Table: providers
 * Persists upstream provider registrations (Section 13 & 14).
 */
@Entity(
    tableName = "providers",
    indices = [
        Index(value = ["name"])
    ]
)
data class ProviderEntity(
    @PrimaryKey
    @ColumnInfo(name = "provider_id")
    val providerId: String,
    val name: String,
    @ColumnInfo(name = "base_url")
    val baseUrl: String,
    val enabled: Boolean = true,
    val status: String = "HEALTHY",
    @ColumnInfo(name = "last_success_at")
    val lastSuccessAt: Long? = null,
    @ColumnInfo(name = "last_failure_at")
    val lastFailureAt: Long? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Table: anime
 * Aggregate root for canonical anime metadata (Section 15).
 */
@Entity(
    tableName = "anime",
    indices = [
        Index(value = ["normalized_title"]),
        Index(value = ["type"]),
        Index(value = ["status"])
    ]
)
data class AnimeEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "canonical_title")
    val canonicalTitle: String,
    @ColumnInfo(name = "normalized_title")
    val normalizedTitle: String,
    val type: String = "Series",
    val status: String = "Unknown",
    @ColumnInfo(name = "primary_provider_id")
    val primaryProviderId: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Table: anime_titles
 * Multiple titles (Romaji, English, Japanese, Synonyms) per anime (Section 16, 17, 18).
 */
@Entity(
    tableName = "anime_titles",
    foreignKeys = [
        ForeignKey(
            entity = AnimeEntity::class,
            parentColumns = ["id"],
            childColumns = ["anime_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["anime_id", "normalized_title"], unique = true),
        Index(value = ["normalized_title"])
    ]
)
data class AnimeTitleEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "anime_id")
    val animeId: String,
    val title: String,
    @ColumnInfo(name = "normalized_title")
    val normalizedTitle: String,
    val language: String? = null,
    @ColumnInfo(name = "title_type")
    val titleType: String = "Official",
    @ColumnInfo(name = "is_preferred")
    val isPreferred: Boolean = false
)

/**
 * Table: anime_titles_fts
 * Full-Text Search virtual table over anime_titles (Section 19 & 20).
 */
@Entity(tableName = "anime_titles_fts")
@Fts4(contentEntity = AnimeTitleEntity::class)
data class AnimeTitleFtsEntity(
    val title: String,
    @ColumnInfo(name = "normalized_title")
    val normalizedTitle: String
)

/**
 * Table: seasons
 * Seasons belonging to an anime (Section 21).
 */
@Entity(
    tableName = "seasons",
    foreignKeys = [
        ForeignKey(
            entity = AnimeEntity::class,
            parentColumns = ["id"],
            childColumns = ["anime_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["anime_id", "number"], unique = true),
        Index(value = ["anime_id"])
    ]
)
data class SeasonEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "anime_id")
    val animeId: String,
    val number: Int,
    val title: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Table: episodes
 * Canonical episode records (Section 22, 23, 24).
 */
@Entity(
    tableName = "episodes",
    foreignKeys = [
        ForeignKey(
            entity = AnimeEntity::class,
            parentColumns = ["id"],
            childColumns = ["anime_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SeasonEntity::class,
            parentColumns = ["id"],
            childColumns = ["season_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["anime_id", "season_id", "episode_key"], unique = true),
        Index(value = ["season_id", "episode_key"]),
        Index(value = ["anime_id"])
    ]
)
data class EpisodeEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "anime_id")
    val animeId: String,
    @ColumnInfo(name = "season_id")
    val seasonId: String?,
    @ColumnInfo(name = "episode_key")
    val episodeKey: String,
    @ColumnInfo(name = "display_number")
    val displayNumber: String,
    @ColumnInfo(name = "major_number")
    val majorNumber: Int,
    @ColumnInfo(name = "minor_number")
    val minorNumber: Int = 0,
    val title: String?,
    val type: String = "Main",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
