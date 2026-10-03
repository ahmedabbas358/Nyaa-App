package com.aniflow.di

import com.aniflow.domain.event.AniFlowEventBus
import com.aniflow.domain.repository.DownloadRepository
import com.aniflow.domain.repository.LibraryRepository
import com.aniflow.domain.repository.ProfileRepository
import com.aniflow.domain.repository.ReleaseRepository
import com.aniflow.domain.repository.RuleRepository
import com.aniflow.domain.repository.SettingsRepository
import com.aniflow.domain.service.ReleaseParser
import com.aniflow.domain.service.ReleaseParserImpl
import com.aniflow.domain.usecase.CancelDownloadUseCase
import com.aniflow.domain.usecase.EvaluateReleaseSelectionUseCase
import com.aniflow.domain.usecase.ExecuteDownloadPlanUseCase
import com.aniflow.domain.usecase.FinalizeDownloadUseCase
import com.aniflow.domain.usecase.PauseDownloadUseCase
import com.aniflow.domain.usecase.PrepareDownloadPlanUseCase
import com.aniflow.domain.usecase.QueueDownloadUseCase
import com.aniflow.domain.usecase.ReconcileDownloadsUseCase
import com.aniflow.domain.usecase.ResumeDownloadUseCase
import com.aniflow.domain.usecase.SearchReleasesCoordinatorUseCase
import com.aniflow.provider.core.coordinator.ProviderSearchCoordinator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DomainModule (Section 4).
 * Pure Domain Use Cases and Engine Bindings without direct Android framework imports.
 */
@Module
@InstallIn(SingletonComponent::class)
object DomainModule {

    @Provides
    @Singleton
    fun provideReleaseParser(): ReleaseParser = ReleaseParserImpl()

    @Provides
    fun provideSearchReleasesCoordinatorUseCase(
        coordinator: ProviderSearchCoordinator,
        releaseRepository: ReleaseRepository,
        releaseParser: ReleaseParser,
        eventBus: AniFlowEventBus
    ): SearchReleasesCoordinatorUseCase = SearchReleasesCoordinatorUseCase(
        coordinator = coordinator,
        releaseRepository = releaseRepository,
        releaseParser = releaseParser,
        eventBus = eventBus
    )

    @Provides
    fun providePrepareDownloadPlanUseCase(): PrepareDownloadPlanUseCase =
        PrepareDownloadPlanUseCase()

    @Provides
    fun provideExecuteDownloadPlanUseCase(
        downloadRepository: DownloadRepository,
        eventBus: AniFlowEventBus
    ): ExecuteDownloadPlanUseCase = ExecuteDownloadPlanUseCase(
        downloadRepository = downloadRepository,
        eventBus = eventBus
    )

    @Provides
    fun provideFinalizeDownloadUseCase(
        downloadRepository: DownloadRepository,
        libraryRepository: LibraryRepository,
        eventBus: AniFlowEventBus
    ): FinalizeDownloadUseCase = FinalizeDownloadUseCase(
        downloadRepository = downloadRepository,
        libraryRepository = libraryRepository,
        eventBus = eventBus
    )

    @Provides
    fun provideReconcileDownloadsUseCase(
        downloadRepository: DownloadRepository
    ): ReconcileDownloadsUseCase = ReconcileDownloadsUseCase(
        downloadRepository = downloadRepository
    )

    @Provides
    fun provideQueueDownloadUseCase(
        downloadRepository: DownloadRepository
    ): QueueDownloadUseCase = QueueDownloadUseCase(
        downloadRepository = downloadRepository
    )

    @Provides
    fun providePauseDownloadUseCase(
        downloadRepository: DownloadRepository
    ): PauseDownloadUseCase = PauseDownloadUseCase(
        downloadRepository = downloadRepository
    )

    @Provides
    fun provideResumeDownloadUseCase(
        downloadRepository: DownloadRepository
    ): ResumeDownloadUseCase = ResumeDownloadUseCase(
        downloadRepository = downloadRepository
    )

    @Provides
    fun provideCancelDownloadUseCase(
        downloadRepository: DownloadRepository
    ): CancelDownloadUseCase = CancelDownloadUseCase(
        downloadRepository = downloadRepository
    )

    @Provides
    fun provideEvaluateReleaseSelectionUseCase(
        profileRepository: ProfileRepository,
        ruleRepository: RuleRepository
    ): EvaluateReleaseSelectionUseCase = EvaluateReleaseSelectionUseCase(
        profileRepository = profileRepository,
        ruleRepository = ruleRepository
    )

    @Provides
    fun provideSearchReleasesUseCase(
        searchRepository: com.aniflow.domain.repository.SearchRepository
    ): com.aniflow.domain.usecase.SearchReleasesUseCase =
        com.aniflow.domain.usecase.SearchReleasesUseCase(searchRepository)

    @Provides
    fun provideGetReleaseDetailsUseCase(
        releaseRepository: ReleaseRepository
    ): com.aniflow.domain.usecase.GetReleaseDetailsUseCase =
        com.aniflow.domain.usecase.GetReleaseDetailsUseCase(releaseRepository)

