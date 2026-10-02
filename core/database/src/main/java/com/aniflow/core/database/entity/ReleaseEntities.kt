package com.aniflow.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Table: uploaders
 * Provider-specific uploader identity (Section 25 & 26).
 */
@Entity(
    tableName = "uploaders",
    indices = [
        Index(value = ["provider_id", "provider_uploader_id"], unique = true),
        Index(value = ["provider_id", "normalized_name"])
    ]
)
data class UploaderEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "provider_id")
    val providerId: String,
    @ColumnInfo(name = "provider_uploader_id")
    val providerUploaderId: String?,
    val name: String,
    @ColumnInfo(name = "normalized_name")
    val normalizedName: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Table: release_groups
 * Provider-specific fansub or release group identity (Section 27).
 */
@Entity(
    tableName = "release_groups",
    indices = [
        Index(value = ["provider_id", "provider_group_id"], unique = true),
        Index(value = ["provider_id", "normalized_name"])
    ]
)
data class ReleaseGroupEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "provider_id")
    val providerId: String,
    @ColumnInfo(name = "provider_group_id")
    val providerGroupId: String?,
    val name: String,
    @ColumnInfo(name = "normalized_name")
    val normalizedName: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Table: releases
 * The core Release entity holding normalized discovery metadata (Section 28, 29, 30, 31).
 */
@Entity(
    tableName = "releases",
    foreignKeys = [
        ForeignKey(
            entity = AnimeEntity::class,
            parentColumns = ["id"],
            childColumns = ["anime_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = SeasonEntity::class,
            parentColumns = ["id"],
            childColumns = ["season_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = UploaderEntity::class,
            parentColumns = ["id"],
            childColumns = ["uploader_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = ReleaseGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["release_group_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["provider_id", "provider_release_id"], unique = true),
        Index(value = ["anime_id", "season_id", "published_at"]),
        Index(value = ["uploader_id", "published_at"]),
        Index(value = ["release_group_id", "published_at"]),
        Index(value = ["torrent_hash"]),
        Index(value = ["normalized_title"]),
        Index(value = ["published_at"]),
        Index(value = ["release_type"])
    ]
)
data class ReleaseEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "provider_id")
    val providerId: String,
    @ColumnInfo(name = "provider_release_id")
    val providerReleaseId: String?,
    val title: String,
    @ColumnInfo(name = "normalized_title")
    val normalizedTitle: String,
    @ColumnInfo(name = "release_type")
    val releaseType: String,
    @ColumnInfo(name = "anime_id")
    val animeId: String? = null,
    @ColumnInfo(name = "season_id")
    val seasonId: String? = null,
    @ColumnInfo(name = "uploader_id")
    val uploaderId: String? = null,
    @ColumnInfo(name = "release_group_id")
    val releaseGroupId: String? = null,
    val resolution: String? = null,
    @ColumnInfo(name = "video_codec")
    val videoCodec: String? = null,
    @ColumnInfo(name = "source_type")
    val sourceType: String = "Torrent",
    @ColumnInfo(name = "size_bytes")
    val sizeBytes: Long = 0L,
    val seeders: Int = 0,
    val leechers: Int = 0,
    @ColumnInfo(name = "completed_downloads")
    val completedDownloads: Long = 0L,
    @ColumnInfo(name = "published_at")
    val publishedAt: Long? = null,
    @ColumnInfo(name = "discovered_at")
    val discoveredAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "last_updated_at")
    val lastUpdatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "torrent_hash")
    val torrentHash: String? = null,
    @ColumnInfo(name = "magnet_uri")
    val magnetUri: String? = null,
    @ColumnInfo(name = "download_uri")
    val downloadUri: String? = null,
    val trusted: Boolean = false,
    val remake: Boolean = false,
    @ColumnInfo(name = "audio_tracks_json")
    val audioTracksJson: String? = null,
    @ColumnInfo(name = "subtitles_json")
    val subtitlesJson: String? = null,
    @ColumnInfo(name = "parser_version")
    val parserVersion: String = "1.0.0",
    @ColumnInfo(name = "parse_confidence")
    val parseConfidence: Double = 1.0,
    @ColumnInfo(name = "parse_state")
    val parseState: String = "Parsed",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Table: releases_fts
 * Full-Text Search virtual table over releases (Section 78, 79, 80).
 */
@Entity(tableName = "releases_fts")
@Fts4(contentEntity = ReleaseEntity::class)
data class ReleaseSearchFtsEntity(
    val title: String,
    @ColumnInfo(name = "normalized_title")
    val normalizedTitle: String
)

/**
 * Table: release_episodes
 * Critical Many-to-Many junction table connecting Releases and Episodes (Section 32 & 33).
 */
@Entity(
    tableName = "release_episodes",
    primaryKeys = ["release_id", "episode_id"],
    foreignKeys = [
        ForeignKey(
            entity = ReleaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["release_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = EpisodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["episode_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["episode_id", "release_id"]),
        Index(value = ["release_id"])
    ]
)
data class ReleaseEpisodeEntity(
    @ColumnInfo(name = "release_id")
    val releaseId: String,
    @ColumnInfo(name = "episode_id")
    val episodeId: String,
    @ColumnInfo(name = "relation_type")
    val relationType: String = "Primary",
    val confidence: Double = 1.0,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Table: parser_metadata
 * Separate diagnostic and telemetry table for parser confidence and warnings (Section 58).
 */
@Entity(
    tableName = "parser_metadata",
    foreignKeys = [
        ForeignKey(
            entity = ReleaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["release_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ParserMetadataEntity(
    @PrimaryKey
    @ColumnInfo(name = "release_id")
    val releaseId: String,
    @ColumnInfo(name = "parser_version")
    val parserVersion: String,
    val confidence: Double,
    val state: String,
    @ColumnInfo(name = "warnings_json")
    val warningsJson: String? = null,
    @ColumnInfo(name = "parsed_at")
    val parsedAt: Long = System.currentTimeMillis()
)
