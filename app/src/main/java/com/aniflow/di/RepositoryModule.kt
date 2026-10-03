package com.aniflow.di

import com.aniflow.core.database.dao.AnimeDao
import com.aniflow.core.database.dao.CollectionDao
import com.aniflow.core.database.dao.DownloadFileDao
import com.aniflow.core.database.dao.DownloadTaskDao
import com.aniflow.core.database.dao.EpisodeDao
import com.aniflow.core.database.dao.LibraryDao
import com.aniflow.core.database.dao.ProfileDao
import com.aniflow.core.database.dao.ReleaseDao
import com.aniflow.core.database.dao.ReleaseEpisodeDao
import com.aniflow.core.database.dao.ReleaseGroupDao
import com.aniflow.core.database.dao.RuleDao
import com.aniflow.core.database.dao.SavedSearchDao
import com.aniflow.core.database.dao.SeasonDao
import com.aniflow.core.database.dao.UploaderDao
import com.aniflow.data.repository.AnimeRepositoryImpl
import com.aniflow.data.repository.CollectionRepositoryImpl
import com.aniflow.data.repository.DownloadRepositoryImpl
import com.aniflow.data.repository.EpisodeRepositoryImpl
import com.aniflow.data.repository.LibraryRepositoryImpl
import com.aniflow.data.repository.ProfileRepositoryImpl
import com.aniflow.data.repository.ReleaseGroupRepositoryImpl
import com.aniflow.data.repository.ReleaseRepositoryImpl
import com.aniflow.data.repository.RuleRepositoryImpl
import com.aniflow.data.repository.SavedSearchRepositoryImpl
import com.aniflow.data.repository.SeasonRepositoryImpl
import com.aniflow.data.repository.UploaderRepositoryImpl
import com.aniflow.domain.repository.AnimeRepository
import com.aniflow.domain.repository.CollectionRepository
import com.aniflow.domain.repository.DownloadRepository
import com.aniflow.domain.repository.EpisodeRepository
import com.aniflow.domain.repository.LibraryRepository
import com.aniflow.domain.repository.ProfileRepository
import com.aniflow.domain.repository.ReleaseGroupRepository
import com.aniflow.domain.repository.ReleaseRepository
import com.aniflow.domain.repository.RuleRepository
import com.aniflow.domain.repository.SavedSearchRepository
import com.aniflow.domain.repository.SeasonRepository
import com.aniflow.domain.repository.UploaderRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideReleaseRepository(releaseDao: ReleaseDao): ReleaseRepository =
        ReleaseRepositoryImpl(releaseDao)

    @Provides
    @Singleton
    fun provideAnimeRepository(animeDao: AnimeDao): AnimeRepository =
        AnimeRepositoryImpl(animeDao)

    @Provides
    @Singleton
    fun provideSeasonRepository(seasonDao: SeasonDao): SeasonRepository =
        SeasonRepositoryImpl(seasonDao)

    @Provides
    @Singleton
    fun provideEpisodeRepository(
        episodeDao: EpisodeDao,
        releaseEpisodeDao: ReleaseEpisodeDao
    ): EpisodeRepository =
        EpisodeRepositoryImpl(episodeDao, releaseEpisodeDao)

    @Provides
    @Singleton
    fun provideUploaderRepository(uploaderDao: UploaderDao): UploaderRepository =
        UploaderRepositoryImpl(uploaderDao)

    @Provides
    @Singleton
    fun provideReleaseGroupRepository(groupDao: ReleaseGroupDao): ReleaseGroupRepository =
        ReleaseGroupRepositoryImpl(groupDao)

    @Provides
    @Singleton
    fun provideCollectionRepository(collectionDao: CollectionDao): CollectionRepository =
        CollectionRepositoryImpl(collectionDao)

    @Provides
    @Singleton
    fun provideDownloadRepository(
        taskDao: DownloadTaskDao,
        fileDao: DownloadFileDao
    ): DownloadRepository =
        DownloadRepositoryImpl(taskDao, fileDao)

    @Provides
    @Singleton
    fun provideLibraryRepository(libraryDao: LibraryDao): LibraryRepository =
        LibraryRepositoryImpl(libraryDao)

    @Provides
    @Singleton
    fun provideProfileRepository(profileDao: ProfileDao): ProfileRepository =
        ProfileRepositoryImpl(profileDao)

    @Provides
    @Singleton
    fun provideRuleRepository(ruleDao: RuleDao): RuleRepository =
        RuleRepositoryImpl(ruleDao)

    @Provides
    @Singleton
    fun provideSavedSearchRepository(searchDao: SavedSearchDao): SavedSearchRepository =
        SavedSearchRepositoryImpl(searchDao)

    @Provides
    @Singleton
    fun provideSearchRepository(
        coordinator: com.aniflow.provider.core.coordinator.ProviderSearchCoordinator,
        releaseDao: ReleaseDao,
        cacheDao: com.aniflow.core.database.dao.ProviderCacheDao
    ): com.aniflow.domain.repository.SearchRepository =
        com.aniflow.data.repository.SearchRepositoryImpl(coordinator, releaseDao, cacheDao)

    @Provides
    @Singleton
    fun provideAnimeExperienceRepository(): com.aniflow.domain.anime.AnimeExperienceRepository =
        com.aniflow.data.repository.AnimeExperienceRepositoryImpl()

    @Provides
    @Singleton
    fun provideUserMappingRepository(): com.aniflow.domain.repository.UserMappingRepository =
        com.aniflow.data.repository.UserMappingRepositoryImpl()

    @Provides
    @Singleton
    fun provideReviewQueueRepository(): com.aniflow.domain.repository.ReviewQueueRepository =
        com.aniflow.data.repository.ReviewQueueRepositoryImpl()

    @Provides
    @Singleton
    fun provideWatchProgressRepository(): com.aniflow.domain.library.repository.WatchProgressRepository =
        com.aniflow.data.repository.WatchProgressRepositoryImpl()
}
