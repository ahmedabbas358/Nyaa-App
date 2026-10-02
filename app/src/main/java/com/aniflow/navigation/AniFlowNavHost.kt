package com.aniflow.navigation

import androidx.compose.runtime.Composable
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

    AppShell(
        navController = navController,
        currentRoute = currentRoute,
        onQuickImportClick = { navController.navigate(ScreenRoute.ImportLink.route) }
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
                    }
                )
            }

            composable(ScreenRoute.Search.route) {
                SearchScreen(
                    viewModel = searchViewModel,
                    onReleaseClick = { releaseId ->
                        navController.navigate(ScreenRoute.ReleaseDetails.createRoute(releaseId))
                    },
                    onReviewPlanClick = {
                        navController.navigate(ScreenRoute.DownloadPlan.route)
                    }
                )
            }

            composable(ScreenRoute.Downloads.route) {
                DownloadsScreen(
                    viewModel = downloadsViewModel,
                    onDownloadClick = { taskId ->
                        navController.navigate(ScreenRoute.DownloadDetails.createRoute(taskId))
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
                    onNavigateToAutomation = { navController.navigate(ScreenRoute.Automation.route) },
                    onNavigateToRules = { navController.navigate(ScreenRoute.Rules.route) },
                    onNavigateToProfiles = { navController.navigate(ScreenRoute.Profiles.route) },
                    onNavigateToHistory = { navController.navigate(ScreenRoute.DeveloperMode.route) },
                    onNavigateToStorage = { navController.navigate(ScreenRoute.Storage.route) },
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

            composable(ScreenRoute.DownloadDetails.route) {
                DownloadDetailScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(ScreenRoute.Collections.route) {
                CollectionsScreen(
                    onCollectionClick = { /* Navigate to collection details */ }
                )
            }

            composable(ScreenRoute.Favorites.route) {
                FavoritesScreen(
                    onItemClick = { /* Navigate to item */ }
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
                    onAddToQueue = {
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
                    onAutomate = { navController.navigate(ScreenRoute.Automation.route) }
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
        }
    }
}
