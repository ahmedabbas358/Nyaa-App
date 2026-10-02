package com.aniflow.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.aniflow.core.database.entity.AnimeEntity
import com.aniflow.core.database.entity.AnimeTitleEntity
import com.aniflow.core.database.entity.EpisodeEntity
import com.aniflow.core.database.entity.ProviderEntity
import com.aniflow.core.database.entity.SeasonEntity
import com.aniflow.core.database.relation.AnimeWithSeasons
import com.aniflow.core.database.relation.SeasonWithEpisodes
import kotlinx.coroutines.flow.Flow

@Dao
interface ProviderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(provider: ProviderEntity)

    @Update
    suspend fun update(provider: ProviderEntity)

    @Query("SELECT * FROM providers WHERE provider_id = :providerId LIMIT 1")
    suspend fun getById(providerId: String): ProviderEntity?

    @Query("SELECT * FROM providers ORDER BY name ASC")
    fun observeAll(): Flow<List<ProviderEntity>>
}

@Dao
interface AnimeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(anime: AnimeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTitles(titles: List<AnimeTitleEntity>)

    @Update
    suspend fun update(anime: AnimeEntity)

    @Query("SELECT * FROM anime WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): AnimeEntity?

    @Query("SELECT * FROM anime WHERE normalized_title = :normalizedTitle LIMIT 1")
    suspend fun findByNormalizedTitle(normalizedTitle: String): AnimeEntity?

    @Query("""
        SELECT a.* FROM anime a
        JOIN anime_titles_fts fts ON fts.rowid = a.rowid
        WHERE anime_titles_fts MATCH :queryText
        LIMIT :limit
    """)
    suspend fun searchFts(queryText: String, limit: Int = 50): List<AnimeEntity>

    @Transaction
    @Query("SELECT * FROM anime WHERE id = :id")
    suspend fun getAnimeWithSeasons(id: String): AnimeWithSeasons?

    @Query("SELECT * FROM anime ORDER BY canonical_title ASC")
    fun observeAll(): Flow<List<AnimeEntity>>

    @Query("DELETE FROM anime WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface SeasonDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(season: SeasonEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(seasons: List<SeasonEntity>)

    @Query("SELECT * FROM seasons WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SeasonEntity?

    @Query("SELECT * FROM seasons WHERE anime_id = :animeId ORDER BY number ASC")
    suspend fun getByAnimeId(animeId: String): List<SeasonEntity>

    @Query("SELECT * FROM seasons WHERE anime_id = :animeId ORDER BY number ASC")
    fun observeByAnimeId(animeId: String): Flow<List<SeasonEntity>>

    @Transaction
    @Query("SELECT * FROM seasons WHERE id = :id")
    suspend fun getSeasonWithEpisodes(id: String): SeasonWithEpisodes?

    @Query("DELETE FROM seasons WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface EpisodeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(episode: EpisodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(episodes: List<EpisodeEntity>)

    @Query("SELECT * FROM episodes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): EpisodeEntity?

    @Query("SELECT * FROM episodes WHERE season_id = :seasonId ORDER BY major_number ASC, minor_number ASC")
    suspend fun getBySeasonId(seasonId: String): List<EpisodeEntity>

    @Query("SELECT * FROM episodes WHERE season_id = :seasonId ORDER BY major_number ASC, minor_number ASC")
    fun observeBySeasonId(seasonId: String): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE anime_id = :animeId ORDER BY major_number ASC")
    suspend fun getByAnimeId(animeId: String): List<EpisodeEntity>

    @Query("DELETE FROM episodes WHERE id = :id")
    suspend fun delete(id: String)
}
