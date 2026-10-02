package com.aniflow.core.database.relation

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.aniflow.core.database.entity.AnimeEntity
import com.aniflow.core.database.entity.CollectionEntity
import com.aniflow.core.database.entity.CollectionItemEntity
import com.aniflow.core.database.entity.DownloadFileEntity
import com.aniflow.core.database.entity.DownloadPlanEntity
import com.aniflow.core.database.entity.DownloadPlanItemEntity
import com.aniflow.core.database.entity.DownloadSegmentEntity
import com.aniflow.core.database.entity.DownloadTaskEntity
import com.aniflow.core.database.entity.EpisodeEntity
import com.aniflow.core.database.entity.LibraryFileEntity
import com.aniflow.core.database.entity.LibraryItemEntity
import com.aniflow.core.database.entity.ReleaseEntity
import com.aniflow.core.database.entity.ReleaseEpisodeEntity
import com.aniflow.core.database.entity.SeasonEntity

/**
 * 1:N Relation: Anime -> Seasons
 */
data class AnimeWithSeasons(
    @Embedded val anime: AnimeEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "anime_id"
    )
    val seasons: List<SeasonEntity>
)

/**
 * 1:N Relation: Season -> Episodes
 */
data class SeasonWithEpisodes(
    @Embedded val season: SeasonEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "season_id"
    )
    val episodes: List<EpisodeEntity>
)

/**
 * N:M Junction Relation: Episode -> Releases
 */
data class EpisodeWithReleases(
    @Embedded val episode: EpisodeEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ReleaseEpisodeEntity::class,
            parentColumn = "episode_id",
            entityColumn = "release_id"
        )
    )
    val releases: List<ReleaseEntity>
)

/**
 * N:M Junction Relation: Release -> Episodes
 */
data class ReleaseWithEpisodes(
    @Embedded val release: ReleaseEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ReleaseEpisodeEntity::class,
            parentColumn = "release_id",
            entityColumn = "episode_id"
        )
    )
    val episodes: List<EpisodeEntity>
)

/**
 * 1:N Relation: Collection -> Items
 */
data class CollectionWithItems(
    @Embedded val collection: CollectionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "collection_id"
    )
    val items: List<CollectionItemEntity>
)

/**
 * 1:N Relation: DownloadPlan -> PlanItems
 */
data class DownloadPlanWithItems(
    @Embedded val plan: DownloadPlanEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "plan_id"
    )
    val items: List<DownloadPlanItemEntity>
)

/**
 * 1:N Relation: DownloadTask -> Files
 */
data class DownloadTaskWithFiles(
    @Embedded val task: DownloadTaskEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "task_id"
    )
    val files: List<DownloadFileEntity>
)

/**
 * 1:N Relation: DownloadFile -> Segments
 */
data class DownloadFileWithSegments(
    @Embedded val file: DownloadFileEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "file_id"
    )
    val segments: List<DownloadSegmentEntity>
)

/**
 * 1:N Relation: LibraryItem -> Files
 */
data class LibraryItemWithFiles(
    @Embedded val item: LibraryItemEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "library_item_id"
    )
    val files: List<LibraryFileEntity>
)

/**
 * Lightweight Query Projection for search result lists (Section 75).
 * Bypasses reading large JSON blobs and unused diagnostics.
 */
data class ReleaseListProjection(
    val id: String,
    @ColumnInfo(name = "provider_id") val providerId: String,
    val title: String,
    @ColumnInfo(name = "normalized_title") val normalizedTitle: String,
    val resolution: String?,
    @ColumnInfo(name = "video_codec") val videoCodec: String?,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
    val seeders: Int,
    val leechers: Int,
    @ColumnInfo(name = "completed_downloads") val completedDownloads: Long,
    @ColumnInfo(name = "published_at") val publishedAt: Long?,
    @ColumnInfo(name = "release_type") val releaseType: String,
    @ColumnInfo(name = "uploader_id") val uploaderId: String?,
    @ColumnInfo(name = "release_group_id") val releaseGroupId: String?,
    @ColumnInfo(name = "torrent_hash") val torrentHash: String?
)
