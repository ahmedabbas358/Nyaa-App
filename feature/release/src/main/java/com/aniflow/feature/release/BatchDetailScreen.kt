package com.aniflow.feature.release

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aniflow.core.ui.components.AniEmptyState
import com.aniflow.core.ui.theme.AppSemanticColors
import com.aniflow.core.ui.theme.DarkBackground
import com.aniflow.core.ui.theme.DarkCardBorder
import com.aniflow.core.ui.theme.DarkSurface
import com.aniflow.core.ui.theme.DarkSurfaceVariant
import com.aniflow.core.ui.theme.PrimaryIndigo
import com.aniflow.core.ui.theme.TextMuted
import com.aniflow.core.ui.theme.TextPrimary
import com.aniflow.core.ui.theme.TextSecondary
import com.aniflow.core.ui.util.TorrentClientBridge

/**
 * Step 20 — Advanced Batch Release Controller.
 * Supports granular episode selection, multi-client dispatching (1DM, Flud, LibreTorrent),
 * direct .torrent saving, and magnet clipboard export.
 */
data class BatchEpisodeCoverageItem(
    val episodeNumber: Int,
    val isCovered: Boolean
)

data class BatchDetailUiState(
    val releaseId: String = "",
    val batchTitle: String = "",
    val size: String = "",
    val uploader: String? = null,
    val releaseGroup: String? = null,
    val seeders: Int = 0,
    val resolution: String? = null,
    val videoCodec: String? = null,
    val audioCodec: String? = null,
    val subtitleSummary: String? = null,
    val isCoverageInferred: Boolean = false,
    val coveredEpisodes: List<BatchEpisodeCoverageItem> = emptyList(),
    val magnetUri: String? = null,
    val torrentUrl: String? = null
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BatchDetailScreen(
    uiState: BatchDetailUiState,
    onBackClick: () -> Unit,
    onDownloadBatchClick: (releaseId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val effectiveMagnet = uiState.magnetUri
    val effectiveTorrentUrl = uiState.torrentUrl ?: "https://nyaa.si/download/${uiState.releaseId}.torrent"
    val nyaaWebUrl = "https://nyaa.si/view/${uiState.releaseId}"

    // Episode selection tracking
    val selectionMap = remember(uiState.coveredEpisodes) {
        val map = mutableStateMapOf<Int, Boolean>()
        uiState.coveredEpisodes.forEach { ep ->
            map[ep.episodeNumber] = ep.isCovered
        }
        map
    }

    val selectedEpisodesCount by remember {
        derivedStateOf { selectionMap.values.count { it } }
    }

    var showRangeDialog by remember { mutableStateOf(false) }
    var rangeStart by remember { mutableStateOf("1") }
    var rangeEnd by remember { mutableStateOf("${uiState.coveredEpisodes.size.coerceAtLeast(1)}") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Batch Release Details",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
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
                    IconButton(onClick = {
                        TorrentClientBridge.openWebPage(context, nyaaWebUrl)
                    }) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = "Open on Nyaa",
                            tint = PrimaryIndigo
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        bottomBar = {
            if (uiState.releaseId.isNotBlank()) {
                Surface(
                    color = DarkSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Quick Action Buttons Row: External App + Save .torrent + Copy Magnet
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (!effectiveMagnet.isNullOrBlank()) {
                                        TorrentClientBridge.openInExternalTorrentClient(
                                            context,
                                            effectiveMagnet,
                                            uiState.batchTitle
                                        )
                                    } else {
                                        TorrentClientBridge.downloadTorrentFileDirectly(
                                            context,
                                            effectiveTorrentUrl,
                                            uiState.batchTitle,
                                            openAfterDownload = true
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("1DM / Torrent App", fontSize = 11.sp, maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = {
                                    TorrentClientBridge.downloadTorrentFileDirectly(
                                        context,
                                        effectiveTorrentUrl,
                                        uiState.batchTitle,
                                        openAfterDownload = false
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save .torrent", fontSize = 11.sp, maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = {
                                    if (!effectiveMagnet.isNullOrBlank()) {
                                        TorrentClientBridge.copyToClipboard(context, effectiveMagnet)
                                    } else {
                                        TorrentClientBridge.copyToClipboard(context, nyaaWebUrl, toastMessage = "Page URL copied")
                                    }
                                },
                                modifier = Modifier.weight(0.8f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Magnet", fontSize = 11.sp)
                            }
                        }

                        // Main Action: Download in AniFlow
                        Button(
                            onClick = { onDownloadBatchClick(uiState.releaseId) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Download Batch in AniFlow ($selectedEpisodesCount/${uiState.coveredEpisodes.size} Ep)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (uiState.batchTitle.isBlank() && uiState.releaseId.isBlank()) {
            AniEmptyState(
                title = "Batch release not found",
                description = "The requested batch release metadata is not available.",
                icon = Icons.Default.FolderZip,
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Info Card
                item {
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Inventory2, contentDescription = null, tint = PrimaryIndigo)
                                Text(text = "Complete Season Batch", color = PrimaryIndigo, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            Text(
                                text = uiState.batchTitle,
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = uiState.size.ifBlank { "Unknown size" }, color = TextMuted, fontSize = 13.sp)
                                Text(text = "•", color = TextMuted, fontSize = 13.sp)
                                Text(text = "${uiState.seeders} seeders", color = AppSemanticColors.Success, fontSize = 13.sp)
                                if (uiState.uploader != null) {
                                    Text(text = "•", color = TextMuted, fontSize = 13.sp)
                                    Text(text = uiState.uploader, color = TextSecondary, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                // Granular Episode Selection & Matrix
                item {
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
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Select Episodes to Download",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = "$selectedEpisodesCount / ${uiState.coveredEpisodes.size} selected",
                                    color = PrimaryIndigo,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Quick selection chips (Select All, Deselect All, Range)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        uiState.coveredEpisodes.forEach { selectionMap[it.episodeNumber] = true }
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Select All", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        uiState.coveredEpisodes.forEach { selectionMap[it.episodeNumber] = false }
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Deselect", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = { showRangeDialog = true },
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Range…", fontSize = 11.sp)
                                }
                            }

                            // Flow Row of Interactive Episode Chips
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                uiState.coveredEpisodes.forEach { ep ->
                                    val isSelected = selectionMap[ep.episodeNumber] == true
                                    val chipColor = if (isSelected) PrimaryIndigo else TextMuted
                                    Surface(
                                        color = if (isSelected) PrimaryIndigo.copy(alpha = 0.2f) else DarkSurfaceVariant,
                                        shape = RoundedCornerShape(8.dp),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSelected) PrimaryIndigo else DarkCardBorder
                                        ),
                                        modifier = Modifier.clickable {
                                            selectionMap[ep.episodeNumber] = !isSelected
                                        }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = PrimaryIndigo,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                            }
                                            Text(
                                                text = String.format("EP %02d", ep.episodeNumber),
                                                color = if (isSelected) TextPrimary else TextMuted,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Technical Metadata
                item {
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
                            Text(text = "Release Specifications", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)

                            TechnicalRow("Resolution", uiState.resolution ?: "1080p")
                            TechnicalRow("Video Codec", uiState.videoCodec ?: "HEVC")
                            TechnicalRow("Audio", uiState.audioCodec ?: "AAC")
                            TechnicalRow("Subtitles", uiState.subtitleSummary ?: "Embedded")
                            TechnicalRow("Uploader", uiState.uploader ?: "Anonymous")
                            TechnicalRow("Release Group", uiState.releaseGroup ?: "None")
                        }
                    }
                }
            }
        }
    }

    if (showRangeDialog) {
        AlertDialog(
            onDismissRequest = { showRangeDialog = false },
            title = { Text("Select Episode Range", style = androidx.compose.material3.MaterialTheme.typography.titleMedium, color = TextPrimary) },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = rangeStart,
                        onValueChange = { rangeStart = it },
                        label = { Text("From EP") },
                        modifier = Modifier.weight(1f)
                    )
                    Text("to", color = TextSecondary)
                    OutlinedTextField(
                        value = rangeEnd,
                        onValueChange = { rangeEnd = it },
                        label = { Text("To EP") },
                        modifier = Modifier.weight(1f)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val start = rangeStart.toIntOrNull() ?: 1
                        val end = rangeEnd.toIntOrNull() ?: uiState.coveredEpisodes.size
                        val range = minOf(start, end)..maxOf(start, end)
                        uiState.coveredEpisodes.forEach { ep ->
                            selectionMap[ep.episodeNumber] = ep.episodeNumber in range
                        }
                        showRangeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) {
                    Text("Apply Range")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRangeDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
private fun TechnicalRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextMuted, fontSize = 13.sp)
        Text(text = value, color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
