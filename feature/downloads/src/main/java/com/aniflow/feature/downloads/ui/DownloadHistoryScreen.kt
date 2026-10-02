package com.aniflow.feature.downloads.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.aniflow.feature.downloads.model.DownloadHistoryItemUiModel

/**
 * DownloadHistoryScreen (Sections 60, 61, 62, 63).
 * Paginated history logs, retry through DownloadPlanner, and safe cleanup without deleting physical files.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadHistoryScreen(
    onBack: () -> Unit = {},
    onRetryTask: (String) -> Unit = {},
    onOpenLocation: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("All") }
    val filterTabs = listOf("All", "Completed", "Failed")
    var showCleanupDialog by remember { mutableStateOf(false) }
    var deletePhysicalFilesToo by remember { mutableStateOf(false) }

    val historyItems = remember {
        mutableStateListOf(
            DownloadHistoryItemUiModel(
                id = "hist_1",
                taskId = "task_1001",
                title = "One Piece — Episode 1109",
                animeTitle = "One Piece",
                episodeNumber = 1109.0,
                sizeFormatted = "1.4 GB",
                durationFormatted = "1m 15s",
                completedAtFormatted = "Today at 09:20",
                finalLocation = "Anime/One Piece/Season 01/Episode 1109.mkv",
                statusText = "Completed",
                isSuccessful = true
            ),
            DownloadHistoryItemUiModel(
                id = "hist_2",
                taskId = "task_1002",
                title = "Bleach TYBW Part 2 — Episode 26",
                animeTitle = "Bleach",
                episodeNumber = 26.0,
                sizeFormatted = "2.1 GB",
                durationFormatted = "1m 45s",
                completedAtFormatted = "Yesterday at 22:40",
                finalLocation = "Anime/Bleach/Season 02/Episode 26.mkv",
                statusText = "Completed",
                isSuccessful = true
            ),
            DownloadHistoryItemUiModel(
                id = "hist_3",
                taskId = "task_1003",
                title = "Jujutsu Kaisen — Episode 24 (720p)",
                animeTitle = "Jujutsu Kaisen",
                episodeNumber = 24.0,
                sizeFormatted = "750 MB",
                durationFormatted = "0s",
                completedAtFormatted = "2 days ago",
                finalLocation = "",
                statusText = "Failed: Storage full",
                isSuccessful = false
            )
        )
    }

    val filteredList = when (selectedFilter) {
        "Completed" -> historyItems.filter { it.isSuccessful }
        "Failed" -> historyItems.filter { !it.isSuccessful }
        else -> historyItems
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
                    IconButton(onClick = { showCleanupDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear History", tint = TextMuted)
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
            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                filterTabs.forEach { tab ->
                    FilterChip(
                        selected = selectedFilter == tab,
                        onClick = { selectedFilter = tab },
                        label = { Text(tab) }
                    )
                }
            }

            // History List
            LazyColumn(
                contentPadding = PaddingValues(AppSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredList, key = { it.id }) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(AppShapes.sm),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(AppSpacing.md)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = if (item.isSuccessful) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = if (item.isSuccessful) AppSemanticColors.Success else AppSemanticColors.Error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(item.title, style = AppTypography.body.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp), color = TextPrimary, maxLines = 1)
                                        Text("${item.sizeFormatted} • ${item.durationFormatted} • ${item.completedAtFormatted}", style = AppTypography.caption, color = TextMuted)
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (item.isSuccessful) {
                                        IconButton(onClick = { onOpenLocation(item.finalLocation) }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Default.Folder, contentDescription = "Folder", tint = TextMuted, modifier = Modifier.size(16.dp))
                                        }
                                    } else {
                                        IconButton(onClick = { onRetryTask(item.taskId) }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = PrimaryIndigo, modifier = Modifier.size(16.dp))
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

    // History Cleanup Confirmation Dialog (Section 35, 36, 63)
    if (showCleanupDialog) {
        AlertDialog(
            onDismissRequest = { showCleanupDialog = false },
            title = { Text("Clear Download History") },
            text = {
                Column {
                    Text("Are you sure you want to remove records from download history?", style = AppTypography.body, color = TextSecondary)
                    Spacer(Modifier.height(AppSpacing.md))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = deletePhysicalFilesToo,
                            onCheckedChange = { deletePhysicalFilesToo = it }
                        )
                        Text(
                            text = "Also delete downloaded files from storage",
                            style = AppTypography.caption,
                            color = if (deletePhysicalFilesToo) AppSemanticColors.Error else TextMuted
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        historyItems.clear()
                        showCleanupDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppSemanticColors.Error)
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCleanupDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = DarkSurface
        )
    }
}
