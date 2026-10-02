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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

/**
 * Candidate Grouping Filter options (Section 40).
 */
enum class CandidateGrouping {
    None,
    ByGroup,
    ByQuality,
    ByUploader
}

data class LocalFileUiModel(
    val fileName: String,
    val filePath: String,
    val fileSizeFormatted: String,
    val resolution: String? = null
)

data class CandidateReleaseUiModel(
    val releaseId: String,
    val rawTitle: String,
    val resolution: String?,
    val videoCodec: String?,
    val source: String?,
    val audioSummary: String?,
    val subSummary: String?,
    val size: String,
    val seeders: Int,
    val uploader: String?,
    val releaseGroup: String?,
    val isBatch: Boolean,
    val batchLabel: String,
    val confidence: Float,
    val recommendationLabel: String? = null
)

data class BatchMembershipUiModel(
    val batchReleaseId: String,
    val batchTitle: String,
    val episodeRangeSummary: String
)

data class EpisodeDetailUiState(
    val episodeId: String = "",
    val animeTitle: String = "One Piece",
    val seasonNumber: Int? = 1,
    val episodeNumber: String = "3",
    val episodeTitle: String? = null,
    val state: EpisodeAvailabilityState = EpisodeAvailabilityState.Available,
    val localFiles: List<LocalFileUiModel> = emptyList(),
    val candidateReleases: List<CandidateReleaseUiModel> = emptyList(),
    val batchCoverages: List<BatchMembershipUiModel> = emptyList(),
    val selectedReleaseIdsForCompare: Set<String> = emptySet(),
    val grouping: CandidateGrouping = CandidateGrouping.None
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeDetailScreen(
    uiState: EpisodeDetailUiState,
    onBackClick: () -> Unit,
    onSearchAgainClick: (episodeId: String) -> Unit,
    onViewBatchClick: (batchReleaseId: String) -> Unit,
    onSelectReleaseToDownload: (releaseId: String) -> Unit,
    onToggleCompareSelection: (releaseId: String) -> Unit,
    onCompareClick: (releaseIds: List<String>) -> Unit,
    onGroupingChange: (CandidateGrouping) -> Unit,
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
                            text = "Episode ${uiState.episodeNumber}",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${uiState.animeTitle} • Season ${uiState.seasonNumber ?: "?"}",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
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
                    IconButton(onClick = { onSearchAgainClick(uiState.episodeId) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Search Again",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        bottomBar = {
            if (uiState.selectedReleaseIdsForCompare.size in 2..5) {
                Surface(
                    color = DarkSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${uiState.selectedReleaseIdsForCompare.size} releases selected",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Button(
                            onClick = { onCompareClick(uiState.selectedReleaseIdsForCompare.toList()) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CompareArrows, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Compare Releases", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Episode Summary Card
            item {
                EpisodeHeaderCard(uiState = uiState)
            }

            // 2. Local Library Files (Section 54, 55)
            if (uiState.localFiles.isNotEmpty()) {
                item {
                    LocalFilesSection(localFiles = uiState.localFiles)
                }
            }

            // 3. Batch Coverage Notice (Section 43)
            if (uiState.batchCoverages.isNotEmpty()) {
                item {
                    BatchCoverageSection(
                        batches = uiState.batchCoverages,
                        onViewBatchClick = onViewBatchClick
                    )
                }
            }

            // 4. Candidate Releases Header & Filters (Section 39, 40)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Release Candidates (${uiState.candidateReleases.size})",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Select 2-5 to compare",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }

                    // Grouping Chips
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = uiState.grouping == CandidateGrouping.None,
                            onClick = { onGroupingChange(CandidateGrouping.None) },
                            label = { Text("All", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryIndigo.copy(alpha = 0.2f),
                                selectedLabelColor = PrimaryIndigo
                            )
                        )
                        FilterChip(
                            selected = uiState.grouping == CandidateGrouping.ByQuality,
                            onClick = { onGroupingChange(CandidateGrouping.ByQuality) },
                            label = { Text("Quality", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryIndigo.copy(alpha = 0.2f),
                                selectedLabelColor = PrimaryIndigo
                            )
                        )
                        FilterChip(
                            selected = uiState.grouping == CandidateGrouping.ByGroup,
                            onClick = { onGroupingChange(CandidateGrouping.ByGroup) },
                            label = { Text("Release Group", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryIndigo.copy(alpha = 0.2f),
                                selectedLabelColor = PrimaryIndigo
                            )
                        )
                    }
                }
            }

            // 5. Candidate Release Items
            items(uiState.candidateReleases, key = { it.releaseId }) { candidate ->
                CandidateReleaseCard(
                    candidate = candidate,
                    isSelectedForCompare = uiState.selectedReleaseIdsForCompare.contains(candidate.releaseId),
                    onToggleCompare = { onToggleCompareSelection(candidate.releaseId) },
                    onDownloadClick = { onSelectReleaseToDownload(candidate.releaseId) }
                )
            }
        }
    }
}

@Composable
private fun EpisodeHeaderCard(uiState: EpisodeDetailUiState) {
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Episode ${uiState.episodeNumber}",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    color = when (uiState.state) {
                        EpisodeAvailabilityState.Available -> AppSemanticColors.Success.copy(alpha = 0.15f)
                        EpisodeAvailabilityState.Downloaded -> AppSemanticColors.Success.copy(alpha = 0.15f)
                        EpisodeAvailabilityState.Downloading -> AppSemanticColors.Info.copy(alpha = 0.15f)
                        EpisodeAvailabilityState.ReviewRequired -> AppSemanticColors.Warning.copy(alpha = 0.15f)
                        else -> TextMuted.copy(alpha = 0.15f)
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${uiState.state.symbol} ${uiState.state.name}",
                        color = when (uiState.state) {
                            EpisodeAvailabilityState.Available -> AppSemanticColors.Success
                            EpisodeAvailabilityState.Downloaded -> AppSemanticColors.Success
                            EpisodeAvailabilityState.Downloading -> AppSemanticColors.Info
                            EpisodeAvailabilityState.ReviewRequired -> AppSemanticColors.Warning
                            else -> TextSecondary
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (!uiState.episodeTitle.isNullOrBlank()) {
                Text(
                    text = uiState.episodeTitle,
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun LocalFilesSection(localFiles: List<LocalFileUiModel>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(imageVector = Icons.Default.Folder, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(16.dp))
                Text(text = "Local Library Files (${localFiles.size})", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            localFiles.forEach { file ->
                Surface(
                    color = DarkBackground,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = file.fileName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                            Text(text = "${file.fileSizeFormatted} • ${file.resolution ?: "Standard"}", color = TextMuted, fontSize = 11.sp)
                        }
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Local", tint = AppSemanticColors.Success, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun BatchCoverageSection(
    batches: List<BatchMembershipUiModel>,
    onViewBatchClick: (String) -> Unit
) {
    batches.forEach { batch ->
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onViewBatchClick(batch.batchReleaseId) },
            color = PrimaryIndigo.copy(alpha = 0.08f),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(imageVector = Icons.Default.Inventory2, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Included in Batch", color = PrimaryIndigo, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(text = batch.batchTitle, color = TextPrimary, fontSize = 13.sp, maxLines = 1)
                }
                OutlinedButton(
                    onClick = { onViewBatchClick(batch.batchReleaseId) },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("View Batch", fontSize = 11.sp, color = PrimaryIndigo)
                }
            }
        }
    }
}

@Composable
private fun CandidateReleaseCard(
    candidate: CandidateReleaseUiModel,
    isSelectedForCompare: Boolean,
    onToggleCompare: () -> Unit,
    onDownloadClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DarkSurface,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelectedForCompare) PrimaryIndigo else DarkCardBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(
                    checked = isSelectedForCompare,
                    onCheckedChange = { onToggleCompare() },
                    colors = CheckboxDefaults.colors(checkedColor = PrimaryIndigo),
                    modifier = Modifier.size(20.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = candidate.rawTitle,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = DarkBackground,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = candidate.batchLabel,
                                color = PrimaryIndigo,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (candidate.recommendationLabel != null) {
                            Surface(
                                color = AppSemanticColors.Success.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = candidate.recommendationLabel,
                                    color = AppSemanticColors.Success,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Technical details row (Section 39)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = candidate.resolution ?: "1080p", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(text = "•", color = TextMuted, fontSize = 12.sp)
                Text(text = candidate.videoCodec ?: "x264", color = TextMuted, fontSize = 12.sp)
                Text(text = "•", color = TextMuted, fontSize = 12.sp)
                Text(text = candidate.size, color = TextMuted, fontSize = 12.sp)
                Text(text = "•", color = TextMuted, fontSize = 12.sp)
                Text(text = "${candidate.seeders} seeds", color = AppSemanticColors.Success, fontSize = 12.sp)
                if (candidate.releaseGroup != null) {
                    Text(text = "•", color = TextMuted, fontSize = 12.sp)
                    Text(text = candidate.releaseGroup, color = TextSecondary, fontSize = 12.sp)
                }
            }

            // Download action (Section 90, 91: passes to planner, not direct task creation)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onDownloadClick,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Select & Download", fontSize = 12.sp)
                }
            }
        }
    }
}
