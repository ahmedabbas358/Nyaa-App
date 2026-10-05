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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Layers
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
 * YouTube & 1DM-style Smart Episode Selection & Multi-Download Bottom Sheet.
 *
 * Enables users to:
 * - Select specific uploaders (e.g., SubsPlease, Erai-raws, EMBER, Judas) with size/codec trade-offs.
 * - View fragmented individual episodes unified into a structured Season/Anime episode checklist.
 * - Perform fast bulk selection: "Select All", "Select Missing Only", "Deselect All", and Episode Range.
 * - View real-time aggregated batch size and seeder health.
 * - 1-Click Multi-Download queuing directly into the 1DM/FDM parallel engine.
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
                    .width(40.dp)
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
            // Header Section: Anime Title, Season, and Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = groupedAnime.title,
                        style = AppTypography.headline.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = PrimaryIndigo.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(AppShapes.sm),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = "Season ${groupedAnime.seasonNumber}",
                                style = AppTypography.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                color = PrimaryIndigo,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${groupedAnime.releases.size} Episode Releases Available",
                            style = AppTypography.caption,
                            color = TextMuted
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            // Target Storage Folder Display
            Surface(
                color = DarkSurfaceVariant,
                shape = RoundedCornerShape(AppShapes.sm),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.md, vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Auto-organize: $targetFolder",
                        style = AppTypography.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Uploader Filter Chips (SubsPlease, Erai-raws, EMBER, etc.)
            Column(modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = 4.dp)) {
                Text(
                    text = "Preferred Uploader / Quality Group:",
                    style = AppTypography.caption.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                    color = TextSecondary
                )
                Spacer(Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                    contentPadding = PaddingValues(end = AppSpacing.md)
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
                                    style = AppTypography.caption.copy(
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = DarkSurface,
                                labelColor = TextSecondary,
                                selectedContainerColor = PrimaryIndigo.copy(alpha = 0.25f),
                                selectedLabelColor = PrimaryIndigo
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = DarkCardBorder,
                                selectedBorderColor = PrimaryIndigo
                            ),
                            shape = RoundedCornerShape(AppShapes.sm)
                        )
                    }
                }
            }

            // Fast 1DM-style Selection Action Bar with Sorting and Range Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.md, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = {
                            filteredReleases.forEach { rel -> selectionMap[rel.id] = true }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        shape = RoundedCornerShape(AppShapes.sm),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Select All", style = AppTypography.caption.copy(fontSize = 11.sp))
                    }

                    OutlinedButton(
                        onClick = {
                            filteredReleases.forEach { rel -> selectionMap[rel.id] = false }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
                        shape = RoundedCornerShape(AppShapes.sm),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Deselect", style = AppTypography.caption.copy(fontSize = 11.sp))
                    }

                    OutlinedButton(
                        onClick = { showRangeDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        shape = RoundedCornerShape(AppShapes.sm),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Range", style = AppTypography.caption.copy(fontSize = 11.sp))
                    }

                    IconButton(
                        onClick = { isAscending = !isAscending },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            Icons.Default.SwapVert,
                            contentDescription = "Toggle Sort Order",
                            tint = PrimaryIndigo,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = "$selectedCount / ${groupedAnime.releases.size} selected",
                    style = AppTypography.caption.copy(fontWeight = FontWeight.Bold),
                    color = AccentCyan
                )
            }

            // Episode Release Checklist
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = AppSpacing.md),
                contentPadding = PaddingValues(vertical = AppSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(6.dp)
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

            // Floating Bottom 1DM-style Batch Download CTA
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (selectedCount > 0) "$selectedCount Episodes (~$selectedEstimatedSize)" else "No episodes selected",
                            style = AppTypography.body.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                            color = TextPrimary
                        )
                        Text(
                            text = "Parallel 1DM Engine Queue",
                            style = AppTypography.caption,
                            color = TextMuted
                        )
                    }

                    val context = LocalContext.current

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = {
                                    val items = selectedReleases.map {
                                        it.title to "https://nyaa.si/download/${it.id}.torrent"
                                    }
                                    val safeTitle = groupedAnime.title.replace(Regex("""[\\/:*?"<>|]"""), "_").trim()
                                    TorrentClientBridge.batchDownloadTorrentFiles(
                                        context = context,
                                        items = items,
                                        destinationSubFolder = "$safeTitle/Season_${groupedAnime.seasonNumber}"
                                    )
                                },
                                enabled = selectedCount > 0,
                                shape = RoundedCornerShape(AppShapes.sm),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Save .torrents ($selectedCount)", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    val magnetItems = selectedReleases.mapNotNull { rel ->
                                        val uri = rel.magnetUri?.takeIf { it.isNotBlank() }
                                            ?: "https://nyaa.si/download/${rel.id}.torrent"
                                        rel.title to uri
                                    }
                                    TorrentClientBridge.exportBatchMagnetsToTextFile(
                                        context = context,
                                        batchTitle = "${groupedAnime.title}_S${groupedAnime.seasonNumber}",
                                        items = magnetItems
                                    )
                                    val allMagnets = magnetItems.map { it.second }.joinToString("\n")
                                    TorrentClientBridge.copyToClipboard(
                                        context,
                                        allMagnets,
                                        toastMessage = "Copied $selectedCount links & exported file for 1DM"
                                    )
                                },
                                enabled = selectedCount > 0,
                                shape = RoundedCornerShape(AppShapes.sm),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("1DM / Export", fontSize = 11.sp)
                            }

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
                                shape = RoundedCornerShape(AppShapes.sm),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Queue ($selectedCount)",
                                    style = AppTypography.body.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showRangeDialog) {
            AlertDialog(
                onDismissRequest = { showRangeDialog = false },
                title = { Text("Select Episode Range", style = AppTypography.headline.copy(fontSize = 16.sp), color = TextPrimary) },
                text = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = rangeStartText,
                            onValueChange = { rangeStartText = it },
                            label = { Text("From") },
                            modifier = Modifier.weight(1f)
                        )
                        Text("to", color = TextSecondary)
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
        shape = RoundedCornerShape(AppShapes.sm),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isChecked) PrimaryIndigo.copy(alpha = 0.6f) else DarkCardBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.sm, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = onToggle,
                colors = CheckboxDefaults.colors(
                    checkedColor = PrimaryIndigo,
                    uncheckedColor = TextMuted
                ),
                modifier = Modifier.size(24.dp)
            )

            Spacer(Modifier.width(AppSpacing.sm))

            val epNum = remember(release.title) {
                AnimeReleaseGrouper.extractEpisodeNumber(release.title)
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (epNum != null) {
                        Surface(
                            color = PrimaryIndigo,
                            shape = RoundedCornerShape(3.dp),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = "EP %02d".format(epNum),
                                style = AppTypography.caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = release.title,
                        style = AppTypography.body.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
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
                    // Uploader Pill
                    Surface(
                        color = DarkCardBorder,
                        shape = RoundedCornerShape(3.dp)
                    ) {
                        Text(
                            text = release.uploader.ifBlank { "Nyaa" },
                            style = AppTypography.caption.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }

                    // Resolution Pill
                    Text(
                        text = release.resolution,
                        style = AppTypography.caption.copy(fontSize = 10.sp),
                        color = AccentCyan
                    )

                    Text(
                        text = "•",
                        style = AppTypography.caption.copy(fontSize = 10.sp),
                        color = TextMuted
                    )

                    // Size
                    Text(
                        text = release.sizeFormatted,
                        style = AppTypography.caption.copy(fontSize = 10.sp),
                        color = TextMuted
                    )

                    Text(
                        text = "•",
                        style = AppTypography.caption.copy(fontSize = 10.sp),
                        color = TextMuted
                    )

                    // Seeders
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Speed,
                            contentDescription = null,
                            tint = AppSemanticColors.Success,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = "${release.seeders} seeds",
                            style = AppTypography.caption.copy(fontSize = 10.sp),
                            color = AppSemanticColors.Success
                        )
                    }
                }
            }
        }
    }
}
