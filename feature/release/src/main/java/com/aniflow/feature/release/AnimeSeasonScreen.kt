package com.aniflow.feature.release

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import com.aniflow.core.ui.components.AniAppBar
import com.aniflow.core.ui.components.AniEmptyState
import com.aniflow.core.ui.components.EpisodeRow
import com.aniflow.core.ui.components.QualityBadge
import com.aniflow.core.ui.model.EpisodeUiModel
import com.aniflow.core.ui.model.ReleaseUiModel
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

data class AnimeSeasonUiState(
    val animeTitle: String = "",
    val seasons: List<Int> = emptyList(),
    val currentSeason: Int = 1,
    val totalEpisodes: Int = 0,
    val availableEpisodesCount: Int = 0,
    val downloadedEpisodesCount: Int = 0,
    val episodes: List<EpisodeUiModel> = emptyList(),
    val selectedEpisodeForDetail: EpisodeUiModel? = null,
    val hasBatchTorrent: Boolean = false,
    val batchTorrentSizeFormatted: String = ""
)

class AnimeSeasonViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AnimeSeasonUiState())
    val uiState: StateFlow<AnimeSeasonUiState> = _uiState.asStateFlow()

    fun selectSeason(season: Int) {
        _uiState.value = _uiState.value.copy(currentSeason = season)
    }

    fun openEpisodeDetails(episode: EpisodeUiModel) {
        _uiState.value = _uiState.value.copy(selectedEpisodeForDetail = episode)
    }

    fun closeEpisodeDetails() {
        _uiState.value = _uiState.value.copy(selectedEpisodeForDetail = null)
    }
}

/**
 * AnimeSeasonScreen (Sections 43-51).
 * Shows Anime series header, season selector tabs, coverage status, episode list,
 * "Why selected?" explanation, and Batch comparison choices.
 * Zero hardcoded production data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeSeasonScreen(
    viewModel: AnimeSeasonViewModel = remember { AnimeSeasonViewModel() },
    onBack: () -> Unit = {},
    onPlanReviewClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            AniAppBar(
                title = if (state.animeTitle.isNotBlank()) state.animeTitle else "Season Details",
                subtitle = if (state.seasons.isNotEmpty()) "Season ${state.currentSeason}" else null,
                onBack = onBack
            )
        },
        bottomBar = {
            if (state.totalEpisodes > 0 && state.downloadedEpisodesCount < state.totalEpisodes) {
                Surface(
                    color = DarkSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppSpacing.md)
                    ) {
                        Column {
                            Text(
                                text = "${state.totalEpisodes - state.downloadedEpisodesCount} Missing Episodes",
                                style = AppTypography.title,
                                color = TextPrimary
                            )
                            Text(
                                text = "Season ${state.currentSeason}",
                                style = AppTypography.caption,
                                color = TextSecondary
                            )
                        }
                        Button(
                            onClick = onPlanReviewClick,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            shape = AppShapes.pill
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download Season", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        containerColor = DarkBackground,
        modifier = modifier
    ) { paddingValues ->
        if (state.episodes.isEmpty()) {
            AniEmptyState(
                title = "No episodes recorded",
                description = "No episodes are currently indexed for this season. Search providers or scan storage to populate episodes.",
                icon = Icons.Default.Download,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = AppSpacing.md,
                    end = AppSpacing.md,
                    bottom = AppSpacing.xxxl,
                    top = AppSpacing.sm
                ),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
            // Season Selector Tabs
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                    items(state.seasons) { season ->
                        val isSelected = state.currentSeason == season
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectSeason(season) },
                            label = { Text("Season $season") }
                        )
                    }
                }
            }

            // Coverage Bar (Section 43)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = AppShapes.medium,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(AppSpacing.md)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Season Coverage", style = AppTypography.title, color = TextPrimary)
                            Text(
                                "${state.downloadedEpisodesCount} / ${state.totalEpisodes} downloaded",
                                style = AppTypography.caption,
                                color = PrimaryIndigo
                            )
                        }
                        Spacer(modifier = Modifier.height(AppSpacing.xs))
                        LinearProgressIndicator(
                            progress = { state.downloadedEpisodesCount.toFloat() / state.totalEpisodes.toFloat() },
                            color = AppSemanticColors.Success,
                            trackColor = DarkCardBorder,
                            strokeCap = StrokeCap.Round,
                            modifier = Modifier.fillMaxWidth().height(6.dp)
                        )
                    }
                }
            }

            // Batch Option Notice (Section 50, 51)
            if (state.hasBatchTorrent) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = AppShapes.medium,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppSemanticColors.Accent.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(AppSpacing.md)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Complete Season Batch Available", style = AppTypography.title, color = TextPrimary)
                                Text("All episodes bundled into one torrent (${state.batchTorrentSizeFormatted})", style = AppTypography.caption, color = TextSecondary)
                            }
                            Button(
                                onClick = onPlanReviewClick,
                                colors = ButtonDefaults.buttonColors(containerColor = AppSemanticColors.Accent),
                                shape = AppShapes.small
                            ) {
                                Text("Use Batch")
                            }
                        }
                    }
                }
            }

            // Episode List (Section 44, 45, 46)
            items(state.episodes, key = { it.id }) { ep ->
                EpisodeRow(
                    episode = ep,
                    onClick = { viewModel.openEpisodeDetails(ep) },
                    onExplainClick = { viewModel.openEpisodeDetails(ep) },
                    onDownloadClick = onPlanReviewClick
                )
            }
        }

        // Episode Release Selection & "Why selected?" Bottom Sheet (Sections 47, 48, 49)
        state.selectedEpisodeForDetail?.let { selectedEp ->
            ModalBottomSheet(
                onDismissRequest = { viewModel.closeEpisodeDetails() },
                containerColor = DarkSurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppSpacing.lg)
                ) {
                    Text(
                        text = "Episode ${selectedEp.episodeNumber} Releases",
                        style = AppTypography.headline,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.md))

                    // Why selected explanation box (Section 48)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PrimaryIndigo.copy(alpha = 0.1f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.3f)),
                        shape = AppShapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(AppSpacing.md)) {
                            Text(
                                text = "Why this release was selected?",
                                style = AppTypography.title,
                                color = PrimaryIndigo,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(AppSpacing.xs))
                            selectedEp.whySelectedReasons.forEach { reason ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = AppSemanticColors.Success,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = reason, style = AppTypography.bodySecondary, color = TextPrimary)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(AppSpacing.lg))
                    Text(text = "Alternative Releases", style = AppTypography.title, color = TextSecondary)
                    Spacer(modifier = Modifier.height(AppSpacing.sm))

                    selectedEp.alternativeReleases.forEach { alt ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = AppSpacing.xs)
                        ) {
                            Column {
                                Text(alt.title, style = AppTypography.body, color = TextPrimary, maxLines = 1)
                                Text("${alt.resolution} • ${alt.sizeFormatted} • ${alt.uploader}", style = AppTypography.caption, color = TextMuted)
                            }
                            Button(
                                onClick = { viewModel.closeEpisodeDetails() },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkCardBorder),
                                shape = AppShapes.small
                            ) {
                                Text("Choose")
                            }
                        }
                    }
                }
            }
        }
    }
}
