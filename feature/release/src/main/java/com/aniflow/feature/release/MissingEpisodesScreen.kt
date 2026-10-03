package com.aniflow.feature.release

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aniflow.core.ui.theme.AppSemanticColors
import com.aniflow.core.ui.theme.DarkBackground
import com.aniflow.core.ui.theme.DarkCardBorder
import com.aniflow.core.ui.theme.DarkSurface
import com.aniflow.core.ui.theme.PrimaryIndigo
import com.aniflow.core.ui.theme.TextMuted
import com.aniflow.core.ui.theme.TextPrimary
import com.aniflow.core.ui.theme.TextSecondary
import com.aniflow.domain.coverage.EpisodeAvailabilityState

import com.aniflow.core.ui.components.AniEmptyState
import androidx.compose.material.icons.filled.CheckCircle

/**
 * Step 20 — Missing Episodes Screen (Section 47, 48, 51).
 * Zero hardcoded production data.
 */
data class MissingEpisodeUiModel(
    val episodeId: String,
    val seasonNumber: Int?,
    val episodeNumber: String,
    val title: String?,
    val state: EpisodeAvailabilityState,
    val candidateCount: Int,
    val lastSearchedSummary: String? = null,
    val isStale: Boolean = false
)

data class MissingEpisodesUiState(
    val animeId: String = "",
    val animeTitle: String = "",
    val isSearchingBatch: Boolean = false,
    val currentSearchingLabel: String? = null,
    val missingEpisodes: List<MissingEpisodeUiModel> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissingEpisodesScreen(
    uiState: MissingEpisodesUiState,
    onBackClick: () -> Unit,
    onSearchAllMissingClick: (animeId: String) -> Unit,
    onSearchSingleMissingClick: (episodeId: String) -> Unit,
    onEpisodeClick: (episodeId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Missing Episodes",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (uiState.animeTitle.isNotBlank()) {
                            Text(
                                text = "${uiState.animeTitle} • ${uiState.missingEpisodes.size} missing",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    if (uiState.missingEpisodes.isNotEmpty()) {
                        IconButton(onClick = { onSearchAllMissingClick(uiState.animeId) }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Find All Missing",
                                tint = PrimaryIndigo
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        bottomBar = {
            if (uiState.missingEpisodes.isNotEmpty()) {
                Surface(
                    color = DarkSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${uiState.missingEpisodes.size} episodes need releases",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )

                        Button(
                            onClick = { onSearchAllMissingClick(uiState.animeId) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Search All Missing", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (uiState.missingEpisodes.isEmpty()) {
            AniEmptyState(
                title = "No missing episodes",
                description = "All episodes for this series are available and indexed in your library.",
                icon = Icons.Default.CheckCircle,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            // Searching Progress Notice (Section 49)
            if (uiState.isSearchingBatch && uiState.currentSearchingLabel != null) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = PrimaryIndigo.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = PrimaryIndigo)
                            Text(
                                text = "Searching: ${uiState.currentSearchingLabel}",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Missing Episodes list (Section 47, 48, 51)
            items(uiState.missingEpisodes, key = { it.episodeId }) { item ->
                MissingEpisodeCardItem(
                    item = item,
                    onSearchClick = { onSearchSingleMissingClick(item.episodeId) },
                    onEpisodeClick = { onEpisodeClick(item.episodeId) }
                )
            }
        }
    }
}

@Composable
private fun MissingEpisodeCardItem(
    item: MissingEpisodeUiModel,
    onSearchClick: () -> Unit,
    onEpisodeClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Semantic state symbol badge
            val badgeColor = when {
                item.isStale -> AppSemanticColors.Warning
                item.state == EpisodeAvailabilityState.NoReleaseFound -> AppSemanticColors.Error
                item.state == EpisodeAvailabilityState.NoEligibleRelease -> AppSemanticColors.Warning
                else -> TextMuted
            }

            Surface(
                color = badgeColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = item.state.symbol,
                        color = badgeColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Season ${item.seasonNumber ?: "?"} • Episode ${item.episodeNumber}",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(2.dp))

                val statusLabel = when {
                    item.isStale -> "Stale: Searched >30 days ago"
                    item.state == EpisodeAvailabilityState.NoEligibleRelease -> "Releases found (${item.candidateCount}) but ineligible by policy"
                    item.state == EpisodeAvailabilityState.NoReleaseFound -> "No releases found on provider"
                    item.state == EpisodeAvailabilityState.NotSearched -> "Not searched yet"
                    else -> item.state.name
                }

                Text(
                    text = statusLabel,
                    color = badgeColor,
                    fontSize = 12.sp
                )

                if (item.lastSearchedSummary != null) {
                    Text(
                        text = "Last search: ${item.lastSearchedSummary}",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            OutlinedButton(
                onClick = onSearchClick,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryIndigo)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Search", fontSize = 11.sp, color = PrimaryIndigo)
            }
        }
    }
}
