package com.aniflow.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aniflow.core.ui.components.AniSectionHeader
import com.aniflow.core.ui.components.AniSettingRow
import com.aniflow.core.ui.theme.AppShapes
import com.aniflow.core.ui.theme.AppSpacing
import com.aniflow.core.ui.theme.AppTypography
import com.aniflow.core.ui.theme.DarkBackground
import com.aniflow.core.ui.theme.DarkCardBorder
import com.aniflow.core.ui.theme.DarkSurface
import com.aniflow.core.ui.theme.PrimaryIndigo
import com.aniflow.core.ui.theme.TextMuted
import com.aniflow.core.ui.theme.TextPrimary

import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Upgrade

/**
 * MoreScreen (Section 12, 146).
 * Central hub for secondary destinations, structured into 4 clean editorial groups:
 * 1. Media & Personal (Favorites, Collections, Watchlist, Saved Searches, Notifications)
 * 2. Intelligence & Automation (Automation, Rules, Rule History, Profiles, History)
 * 3. Storage & Infrastructure (Storage Manager, Storage Setup, Duplicates, Unidentified, Upgrades)
 * 4. System & Support (Settings, Diagnostics, About)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onNavigateToFavorites: () -> Unit = {},
    onNavigateToCollections: () -> Unit = {},
    onNavigateToWatchlist: () -> Unit = {},
    onNavigateToSavedSearches: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToAutomation: () -> Unit = {},
    onNavigateToRules: () -> Unit = {},
    onNavigateToRuleHistory: () -> Unit = {},
    onNavigateToProfiles: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToStorage: () -> Unit = {},
    onNavigateToStorageSetup: () -> Unit = {},
    onNavigateToDuplicates: () -> Unit = {},
    onNavigateToUnidentifiedMedia: () -> Unit = {},
    onNavigateToUpgrades: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToDiagnostics: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "More",
                        style = AppTypography.LargeTitle,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground,
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = AppSpacing.md,
                end = AppSpacing.md,
                top = AppSpacing.sm,
                bottom = AppSpacing.xxxl
            ),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.lg),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Group 1: Media & Personal
            item {
                Column {
                    AniSectionHeader(
                        title = "Media & Personal",
                        subtitle = "Curated collections, watchlist, notifications, and saved searches"
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = AppShapes.medium,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            AniSettingRow(
                                title = "Notifications",
                                description = "Downloads, anime, and automation alerts",
                                icon = Icons.Default.Notifications,
                                onClick = onNavigateToNotifications
                            )
                            AniSettingRow(
                                title = "Favorites",
                                description = "Bookmarked anime, release groups, and uploaders",
                                icon = Icons.Default.Favorite,
                                onClick = onNavigateToFavorites
                            )
                            AniSettingRow(
                                title = "Collections",
                                description = "Custom and smart franchise collections",
                                icon = Icons.Default.CollectionsBookmark,
                                onClick = onNavigateToCollections
                            )
                            AniSettingRow(
                                title = "Watchlist",
                                description = "Queue of anime planned to watch",
                                icon = Icons.Default.Bookmark,
                                onClick = onNavigateToWatchlist
                            )
                            AniSettingRow(
                                title = "Saved Searches",
                                description = "Monitored provider queries with recurring triggers",
                                icon = Icons.Default.BookmarkBorder,
                                onClick = onNavigateToSavedSearches
                            )
                        }
                    }
                }
            }

            // Group 2: Intelligence & Automation
            item {
                Column {
                    AniSectionHeader(
                        title = "Intelligence & Automation",
                        subtitle = "Autonomous download rules, quality profiles, and execution logs"
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = AppShapes.medium,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            AniSettingRow(
                                title = "Automation Dashboard",
                                description = "Monitor active background triggers and safety limits",
                                icon = Icons.Default.AutoMode,
                                onClick = onNavigateToAutomation
                            )
                            AniSettingRow(
                                title = "Automation Rules",
                                description = "Programmable WHEN / IF / THEN conditions",
                                icon = Icons.Default.Rule,
                                onClick = onNavigateToRules
                            )
                            AniSettingRow(
                                title = "Rule Execution History",
                                description = "Decision audit logs explaining why rules fired or skipped",
                                icon = Icons.Default.History,
                                onClick = onNavigateToRuleHistory
                            )
                            AniSettingRow(
                                title = "Quality Profiles",
                                description = "Resolution, codec, audio, and size preference sets",
                                icon = Icons.Default.Tune,
                                onClick = onNavigateToProfiles
                            )
                            AniSettingRow(
                                title = "Unified History",
                                description = "Watch, search, download, and automation logs",
                                icon = Icons.Default.History,
                                onClick = onNavigateToHistory
                            )
                        }
                    }
                }
            }

            // Group 3: Storage & Infrastructure
            item {
                Column {
                    AniSectionHeader(
                        title = "Storage & Infrastructure",
                        subtitle = "Library disks, download pools, duplicates, and health reconciliation"
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = AppShapes.medium,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            AniSettingRow(
                                title = "Storage Manager",
                                description = "Roots, free space thresholds, and SAF permissions",
                                icon = Icons.Default.Storage,
                                onClick = onNavigateToStorage
                            )
                            AniSettingRow(
                                title = "Storage Setup",
                                description = "First-run storage and download location roles",
                                icon = Icons.Default.Storage,
                                onClick = onNavigateToStorageSetup
                            )
                            AniSettingRow(
                                title = "Duplicate Media",
                                description = "Identify redundant downloads and compare qualities",
                                icon = Icons.Default.FolderCopy,
                                onClick = onNavigateToDuplicates
                            )
                            AniSettingRow(
                                title = "Unidentified Files",
                                description = "Ambiguous files requiring manual mapping",
                                icon = Icons.Default.HelpOutline,
                                onClick = onNavigateToUnidentifiedMedia
                            )
                            AniSettingRow(
                                title = "Quality Upgrades",
                                description = "Higher-quality candidates for existing library episodes",
                                icon = Icons.Default.Upgrade,
                                onClick = onNavigateToUpgrades
                            )
                        }
                    }
                }
            }

            // Group 4: System & Support
            item {
                Column {
                    AniSectionHeader(
                        title = "System & Support",
                        subtitle = "App configuration, diagnostics, and build details"
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = AppShapes.medium,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            AniSettingRow(
                                title = "Settings",
                                description = "Appearance, playback, networking, and backup",
                                icon = Icons.Default.Settings,
                                onClick = onNavigateToSettings
                            )
                            AniSettingRow(
                                title = "System Diagnostics",
                                description = "Health checks across engines and sanitized export",
                                icon = Icons.Default.Healing,
                                onClick = onNavigateToDiagnostics
                            )
                            AniSettingRow(
                                title = "About AniFlow",
                                description = "Version 1.0.0, open source notices, and architecture",
                                icon = Icons.Default.Info,
                                onClick = onNavigateToAbout
                            )
                        }
                    }
                }
            }
        }
    }
}
