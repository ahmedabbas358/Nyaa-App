package com.aniflow.feature.search.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.aniflow.core.ui.util.TorrentClientBridge
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aniflow.core.ui.theme.AccentCyan
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
import com.aniflow.domain.search.model.SearchResultItem

/**
 * Episode Selection & Multi-Download Bottom Sheet.
 *
 * Enables users to:
 * - Filter by uploader (SubsPlease, Erai-raws, EMBER, Judas)
 * - Select specific episodes from a unified checklist
 * - Bulk selection: Select All, Deselect, and Range
 * - View total size estimate and seeder health
 * - Download via: Internal queue, .torrent files, magnet export, or open in external apps
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeBatchSelectionSheet(
    groupedAnime: SearchResultItem.GroupedAnimeResult,
    onQueueBatchDownload: (List<SearchResultItem.ReleaseResult>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    // Distinct uploaders present in this group
    val availableUploaders = remember(groupedAnime) {
        val list = groupedAnime.releases
            .map { it.uploader.ifBlank { "Unknown" } }
            .distinct()
            .sorted()
        if (list.isEmpty()) listOf("All") else listOf("All") + list
    }

    var selectedUploader by remember { mutableStateOf("All") }

    // Filter releases by selected uploader (if not "All")
    val filteredReleases = remember(selectedUploader, groupedAnime) {
        if (selectedUploader == "All") {
            groupedAnime.releases
        } else {
            groupedAnime.releases.filter { it.uploader.equals(selectedUploader, ignoreCase = true) }
        }
    }

    // Selection map: releaseId -> isSelected
    val selectionMap = remember(groupedAnime) {
        val map = mutableStateMapOf<String, Boolean>()
        // Default: select all releases initially for maximum convenience
        groupedAnime.releases.forEach { rel ->
            map[rel.id] = true
        }
        map
    }

    val selectedCount by remember {
        derivedStateOf {
            selectionMap.values.count { it }
        }
    }

    val selectedReleases by remember {
        derivedStateOf {
            groupedAnime.releases.filter { selectionMap[it.id] == true }
        }
    }

    val selectedEstimatedSize by remember {
        derivedStateOf {
            AnimeReleaseGrouper.estimateTotalSize(selectedReleases)
        }
    }

    val targetFolder = remember(groupedAnime) {
        val safeTitle = groupedAnime.title.replace(Regex("""[\\/:*?"<>|]"""), "_").trim()
        "Anime/$safeTitle/Season ${groupedAnime.seasonNumber}"
    }

    var isAscending by remember { mutableStateOf(true) }
    var showRangeDialog by remember { mutableStateOf(false) }
    var rangeStartText by remember { mutableStateOf("1") }
    var rangeEndText by remember { mutableStateOf("12") }

    val sortedReleases = remember(filteredReleases, isAscending) {
        filteredReleases.sortedWith(
            Comparator { a, b ->
                val epA = AnimeReleaseGrouper.extractEpisodeNumber(a.title) ?: 0
                val epB = AnimeReleaseGrouper.extractEpisodeNumber(b.title) ?: 0
                if (isAscending) epA.compareTo(epB) else epB.compareTo(epA)
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkBackground,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(DarkCardBorder)
            )
        },
        modifier = modifier.fillMaxHeight(0.92f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            // Header: Title + Close
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = groupedAnime.title,
                        style = AppTypography.Title.copy(fontSize = 16.sp),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(PrimaryIndigo.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Season ${groupedAnime.seasonNumber}",
                                style = AppTypography.Caption.copy(fontWeight = FontWeight.SemiBold),
                                color = PrimaryIndigo
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${groupedAnime.releases.size} releases",
                            style = AppTypography.Caption,
                            color = TextMuted
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            // Storage path indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(DarkSurfaceVariant)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Folder, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = targetFolder,
                    style = AppTypography.Caption.copy(fontSize = 10.sp),
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Uploader Filter Chips
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(availableUploaders) { uploader ->
                    val isSelected = selectedUploader == uploader
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedUploader = uploader
                            if (uploader != "All") {
                                groupedAnime.releases.forEach { rel ->
                                    selectionMap[rel.id] = rel.uploader.equals(uploader, ignoreCase = true)
                                }
                            }
                        },
                        label = {
                            Text(
                                text = uploader,
                                style = AppTypography.Caption.copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = DarkSurface,
                            labelColor = TextSecondary,
                            selectedContainerColor = PrimaryIndigo.copy(alpha = 0.15f),
                            selectedLabelColor = PrimaryIndigo
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = DarkCardBorder,
                            selectedBorderColor = PrimaryIndigo.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(6.dp)
                    )
                }
            }

            // Selection Actions Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    SelectionActionChip("All") {
                        filteredReleases.forEach { rel -> selectionMap[rel.id] = true }
                    }
                    SelectionActionChip("None") {
                        filteredReleases.forEach { rel -> selectionMap[rel.id] = false }
                    }
                    SelectionActionChip("Range…") {
                        showRangeDialog = true
                    }

                    IconButton(
                        onClick = { isAscending = !isAscending },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.SwapVert,
                            contentDescription = "Sort",
                            tint = PrimaryIndigo,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Text(
                    text = "$selectedCount selected",
                    style = AppTypography.Caption.copy(fontWeight = FontWeight.SemiBold),
                    color = if (selectedCount > 0) PrimaryIndigo else TextMuted
                )
            }

            // Episode Release Checklist
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(sortedReleases, key = { it.id }) { release ->
                    val isChecked = selectionMap[release.id] == true
                    EpisodeItemRow(
                        release = release,
                        isChecked = isChecked,
                        onToggle = { selectionMap[release.id] = it }
                    )
                }
            }

            // Bottom Action Bar — multi-destination download
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Status line
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (selectedCount > 0) "$selectedCount episodes · ~$selectedEstimatedSize" else "No episodes selected",
                            style = AppTypography.BodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // Action buttons row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Open in External App (1DM/Flud/LibreTorrent)
                        OutlinedButton(
                            onClick = {
                                val items = selectedReleases.map { rel ->
                                    val uri = rel.magnetUri?.takeIf { it.isNotBlank() }
                                        ?: rel.torrentUrl?.takeIf { it.isNotBlank() }
                                        ?: "https://nyaa.si/download/${rel.id}.torrent"
                                    rel.title to uri
                                }
                                TorrentClientBridge.openBatchInExternalTorrentClient(
                                    context = context,
                                    items = items,
                                    batchTitle = "${groupedAnime.title}_S${groupedAnime.seasonNumber}"
                                )
                            },
                            enabled = selectedCount > 0,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Open App", fontSize = 10.sp, maxLines = 1)
                        }

                        // 2. Save .torrent files
                        OutlinedButton(
                            onClick = {
                                val items = selectedReleases.map {
                                    val url = it.torrentUrl?.takeIf { u -> u.isNotBlank() }
                                        ?: "https://nyaa.si/download/${it.id}.torrent"
                                    it.title to url
                                }
                                val safeTitle = groupedAnime.title.replace(Regex("""[\\/:*?"<>|]"""), "_").trim()
                                TorrentClientBridge.batchDownloadTorrentFiles(
                                    context = context,
                                    items = items,
                                    destinationSubFolder = "$safeTitle/Season_${groupedAnime.seasonNumber}"
                                )
                            },
                            enabled = selectedCount > 0,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(".torrent", fontSize = 10.sp, maxLines = 1)
                        }

                        // 3. Copy magnets for 1DM
                        OutlinedButton(
                            onClick = {
                                val magnetItems = selectedReleases.map { rel ->
                                    val uri = rel.magnetUri?.takeIf { it.isNotBlank() }
                                        ?: rel.torrentUrl?.takeIf { it.isNotBlank() }
                                        ?: "https://nyaa.si/download/${rel.id}.torrent"
                                    rel.title to uri
                                }
                                // Export text file + copy to clipboard
                                TorrentClientBridge.exportBatchMagnetsToTextFile(
                                    context = context,
                                    batchTitle = "${groupedAnime.title}_S${groupedAnime.seasonNumber}",
                                    items = magnetItems
                                )
                                val allMagnets = magnetItems.map { it.second }.joinToString("\n")
                                TorrentClientBridge.copyToClipboard(
                                    context,
                                    allMagnets,
                                    toastMessage = "Copied $selectedCount links & exported file"
                                )
                            },
                            enabled = selectedCount > 0,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Copy", fontSize = 10.sp, maxLines = 1)
                        }

                        // 4. Queue internally
                        Button(
                            onClick = {
                                if (selectedReleases.isNotEmpty()) {
                                    onQueueBatchDownload(selectedReleases)
                                }
                            },
                            enabled = selectedCount > 0,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryIndigo,
                                disabledContainerColor = DarkCardBorder
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Queue ($selectedCount)",
                                style = AppTypography.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            )
                        }
                    }
                }
            }
        }

        if (showRangeDialog) {
            AlertDialog(
                onDismissRequest = { showRangeDialog = false },
                title = { Text("Episode Range", style = AppTypography.Title, color = TextPrimary) },
                text = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = rangeStartText,
                            onValueChange = { rangeStartText = it },
                            label = { Text("From") },
                            modifier = Modifier.weight(1f)
                        )
                        Text("—", color = TextSecondary)
                        OutlinedTextField(
                            value = rangeEndText,
                            onValueChange = { rangeEndText = it },
                            label = { Text("To") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val start = rangeStartText.toIntOrNull() ?: 1
                            val end = rangeEndText.toIntOrNull() ?: 12
                            val range = minOf(start, end)..maxOf(start, end)
                            filteredReleases.forEach { rel ->
                                val ep = AnimeReleaseGrouper.extractEpisodeNumber(rel.title)
                                selectionMap[rel.id] = ep != null && ep in range
                            }
                            showRangeDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                    ) {
                        Text("Apply")
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
}

/**
 * Small action chip for selection controls.
 */
