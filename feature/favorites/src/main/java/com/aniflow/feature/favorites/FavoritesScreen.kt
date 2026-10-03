package com.aniflow.feature.favorites

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import com.aniflow.core.ui.components.AniAppBar
import com.aniflow.core.ui.components.AniEmptyState
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FavoriteAnimeItem(
    val id: String,
    val title: String,
    val totalEpisodes: Int = 0,
    val downloadedEpisodes: Int = 0
)

data class PreferredUploaderItem(
    val id: String,
    val name: String,
    val scoreBonus: Int = 10
)

data class PreferredGroupItem(
    val id: String,
    val name: String,
    val scoreBonus: Int = 15
)

data class FavoritesUiState(
    val favoriteAnime: List<FavoriteAnimeItem> = emptyList(),
    val preferredUploaders: List<PreferredUploaderItem> = emptyList(),
    val preferredGroups: List<PreferredGroupItem> = emptyList(),
    val isLoading: Boolean = false
)

class FavoritesViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    fun removeFavoriteAnime(id: String) {
        _uiState.value = _uiState.value.copy(
            favoriteAnime = _uiState.value.favoriteAnime.filterNot { it.id == id }
        )
    }

    fun removePreferredUploader(id: String) {
        _uiState.value = _uiState.value.copy(
            preferredUploaders = _uiState.value.preferredUploaders.filterNot { it.id == id }
        )
    }

    fun removePreferredGroup(id: String) {
        _uiState.value = _uiState.value.copy(
            preferredGroups = _uiState.value.preferredGroups.filterNot { it.id == id }
        )
    }
}

/**
 * FavoritesScreen (Sections 79, 80, 81).
 * Organizes favorite Anime, preferred Uploaders, and Release Groups.
 * Zero fake/hardcoded production data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel = remember { FavoritesViewModel() },
    onItemClick: (String) -> Unit = {},
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "Anime (${state.favoriteAnime.size})",
        "Uploaders (${state.preferredUploaders.size})",
        "Release Groups (${state.preferredGroups.size})"
    )

    Scaffold(
        topBar = {
            AniAppBar(
                title = "Favorites & Preferences",
                subtitle = "Pinned series and release preferences",
                onBack = onBack
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

            when (selectedTab) {
                0 -> {
                    if (state.favoriteAnime.isEmpty()) {
                        AniEmptyState(
                            title = "No favorite anime",
                            description = "Star an anime from Anime Details to keep track of new releases and quick access.",
                            icon = Icons.Default.StarBorder,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(AppSpacing.md),
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                        ) {
                            items(state.favoriteAnime, key = { it.id }) { anime ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                    shape = AppShapes.medium,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onItemClick(anime.id) }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.padding(AppSpacing.md)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(anime.title, style = AppTypography.title, color = TextPrimary)
                                            if (anime.totalEpisodes > 0) {
                                                Text(
                                                    "${anime.downloadedEpisodes}/${anime.totalEpisodes} episodes downloaded",
                                                    style = AppTypography.caption,
                                                    color = TextSecondary
                                                )
                                            }
                                        }
                                        IconButton(onClick = { viewModel.removeFavoriteAnime(anime.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = TextMuted)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    if (state.preferredUploaders.isEmpty()) {
                        AniEmptyState(
                            title = "No preferred uploaders",
                            description = "Configure preferred uploaders in Selection Profiles to bias smart matching.",
                            icon = Icons.Default.Person,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(AppSpacing.md),
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                        ) {
                            items(state.preferredUploaders, key = { it.id }) { uploader ->
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
                                                Text(uploader.name, style = AppTypography.title, color = TextPrimary)
                                                Text("Selection bonus +${uploader.scoreBonus}", style = AppTypography.caption, color = AppSemanticColors.Success)
                                            }
                                        }
                                        IconButton(onClick = { viewModel.removePreferredUploader(uploader.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = TextMuted)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    if (state.preferredGroups.isEmpty()) {
                        AniEmptyState(
                            title = "No preferred release groups",
                            description = "Pin favorite release groups to prioritize their releases during selection.",
                            icon = Icons.Default.Group,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(AppSpacing.md),
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                        ) {
                            items(state.preferredGroups, key = { it.id }) { group ->
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
                                            Column {
                                                Text(group.name, style = AppTypography.title, color = TextPrimary)
                                                Text("Selection bonus +${group.scoreBonus}", style = AppTypography.caption, color = AppSemanticColors.Success)
                                            }
                                        }
                                        IconButton(onClick = { viewModel.removePreferredGroup(group.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = TextMuted)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

