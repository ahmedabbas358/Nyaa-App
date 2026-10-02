package com.aniflow.di

import com.aniflow.domain.repository.DownloadRepository
import com.aniflow.domain.repository.RuleRepository
import com.aniflow.domain.repository.SettingsRepository
import com.aniflow.domain.service.DuplicateDetector
import com.aniflow.domain.service.GroupingEngine
import com.aniflow.domain.service.GroupingEngineImpl
import com.aniflow.domain.service.ReleaseParser
import com.aniflow.domain.service.SelectionEngine
import com.aniflow.domain.service.SelectionEngineImpl
import com.aniflow.domain.service.StorageManager
import com.aniflow.domain.usecase.BuildDownloadPlanUseCase
import com.aniflow.domain.usecase.CancelDownloadUseCase
import com.aniflow.domain.usecase.FindMissingEpisodesUseCase
import com.aniflow.domain.usecase.GroupReleasesUseCase
import com.aniflow.domain.usecase.ParseReleaseUseCase
import com.aniflow.domain.usecase.PauseDownloadUseCase
import com.aniflow.domain.usecase.QueueDownloadUseCase
import com.aniflow.domain.usecase.ResumeDownloadUseCase
import com.aniflow.domain.usecase.SelectReleasesUseCase
import com.aniflow.domain.usecase.ValidateDownloadPlanUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun provideGroupingEngine(): GroupingEngine = GroupingEngineImpl()

    @Provides
    @Singleton
    fun provideSelectionEngine(): SelectionEngine = SelectionEngineImpl()

    // Domain Use Cases
    @Provides
    fun provideParseReleaseUseCase(parser: ReleaseParser) =
        ParseReleaseUseCase(parser)

    @Provides
    fun provideGroupReleasesUseCase(groupingEngine: GroupingEngine) =
        GroupReleasesUseCase(groupingEngine)

    @Provides
    fun provideFindMissingEpisodesUseCase(groupingEngine: GroupingEngine) =
        FindMissingEpisodesUseCase(groupingEngine)

    @Provides
    fun provideSelectReleasesUseCase(
        selectionEngine: SelectionEngine,
        ruleRepository: RuleRepository
    ) = SelectReleasesUseCase(selectionEngine, ruleRepository)

    @Provides
    fun provideBuildDownloadPlanUseCase(
        duplicateDetector: DuplicateDetector,
        storageManager: StorageManager,
        settingsRepository: SettingsRepository
    ) = BuildDownloadPlanUseCase(duplicateDetector, storageManager, settingsRepository)

    @Provides
    fun provideValidateDownloadPlanUseCase(storageManager: StorageManager) =
        ValidateDownloadPlanUseCase(storageManager)

    @Provides
    fun provideQueueDownloadUseCase(
        downloadRepository: DownloadRepository,
        storageManager: StorageManager
    ) = QueueDownloadUseCase(downloadRepository, storageManager)

    @Provides
    fun providePauseDownloadUseCase(downloadRepository: DownloadRepository) =
        PauseDownloadUseCase(downloadRepository)

    @Provides
    fun provideResumeDownloadUseCase(downloadRepository: DownloadRepository) =
        ResumeDownloadUseCase(downloadRepository)

    @Provides
    fun provideCancelDownloadUseCase(
        downloadRepository: DownloadRepository,
        storageManager: StorageManager
    ) = CancelDownloadUseCase(downloadRepository, storageManager)
}
