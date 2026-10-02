package com.aniflow.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.aniflow.core.database.entity.EpisodeEntity
import com.aniflow.core.database.entity.ParserMetadataEntity
import com.aniflow.core.database.entity.ReleaseEntity
import com.aniflow.core.database.entity.ReleaseEpisodeEntity
import com.aniflow.core.database.entity.ReleaseGroupEntity
import com.aniflow.core.database.entity.UploaderEntity
import com.aniflow.core.database.relation.ReleaseListProjection
import com.aniflow.core.database.relation.ReleaseWithEpisodes
import kotlinx.coroutines.flow.Flow

@Dao
interface UploaderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(uploader: UploaderEntity)

    @Query("SELECT * FROM uploaders WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): UploaderEntity?

    @Query("SELECT * FROM uploaders WHERE provider_id = :providerId AND provider_uploader_id = :providerUploaderId LIMIT 1")
    suspend fun getByProviderUploaderId(providerId: String, providerUploaderId: String): UploaderEntity?

    @Query("SELECT * FROM uploaders ORDER BY name ASC")
    fun observeAll(): Flow<List<UploaderEntity>>
}

@Dao
interface ReleaseGroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(group: ReleaseGroupEntity)

    @Query("SELECT * FROM release_groups WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ReleaseGroupEntity?

    @Query("SELECT * FROM release_groups WHERE provider_id = :providerId AND provider_group_id = :providerGroupId LIMIT 1")
    suspend fun getByProviderGroupId(providerId: String, providerGroupId: String): ReleaseGroupEntity?

    @Query("SELECT * FROM release_groups ORDER BY name ASC")
    fun observeAll(): Flow<List<ReleaseGroupEntity>>
}

@Dao
interface ReleaseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(release: ReleaseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(releases: List<ReleaseEntity>)

    @Update
    suspend fun update(release: ReleaseEntity)

    @Query("SELECT * FROM releases WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ReleaseEntity?

    @Query("SELECT * FROM releases WHERE provider_id = :providerId AND provider_release_id = :providerReleaseId LIMIT 1")
    suspend fun getByProviderId(providerId: String, providerReleaseId: String): ReleaseEntity?

    @Query("SELECT * FROM releases WHERE torrent_hash = :torrentHash LIMIT 1")
    suspend fun getByTorrentHash(torrentHash: String): ReleaseEntity?

    @Query("""
        SELECT r.* FROM releases r
        JOIN releases_fts fts ON fts.rowid = r.rowid
        WHERE releases_fts MATCH :queryText
        ORDER BY r.published_at DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun searchFts(queryText: String, limit: Int = 50, offset: Int = 0): List<ReleaseEntity>

    @Query("""
        SELECT id, provider_id, title, normalized_title, resolution, video_codec,
               size_bytes, seeders, leechers, completed_downloads, published_at,
               release_type, uploader_id, release_group_id, torrent_hash
        FROM releases
        WHERE (:animeId IS NULL OR anime_id = :animeId)
          AND (:uploaderId IS NULL OR uploader_id = :uploaderId)
          AND (:resolution IS NULL OR resolution = :resolution)
        ORDER BY published_at DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun queryProjectionsPaged(
        animeId: String? = null,
        uploaderId: String? = null,
        resolution: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): List<ReleaseListProjection>

    @Transaction
    @Query("SELECT * FROM releases WHERE id = :id")
    suspend fun getReleaseWithEpisodes(id: String): ReleaseWithEpisodes?

    @Query("SELECT * FROM releases WHERE id = :id")
    fun observeById(id: String): Flow<ReleaseEntity?>

    @Query("DELETE FROM releases WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface ReleaseEpisodeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(link: ReleaseEpisodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(links: List<ReleaseEpisodeEntity>)

    @Query("""
        SELECT e.* FROM episodes e
        JOIN release_episodes re ON re.episode_id = e.id
        WHERE re.release_id = :releaseId
    """)
    suspend fun getEpisodesForRelease(releaseId: String): List<EpisodeEntity>

    @Query("""
        SELECT r.* FROM releases r
        JOIN release_episodes re ON re.release_id = r.id
        WHERE re.episode_id = :episodeId
    """)
    suspend fun getReleasesForEpisode(episodeId: String): List<ReleaseEntity>

    @Query("DELETE FROM release_episodes WHERE release_id = :releaseId")
    suspend fun deleteForRelease(releaseId: String)
}

@Dao
interface ParserMetadataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(metadata: ParserMetadataEntity)

    @Query("SELECT * FROM parser_metadata WHERE release_id = :releaseId LIMIT 1")
    suspend fun getByReleaseId(releaseId: String): ParserMetadataEntity?
}
