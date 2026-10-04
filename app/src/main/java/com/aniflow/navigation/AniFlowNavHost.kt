package com.aniflow.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aniflow.feature.collections.CollectionsScreen
import com.aniflow.feature.downloads.DownloadDetailScreen
import com.aniflow.feature.downloads.DownloadPlanScreen
import com.aniflow.feature.downloads.DownloadsScreen
import com.aniflow.feature.downloads.DownloadsViewModel
import com.aniflow.feature.favorites.FavoritesScreen
import com.aniflow.feature.home.HomeScreen
import com.aniflow.feature.home.HomeViewModel
import com.aniflow.feature.library.LibraryScreen
import com.aniflow.feature.library.LibraryViewModel
import com.aniflow.feature.release.AnimeSeasonScreen
import com.aniflow.feature.release.ReleaseDetailScreen
import com.aniflow.feature.release.ReleaseDetailViewModel
import com.aniflow.feature.search.SearchScreen
import com.aniflow.feature.search.SearchViewModel
import com.aniflow.feature.settings.DeveloperModeScreen
import com.aniflow.feature.settings.ImportLinkScreen
import com.aniflow.feature.settings.MoreScreen
import com.aniflow.feature.settings.AboutScreen
import com.aniflow.feature.settings.SettingsScreen
import com.aniflow.feature.settings.SettingsViewModel
import com.aniflow.feature.settings.StorageScreen
import com.aniflow.feature.collections.CollectionDetailScreen
import com.aniflow.feature.storage.StorageSetupScreen
import com.aniflow.feature.library.DuplicatesScreen
import com.aniflow.feature.library.UnidentifiedMediaScreen
import com.aniflow.feature.library.UpgradesScreen
import com.aniflow.feature.settings.NotificationsScreen
import com.aniflow.feature.settings.HistoryScreen
import com.aniflow.feature.settings.RuleHistoryScreen
import com.aniflow.feature.search.SearchHistoryScreen

