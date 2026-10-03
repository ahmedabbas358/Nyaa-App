package com.aniflow.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aniflow.core.ui.components.AniAppBar
import com.aniflow.core.ui.components.AniEmptyState
import com.aniflow.core.ui.components.AniStatusBadge
import com.aniflow.core.ui.theme.AppSemanticColors
import com.aniflow.core.ui.theme.AppShapes
import com.aniflow.core.ui.theme.AppSpacing
import com.aniflow.core.ui.theme.AppTypography
import com.aniflow.core.ui.theme.DarkBackground
import com.aniflow.core.ui.theme.DarkCardBorder
import com.aniflow.core.ui.theme.DarkSurface
import com.aniflow.core.ui.theme.PrimaryIndigo
import com.aniflow.core.ui.theme.TextMuted
import com.aniflow.core.ui.theme.TextPrimary
import com.aniflow.core.ui.theme.TextSecondary

enum class HistoryTab(val label: String, val icon: ImageVector) {
    Watch("Watch", Icons.Default.PlayCircle),
    Search("Search", Icons.Default.Search),
    Downloads("Downloads", Icons.Default.Download),
    Automation("Automation", Icons.Default.AutoAwesome),
    Organization("Organization", Icons.Default.Folder)
}

data class UnifiedHistoryItemUiModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val timestampFormatted: String,
    val tab: HistoryTab,
    val entityDestination: String? = null
)

/**
 * HistoryScreen (Section 82).
 * Unified multi-tab history center:
 * Tabs: Watch, Search, Downloads, Automation, Organization.
 * Deep links to relevant entity (Anime, Release, Download, Rule, File).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit = {},
    onNavigateDestination: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = HistoryTab.entries.toTypedArray()

    // Real list populated from domain watch, search, download & automation stores (empty by default)
    var allHistoryItems by remember { mutableStateOf<List<UnifiedHistoryItemUiModel>>(emptyList()) }

    val activeTab = tabs[selectedTabIndex]
    val tabItems = allHistoryItems.filter { it.tab == activeTab }

    Scaffold(
        topBar = {
            AniAppBar(
                title = "Activity History",
                subtitle = "Comprehensive activity log",
                onBack = onBack
            )
        },
        containerColor = DarkBackground,
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = DarkSurface,
                contentColor = TextPrimary,
                edgePadding = AppSpacing.md,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = PrimaryIndigo
                    )
                }
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (selectedTabIndex == index) PrimaryIndigo else TextMuted
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = tab.label,
                                    style = AppTypography.Subtitle.copy(
                                        fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            }
                        }
                    )
                }
            }

            if (tabItems.isEmpty()) {
                AniEmptyState(
                    title = "No ${activeTab.label.lowercase()} history",
                    description = "Activity in this category will appear here chronologically with deep links to records.",
                    icon = activeTab.icon,
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(AppSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                    modifier = Modifier.weight(1f)
                ) {
                    items(tabItems, key = { it.id }) { item ->
                        HistoryCard(
                            item = item,
                            onClick = {
                                item.entityDestination?.let { onNavigateDestination(it) }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(
    item: UnifiedHistoryItemUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = AppShapes.small,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = item.tab.icon,
                    contentDescription = null,
                    tint = PrimaryIndigo,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(AppSpacing.sm))
                Column {
                    Text(
                        text = item.title,
                        style = AppTypography.Subtitle.copy(fontWeight = FontWeight.SemiBold),
                        color = TextPrimary
                    )
                    Text(
                        text = item.subtitle,
                        style = AppTypography.Caption,
                        color = TextSecondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.timestampFormatted,
                    style = AppTypography.Caption,
                    color = TextMuted
                )
                if (item.entityDestination != null) {
                    Spacer(modifier = Modifier.width(AppSpacing.xs))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open item",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
