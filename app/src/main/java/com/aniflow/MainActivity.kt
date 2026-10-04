package com.aniflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aniflow.core.ui.theme.AniFlowTheme
import com.aniflow.domain.repository.DownloadRepository
import com.aniflow.domain.repository.LibraryRepository
import com.aniflow.domain.repository.ReleaseRepository
import com.aniflow.domain.repository.SettingsRepository
import com.aniflow.domain.usecase.BuildDownloadPlanUseCase
import com.aniflow.domain.usecase.CancelDownloadUseCase
import com.aniflow.domain.usecase.PauseDownloadUseCase
import com.aniflow.domain.usecase.QueueDownloadUseCase
import com.aniflow.domain.usecase.ResumeDownloadUseCase
import com.aniflow.feature.downloads.DownloadsViewModel
import com.aniflow.feature.home.HomeViewModel
import com.aniflow.feature.library.LibraryViewModel
import com.aniflow.feature.release.ReleaseDetailViewModel
import com.aniflow.feature.search.SearchViewModel
import com.aniflow.feature.settings.SettingsViewModel
import com.aniflow.navigation.AniFlowApp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var releaseRepository: ReleaseRepository
    @Inject lateinit var downloadRepository: DownloadRepository
    @Inject lateinit var libraryRepository: LibraryRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var searchReleasesCoordinatorUseCase: com.aniflow.domain.usecase.SearchReleasesCoordinatorUseCase
    @Inject lateinit var searchReleasesUseCase: com.aniflow.domain.usecase.SearchReleasesUseCase
    @Inject lateinit var prepareDownloadPlanUseCase: com.aniflow.domain.usecase.PrepareDownloadPlanUseCase
    @Inject lateinit var queueDownloadUseCase: QueueDownloadUseCase
    @Inject lateinit var pauseDownloadUseCase: PauseDownloadUseCase
    @Inject lateinit var resumeDownloadUseCase: ResumeDownloadUseCase
    @Inject lateinit var cancelDownloadUseCase: CancelDownloadUseCase
    @Inject lateinit var getReleaseDetailsUseCase: com.aniflow.domain.usecase.GetReleaseDetailsUseCase
    @Inject lateinit var watchProgressRepository: com.aniflow.domain.library.repository.WatchProgressRepository
    @Inject lateinit var downloadExecutionCoordinator: com.aniflow.download.service.DownloadExecutionCoordinator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Start active download execution coordinator (Nyaa torrent + HTTP engines, concurrency slots, notifications)
        downloadExecutionCoordinator.start()

        val realtimeSyncScheduler = com.aniflow.feature.automation.scheduler.NyaaRealtimeSyncScheduler(
            searchCoordinatorUseCase = searchReleasesCoordinatorUseCase,
            queueDownloadUseCase = queueDownloadUseCase
        )

        val homeViewModel = HomeViewModel(
            releaseRepository = releaseRepository,
            downloadRepository = downloadRepository,
            watchProgressRepository = watchProgressRepository,
            searchCoordinatorUseCase = searchReleasesCoordinatorUseCase,
            queueDownloadUseCase = queueDownloadUseCase,
            syncScheduler = realtimeSyncScheduler
        )
        val searchViewModel = SearchViewModel(
            searchReleasesUseCase = searchReleasesUseCase,
            searchCoordinatorUseCase = searchReleasesCoordinatorUseCase,
            preparePlanUseCase = prepareDownloadPlanUseCase,
            queueDownloadUseCase = queueDownloadUseCase
        )
        val downloadsViewModel = DownloadsViewModel(
            downloadRepository = downloadRepository,
            pauseDownloadUseCase = pauseDownloadUseCase,
            resumeDownloadUseCase = resumeDownloadUseCase,
            cancelDownloadUseCase = cancelDownloadUseCase,
            queueDownloadUseCase = queueDownloadUseCase
        )
        val libraryViewModel = LibraryViewModel(libraryRepository)
        val settingsViewModel = SettingsViewModel(settingsRepository)

        setContent {
            AniFlowTheme {
                AniFlowApp(
                    homeViewModel = homeViewModel,
                    searchViewModel = searchViewModel,
                    downloadsViewModel = downloadsViewModel,
                    libraryViewModel = libraryViewModel,
                    settingsViewModel = settingsViewModel,
                    releaseDetailViewModelFactory = { releaseId ->
                        ReleaseDetailViewModel(
                            releaseRepository = releaseRepository,
                            queueDownloadUseCase = queueDownloadUseCase,
                            releaseId = releaseId,
                            getReleaseDetailsUseCase = getReleaseDetailsUseCase
                        )
                    }
                )
            }
        }
    }
}
