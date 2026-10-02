package com.aniflow.di

import android.content.Context
import com.aniflow.core.database.AniFlowDatabase
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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AniFlowDatabase =
        AniFlowDatabase.getInstance(context)

    @Provides
    fun provideProviderDao(db: AniFlowDatabase): ProviderDao = db.providerDao()

    @Provides
    fun provideAnimeDao(db: AniFlowDatabase): AnimeDao = db.animeDao()

    @Provides
    fun provideSeasonDao(db: AniFlowDatabase): SeasonDao = db.seasonDao()

    @Provides
    fun provideEpisodeDao(db: AniFlowDatabase): EpisodeDao = db.episodeDao()

    @Provides
    fun provideUploaderDao(db: AniFlowDatabase): UploaderDao = db.uploaderDao()

    @Provides
    fun provideReleaseGroupDao(db: AniFlowDatabase): ReleaseGroupDao = db.releaseGroupDao()

    @Provides
    fun provideReleaseDao(db: AniFlowDatabase): ReleaseDao = db.releaseDao()

    @Provides
    fun provideReleaseEpisodeDao(db: AniFlowDatabase): ReleaseEpisodeDao = db.releaseEpisodeDao()

    @Provides
    fun provideParserMetadataDao(db: AniFlowDatabase): ParserMetadataDao = db.parserMetadataDao()

    @Provides
    fun provideCollectionDao(db: AniFlowDatabase): CollectionDao = db.collectionDao()

    @Provides
    fun provideSavedSearchDao(db: AniFlowDatabase): SavedSearchDao = db.savedSearchDao()

    @Provides
    fun provideProfileDao(db: AniFlowDatabase): ProfileDao = db.profileDao()

    @Provides
    fun provideRuleDao(db: AniFlowDatabase): RuleDao = db.ruleDao()

    @Provides
    fun provideFavoriteDao(db: AniFlowDatabase): FavoriteDao = db.favoriteDao()

    @Provides
    fun providePreferenceDao(db: AniFlowDatabase): PreferenceDao = db.preferenceDao()

    @Provides
    fun provideDownloadPlanDao(db: AniFlowDatabase): DownloadPlanDao = db.downloadPlanDao()

    @Provides
    fun provideDownloadPlanItemDao(db: AniFlowDatabase): DownloadPlanItemDao = db.downloadPlanItemDao()

    @Provides
    fun provideDownloadTaskDao(db: AniFlowDatabase): DownloadTaskDao = db.downloadTaskDao()

    @Provides
    fun provideDownloadFileDao(db: AniFlowDatabase): DownloadFileDao = db.downloadFileDao()

    @Provides
    fun provideDownloadSegmentDao(db: AniFlowDatabase): DownloadSegmentDao = db.downloadSegmentDao()

    @Provides
    fun provideDownloadHistoryDao(db: AniFlowDatabase): DownloadHistoryDao = db.downloadHistoryDao()

    @Provides
    fun provideLibraryDao(db: AniFlowDatabase): LibraryDao = db.libraryDao()

    @Provides
    fun provideProviderCacheDao(db: AniFlowDatabase): ProviderCacheDao = db.providerCacheDao()

    @Provides
    fun provideProviderRequestDao(db: AniFlowDatabase): ProviderRequestDao = db.providerRequestDao()
}