@Composable
fun AniFlowApp(
    homeViewModel: HomeViewModel,
    searchViewModel: SearchViewModel,
    downloadsViewModel: DownloadsViewModel,
    libraryViewModel: LibraryViewModel,
    settingsViewModel: SettingsViewModel,
    releaseDetailViewModelFactory: (String) -> ReleaseDetailViewModel,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val downloadsUiState by downloadsViewModel.uiState.collectAsState()
    val activeTask = downloadsUiState.rawTasks.firstOrNull { it.state.isActive }
        ?: downloadsUiState.rawTasks.firstOrNull { it.state == com.aniflow.feature.downloads.model.DownloadStateUi.Queued }
        ?: downloadsUiState.rawTasks.firstOrNull { it.state == com.aniflow.feature.downloads.model.DownloadStateUi.Paused }

    val activeTitle = activeTask?.title
    val activeSpeed = activeTask?.speedFormatted ?: downloadsUiState.totalDownloadSpeedFormatted
    val activeProgress = (activeTask?.progressPercent ?: downloadsUiState.overallProgressPercent) / 100f
    val isDownloading = activeTask?.state == com.aniflow.feature.downloads.model.DownloadStateUi.Downloading

    AppShell(
        navController = navController,
        currentRoute = currentRoute,
        onQuickImportClick = { navController.navigate(ScreenRoute.ImportLink.route) },
        activeDownloadTitle = activeTitle,
        activeDownloadSpeed = activeSpeed,
        activeDownloadProgress = activeProgress,
        isDownloadActive = isDownloading,
        onTogglePauseResume = {
            activeTask?.let { task ->
                if (task.state == com.aniflow.feature.downloads.model.DownloadStateUi.Downloading) {
                    downloadsViewModel.pauseTask(task.id)
                } else {
                    downloadsViewModel.resumeTask(task.id)
                }
            }
        }
    ) { contentModifier ->
        NavHost(
            navController = navController,
            startDestination = ScreenRoute.Home.route,
            modifier = contentModifier
        ) {
            // Top Level Destinations
            composable(ScreenRoute.Home.route) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToSearch = { navController.navigate(ScreenRoute.Search.route) },
                    onNavigateToDownloads = { navController.navigate(ScreenRoute.Downloads.route) },
                    onReleaseClick = { releaseId ->
                        navController.navigate(ScreenRoute.ReleaseDetails.createRoute(releaseId))
                    },
                    onMediaPlayClick = { mediaId ->
                        navController.navigate(ScreenRoute.Player.createRoute(mediaId))
                    }
                )
            }

            composable(ScreenRoute.Search.route) {
                SearchScreen(
                    viewModel = searchViewModel,
                    onAnimeClick = { animeId ->
                        navController.navigate(ScreenRoute.AnimeDetails.createRoute(animeId))
                    },
                    onReleaseClick = { releaseId ->
                        navController.navigate(ScreenRoute.ReleaseDetails.createRoute(releaseId))
                    }
                )
            }

            composable(ScreenRoute.Downloads.route) {
                DownloadsScreen(
                    viewModel = downloadsViewModel,
                    onOpenTaskDetails = { taskId ->
                        navController.navigate(ScreenRoute.DownloadDetails.createRoute(taskId))
                    },
                    onOpenStatistics = {
                        navController.navigate(ScreenRoute.DownloadStatistics.route)
                    },
                    onOpenHistory = {
                        navController.navigate(ScreenRoute.DownloadHistory.route)
                    }
                )
            }

            composable(ScreenRoute.Library.route) {
                LibraryScreen(
                    viewModel = libraryViewModel,
                    onAnimeClick = { animeId ->
                        navController.navigate(ScreenRoute.AnimeDetails.createRoute(animeId))
                    }
                )
            }

            composable(ScreenRoute.More.route) {
                MoreScreen(
                    onNavigateToFavorites = { navController.navigate(ScreenRoute.Favorites.route) },
                    onNavigateToCollections = { navController.navigate(ScreenRoute.Collections.route) },
                    onNavigateToWatchlist = { navController.navigate(ScreenRoute.Watchlist.route) },
                    onNavigateToSavedSearches = { navController.navigate(ScreenRoute.SavedSearches.route) },
                    onNavigateToNotifications = { navController.navigate(ScreenRoute.Notifications.route) },
                    onNavigateToAutomation = { navController.navigate(ScreenRoute.Automation.route) },
                    onNavigateToRules = { navController.navigate(ScreenRoute.Rules.route) },
                    onNavigateToRuleHistory = { navController.navigate(ScreenRoute.RuleHistory.route) },
                    onNavigateToProfiles = { navController.navigate(ScreenRoute.Profiles.route) },
                    onNavigateToHistory = { navController.navigate(ScreenRoute.History.route) },
                    onNavigateToStorage = { navController.navigate(ScreenRoute.Storage.route) },
                    onNavigateToStorageSetup = { navController.navigate(ScreenRoute.StorageSetup.route) },
                    onNavigateToDuplicates = { navController.navigate(ScreenRoute.Duplicates.route) },
                    onNavigateToUnidentifiedMedia = { navController.navigate(ScreenRoute.UnidentifiedMedia.route) },
                    onNavigateToUpgrades = { navController.navigate(ScreenRoute.Upgrades.route) },
                    onNavigateToSettings = { navController.navigate(ScreenRoute.Settings.route) },
                    onNavigateToDiagnostics = { navController.navigate(ScreenRoute.Diagnostics.route) },
                    onNavigateToAbout = { navController.navigate(ScreenRoute.About.route) }
                )
            }

            // Detail & Flow Destinations
            composable(ScreenRoute.ReleaseDetails.route) { backStackEntry ->
                val releaseId = backStackEntry.arguments?.getString("releaseId") ?: ""
                val viewModel = releaseDetailViewModelFactory(releaseId)
                ReleaseDetailScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(ScreenRoute.BatchDetails.route) { backStackEntry ->
                val releaseId = backStackEntry.arguments?.getString("releaseId") ?: ""
                val releaseViewModel = releaseDetailViewModelFactory(releaseId)
                val state by releaseViewModel.uiState.collectAsState()
                val rel = state.release
                val batchState = com.aniflow.feature.release.BatchDetailUiState(
                    releaseId = releaseId,
                    batchTitle = rel?.title ?: "Batch Release #$releaseId",
                    size = rel?.availability?.size?.let { "${it.bytes / (1024 * 1024)} MB" } ?: "Unknown",
                    uploader = rel?.uploader?.name,
                    releaseGroup = rel?.releaseGroup?.name,
                    seeders = rel?.availability?.seeders ?: 0,
                    resolution = rel?.technical?.resolution?.displayName,
                    videoCodec = rel?.technical?.videoCodec?.displayName,
                    audioCodec = rel?.technical?.audioTracks?.firstOrNull()?.codec?.displayName,
                    subtitleSummary = rel?.technical?.subtitles?.mapNotNull { it.language?.code ?: it.label }?.joinToString(", "),
                    isCoverageInferred = true,
                    coveredEpisodes = when (val er = rel?.episodeRange) {
                        is com.aniflow.domain.valueobject.EpisodeRange.Range -> (er.start.major..er.end.major).map { ep ->
                            com.aniflow.feature.release.BatchEpisodeCoverageItem(
                                episodeNumber = ep,
                                isCovered = true
                            )
                        }
                        else -> (1..12).map { ep ->
                            com.aniflow.feature.release.BatchEpisodeCoverageItem(
                                episodeNumber = ep,
                                isCovered = true
                            )
                        }
                    }
                )
                com.aniflow.feature.release.BatchDetailScreen(
                    uiState = batchState,
                    onBackClick = { navController.popBackStack() },
                    onDownloadBatchClick = {
                        rel?.let { release ->
                            val magnetOrUrl = (release.source as? com.aniflow.domain.model.aggregate.release.ReleaseSource.Torrent)?.magnetUri?.rawValue
                                ?: (release.source as? com.aniflow.domain.model.aggregate.release.ReleaseSource.Torrent)?.torrentUrl?.rawValue
                                ?: "https://nyaa.si/download/$releaseId.torrent"
                            downloadsViewModel.queueFromLink(
                                link = magnetOrUrl,
                                title = release.title
                            )
                            navController.navigate(ScreenRoute.Downloads.route) {
                                popUpTo(ScreenRoute.Home.route)
                            }
                        }
                    }
                )
            }

            composable(ScreenRoute.AnimeDetails.route) {
                AnimeSeasonScreen(
                    onBack = { navController.popBackStack() },
                    onPlanReviewClick = { navController.navigate(ScreenRoute.DownloadPlan.route) }
                )
            }

            composable(ScreenRoute.DownloadPlan.route) {
                DownloadPlanScreen(
                    onBack = { navController.popBackStack() },
                    onChangeStorageClick = { navController.navigate(ScreenRoute.Storage.route) },
                    onStartDownloadsClick = {
                        navController.navigate(ScreenRoute.Downloads.route) {
                            popUpTo(ScreenRoute.Home.route)
                        }
                    }
                )
            }

            composable(ScreenRoute.DownloadDetails.route) { backStackEntry ->
                val taskId = backStackEntry.arguments?.getString("taskId") ?: ""
                DownloadDetailScreen(
                    taskId = taskId,
                    viewModel = downloadsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(ScreenRoute.Collections.route) {
                CollectionsScreen(
                    onCollectionClick = { collectionId ->
                        navController.navigate(ScreenRoute.CollectionDetails.createRoute(collectionId))
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(ScreenRoute.Favorites.route) {
                FavoritesScreen(
                    onItemClick = { animeId ->
                        navController.navigate(ScreenRoute.AnimeDetails.createRoute(animeId))
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(ScreenRoute.Storage.route) {
                StorageScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(ScreenRoute.Profiles.route) {
                com.aniflow.feature.settings.ProfileManagementScreen(
                    onBack = { navController.popBackStack() },
                    onSelectProfile = { profileId ->
                        navController.navigate(ScreenRoute.ProfileDetails.createRoute(profileId.value))
                    }
                )
            }

            composable(ScreenRoute.ProfileDetails.route) { backStackEntry ->
                val profileId = backStackEntry.arguments?.getString("profileId") ?: ""
                com.aniflow.feature.settings.ProfileDetailScreen(
                    profileId = com.aniflow.domain.identity.ProfileId(profileId),
                    onBack = { navController.popBackStack() }
                )
            }

            composable(ScreenRoute.Rules.route) {
                com.aniflow.feature.settings.RuleListScreen(
                    onBack = { navController.popBackStack() },
                    onCreateNewRule = { navController.navigate(ScreenRoute.RuleBuilder.route) }
                )
            }

            composable(ScreenRoute.RuleBuilder.route) {
                com.aniflow.feature.settings.RuleBuilderScreen(
                    onBack = { navController.popBackStack() },
                    onSaveRule = { navController.popBackStack() }
                )
            }

            composable(ScreenRoute.DeveloperMode.route) {
                DeveloperModeScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(ScreenRoute.ImportLink.route) {
                ImportLinkScreen(
                    onBack = { navController.popBackStack() },
                    onAddToQueue = { link ->
                        downloadsViewModel.queueFromLink(link)
                        navController.navigate(ScreenRoute.Downloads.route) {
                            popUpTo(ScreenRoute.Home.route)
                        }
                    }
                )
            }

            composable(ScreenRoute.Settings.route) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToProfiles = { navController.navigate(ScreenRoute.Profiles.route) },
                    onNavigateToAutomation = { navController.navigate(ScreenRoute.Automation.route) },
                    onNavigateToStorage = { navController.navigate(ScreenRoute.Storage.route) },
                    onNavigateToDiagnostics = { navController.navigate(ScreenRoute.Diagnostics.route) },
                    onNavigateToAbout = { navController.navigate(ScreenRoute.About.route) }
                )
            }

            composable(ScreenRoute.Watchlist.route) {
                com.aniflow.feature.watchlist.ui.WatchlistScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(ScreenRoute.SavedSearches.route) {
                com.aniflow.feature.savedsearch.ui.SavedSearchScreen(
                    onBack = { navController.popBackStack() },
                    onAutomate = { _ -> navController.navigate(ScreenRoute.Automation.route) }
                )
            }

            composable(ScreenRoute.Automation.route) {
                com.aniflow.feature.automation.ui.AutomationDashboardScreen(
                    onBack = { navController.popBackStack() },
                    onOpenSettings = { navController.navigate(ScreenRoute.Settings.route) }
                )
            }

            composable(ScreenRoute.About.route) {
                AboutScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(ScreenRoute.Player.route) { backStackEntry ->
                val mediaId = backStackEntry.arguments?.getString("mediaId") ?: ""
                com.aniflow.feature.library.PlayerScreen(
                    mediaId = com.aniflow.domain.identity.LibraryMediaId(mediaId),
                    onBack = { navController.popBackStack() },
                    onDownloadNextEpisode = { nextEp ->
                        navController.navigate(ScreenRoute.Downloads.route)
                    },
                    onSearchReleases = { query ->
                        navController.navigate(ScreenRoute.Search.route)
                    }
                )
            }

            composable(ScreenRoute.Onboarding.route) {
                com.aniflow.feature.home.OnboardingScreen(
                    onFinishOnboarding = {
                        navController.navigate(ScreenRoute.Home.route) {
                            popUpTo(ScreenRoute.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(ScreenRoute.Diagnostics.route) {
                com.aniflow.feature.settings.DiagnosticsScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(ScreenRoute.StorageSetup.route) {
                StorageSetupScreen(
                    onContinue = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(ScreenRoute.Duplicates.route) {
                DuplicatesScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(ScreenRoute.UnidentifiedMedia.route) {
                UnidentifiedMediaScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(ScreenRoute.Upgrades.route) {
                UpgradesScreen(
                    onBack = { navController.popBackStack() },
                    onReviewRelease = { releaseId ->
                        navController.navigate(ScreenRoute.ReleaseDetails.createRoute(releaseId))
                    }
                )
            }

            composable(ScreenRoute.Notifications.route) {
                NotificationsScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateRoute = { route -> navController.navigate(route) }
                )
            }

            composable(ScreenRoute.History.route) {
                HistoryScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateDestination = { route -> navController.navigate(route) }
                )
            }

            composable(ScreenRoute.SearchHistory.route) {
                SearchHistoryScreen(
                    onBack = { navController.popBackStack() },
                    onSearchAgain = { query ->
                        searchViewModel.onQueryChanged(query)
                        navController.navigate(ScreenRoute.Search.route)
                    }
                )
            }

            composable(ScreenRoute.RuleHistory.route) {
                RuleHistoryScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(ScreenRoute.CollectionDetails.route) { backStackEntry ->
                val collectionId = backStackEntry.arguments?.getString("collectionId") ?: ""
                CollectionDetailScreen(
                    collectionId = collectionId,
                    onBack = { navController.popBackStack() },
                    onAnimeClick = { animeId ->
                        navController.navigate(ScreenRoute.AnimeDetails.createRoute(animeId))
                    }
                )
            }
        }
    }
}

