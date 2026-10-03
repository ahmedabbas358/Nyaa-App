package com.aniflow.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.aniflow.core.ui.R
import com.aniflow.core.ui.theme.AniFlowTheme
import com.aniflow.core.ui.theme.AppElevation

enum class WindowWidthSize {
    Compact,   // Phones (< 600dp)
    Medium,    // Small Tablets / Foldables (600dp - 840dp)
    Expanded   // Tablets / Desktop (> 840dp)
}

/**
 * AppShell (Sections 11, 106, 107, 110, 113, 114).
 *
 * Responsive, localized shell adapting between:
 * - Bottom Navigation for compact phones (< 600dp)
 * - Navigation Rail for medium and expanded tablets (>= 600dp)
 *
 * Fully themed for Light/Dark modes, RTL-safe, and listening for global Ctrl+K search shortcut.
 */
@Composable
fun AppShell(
    navController: NavHostController,
    currentRoute: String?,
    windowWidthSize: WindowWidthSize? = null,
    onQuickImportClick: () -> Unit = {},
    content: @Composable (Modifier) -> Unit
) {
    val configuration = LocalConfiguration.current
    val effectiveWindowSize = windowWidthSize ?: when {
        configuration.screenWidthDp < 600 -> WindowWidthSize.Compact
        configuration.screenWidthDp < 840 -> WindowWidthSize.Medium
        else -> WindowWidthSize.Expanded
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val colors = AniFlowTheme.colors

    val isTopLevelRoute = mainBottomNavDestinations.any { it.route.route == currentRoute }

    val keyModifier = Modifier.onKeyEvent { keyEvent ->
        if (keyEvent.isCtrlPressed && keyEvent.key == Key.K) {
            navController.navigate(ScreenRoute.Search.route) {
                launchSingleTop = true
            }
            true
        } else {
            false
        }
    }

    if (effectiveWindowSize == WindowWidthSize.Compact) {
        // Phone Layout: Bottom Navigation Bar
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (isTopLevelRoute) {
                    NavigationBar(
                        containerColor = colors.surface,
                        tonalElevation = AppElevation.card
                    ) {
                        mainBottomNavDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route.route
                            val title = stringResource(dest.titleRes)

                            NavigationBarItem(
                                icon = { Icon(dest.icon, contentDescription = title) },
                                label = { Text(title, style = AniFlowTheme.typography.caption) },
                                selected = selected,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = colors.primary,
                                    selectedTextColor = colors.primary,
                                    indicatorColor = colors.primary.copy(alpha = 0.15f),
                                    unselectedIconColor = colors.textSecondary,
                                    unselectedTextColor = colors.textSecondary
                                ),
                                onClick = {
                                    navController.navigate(dest.route.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .then(keyModifier)
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.background)
            ) {
                content(Modifier.padding(paddingValues))
            }
        }
    } else {
        // Tablet / Expanded Layout: Navigation Rail Sidebar
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            modifier = Modifier
                .fillMaxSize()
                .then(keyModifier)
        ) { paddingValues ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(colors.background)
            ) {
                NavigationRail(
                    containerColor = colors.surface,
                    header = {
                        FloatingActionButton(
                            onClick = onQuickImportClick,
                            containerColor = colors.primary,
                            contentColor = colors.textPrimary,
                            modifier = Modifier.padding(vertical = 16.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = stringResource(R.string.cd_quick_import)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxHeight()
                ) {
                    mainBottomNavDestinations.forEach { dest ->
                        val selected = currentRoute == dest.route.route
                        val title = stringResource(dest.titleRes)

                        NavigationRailItem(
                            icon = { Icon(dest.icon, contentDescription = title) },
                            label = { Text(title, style = AniFlowTheme.typography.caption) },
                            selected = selected,
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = colors.primary,
                                selectedTextColor = colors.primary,
                                indicatorColor = colors.primary.copy(alpha = 0.15f),
                                unselectedIconColor = colors.textSecondary,
                                unselectedTextColor = colors.textSecondary
                            ),
                            onClick = {
                                navController.navigate(dest.route.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(colors.background)
                ) {
                    content(Modifier.fillMaxSize())
                }
            }
        }
    }
}
