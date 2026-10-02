package com.aniflow.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.aniflow.core.database.converter.Converters
import com.aniflow.core.database.dao.AnimeDao
import com.aniflow.core.database.dao.CollectionDao
import com.aniflow.core.database.dao.DownloadFileDao
import com.aniflow.core.database.dao.DownloadHistoryDao
import com.aniflow.core.database.dao.DownloadPlanDao
import com.aniflow.core.database.dao.DownloadPlanItemDao
import com.aniflow.core.database.dao.DownloadSegmentDao
import com.aniflow.core.database.dao.DownloadTaskDao
import com.aniflow.core.database.dao.EpisodeDao
import com.aniflow.core.database.dao.FavoriteDao
import com.aniflow.core.database.dao.LibraryDao
import com.aniflow.core.database.dao.ParserMetadataDao
import com.aniflow.core.database.dao.PreferenceDao
import com.aniflow.core.database.dao.ProfileDao
import com.aniflow.core.database.dao.ProviderCacheDao
import com.aniflow.core.database.dao.ProviderDao
import com.aniflow.core.database.dao.ProviderRequestDao
import com.aniflow.core.database.dao.ReleaseDao
import com.aniflow.core.database.dao.ReleaseEpisodeDao
import com.aniflow.core.database.dao.ReleaseGroupDao
import com.aniflow.core.database.dao.RuleDao
import com.aniflow.core.database.dao.SavedSearchDao
import com.aniflow.core.database.dao.SeasonDao
import com.aniflow.core.database.dao.UploaderDao
import com.aniflow.core.database.entity.AnimeEntity
import com.aniflow.core.database.entity.AnimeTitleEntity
import com.aniflow.core.database.entity.AnimeTitleFtsEntity
import com.aniflow.core.database.entity.CollectionEntity
import com.aniflow.core.database.entity.CollectionItemEntity
import com.aniflow.core.database.entity.DownloadFileEntity
import com.aniflow.core.database.entity.DownloadHistoryEntity
import com.aniflow.core.database.entity.DownloadPlanEntity
import com.aniflow.core.database.entity.DownloadPlanItemEntity
import com.aniflow.core.database.entity.DownloadSegmentEntity
import com.aniflow.core.database.entity.DownloadTaskEntity
import com.aniflow.core.database.entity.EpisodeEntity
import com.aniflow.core.database.entity.FavoriteEntity
import com.aniflow.core.database.entity.LibraryFileEntity
import com.aniflow.core.database.entity.LibraryItemEntity
import com.aniflow.core.database.entity.ParserMetadataEntity
import com.aniflow.core.database.entity.PreferenceEntity
import com.aniflow.core.database.entity.DownloadProfileEntity
import com.aniflow.core.database.entity.ProviderCacheEntity
import com.aniflow.core.database.entity.ProviderEntity
import com.aniflow.core.database.entity.ProviderRequestEntity
import com.aniflow.core.database.entity.ReleaseEntity
import com.aniflow.core.database.entity.ReleaseEpisodeEntity
import com.aniflow.core.database.entity.ReleaseGroupEntity
import com.aniflow.core.database.entity.ReleaseSearchFtsEntity
import com.aniflow.core.database.entity.RuleEntity
import com.aniflow.core.database.entity.SavedSearchEntity
import com.aniflow.core.database.entity.SeasonEntity
import com.aniflow.core.database.entity.UploaderEntity

/**
 * Room Database for AniFlow Android (Section 10, 11, 134, 135).
 * Database Name: aniflow.db
 * Version: 1
 * Schema Export: Enabled (stored in schemas/)
 * Zero destructive fallback migrations in production.
 */
@Database(
    entities = [
        ProviderEntity::class,
        AnimeEntity::class,
        AnimeTitleEntity::class,
        AnimeTitleFtsEntity::class,
        SeasonEntity::class,
        EpisodeEntity::class,
        UploaderEntity::class,
        ReleaseGroupEntity::class,
        ReleaseEntity::class,
        ReleaseSearchFtsEntity::class,
        ReleaseEpisodeEntity::class,
        CollectionEntity::class,
        CollectionItemEntity::class,
        SavedSearchEntity::class,
        DownloadProfileEntity::class,
        RuleEntity::class,
        FavoriteEntity::class,
        PreferenceEntity::class,
        DownloadPlanEntity::class,
        DownloadPlanItemEntity::class,
        DownloadTaskEntity::class,
        DownloadFileEntity::class,
        DownloadSegmentEntity::class,
        DownloadHistoryEntity::class,
        LibraryItemEntity::class,
        LibraryFileEntity::class,
        ProviderCacheEntity::class,
        ProviderRequestEntity::class,
        ParserMetadataEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AniFlowDatabase : RoomDatabase() {

    abstract fun providerDao(): ProviderDao
    abstract fun animeDao(): AnimeDao
    abstract fun seasonDao(): SeasonDao
    abstract fun episodeDao(): EpisodeDao
    abstract fun uploaderDao(): UploaderDao
    abstract fun releaseGroupDao(): ReleaseGroupDao
    abstract fun releaseDao(): ReleaseDao
    abstract fun releaseEpisodeDao(): ReleaseEpisodeDao
    abstract fun parserMetadataDao(): ParserMetadataDao

    abstract fun collectionDao(): CollectionDao
    abstract fun savedSearchDao(): SavedSearchDao
    abstract fun profileDao(): ProfileDao
    abstract fun ruleDao(): RuleDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun preferenceDao(): PreferenceDao

    abstract fun downloadPlanDao(): DownloadPlanDao
    abstract fun downloadPlanItemDao(): DownloadPlanItemDao
    abstract fun downloadTaskDao(): DownloadTaskDao
    abstract fun downloadFileDao(): DownloadFileDao
    abstract fun downloadSegmentDao(): DownloadSegmentDao
    abstract fun downloadHistoryDao(): DownloadHistoryDao

    abstract fun libraryDao(): LibraryDao
    abstract fun providerCacheDao(): ProviderCacheDao
    abstract fun providerRequestDao(): ProviderRequestDao

    companion object {
        const val DATABASE_NAME = "aniflow.db"

        @Volatile
        private var instance: AniFlowDatabase? = null

        fun getInstance(context: Context): AniFlowDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AniFlowDatabase::class.java,
                    DATABASE_NAME
                )
                    .build()
                    .also { instance = it }
            }
        }

        fun createInMemory(context: Context): AniFlowDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                AniFlowDatabase::class.java
            )
                .allowMainThreadQueries()
                .build()
        }
    }
}
