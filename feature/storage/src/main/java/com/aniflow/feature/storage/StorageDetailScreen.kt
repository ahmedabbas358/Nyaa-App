package com.aniflow.feature.storage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import com.aniflow.domain.identity.LibraryFileId
import com.aniflow.domain.identity.StorageId
import com.aniflow.domain.storage.model.StorageAvailability
import com.aniflow.domain.storage.model.StorageFileSummary
import com.aniflow.domain.storage.model.StorageLocation
import com.aniflow.domain.storage.model.StorageLocationRoot
import com.aniflow.domain.storage.model.StorageRoot
import com.aniflow.domain.storage.model.StorageType

data class StorageDetailUiState(
    val root: StorageRoot = StorageRoot(
        id = StorageId("internal_anime"),
        name = "Internal Anime Library",
        type = StorageType.AppPrivate,
        state = StorageAvailability.Available,
        location = StorageLocationRoot("/storage/emulated/0/AniFlow/Anime"),
        totalSpaceBytes = 128L * 1024 * 1024 * 1024,
        freeSpaceBytes = 42L * 1024 * 1024 * 1024,
        isDefault = true
    ),
    val libraryUsedBytes: Long = 76L * 1024 * 1024 * 1024,
    val downloadTempBytes: Long = 4L * 1024 * 1024 * 1024,
    val orphanedTempBytes: Long = 512L * 1024 * 1024,
    val unidentifiedFilesCount: Int = 3,
    val largestFiles: List<StorageFileSummary> = listOf(
        StorageFileSummary(
            fileId = LibraryFileId("f1"),
            fileName = "One Piece - 1089 [1080p HEVC].mkv",
            animeTitle = "One Piece",
            episodeNumber = 1089.0,
            sizeBytes = 1420L * 1024 * 1024,
            location = StorageLocation(StorageId("internal_anime"), "One Piece/One Piece - 1089 [1080p HEVC].mkv")
        ),
        StorageFileSummary(
            fileId = LibraryFileId("f2"),
            fileName = "Bleach TYBW - 26 [1080p HEVC].mkv",
            animeTitle = "Bleach: Thousand-Year Blood War",
            episodeNumber = 26.0,
            sizeBytes = 1350L * 1024 * 1024,
            location = StorageLocation(StorageId("internal_anime"), "Bleach/Bleach TYBW - 26 [1080p HEVC].mkv")
        )
    ),
    val issuesCount: Int = 1
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageDetailScreen(
    state: StorageDetailUiState = StorageDetailUiState(),
    onNavigateBack: () -> Unit = {},
    onScan: () -> Unit = {},
    onRepair: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Usage", "Large Files", "Issues")

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = state.root.name, style = AppTypography.titleMedium, color = TextPrimary)
                        Text(text = state.root.location.rawUriOrPath, style = AppTypography.bodySmall, color = TextMuted)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = onScan) {
                        Icon(Icons.Default.Refresh, contentDescription = "Scan", tint = PrimaryIndigo)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                                style = AppTypography.labelLarge,
                                color = if (selectedTab == index) PrimaryIndigo else TextSecondary
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> StorageOverviewTab(state = state, onScan = onScan, onRepair = onRepair)
                1 -> StorageUsageBreakdownTab(state = state)
                2 -> StorageLargeFilesTab(files = state.largestFiles)
                3 -> StorageIssuesTab(state = state, onRepair = onRepair)
            }
        }
    }
}

@Composable
fun StorageOverviewTab(
    state: StorageDetailUiState,
    onScan: () -> Unit,
    onRepair: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkCardBorder, AppShapes.large),
                shape = AppShapes.large,
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(AppSpacing.md)) {
                    Text("Volume Capacity", style = AppTypography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(AppSpacing.sm))

                    LinearProgressIndicator(
                        progress = { state.root.usedPercentage / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = PrimaryIndigo,
                        trackColor = DarkBackground,
                        strokeCap = StrokeCap.Round
                    )

                    Spacer(modifier = Modifier.height(AppSpacing.sm))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${formatBytes(state.root.usedSpaceBytes)} used", style = AppTypography.bodySmall, color = TextSecondary)
                        Text("${formatBytes(state.root.freeSpaceBytes)} free", style = AppTypography.bodySmall, color = TextMuted)
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
                Button(
                    onClick = onScan,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(AppSpacing.xs))
                    Text("Full Scan")
                }

                OutlinedButton(
                    onClick = onRepair,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Build, contentDescription = null)
                    Spacer(modifier = Modifier.width(AppSpacing.xs))
                    Text("Reconcile")
                }
            }
        }
    }
}

@Composable
fun StorageUsageBreakdownTab(state: StorageDetailUiState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        item {
            UsageRow(label = "Anime Library Media", bytes = state.libraryUsedBytes, color = PrimaryIndigo)
        }
        item {
            UsageRow(label = "Active Downloads (.part)", bytes = state.downloadTempBytes, color = AppSemanticColors.Success)
        }
        item {
            UsageRow(label = "Orphaned Temporary Leftovers", bytes = state.orphanedTempBytes, color = AppSemanticColors.Warning)
        }
    }
}

@Composable
fun UsageRow(label: String, bytes: Long, color: androidx.compose.ui.graphics.Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkCardBorder, AppShapes.medium),
        shape = AppShapes.medium,
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).background(color, AppShapes.small))
                Spacer(modifier = Modifier.width(AppSpacing.sm))
                Text(label, style = AppTypography.bodyMedium, color = TextPrimary)
            }
            Text(formatBytes(bytes), style = AppTypography.labelLarge, color = TextSecondary)
        }
    }
}

@Composable
fun StorageLargeFilesTab(files: List<StorageFileSummary>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        items(files, key = { it.fileId.value }) { file ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkCardBorder, AppShapes.medium),
                shape = AppShapes.medium,
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppSpacing.md),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(file.fileName, style = AppTypography.bodyMedium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(file.animeTitle ?: "Unidentified", style = AppTypography.bodySmall, color = TextMuted)
                    }
                    Text(formatBytes(file.sizeBytes), style = AppTypography.labelLarge, color = PrimaryIndigo)
                }
            }
        }
    }
}

@Composable
fun StorageIssuesTab(
    state: StorageDetailUiState,
    onRepair: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AppSemanticColors.Warning.copy(alpha = 0.4f), AppShapes.medium),
                shape = AppShapes.medium,
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = AppSemanticColors.Warning)
                    Spacer(modifier = Modifier.width(AppSpacing.sm))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("1 Orphaned Download Chunk Detected", style = AppTypography.titleSmall, color = TextPrimary)
                        Text("512 MB in leftover partial files from interrupted tasks.", style = AppTypography.bodySmall, color = TextSecondary)
                    }
                    Button(onClick = onRepair, colors = ButtonDefaults.buttonColors(containerColor = AppSemanticColors.Warning)) {
                        Text("Clean")
                    }
                }
            }
        }
    }
}
