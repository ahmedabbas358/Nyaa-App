package com.aniflow.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Centrally defined Typed Route Model (Sections 159, 160, 192).
 * Prevents hard-coded magic strings throughout the presentation layer.
 */
sealed class ScreenRoute(val route: String) {
    data object Home : ScreenRoute("home")
    data object Search : ScreenRoute("search")
    data object Downloads : ScreenRoute("downloads")
    data object Library : ScreenRoute("library")
    data object More : ScreenRoute("more")

    // Sub-routes & Detail destinations
    data object ReleaseDetails : ScreenRoute("release/{releaseId}") {
        fun createRoute(releaseId: String): String = "release/$releaseId"
    }

    data object AnimeDetails : ScreenRoute("anime/{animeId}") {
        fun createRoute(animeId: String): String = "anime/$animeId"
    }

    data object SeasonDetails : ScreenRoute("anime/{animeId}/season/{seasonNumber}") {
        fun createRoute(animeId: String, seasonNumber: Int): String = "anime/$animeId/season/$seasonNumber"
    }

    data object SelectionReview : ScreenRoute("selection_review")
    data object DownloadPlan : ScreenRoute("download_plan")
    data object DownloadDetails : ScreenRoute("download_details/{taskId}") {
        fun createRoute(taskId: String): String = "download_details/$taskId"
    }

    data object Collections : ScreenRoute("collections")
    data object CollectionDetails : ScreenRoute("collection/{collectionId}") {
        fun createRoute(collectionId: String): String = "collection/$collectionId"
    }

    data object Favorites : ScreenRoute("favorites")
    data object Watchlist : ScreenRoute("watchlist")
    data object SavedSearches : ScreenRoute("saved_searches")
    data object Automation : ScreenRoute("automation")
    data object History : ScreenRoute("history")
    data object Storage : ScreenRoute("storage")
    data object Profiles : ScreenRoute("profiles")
    data object ProfileDetails : ScreenRoute("profile/{profileId}") {
        fun createRoute(profileId: String): String = "profile/$profileId"
    }
    data object Rules : ScreenRoute("rules")
    data object RuleBuilder : ScreenRoute("rule_builder")
    data object Settings : ScreenRoute("settings")
    data object About : ScreenRoute("about")
    data object DeveloperMode : ScreenRoute("developer_mode")
    data object ImportLink : ScreenRoute("import_link")
    data object Comparison : ScreenRoute("comparison")
    data object Player : ScreenRoute("player/{mediaId}") {
        fun createRoute(mediaId: String): String = "player/$mediaId"
    }
    data object Onboarding : ScreenRoute("onboarding")
    data object Diagnostics : ScreenRoute("diagnostics")
}

data class BottomNavDestination(
    val route: ScreenRoute,
    val title: String,
    val icon: ImageVector
)

val mainBottomNavDestinations = listOf(
    BottomNavDestination(ScreenRoute.Home, "Home", Icons.Default.Home),
    BottomNavDestination(ScreenRoute.Search, "Search", Icons.Default.Search),
    BottomNavDestination(ScreenRoute.Downloads, "Downloads", Icons.Default.Download),
    BottomNavDestination(ScreenRoute.Library, "Library", Icons.Default.Folder),
    BottomNavDestination(ScreenRoute.More, "More", Icons.Default.MoreHoriz)
)
