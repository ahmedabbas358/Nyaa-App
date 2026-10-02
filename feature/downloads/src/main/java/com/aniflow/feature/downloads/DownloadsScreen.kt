package com.aniflow.feature.downloads

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.aniflow.core.ui.theme.DarkSurfaceVariant
import com.aniflow.core.ui.theme.PrimaryIndigo
import com.aniflow.core.ui.theme.TextMuted
import com.aniflow.core.ui.theme.TextPrimary
import com.aniflow.core.ui.theme.TextSecondary
import com.aniflow.domain.identity.DownloadTaskId
import com.aniflow.feature.downloads.aggregator.DownloadDashboardAggregator
import com.aniflow.feature.downloads.model.DownloadEngineBadge
import com.aniflow.feature.downloads.model.DownloadFilterCategory
import com.aniflow.feature.downloads.model.DownloadGroupUiModel
import com.aniflow.feature.downloads.model.DownloadGroupingMode
import com.aniflow.feature.downloads.model.DownloadPriorityUi
import com.aniflow.feature.downloads.model.DownloadSortOption
import com.aniflow.feature.downloads.model.DownloadStateUi
import com.aniflow.feature.downloads.model.DownloadTaskUiModel
import com.aniflow.feature.downloads.model.WaitingReasonUi
import com.aniflow.feature.downloads.ui.DownloadDetailsScreen
import com.aniflow.feature.downloads.ui.DownloadTaskCard
import com.aniflow.feature.downloads.util.ByteSizeFormatter

