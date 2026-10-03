package com.aniflow.feature.collections

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aniflow.core.ui.components.AniAppBar
import com.aniflow.core.ui.components.AniEmptyState
import com.aniflow.core.ui.components.AniMediaCard
import com.aniflow.core.ui.components.AniStatusBadge
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

data class CollectionItemUiModel(
    val animeId: String,
    val title: String,
    val totalEpisodes: Int,
    val downloadedEpisodes: Int,
    val statusText: String
)

/**
 * CollectionDetailScreen (Section 55).
 * Header: Collection name, description, count, progress.
 * Actions: Play, Edit, Add, Remove, Download missing.
 * Smart collections show Dynamic badge and explain the criteria.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionDetailScreen(
    collectionId: String,
    collectionName: String = "Collection",
    collectionDescription: String = "User curated media collection",
    isSmartCollection: Boolean = false,
    smartCriteriaExplanation: String? = null,
    onBack: () -> Unit = {},
    onPlayCollection: () -> Unit = {},
    onDownloadMissing: () -> Unit = {},
    onAnimeClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var items by remember { mutableStateOf<List<CollectionItemUiModel>>(emptyList()) }

    Scaffold(
        topBar = {
            AniAppBar(
                title = collectionName,
                subtitle = if (isSmartCollection) "Smart Collection" else "Manual Collection",
                onBack = onBack,
                actions = {
                    IconButton(onClick = onPlayCollection) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play Collection",
                            tint = PrimaryIndigo
                        )
                    }
                }
            )
        },
        containerColor = DarkBackground,
        modifier = modifier
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = AppShapes.medium,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(AppSpacing.md)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = collectionName,
                                style = AppTypography.Title.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            if (isSmartCollection) {
                                AniStatusBadge(
                                    label = "Dynamic",
                                    color = PrimaryIndigo
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(AppSpacing.xs))

                        Text(
                            text = collectionDescription,
                            style = AppTypography.Body,
                            color = TextSecondary
                        )

                        if (isSmartCollection && smartCriteriaExplanation != null) {
                            Spacer(modifier = Modifier.height(AppSpacing.sm))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkBackground, AppShapes.small)
                                    .padding(AppSpacing.sm)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = PrimaryIndigo,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(AppSpacing.xs))
                                Text(
                                    text = smartCriteriaExplanation,
                                    style = AppTypography.Caption,
                                    color = TextMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(AppSpacing.md))

                        // Progress & Stats
                        val totalDownloaded = items.sumOf { it.downloadedEpisodes }
                        val totalEps = items.sumOf { it.totalEpisodes }
                        val fraction = if (totalEps > 0) totalDownloaded.toFloat() / totalEps.toFloat() else 0f

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${items.size} Series • $totalDownloaded/$totalEps Episodes",
                                style = AppTypography.Caption,
                                color = TextSecondary
                            )
                            Text(
                                text = "${(fraction * 100).toInt()}% Downloaded",
                                style = AppTypography.Caption,
                                color = if (fraction >= 1f) AppSemanticColors.Success else PrimaryIndigo
                            )
                        }

                        Spacer(modifier = Modifier.height(AppSpacing.xs))

                        LinearProgressIndicator(
                            progress = { fraction },
                            color = PrimaryIndigo,
                            trackColor = DarkBackground,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                        )

                        Spacer(modifier = Modifier.height(AppSpacing.md))

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                        ) {
                            Button(
                                onClick = onPlayCollection,
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                                shape = AppShapes.small,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Play All", style = AppTypography.Caption)
                            }

                            OutlinedButton(
                                onClick = onDownloadMissing,
                                shape = AppShapes.small,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Download Missing", style = AppTypography.Caption, color = PrimaryIndigo)
                            }
                        }
                    }
                }
            }

            if (items.isEmpty()) {
                item {
                    AniEmptyState(
                        title = "No anime in this collection",
                        description = "Add anime series from Anime Details to organize them here.",
                        icon = Icons.Default.FolderSpecial
                    )
                }
            } else {
                items(items, key = { it.animeId }) { anime ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = AppShapes.medium,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAnimeClick(anime.animeId) }
                    ) {
                        Row(
                            modifier = Modifier.padding(AppSpacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = anime.title,
                                    style = AppTypography.Subtitle.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${anime.downloadedEpisodes}/${anime.totalEpisodes} Episodes • ${anime.statusText}",
                                    style = AppTypography.Caption,
                                    color = TextSecondary
                                )
                            }
                            IconButton(onClick = {
                                items = items.filter { it.animeId != anime.animeId }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove from collection",
                                    tint = TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