@Composable
private fun SelectionActionChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        color = DarkSurfaceVariant,
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Text(
            text = label,
            style = AppTypography.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun EpisodeItemRow(
    release: SearchResultItem.ReleaseResult,
    isChecked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!isChecked) },
        colors = CardDefaults.cardColors(
            containerColor = if (isChecked) DarkSurfaceVariant else DarkSurface
        ),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isChecked) PrimaryIndigo.copy(alpha = 0.4f) else DarkCardBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = onToggle,
                colors = CheckboxDefaults.colors(
                    checkedColor = PrimaryIndigo,
                    uncheckedColor = TextMuted
                ),
                modifier = Modifier.size(22.dp)
            )

            Spacer(Modifier.width(8.dp))

            val epNum = remember(release.title) {
                AnimeReleaseGrouper.extractEpisodeNumber(release.title)
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (epNum != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(PrimaryIndigo.copy(alpha = 0.15f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "%02d".format(epNum),
                                style = AppTypography.Caption.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = PrimaryIndigo
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = release.title,
                        style = AppTypography.BodySmall.copy(fontWeight = FontWeight.Medium),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Uploader
                    Text(
                        text = release.uploader.ifBlank { "Nyaa" },
                        style = AppTypography.Caption.copy(fontSize = 9.sp, fontWeight = FontWeight.Medium),
                        color = TextMuted
                    )

                    Text("·", style = AppTypography.Caption, color = TextMuted)

                    // Resolution
                    Text(
                        text = release.resolution,
                        style = AppTypography.Caption.copy(fontSize = 9.sp),
                        color = AccentCyan
                    )

                    Text("·", style = AppTypography.Caption, color = TextMuted)

                    // Size
                    Text(
                        text = release.sizeFormatted,
                        style = AppTypography.Caption.copy(fontSize = 9.sp),
                        color = TextMuted
                    )

                    Text("·", style = AppTypography.Caption, color = TextMuted)

                    // Seeders
                    Text(
                        text = "${release.seeders}S",
                        style = AppTypography.Caption.copy(fontSize = 9.sp),
                        color = AppSemanticColors.Success
                    )
                }
            }
        }
    }
}
