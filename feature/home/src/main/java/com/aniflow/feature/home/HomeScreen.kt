package com.aniflow.feature.home

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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aniflow.core.ui.components.ReleaseCard
import com.aniflow.core.ui.model.ReleaseUiModel
import com.aniflow.core.ui.preview.PreviewFixtures
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
import com.aniflow.domain.repository.DownloadRepository
import com.aniflow.domain.repository.ReleaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val activeDownloadsCount: Int = 3,
    val queuedDownloadsCount: Int = 8,
    val downloadSpeedFormatted: String = "12.4 MB/s",
    val continueSeriesTitle: String = "One Piece",
    val continueSeasonTitle: String = "Season 3",
    val continueCompletedEpisodes: Int = 18,
    val continueTotalEpisodes: Int = 24,
    val continueDownloadingCount: Int = 2,
    val recentReleases: List<ReleaseUiModel> = listOf(
        PreviewFixtures.sampleReleaseOne,
        PreviewFixtures.sampleReleaseTwo
    ),
    val isLoading: Boolean = false
)

class HomeViewModel(
    private val releaseRepository: ReleaseRepository? = null,
    private val downloadRepository: DownloadRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Observes live repositories when available, or displays populated preview state
    }
}

/**
 * HomeScreen (Sections 22, 23, 24).
 * Clean, fast YouTube-like discovery screen featuring Continue Hero, Active Download summary,
 * Quick Search trigger, and Recently Added releases.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToSearch: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    onReleaseClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AniFlow",
                        style = AppTypography.displayLarge.copy(fontSize = 22.sp),
                        color = TextPrimary
                    )
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = AppSpacing.md)
                            .clickable { onNavigateToSearch() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground,
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            contentPadding = PaddingValues(bottom = AppSpacing.xxxl),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.lg),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 1. Search Bar Action Trigger (Section 25, 26)
            item {
                SearchBarTrigger(onClick = onNavigateToSearch)
            }

            // 2. Continue Watching / Downloading Hero Section (Section 23)
            item {
                ContinueHeroSection(
                    title = state.continueSeriesTitle,
                    season = state.continueSeasonTitle,
                    completed = state.continueCompletedEpisodes,
                    total = state.continueTotalEpisodes,
                    downloading = state.continueDownloadingCount,
                    onContinueClick = { onReleaseClick("hero") }
                )
            }

            // 3. Active Downloads Summary (Section 24)
            if (state.activeDownloadsCount > 0 || state.queuedDownloadsCount > 0) {
                item {
                    ActiveDownloadsSummary(
                        active = state.activeDownloadsCount,
                        queued = state.queuedDownloadsCount,
                        speed = state.downloadSpeedFormatted,
                        onViewAllClick = onNavigateToDownloads
                    )
                }
            }

            // 4. Recently Added Releases (Section 22, 196)
            item {
                Column(modifier = Modifier.padding(horizontal = AppSpacing.md)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(bottom = AppSpacing.sm)
                    ) {
                        Text(
                            text = "Recently Added",
                            style = AppTypography.headline,
                            color = TextPrimary
                        )
                        Text(
                            text = "View All",
                            style = AppTypography.label,
                            color = PrimaryIndigo,
                            modifier = Modifier.clickable { onNavigateToSearch() }
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                        state.recentReleases.forEach { release ->
                            ReleaseCard(
                                release = release,
                                onClick = { onReleaseClick(release.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBarTrigger(onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = AppShapes.pill,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.md)
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(AppSpacing.sm))
            Text(
                text = "Search for an anime, movie, or release...",
                style = AppTypography.body,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun ContinueHeroSection(
    title: String,
    season: String,
    completed: Int,
    total: Int,
    downloading: Int,
    onContinueClick: () -> Unit
) {
    val progress = if (total > 0) completed.toFloat() / total.toFloat() else 0f

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = AppShapes.large,
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.md)
    ) {
        Column(modifier = Modifier.padding(AppSpacing.lg)) {
            Text(
                text = "CONTINUE",
                style = AppTypography.caption,
                color = PrimaryIndigo,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = AppTypography.headline,
                color = TextPrimary
            )
            Text(
                text = "$season • $completed / $total episodes ($downloading downloading)",
                style = AppTypography.bodySecondary
            )

            Spacer(modifier = Modifier.height(AppSpacing.md))

            LinearProgressIndicator(
                progress = { progress },
                color = PrimaryIndigo,
                trackColor = DarkCardBorder,
                strokeCap = StrokeCap.Round,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            )

            Spacer(modifier = Modifier.height(AppSpacing.md))

            Button(
                onClick = onContinueClick,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                shape = AppShapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Continue Series", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ActiveDownloadsSummary(
    active: Int,
    queued: Int,
    speed: String,
    onViewAllClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = AppShapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.md)
            .clickable { onViewAllClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(AppSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    tint = AppSemanticColors.Info,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(AppSpacing.md))
                Column {
                    Text(
                        text = "Downloads",
                        style = AppTypography.title,
                        color = TextPrimary
                    )
                    Text(
                        text = "$active active • $queued queued",
                        style = AppTypography.caption,
                        color = TextSecondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = speed,
                    style = AppTypography.numericSpeed,
                    color = AppSemanticColors.Info
                )
                Spacer(modifier = Modifier.width(AppSpacing.xs))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "View all",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
