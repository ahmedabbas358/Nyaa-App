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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import com.aniflow.domain.coverage.SeasonCoverage

import com.aniflow.core.ui.components.AniEmptyState
import androidx.compose.material.icons.filled.Folder

/**
 * Step 20 — Season Detail UI State (Section 35, 36, 37).
 * Zero hardcoded production data.
 */
data class SeasonDetailUiState(
    val animeId: String = "",
    val animeTitle: String = "",
    val seasonId: String = "",
    val seasonTitle: String = "",
    val seasonNumber: Int? = null,
    val coverage: SeasonCoverage? = null,
    val episodes: List<EpisodeRowUiModel> = emptyList(),
    val availableBatches: List<BatchSummaryUiModel> = emptyList()
)

data class BatchSummaryUiModel(
    val releaseId: String,
    val title: String,
    val size: String,
    val releaseGroup: String?,
    val seeders: Int,
    val coverageLabel: String
)

data class EpisodeRowUiModel(
    val episodeId: String,
    val displayNumber: String,
    val title: String?,
    val state: EpisodeAvailabilityState,
    val candidateSummary: String,
    val hasLocalFile: Boolean,
    val localFileSummary: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonDetailScreen(
    uiState: SeasonDetailUiState,
    onBackClick: () -> Unit,
    onEpisodeClick: (episodeId: String) -> Unit,
    onBatchClick: (releaseId: String) -> Unit,
    onDownloadMissingClick: (seasonId: String) -> Unit,
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
                            text = if (uiState.seasonTitle.isNotBlank()) uiState.seasonTitle else "Season Details",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (uiState.animeTitle.isNotBlank()) {
                            Text(
                                text = uiState.animeTitle,
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
                    if (uiState.seasonId.isNotBlank()) {
                        IconButton(onClick = { onDownloadMissingClick(uiState.seasonId) }) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download Missing",
                                tint = PrimaryIndigo
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        }
    ) { padding ->
        if (uiState.seasonTitle.isBlank() && uiState.episodes.isEmpty()) {
            AniEmptyState(
                title = "Season not found",
                description = "The requested season could not be found or has not been indexed yet.",
                icon = Icons.Default.Folder,
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // 1. Season Coverage Summary Card
            item {
                SeasonCoverageCard(
                    uiState = uiState,
                    onDownloadMissingClick = { onDownloadMissingClick(uiState.seasonId) }
                )
            }

            // 2. Available Batches Section (Section 43, 44)
            if (uiState.availableBatches.isNotEmpty()) {
                item {
                    Text(
                        text = "Season Batches",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(uiState.availableBatches, key = { it.releaseId }) { batch ->
                    BatchCardItem(
                        batch = batch,
                        onClick = { onBatchClick(batch.releaseId) }
                    )
                }
            }

            // 3. Episodes List Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Episodes (${uiState.episodes.size})",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 4. Episodes Rows (Section 36, 37)
            items(uiState.episodes, key = { it.episodeId }) { episode ->
                EpisodeRowItem(
                    episode = episode,
                    onClick = { onEpisodeClick(episode.episodeId) }
                )
            }
        }
    }
}
}


@Composable
private fun SeasonCoverageCard(
    uiState: SeasonDetailUiState,
    onDownloadMissingClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val cov = uiState.coverage
            val missing = cov?.missingCount
            val pct = cov?.percentage
            val statusText = when {
                cov != null && cov.expectedCount != null ->
                    "${cov.availableCount} of ${cov.expectedCount} Available (${pct?.toInt() ?: 0}%)"
                cov != null ->
                    "${cov.availableCount} Episodes Discovered"
                else -> "Loading coverage..."
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = statusText,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                if (missing != null && missing > 0) {
                    Surface(
                        color = AppSemanticColors.Warning.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "$missing Missing",
                            color = AppSemanticColors.Warning,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            if (pct != null) {
                LinearProgressIndicator(
                    progress = { pct.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = AppSemanticColors.Success,
                    trackColor = DarkBackground,
                    strokeCap = StrokeCap.Round
                )
            }

            if (missing != null && missing > 0) {
                Button(
                    onClick = onDownloadMissingClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Download Missing Episodes (${cov.missingCount})", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun BatchCardItem(
    batch: BatchSummaryUiModel,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = DarkSurface,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = PrimaryIndigo.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = "Batch",
                        tint = PrimaryIndigo,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = batch.title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = batch.size, color = TextMuted, fontSize = 12.sp)
                    Text(text = "•", color = TextMuted, fontSize = 12.sp)
                    Text(text = "${batch.seeders} seeds", color = AppSemanticColors.Success, fontSize = 12.sp)
                    if (batch.releaseGroup != null) {
                        Text(text = "•", color = TextMuted, fontSize = 12.sp)
                        Text(text = batch.releaseGroup, color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View Batch",
                tint = TextMuted
            )
        }
    }
}

@Composable
private fun EpisodeRowItem(
    episode: EpisodeRowUiModel,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = DarkSurface,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Semantic State Icon (Section 37: accessible semantic state symbol + color)
            val stateColor = when (episode.state) {
                EpisodeAvailabilityState.Available -> AppSemanticColors.Success
                EpisodeAvailabilityState.Downloading -> AppSemanticColors.Info
                EpisodeAvailabilityState.Downloaded -> AppSemanticColors.Success
                EpisodeAvailabilityState.Upgradeable -> AppSemanticColors.Info
                EpisodeAvailabilityState.ReviewRequired -> AppSemanticColors.Warning
                EpisodeAvailabilityState.NoReleaseFound,
                EpisodeAvailabilityState.NoEligibleRelease -> AppSemanticColors.Error
                else -> TextMuted
            }

            Surface(
                color = stateColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = episode.state.symbol,
                        color = stateColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Episode info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = episode.displayNumber,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (episode.title != null) {
                        Text(
                            text = "— ${episode.title}",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = episode.candidateSummary,
                        color = TextMuted,
                        fontSize = 12.sp
                    )

                    if (episode.hasLocalFile) {
                        Surface(
                            color = PrimaryIndigo.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = "Local File",
                                    tint = PrimaryIndigo,
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = "Local",
                                    color = PrimaryIndigo,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Details",
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
