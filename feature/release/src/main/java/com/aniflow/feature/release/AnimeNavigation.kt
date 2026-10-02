package com.aniflow.feature.release

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink

/**
 * Step 20 — UI Navigation & Deep Links (Section 74, 75).
 * Only uses strongly typed IDs across screen navigation boundaries.
 */
object AnimeDestinations {
    const val ANIME_DETAIL_ROUTE = "anime/{animeId}"
    const val SEASON_DETAIL_ROUTE = "anime/{animeId}/season/{seasonId}"
    const val EPISODE_DETAIL_ROUTE = "episode/{episodeId}"
    const val BATCH_DETAIL_ROUTE = "batch/{releaseId}"
    const val EPISODE_COMPARISON_ROUTE = "compare/{releaseIds}"
    const val MISSING_EPISODES_ROUTE = "anime/{animeId}/missing"

    const val DEEP_LINK_SCHEME = "aniflow"

    fun animeDetail(animeId: String): String = "anime/$animeId"
    fun seasonDetail(animeId: String, seasonId: String): String = "anime/$animeId/season/$seasonId"
    fun episodeDetail(episodeId: String): String = "episode/$episodeId"
    fun batchDetail(releaseId: String): String = "batch/$releaseId"
    fun compare(releaseIds: List<String>): String = "compare/${releaseIds.joinToString(",")}"
    fun missingEpisodes(animeId: String): String = "anime/$animeId/missing"

    val animeDetailArguments: List<NamedNavArgument> = listOf(
        navArgument("animeId") { type = NavType.StringType }
    )

    val animeDetailDeepLinks = listOf(
        navDeepLink { uriPattern = "$DEEP_LINK_SCHEME://anime/{animeId}" }
    )

    val seasonDetailArguments: List<NamedNavArgument> = listOf(
        navArgument("animeId") { type = NavType.StringType },
        navArgument("seasonId") { type = NavType.StringType }
    )

    val seasonDetailDeepLinks = listOf(
        navDeepLink { uriPattern = "$DEEP_LINK_SCHEME://anime/{animeId}/season/{seasonId}" }
    )

    val episodeDetailArguments: List<NamedNavArgument> = listOf(
        navArgument("episodeId") { type = NavType.StringType }
    )

    val episodeDetailDeepLinks = listOf(
        navDeepLink { uriPattern = "$DEEP_LINK_SCHEME://episode/{episodeId}" }
    )

    val releaseDetailDeepLinks = listOf(
        navDeepLink { uriPattern = "$DEEP_LINK_SCHEME://release/{releaseId}" }
    )
}