    @Provides
    fun provideGetAnimeUseCase(
        repository: com.aniflow.domain.anime.AnimeExperienceRepository
    ): com.aniflow.domain.usecase.GetAnimeUseCase =
        com.aniflow.domain.usecase.GetAnimeUseCase(repository)

    @Provides
    fun provideGetAnimeSeasonsUseCase(
        repository: com.aniflow.domain.anime.AnimeExperienceRepository
    ): com.aniflow.domain.usecase.GetAnimeSeasonsUseCase =
        com.aniflow.domain.usecase.GetAnimeSeasonsUseCase(repository)

    @Provides
    fun provideGetSeasonEpisodesUseCase(
        repository: com.aniflow.domain.anime.AnimeExperienceRepository
    ): com.aniflow.domain.usecase.GetSeasonEpisodesUseCase =
        com.aniflow.domain.usecase.GetSeasonEpisodesUseCase(repository)

    @Provides
    fun provideGetEpisodeUseCase(
        repository: com.aniflow.domain.anime.AnimeExperienceRepository
    ): com.aniflow.domain.usecase.GetEpisodeUseCase =
        com.aniflow.domain.usecase.GetEpisodeUseCase(repository)

    @Provides
    fun provideGetEpisodeCandidatesUseCase(
        repository: com.aniflow.domain.anime.AnimeExperienceRepository,
        releaseRepository: ReleaseRepository
    ): com.aniflow.domain.usecase.GetEpisodeCandidatesUseCase =
        com.aniflow.domain.usecase.GetEpisodeCandidatesUseCase(repository, releaseRepository)

    @Provides
    fun provideGetEpisodeCoverageUseCase(
        repository: com.aniflow.domain.anime.AnimeExperienceRepository
    ): com.aniflow.domain.usecase.GetEpisodeCoverageUseCase =
        com.aniflow.domain.usecase.GetEpisodeCoverageUseCase(repository)

    @Provides
    fun provideGetSeasonCoverageUseCase(
        repository: com.aniflow.domain.anime.AnimeExperienceRepository
    ): com.aniflow.domain.usecase.GetSeasonCoverageUseCase =
        com.aniflow.domain.usecase.GetSeasonCoverageUseCase(repository)

    @Provides
    fun provideGetAnimeCoverageUseCase(
        repository: com.aniflow.domain.anime.AnimeExperienceRepository
    ): com.aniflow.domain.usecase.GetAnimeCoverageUseCase =
        com.aniflow.domain.usecase.GetAnimeCoverageUseCase(repository)

    @Provides
    fun provideGetMissingEpisodesUseCase(
        repository: com.aniflow.domain.anime.AnimeExperienceRepository
    ): com.aniflow.domain.usecase.GetMissingEpisodesUseCase =
        com.aniflow.domain.usecase.GetMissingEpisodesUseCase(repository)

    @Provides
    fun provideGetReviewItemsUseCase(
        repository: com.aniflow.domain.repository.ReviewQueueRepository
    ): com.aniflow.domain.usecase.GetReviewItemsUseCase =
        com.aniflow.domain.usecase.GetReviewItemsUseCase(repository)

    @Provides
    fun provideCompareReleasesUseCase(
        releaseRepository: ReleaseRepository
    ): com.aniflow.domain.usecase.CompareReleasesUseCase =
        com.aniflow.domain.usecase.CompareReleasesUseCase(releaseRepository)

    @Provides
    fun provideSaveUserMappingUseCase(
        repository: com.aniflow.domain.repository.UserMappingRepository
    ): com.aniflow.domain.usecase.SaveUserMappingUseCase =
        com.aniflow.domain.usecase.SaveUserMappingUseCase(repository)

    @Provides
    @Singleton
    fun provideSmartSelectionEngine(): com.aniflow.domain.selection.engine.SelectionEngine =
        com.aniflow.domain.selection.engine.SmartSelectionEngine()

    @Provides
    fun provideSelectEpisodeReleaseUseCase(
        engine: com.aniflow.domain.selection.engine.SelectionEngine
    ): com.aniflow.domain.selection.usecase.SelectEpisodeReleaseUseCase =
        com.aniflow.domain.selection.usecase.SelectEpisodeReleaseUseCase(engine)

    @Provides
    fun provideSelectSeasonPlanUseCase(): com.aniflow.domain.selection.usecase.SelectSeasonPlanUseCase =
        com.aniflow.domain.selection.usecase.SelectSeasonPlanUseCase()

    @Provides
    fun provideSelectBatchUseCase(): com.aniflow.domain.selection.usecase.SelectBatchUseCase =
        com.aniflow.domain.selection.usecase.SelectBatchUseCase()

    @Provides
    @Singleton
    fun provideSearchHistoryManager(): com.aniflow.domain.search.usecase.SearchHistoryManager =
        com.aniflow.domain.search.usecase.SearchHistoryManager()
}
