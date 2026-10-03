package com.aniflow.feature.downloads.ui

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
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
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
import com.aniflow.feature.downloads.DownloadsViewModel
import com.aniflow.feature.downloads.model.DownloadStateUi
import com.aniflow.feature.downloads.util.ByteSizeFormatter

/**
 * DownloadStatisticsScreen.
 * Metrics and transfer statistics computed from real observed tasks.
 * Zero hardcoded numbers or fake anime summaries.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadStatisticsScreen(
    viewModel: DownloadsViewModel? = null,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState = viewModel?.uiState?.collectAsState()?.value
    val tasks = uiState?.rawTasks ?: emptyList()

    val totalDownloadedBytes = tasks.sumOf { it.downloadedBytes }
    val completedCount = tasks.count { it.state == DownloadStateUi.Completed }
    val failedCount = tasks.count { it.state == DownloadStateUi.Failed }
    val totalCount = tasks.size
    val completionPercent = if (totalCount > 0) ((completedCount.toFloat() / totalCount) * 100).toInt() else 0

    // Real failure reasons
    val failureReasons = tasks
        .filter { it.state == DownloadStateUi.Failed }
        .groupBy { it.errorMessage ?: "Unknown error" }
        .map { it.key to it.value.size }

    // Real anime breakdown
    val animeBreakdown = tasks
        .groupBy { it.animeTitle }
        .map { (anime, animeTasks) ->
            Triple(
                anime,
                ByteSizeFormatter.format(animeTasks.sumOf { it.downloadedBytes }),
                "${animeTasks.size} task${if (animeTasks.size != 1) "s" else ""}"
            )
        }
        .sortedByDescending { it.third }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Download Statistics", style = AppTypography.headline, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(AppSpacing.xl)
                ) {
                    Text("No Download Statistics", style = AppTypography.headline.copy(fontSize = 16.sp), color = TextPrimary)
                    Spacer(Modifier.height(AppSpacing.xs))
                    Text(
                        "Complete download tasks to view real metrics, volume usage, and transfer breakdown.",
                        style = AppTypography.body,
                        color = TextMuted
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(AppSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                    ) {
                        StatMetricCard(
                            modifier = Modifier.weight(1f),
                            label = "Total Downloaded",
                            value = ByteSizeFormatter.format(totalDownloadedBytes)
                        )
                        StatMetricCard(
                            modifier = Modifier.weight(1f),
                            label = "Current Speed",
                            value = uiState?.totalDownloadSpeedFormatted ?: "0 B/s"
                        )
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(AppShapes.sm),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(AppSpacing.md)) {
                            Text("Task Completion Breakdown", style = AppTypography.body.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Completed: $completedCount ($completionPercent%)", style = AppTypography.caption, color = AppSemanticColors.Success)
                                Text("Failed: $failedCount", style = AppTypography.caption, color = AppSemanticColors.Error)
                                Text("Total: $totalCount", style = AppTypography.caption, color = TextMuted)
                            }
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { if (totalCount > 0) completedCount.toFloat() / totalCount else 0f },
                                color = AppSemanticColors.Success,
                                trackColor = DarkCardBorder,
                                strokeCap = StrokeCap.Round,
                                modifier = Modifier.fillMaxWidth().height(8.dp)
                            )
                        }
                    }
                }

                if (failureReasons.isNotEmpty()) {
                    item {
                        Text("Failure Analytics", style = AppTypography.headline.copy(fontSize = 15.sp), color = TextPrimary)
                    }

                    items(failureReasons) { (reason, count) ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            shape = RoundedCornerShape(AppShapes.sm),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(AppSpacing.sm),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = AppSemanticColors.Error, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(reason, style = AppTypography.body.copy(fontSize = 12.sp), color = TextPrimary)
                                }
                                Text("$count times", style = AppTypography.caption.copy(fontWeight = FontWeight.Bold), color = TextMuted)
                            }
                        }
                    }
                }

                if (animeBreakdown.isNotEmpty()) {
                    item {
                        Text("Top Downloaded Anime", style = AppTypography.headline.copy(fontSize = 15.sp), color = TextPrimary)
                    }

                    items(animeBreakdown) { (anime, size, count) ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            shape = RoundedCornerShape(AppShapes.sm),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(AppSpacing.sm),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(anime, style = AppTypography.body.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp), color = TextPrimary)
                                Text("$size • $count", style = AppTypography.caption, color = PrimaryIndigo)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatMetricCard(modifier: Modifier = Modifier, label: String, value: String) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(AppShapes.sm),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            Text(label, style = AppTypography.caption, color = TextMuted)
            Spacer(Modifier.height(4.dp))
            Text(value, style = AppTypography.headline.copy(fontSize = 18.sp), color = TextPrimary)
        }
    }
}