/**
 * DownloadsScreen (Sections 2-12, 28, 30, 32, 33, 37, 39, 40, 79, 99, 100, 101).
 * Professional, high-density Download Manager Dashboard (FDM/1DM style) with:
 * - Real-time aggregate speed and overall byte-based progress
 * - Granular filters, search, and sorting
 * - Multi-selection bulk operations
 * - Grouping by Anime or Plan
 * - Master-Detail layout for Tablets / Landscape displays
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onOpenTaskDetails: (String) -> Unit = {},
    onOpenStatistics: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // In-memory tasks state (synced with DownloadOrchestrator in production)
    val tasks = remember {
        mutableStateListOf(
            DownloadTaskUiModel(
                id = DownloadTaskId("task_1"),
                title = "One Piece — Episode 1110 (1080p HEVC)",
                animeTitle = "One Piece",
                episodeNumber = 1110.0,
                state = DownloadStateUi.Downloading,
                engineBadge = DownloadEngineBadge.HTTP,
                priority = DownloadPriorityUi.High,
                downloadedBytes = 1_700_000_000L,
                totalBytes = 2_100_000_000L,
                progressPercent = 81,
                speedBytesPerSec = 18_400_000L,
                speedFormatted = "18.4 MB/s",
                etaFormatted = "22s",
                etaSeconds = 22L,
                destinationPath = "Anime/One Piece/Season 01",
                whyCreatedReason = "Automation: One Piece Watch (1080p HEVC preferred)"
            ),
            DownloadTaskUiModel(
                id = DownloadTaskId("task_2"),
                title = "Jujutsu Kaisen Season 2 — Episode 23",
                animeTitle = "Jujutsu Kaisen",
                episodeNumber = 23.0,
                state = DownloadStateUi.Downloading,
                engineBadge = DownloadEngineBadge.Torrent,
                priority = DownloadPriorityUi.Normal,
                downloadedBytes = 850_000_000L,
                totalBytes = 1_400_000_000L,
                progressPercent = 60,
                speedBytesPerSec = 6_200_000L,
                speedFormatted = "6.2 MB/s",
                etaFormatted = "1m 28s",
                etaSeconds = 88L,
                destinationPath = "Anime/Jujutsu Kaisen/Season 02"
            ),
            DownloadTaskUiModel(
                id = DownloadTaskId("task_3"),
                title = "Bleach: TYBW — Episode 26",
                animeTitle = "Bleach",
                episodeNumber = 26.0,
                state = DownloadStateUi.Queued,
                engineBadge = DownloadEngineBadge.HTTP,
                priority = DownloadPriorityUi.Normal,
                queuePosition = 1,
                downloadedBytes = 0L,
                totalBytes = 2_100_000_000L,
                destinationPath = "Anime/Bleach/Season 02"
            ),
            DownloadTaskUiModel(
                id = DownloadTaskId("task_4"),
                title = "Frieren — Episode 28 (1080p)",
                animeTitle = "Frieren",
                episodeNumber = 28.0,
                state = DownloadStateUi.Waiting,
                waitingReason = WaitingReasonUi.WaitingForSlot,
                engineBadge = DownloadEngineBadge.HTTP,
                priority = DownloadPriorityUi.Low,
                downloadedBytes = 0L,
                totalBytes = 1_500_000_000L,
                destinationPath = "Anime/Frieren/Season 01"
            ),
            DownloadTaskUiModel(
                id = DownloadTaskId("task_5"),
                title = "Demon Slayer — S04E08",
                animeTitle = "Demon Slayer",
                episodeNumber = 8.0,
                state = DownloadStateUi.Completed,
                engineBadge = DownloadEngineBadge.HTTP,
                downloadedBytes = 1_800_000_000L,
                totalBytes = 1_800_000_000L,
                progressPercent = 100,
                destinationPath = "Anime/Demon Slayer/Season 04"
            )
        )
    }

    var selectedFilter by remember { mutableStateOf(DownloadFilterCategory.All) }
    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(DownloadSortOption.CreatedDate) }
    var groupingMode by remember { mutableStateOf(DownloadGroupingMode.Flat) }
    var isSearchVisible by remember { mutableStateOf(false) }
    var selectedTaskForDetail by remember { mutableStateOf<DownloadTaskUiModel?>(null) }
    var isSpeedMenuExpanded by remember { mutableStateOf(false) }

    // Summary calculations via isolated Aggregator (Section 4, 107)
    val summary = remember(tasks.toList()) {
        DownloadDashboardAggregator.computeSummary(tasks)
    }

    // Filtered, searched, and sorted list
    val filteredTasks = remember(tasks.toList(), selectedFilter, searchQuery, sortOption) {
        DownloadDashboardAggregator.filterAndSort(tasks, selectedFilter, searchQuery, sortOption)
    }

    // Grouped list
    val groupedList = remember(filteredTasks, groupingMode) {
        DownloadDashboardAggregator.groupTasks(filteredTasks, groupingMode)
    }

    val selectedBulkCount = tasks.count { it.isSelectedForBulk }
    val isSelectionMode = selectedBulkCount > 0

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTabletLandscape = maxWidth > 840.dp

        Row(modifier = Modifier.fillMaxSize()) {
            // Main Task List (Master)
            Scaffold(
                modifier = Modifier
                    .weight(if (isTabletLandscape) 1.2f else 1f)
                    .fillMaxHeight(),
                topBar = {
                    TopAppBar(
                        title = {
                            if (isSearchVisible) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Search downloads…", style = AppTypography.caption) },
                                    singleLine = true,
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            searchQuery = ""
                                            isSearchVisible = false
                                        }) {
                                            Icon(Icons.Default.Close, contentDescription = "Close search", tint = TextMuted)
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PrimaryIndigo,
                                        unfocusedBorderColor = DarkCardBorder
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Text("Download Manager", style = AppTypography.headline, color = TextPrimary)
                            }
                        },
                        actions = {
                            if (!isSearchVisible) {
                                IconButton(onClick = { isSearchVisible = true }) {
                                    Icon(Icons.Default.Search, contentDescription = "Search", tint = TextPrimary)
                                }
                            }

                            // Speed Limiter Shortcut (Section 26, 117)
                            Box {
                                IconButton(onClick = { isSpeedMenuExpanded = true }) {
                                    Icon(Icons.Default.Speed, contentDescription = "Speed Presets", tint = PrimaryIndigo)
                                }
                                DropdownMenu(
                                    expanded = isSpeedMenuExpanded,
                                    onDismissRequest = { isSpeedMenuExpanded = false },
                                    modifier = Modifier.background(DarkSurface)
                                ) {
                                    DropdownMenuItem(text = { Text("Unlimited Speed") }, onClick = { isSpeedMenuExpanded = false })
                                    DropdownMenuItem(text = { Text("Limit to 10 MB/s") }, onClick = { isSpeedMenuExpanded = false })
                                    DropdownMenuItem(text = { Text("Limit to 25 MB/s") }, onClick = { isSpeedMenuExpanded = false })
                                    DropdownMenuItem(text = { Text("Night Schedule Active") }, onClick = { isSpeedMenuExpanded = false })
                                }
                            }

                            // Pause All (Section 32)
                            IconButton(onClick = {
                                tasks.indices.forEach { idx ->
                                    if (tasks[idx].state.isActive) {
                                        tasks[idx] = tasks[idx].copy(state = DownloadStateUi.Paused)
                                    }
                                }
                            }) {
                                Icon(Icons.Default.Pause, contentDescription = "Pause All", tint = TextSecondary)
                            }

                            // Resume All (Section 33)
                            IconButton(onClick = {
                                tasks.indices.forEach { idx ->
                                    if (tasks[idx].state == DownloadStateUi.Paused) {
                                        tasks[idx] = tasks[idx].copy(state = DownloadStateUi.Downloading)
                                    }
                                }
                            }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Resume All", tint = PrimaryIndigo)
                            }

                            IconButton(onClick = onOpenStatistics) {
                                Icon(Icons.Default.BarChart, contentDescription = "Statistics", tint = TextPrimary)
                            }

                            IconButton(onClick = onOpenHistory) {
                                Icon(Icons.Default.History, contentDescription = "History", tint = TextPrimary)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
                    )
                },
                containerColor = DarkBackground
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    // Top Summary Card: Speed, Status counts & overall byte progress (Section 3, 4)
                    SummaryDashboardCard(summary = summary)

                    // Multi-Select Action Bar (Section 79, 80, 81)
                    AnimatedVisibility(visible = isSelectionMode, enter = fadeIn(), exit = fadeOut()) {
                        BulkActionsBar(
                            selectedCount = selectedBulkCount,
                            onPauseSelected = {
                                tasks.indices.forEach { idx ->
                                    if (tasks[idx].isSelectedForBulk && tasks[idx].state.isActive) {
                                        tasks[idx] = tasks[idx].copy(state = DownloadStateUi.Paused)
                                    }
                                }
                            },
                            onResumeSelected = {
                                tasks.indices.forEach { idx ->
                                    if (tasks[idx].isSelectedForBulk && tasks[idx].state == DownloadStateUi.Paused) {
                                        tasks[idx] = tasks[idx].copy(state = DownloadStateUi.Downloading)
                                    }
                                }
                            },
                            onCancelSelected = {
                                tasks.removeAll { it.isSelectedForBulk }
                            },
                            onClearSelection = {
                                tasks.indices.forEach { idx ->
                                    tasks[idx] = tasks[idx].copy(isSelectedForBulk = false)
                                }
                            }
                        )
                    }

                    // Status Filters Row (Section 5)
                    StatusFiltersRow(
                        selectedCategory = selectedFilter,
                        summary = summary,
                        onSelectCategory = { selectedFilter = it }
                    )

                    // Grouping Mode & Sort Row (Section 39, 40)
                    GroupingAndSortRow(
                        groupingMode = groupingMode,
                        onGroupingModeChange = { groupingMode = it },
                        sortOption = sortOption,
                        onSortChange = { sortOption = it }
                    )

                    // Task List / Groups
                    if (filteredTasks.isEmpty()) {
                        EmptyDownloadsView(filter = selectedFilter)
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(AppSpacing.md),
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            groupedList.forEach { group ->
                                if (groupingMode != DownloadGroupingMode.Flat) {
                                    item(key = group.groupKey) {
                                        GroupHeaderCard(group = group)
                                    }
                                }

                                items(group.tasks, key = { it.id.value }) { task ->
                                    DownloadTaskCard(
                                        task = task,
                                        isSelectionMode = isSelectionMode,
                                        onClick = {
                                            if (isTabletLandscape) {
                                                selectedTaskForDetail = task
                                            } else {
                                                onOpenTaskDetails(task.id.value)
                                            }
                                        },
                                        onToggleSelect = { selected ->
                                            val idx = tasks.indexOfFirst { it.id == task.id }
                                            if (idx != -1) {
                                                tasks[idx] = tasks[idx].copy(isSelectedForBulk = selected)
                                            }
                                        },
                                        onPause = {
                                            val idx = tasks.indexOfFirst { it.id == task.id }
                                            if (idx != -1) {
                                                tasks[idx] = tasks[idx].copy(state = DownloadStateUi.Paused)
                                            }
                                        },
                                        onResume = {
                                            val idx = tasks.indexOfFirst { it.id == task.id }
                                            if (idx != -1) {
                                                tasks[idx] = tasks[idx].copy(state = DownloadStateUi.Downloading)
                                            }
                                        },
                                        onCancel = {
                                            tasks.removeAll { it.id == task.id }
                                        },
                                        onRetry = {
                                            val idx = tasks.indexOfFirst { it.id == task.id }
                                            if (idx != -1) {
                                                tasks[idx] = tasks[idx].copy(state = DownloadStateUi.Downloading)
                                            }
                                        },
                                        onMoveUp = {
                                            val idx = tasks.indexOfFirst { it.id == task.id }
                                            if (idx > 0) {
                                                val item = tasks.removeAt(idx)
                                                tasks.add(idx - 1, item)
                                            }
                                        },
                                        onMoveDown = {
                                            val idx = tasks.indexOfFirst { it.id == task.id }
                                            if (idx != -1 && idx < tasks.lastIndex) {
                                                val item = tasks.removeAt(idx)
                                                tasks.add(idx + 1, item)
                                            }
                                        },
                                        onRemoveHistory = {
                                            tasks.removeAll { it.id == task.id }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Tablet Master-Detail Pane (Section 99, 100, 101)
            if (isTabletLandscape) {
                Box(
                    modifier = Modifier
                        .weight(1.8f)
                        .fillMaxHeight()
                        .background(DarkBackground)
                        .border(1.dp, DarkCardBorder)
                ) {
                    val detailTask = selectedTaskForDetail ?: filteredTasks.firstOrNull()
                    if (detailTask != null) {
                        DownloadDetailsScreen(task = detailTask)
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Select a task to monitor details", style = AppTypography.body, color = TextMuted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryDashboardCard(summary: com.aniflow.feature.downloads.aggregator.DashboardAggregateSummary) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(AppShapes.sm),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = summary.totalActiveSpeedFormatted,
                        style = AppTypography.headline.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                        color = AppSemanticColors.Info
                    )
                    Text(
                        text = "${summary.activeCount} active • ${summary.queuedCount} queued • ${summary.pausedCount} paused",
                        style = AppTypography.caption,
                        color = TextSecondary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${summary.overallProgressPercent}% Complete",
                        style = AppTypography.body.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "${ByteSizeFormatter.format(summary.overallDownloadedBytes)} / ${ByteSizeFormatter.format(summary.overallTotalBytes)}",
                        style = AppTypography.caption,
                        color = TextMuted
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Overall Linear Progress Indicator (Section 4)
            LinearProgressIndicator(
                progress = { (summary.overallProgressPercent.toFloat() / 100f).coerceIn(0f, 1f) },
                color = PrimaryIndigo,
                trackColor = DarkCardBorder,
                strokeCap = StrokeCap.Round,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            )
        }
    }
}

@Composable
private fun BulkActionsBar(
    selectedCount: Int,
    onPauseSelected: () -> Unit,
    onResumeSelected: () -> Unit,
    onCancelSelected: () -> Unit,
    onClearSelection: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.md, vertical = 2.dp),
        color = DarkSurfaceVariant,
        shape = RoundedCornerShape(AppShapes.sm),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryIndigo)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("$selectedCount selected", style = AppTypography.body.copy(fontWeight = FontWeight.Bold), color = PrimaryIndigo)

            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                IconButton(onClick = onPauseSelected, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Pause, contentDescription = "Pause", tint = TextPrimary)
                }
                IconButton(onClick = onResumeSelected, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = PrimaryIndigo)
                }
                IconButton(onClick = onCancelSelected, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Cancel", tint = AppSemanticColors.Error)
                }
                IconButton(onClick = onClearSelection, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }
        }
    }
}

@Composable
private fun StatusFiltersRow(
    selectedCategory: DownloadFilterCategory,
    summary: com.aniflow.feature.downloads.aggregator.DashboardAggregateSummary,
    onSelectCategory: (DownloadFilterCategory) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = AppSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        items(DownloadFilterCategory.values()) { category ->
            val count = when (category) {
                DownloadFilterCategory.All -> summary.totalCount
                DownloadFilterCategory.Active -> summary.activeCount
                DownloadFilterCategory.Queued -> summary.queuedCount
                DownloadFilterCategory.Paused -> summary.pausedCount
                DownloadFilterCategory.Waiting -> summary.waitingCount
                DownloadFilterCategory.Failed -> summary.failedCount
                DownloadFilterCategory.Completed -> summary.completedCount
            }
            val isSelected = selectedCategory == category
            FilterChip(
                selected = isSelected,
                onClick = { onSelectCategory(category) },
                label = { Text("${category.displayName} ($count)", style = AppTypography.caption) },
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
}

@Composable
private fun GroupingAndSortRow(
    groupingMode: DownloadGroupingMode,
    onGroupingModeChange: (DownloadGroupingMode) -> Unit,
    sortOption: DownloadSortOption,
    onSortChange: (DownloadSortOption) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.md, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            DownloadGroupingMode.values().forEach { mode ->
                Text(
                    text = mode.displayName,
                    style = AppTypography.caption.copy(fontSize = 11.sp),
                    color = if (groupingMode == mode) PrimaryIndigo else TextMuted,
                    modifier = Modifier
                        .clickable { onGroupingModeChange(mode) }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }

        Text(
            text = "Sort: ${sortOption.displayName}",
            style = AppTypography.caption.copy(fontSize = 11.sp),
            color = TextMuted,
            modifier = Modifier.clickable {
                // Cycle sort
                val next = when (sortOption) {
                    DownloadSortOption.CreatedDate -> DownloadSortOption.Priority
                    DownloadSortOption.Priority -> DownloadSortOption.Progress
                    DownloadSortOption.Progress -> DownloadSortOption.Speed
                    DownloadSortOption.Speed -> DownloadSortOption.Size
                    DownloadSortOption.Size -> DownloadSortOption.ETA
                    DownloadSortOption.ETA -> DownloadSortOption.Name
                    DownloadSortOption.Name -> DownloadSortOption.CreatedDate
                }
                onSortChange(next)
            }
        )
    }
}

@Composable
private fun GroupHeaderCard(group: DownloadGroupUiModel) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        shape = RoundedCornerShape(AppShapes.sm),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(group.groupTitle, style = AppTypography.body.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp), color = TextPrimary)
                Text(
                    text = "${group.completedCount}/${group.totalCount} completed • ${group.progressPercent}% • ${ByteSizeFormatter.format(group.downloadedBytes)}",
                    style = AppTypography.caption,
                    color = TextMuted
                )
            }
            Text(group.totalSpeedFormatted, style = AppTypography.caption.copy(fontWeight = FontWeight.Bold), color = AppSemanticColors.Info)
        }
    }
}

@Composable
private fun EmptyDownloadsView(filter: DownloadFilterCategory) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppSpacing.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val message = when (filter) {
            DownloadFilterCategory.Active -> "No active downloads currently running."
            DownloadFilterCategory.Queued -> "Queue is empty."
            DownloadFilterCategory.Paused -> "No paused downloads."
            DownloadFilterCategory.Waiting -> "No downloads waiting for network or storage."
            DownloadFilterCategory.Failed -> "No failed downloads."
            DownloadFilterCategory.Completed -> "No completed downloads in this view."
            DownloadFilterCategory.All -> "No downloads found. Search for an anime to start downloading."
        }
        Text("No Downloads", style = AppTypography.headline.copy(fontSize = 16.sp), color = TextPrimary)
        Spacer(Modifier.height(AppSpacing.xs))
        Text(message, style = AppTypography.body, color = TextMuted)
    }
}
