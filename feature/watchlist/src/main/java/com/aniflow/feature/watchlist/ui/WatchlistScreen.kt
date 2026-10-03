package com.aniflow.feature.watchlist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.aniflow.domain.watchlist.model.FollowMode
import com.aniflow.domain.watchlist.model.WatchlistItem
import com.aniflow.domain.watchlist.model.WatchlistPolicy
import com.aniflow.domain.watchlist.model.WatchlistState
import com.aniflow.domain.watchlist.model.WatchlistTargetType
import com.aniflow.core.ui.components.AniAppBar
import com.aniflow.core.ui.components.AniEmptyState
import com.aniflow.domain.identity.WatchlistItemId

/**
 * WatchlistScreen (Section 7, 8, 9, 10, 11, 12, 71, 72, 73, 109, 152).
 * Distinct from Saved Searches: Watchlist tracks "Watch this entity" (Anime, Season, Episode).
 * Zero hardcoded production data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    initialItems: List<WatchlistItem> = emptyList(),
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val watchlistItems = remember { mutableStateListOf<WatchlistItem>().apply { addAll(initialItems) } }
    val tabs = listOf("All (${watchlistItems.size})", "Watching", "Paused")

    Scaffold(
        topBar = {
            AniAppBar(
                title = "Release Watchlists",
                subtitle = "Monitored entities and automatic release tracking",
                onBack = onBack
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkSurface,
                contentColor = PrimaryIndigo,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = PrimaryIndigo
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                style = AppTypography.body,
                                color = if (selectedTab == index) PrimaryIndigo else TextMuted
                            )
                        }
                    )
                }
            }

            val filteredItems = when (selectedTab) {
                1 -> watchlistItems.filter { it.state == WatchlistState.Watching }
                2 -> watchlistItems.filter { it.state == WatchlistState.Paused }
                else -> watchlistItems
            }

            if (filteredItems.isEmpty()) {
                AniEmptyState(
                    title = if (watchlistItems.isEmpty()) "No watchlist items" else "No items in this category",
                    description = if (watchlistItems.isEmpty()) {
                        "Track anime or episodes from Anime Details to automatically monitor new releases and follow updates."
                    } else {
                        "No watchlisted items match the selected state filter."
                    },
                    icon = Icons.Default.Bookmark,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(AppSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
                ) {
                    items(filteredItems, key = { it.id.value }) { item ->
                        WatchlistCard(
                            item = item,
                            onToggleState = {
                                val idx = watchlistItems.indexOf(item)
                                if (idx != -1) {
                                    val newState = if (item.state == WatchlistState.Watching) WatchlistState.Paused else WatchlistState.Watching
                                    watchlistItems[idx] = item.copy(state = newState)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WatchlistCard(
    item: WatchlistItem,
    onToggleState: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(AppShapes.md),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.title, style = AppTypography.headline.copy(fontSize = 16.sp), color = TextPrimary)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Coverage: ${item.coveredEpisodesCount}/${item.knownEpisodesCount} episodes",
                        style = AppTypography.caption,
                        color = if (item.coveredEpisodesCount >= item.knownEpisodesCount) AppSemanticColors.Success else AppSemanticColors.Warning
                    )
                }
                IconButton(onClick = onToggleState) {
                    Icon(
                        imageVector = if (item.state == WatchlistState.Watching) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Toggle Watching",
                        tint = PrimaryIndigo
                    )
                }
            }

            Spacer(Modifier.height(AppSpacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                    WatchlistChip(
                        label = item.state.name,
                        color = if (item.state == WatchlistState.Watching) AppSemanticColors.Success else TextMuted
                    )
                    WatchlistChip(
                        label = "Mode: ${item.policy.followMode.name}",
                        color = PrimaryIndigo
                    )
                }

                if (item.newReleasesCount > 0) {
                    WatchlistChip(
                        label = "${item.newReleasesCount} New Releases",
                        color = AppSemanticColors.Warning
                    )
                }
            }
        }
    }
}

@Composable
private fun WatchlistChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(label, style = AppTypography.caption.copy(fontSize = 11.sp), color = color)
    }
}
