package com.aniflow.feature.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.aniflow.core.ui.theme.TextPrimary
import com.aniflow.core.ui.theme.TextSecondary

/**
 * FavoritesScreen (Sections 79, 80, 81).
 * Organizes favorite Anime, preferred Uploaders, and Release Groups.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onItemClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Anime", "Uploaders", "Release Groups")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favorites & Preferences", style = AppTypography.headline, color = TextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> FavoriteAnimeList()
                1 -> PreferredUploaderList()
                2 -> PreferredGroupList()
            }
        }
    }
}

@Composable
private fun FavoriteAnimeList() {
    val items = listOf("One Piece", "Bleach", "Frieren: Beyond Journey's End")
    LazyColumn(
        contentPadding = PaddingValues(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        items(items) { title ->
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = AppShapes.medium,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(AppSpacing.md)
                ) {
                    Text(title, style = AppTypography.title, color = TextPrimary)
                    Icon(Icons.Default.Star, contentDescription = null, tint = AppSemanticColors.Warning)
                }
            }
        }
    }
}

@Composable
private fun PreferredUploaderList() {
    val uploaders = listOf("SubsPlease", "Erai-raws", "Judas")
    LazyColumn(
        contentPadding = PaddingValues(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        items(uploaders) { name ->
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = AppShapes.medium,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(AppSpacing.md)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(AppSpacing.sm))
                        Column {
                            Text(name, style = AppTypography.title, color = TextPrimary)
                            Text("Preferred in Smart Selection", style = AppTypography.caption, color = AppSemanticColors.Success)
                        }
                    }
                    Button(
                        onClick = {},
                        colors = ButtonDefaults.buttonColors(containerColor = DarkCardBorder),
                        shape = AppShapes.small
                    ) {
                        Text("Config", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PreferredGroupList() {
    val groups = listOf("ASW", "Yameii", "Kaleido")
    LazyColumn(
        contentPadding = PaddingValues(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        items(groups) { name ->
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = AppShapes.medium,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(AppSpacing.md)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Group, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(AppSpacing.sm))
                        Text(name, style = AppTypography.title, color = TextPrimary)
                    }
                    Button(
                        onClick = {},
                        colors = ButtonDefaults.buttonColors(containerColor = DarkCardBorder),
                        shape = AppShapes.small
                    ) {
                        Text("Config", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
