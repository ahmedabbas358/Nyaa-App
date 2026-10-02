package com.aniflow.feature.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.unit.dp
import com.aniflow.core.ui.model.DownloadUiModel
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

/**
 * DownloadDetailScreen (Sections 65, 66, 67, 68).
 * In-depth task monitoring screen with tabs for Overview, Files (for Batch/Torrents),
 * Network Connections, and Engine Diagnostics.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadDetailScreen(
    download: DownloadUiModel = PreviewFixtures.sampleDownloadActive,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Files", "Connections", "Logs")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(download.title, style = AppTypography.headline, color = TextPrimary, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
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
            // Header: Linear progress, Speed, ETA
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = AppShapes.medium,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.md)
            ) {
                Column(modifier = Modifier.padding(AppSpacing.md)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("${download.progressPercent}% Completed", style = AppTypography.title, color = TextPrimary)
                        Text(download.speedFormatted, style = AppTypography.numericSpeed, color = AppSemanticColors.Info)
                    }
                    Spacer(modifier = Modifier.height(AppSpacing.xs))
                    LinearProgressIndicator(
                        progress = { download.progressPercent.toFloat() / 100f },
                        color = PrimaryIndigo,
                        trackColor = DarkCardBorder,
                        strokeCap = StrokeCap.Round,
                        modifier = Modifier.fillMaxWidth().height(8.dp)
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.xs))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("${download.downloadedBytesFormatted} of ${download.totalBytesFormatted}", style = AppTypography.caption, color = TextSecondary)
                        Text("ETA: ${download.etaFormatted}", style = AppTypography.numericEta, color = TextMuted)
                    }
                }
            }

            // Tab Navigation
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = DarkSurface,
                contentColor = PrimaryIndigo,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = PrimaryIndigo
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            // Tab Content
            when (selectedTabIndex) {
                0 -> OverviewTabContent(download)
                1 -> FilesTabContent()
                2 -> ConnectionsTabContent(download)
                3 -> LogsTabContent()
            }
        }
    }
}

@Composable
private fun OverviewTabContent(download: DownloadUiModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        DetailRow("Engine Type", download.engineType)
        DetailRow("Status", download.status.name)
        DetailRow("Active Connections", "${download.activeConnections}")
        DetailRow("Total Segments", "${download.totalSegments}")
        DetailRow("Target Path", "Anime/One Piece/Season 01/Episode 1050.mkv")
    }
}

@Composable
private fun FilesTabContent() {
    val batchFiles = listOf(
        "Episode 01.mkv" to "100%",
        "Episode 02.mkv" to "100%",
        "Episode 03.mkv" to "68%",
        "Episode 04.mkv" to "0%"
    )
    LazyColumn(
        contentPadding = PaddingValues(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
    ) {
        items(batchFiles) { (name, prog) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppSpacing.xs)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (prog == "100%") Icons.Default.CheckCircle else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (prog == "100%") AppSemanticColors.Success else PrimaryIndigo,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.sm))
                    Text(name, style = AppTypography.body, color = TextPrimary)
                }
                Text(prog, style = AppTypography.numericSize, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun ConnectionsTabContent(download: DownloadUiModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        if (download.engineType == "HTTP") {
            Text("HTTP Segment Pool", style = AppTypography.title, color = TextPrimary)
            (1..8).forEach { seg ->
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                ) {
                    Text("Segment #$seg", style = AppTypography.bodySecondary)
                    Text("1.05 MB/s • Active", style = AppTypography.numericSpeed, color = AppSemanticColors.Info)
                }
            }
        } else {
            Text("Torrent Swarm", style = AppTypography.title, color = TextPrimary)
            DetailRow("Seeders", "${download.seeders ?: 0}")
            DetailRow("Peers", "${download.peers ?: 0}")
            DetailRow("Share Ratio", "0.42")
        }
    }
}

@Composable
private fun LogsTabContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppSpacing.md)
    ) {
        Text("Task Diagnostics", style = AppTypography.title, color = TextPrimary)
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        Text("[10:14:02] Engine initialized (Parallel: 8 segments)", style = AppTypography.caption)
        Text("[10:14:03] Byte-range HTTP 206 Partial Content verified", style = AppTypography.caption)
        Text("[10:14:05] Buffer stream active at 8.4 MB/s", style = AppTypography.caption)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Text(label, style = AppTypography.bodySecondary)
        Text(value, style = AppTypography.body, color = TextPrimary)
    }
}
