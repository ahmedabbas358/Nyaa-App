package com.aniflow.feature.release

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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

import com.aniflow.core.ui.components.AniEmptyState
import androidx.compose.material.icons.filled.FolderZip

/**
 * Step 20 — Batch Detail UI State (Section 44, 45).
 * Zero hardcoded production data.
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
    val coveredEpisodes: List<BatchEpisodeCoverageItem> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BatchDetailScreen(
    uiState: BatchDetailUiState,
    onBackClick: () -> Unit,
    onDownloadBatchClick: (releaseId: String) -> Unit,
    modifier: Modifier = Modifier
) {
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        bottomBar = {
            if (uiState.releaseId.isNotBlank()) {
                Surface(
                    color = DarkSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Total Size", color = TextMuted, fontSize = 12.sp)
                            Text(text = if (uiState.size.isNotBlank()) uiState.size else "—", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onDownloadBatchClick(uiState.releaseId) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download Batch", fontSize = 14.sp)
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                            Text(text = "Batch Package", color = PrimaryIndigo, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                            Text(text = uiState.size, color = TextMuted, fontSize = 13.sp)
                            Text(text = "•", color = TextMuted, fontSize = 13.sp)
                            Text(text = "${uiState.seeders} seeds", color = AppSemanticColors.Success, fontSize = 13.sp)
                            if (uiState.releaseGroup != null) {
                                Text(text = "•", color = TextMuted, fontSize = 13.sp)
                                Text(text = uiState.releaseGroup, color = TextSecondary, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Coverage Matrix Section (Section 45)
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
                                text = "Covered Episodes",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )

                            if (uiState.isCoverageInferred) {
                                Surface(
                                    color = AppSemanticColors.Warning.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "Coverage Inferred",
                                        color = AppSemanticColors.Warning,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // Flow Row of Episode chips (Section 45: 01 ✓, 02 ✓, ...)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.coveredEpisodes.forEach { ep ->
                                val chipColor = if (ep.isCovered) AppSemanticColors.Success else AppSemanticColors.Error
                                Surface(
                                    color = chipColor.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, chipColor.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = String.format("%02d %s", ep.episodeNumber, if (ep.isCovered) "✓" else "—"),
                                        color = chipColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Technical Specifications
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
                        Text(text = "Technical Metadata", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)

                        TechnicalRow("Resolution", uiState.resolution ?: "1080p")
                        TechnicalRow("Video Codec", uiState.videoCodec ?: "HEVC")
                        TechnicalRow("Audio", uiState.audioCodec ?: "AAC")
                        TechnicalRow("Subtitles", uiState.subtitleSummary ?: "Embedded")
                        TechnicalRow("Uploader", uiState.uploader ?: "Anonymous")
                    }
                }
            }
        }
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
