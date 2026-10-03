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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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

/**
 * DownloadHistoryScreen.
 * Displays completed and failed historical tasks from real observed tasks.
 * Zero hardcoded fake history items.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadHistoryScreen(
    viewModel: DownloadsViewModel? = null,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState = viewModel?.uiState?.collectAsState()?.value
    val allTasks = uiState?.rawTasks ?: emptyList()
    val terminalTasks = allTasks.filter { it.state == DownloadStateUi.Completed || it.state == DownloadStateUi.Failed }

    var selectedFilter by remember { mutableStateOf("All") }
    val filterTabs = listOf("All", "Completed", "Failed")

    val displayedTasks = when (selectedFilter) {
        "Completed" -> terminalTasks.filter { it.state == DownloadStateUi.Completed }
        "Failed" -> terminalTasks.filter { it.state == DownloadStateUi.Failed }
        else -> terminalTasks
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Download History", style = AppTypography.headline, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    if (terminalTasks.isNotEmpty()) {
                        IconButton(onClick = {
                            terminalTasks.forEach { task -> viewModel?.cancelTask(task.id) }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear History", tint = TextMuted)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filter Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                filterTabs.forEach { tab ->
                    val isSelected = selectedFilter == tab
                    val count = when (tab) {
                        "Completed" -> terminalTasks.count { it.state == DownloadStateUi.Completed }
                        "Failed" -> terminalTasks.count { it.state == DownloadStateUi.Failed }
                        else -> terminalTasks.size
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = tab },
                        label = { Text("$tab ($count)", style = AppTypography.caption) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryIndigo.copy(alpha = 0.2f),
                            selectedLabelColor = PrimaryIndigo,
                            containerColor = DarkSurface,
                            labelColor = TextMuted
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (isSelected) PrimaryIndigo else DarkCardBorder,
                            enabled = true,
                            selected = isSelected
                        )
                    )
                }
            }

            if (displayedTasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(AppSpacing.xl)
                    ) {
                        Text("No Download History", style = AppTypography.headline.copy(fontSize = 16.sp), color = TextPrimary)
                        Spacer(Modifier.height(AppSpacing.xs))
                        Text(
                            "Finished and failed download tasks will appear here.",
                            style = AppTypography.body,
                            color = TextMuted
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(AppSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(displayedTasks, key = { it.id.value }) { task ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            shape = RoundedCornerShape(AppShapes.sm),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(AppSpacing.sm),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    val isSuccess = task.state == DownloadStateUi.Completed
                                    Icon(
                                        imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = if (isSuccess) AppSemanticColors.Success else AppSemanticColors.Error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.padding(horizontal = 4.dp))
                                    Column {
                                        Text(
                                            text = task.title,
                                            style = AppTypography.body.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                            color = TextPrimary,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "${task.downloadedBytesFormatted} • ${task.destinationPath}",
                                            style = AppTypography.caption,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (task.state == DownloadStateUi.Failed) {
                                        IconButton(onClick = { viewModel?.resumeTask(task.id) }) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = PrimaryIndigo)
                                        }
                                    }
                                    IconButton(onClick = { viewModel?.cancelTask(task.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted)
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
