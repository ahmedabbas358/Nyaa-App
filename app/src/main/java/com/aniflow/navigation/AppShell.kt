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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalConfiguration
import com.aniflow.core.ui.theme.DarkBackground
import com.aniflow.core.ui.theme.DarkSurface
import com.aniflow.core.ui.theme.PrimaryIndigo
import com.aniflow.core.ui.theme.TextSecondary
import kotlinx.coroutines.launch

enum class WindowWidthSize {
    Compact,   // Phones (< 600dp)
    Medium,    // Small Tablets / Foldables (600dp - 840dp)
    Expanded   // Tablets / Desktop (> 840dp)
}

/**
 * AppShell (Sections 11, 106, 107, 110, 113, 114).
 * Responsive shell adapting between Bottom Navigation (Phone) and Navigation Rail (Tablet/Expanded),
 * hosting global Snackbars, Ctrl+K search shortcut, and RTL-safe navigation.
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
    val scope = rememberCoroutineScope()

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
                    NavigationBar(containerColor = DarkSurface) {
                        mainBottomNavDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route.route
                            NavigationBarItem(
                                icon = { Icon(dest.icon, contentDescription = dest.title) },
                                label = { Text(dest.title) },
                                selected = selected,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = PrimaryIndigo,
                                    selectedTextColor = PrimaryIndigo,
                                    indicatorColor = PrimaryIndigo.copy(alpha = 0.2f),
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
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
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBackground)
            ) {
                content(Modifier.padding(paddingValues))
            }
        }
    } else {
        // Tablet / Expanded Layout: Navigation Rail Sidebar (Section 20)
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(DarkBackground)
            ) {
                NavigationRail(
                    containerColor = DarkSurface,
                    header = {
                        FloatingActionButton(
                            onClick = onQuickImportClick,
                            containerColor = PrimaryIndigo,
                            modifier = Modifier.padding(vertical = 16.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Quick Import")
                        }
                    },
                    modifier = Modifier.fillMaxHeight()
                ) {
                    mainBottomNavDestinations.forEach { dest ->
                        val selected = currentRoute == dest.route.route
                        NavigationRailItem(
                            icon = { Icon(dest.icon, contentDescription = dest.title) },
                            label = { Text(dest.title) },
                            selected = selected,
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = PrimaryIndigo,
                                selectedTextColor = PrimaryIndigo,
                                indicatorColor = PrimaryIndigo.copy(alpha = 0.2f),
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
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

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    content(Modifier.fillMaxSize())
                }
            }
        }
    }
}
