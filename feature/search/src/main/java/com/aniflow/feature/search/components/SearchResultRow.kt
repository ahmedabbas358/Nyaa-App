package com.aniflow.feature.search.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aniflow.core.ui.theme.AppSemanticColors
import com.aniflow.core.ui.theme.AppShapes
import com.aniflow.core.ui.theme.AppSpacing
import com.aniflow.core.ui.theme.AppTypography
import com.aniflow.core.ui.theme.DarkCardBorder
import com.aniflow.core.ui.theme.DarkSurface
import com.aniflow.core.ui.theme.DarkSurfaceVariant
import com.aniflow.core.ui.theme.PrimaryIndigo
import com.aniflow.core.ui.theme.TextMuted
import com.aniflow.core.ui.theme.TextPrimary
import com.aniflow.core.ui.theme.TextSecondary
import com.aniflow.domain.search.model.SearchResultItem
import com.aniflow.domain.search.model.SearchSource

/**
 * SearchResultRow (Section 16, 21, 30).
 * Polymorphic card rendering for Anime, Episode, Release, Library, and Download results.
 */
@Composable
fun SearchResultRow(
    item: SearchResultItem,
    isSelectedForCompare: Boolean = false,
    showCompareCheckbox: Boolean = false,
    onToggleCompare: (Boolean) -> Unit = {},
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (item is SearchResultItem.GroupedAnimeResult) {
        GroupedAnimeCard(
            item = item,
            onClick = onClick,
            modifier = modifier
        )
        return
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(AppShapes.sm),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelectedForCompare) PrimaryIndigo else DarkCardBorder
        )
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    if (showCompareCheckbox && item is SearchResultItem.ReleaseResult) {
                        Checkbox(
                            checked = isSelectedForCompare,
                            onCheckedChange = onToggleCompare,
                            colors = CheckboxDefaults.colors(checkedColor = PrimaryIndigo),
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }

                    // Entity Icon
                    val icon = when (item) {
                        is SearchResultItem.AnimeResult -> Icons.Default.Movie
                        is SearchResultItem.EpisodeResult -> Icons.Default.PlayCircle
                        is SearchResultItem.ReleaseResult -> Icons.Default.Download
                        is SearchResultItem.LibraryResult -> Icons.Default.Folder
                        is SearchResultItem.DownloadResult -> Icons.Default.Speed
                        else -> Icons.Default.Movie
                    }
                    Icon(icon, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(AppSpacing.sm))

                    Column {
                        Text(
                            text = item.title,
                            style = AppTypography.body.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            color = TextPrimary,
                            maxLines = 1
                        )
                        ItemSubtitle(item)
                    }
                }

                // Source Badge (Local, Provider, Hybrid)
                SearchSourceBadge(item.source)
            }

            // Explainability reasons snippet (Section 21)
            if (item.matchedReasons.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    item.matchedReasons.take(2).forEach { reason ->
                        ReasonChip(reason)
                    }
                }
            }
        }
    }
}

/**
 * Anime & Season card with warm atmospheric design,
 * season badge, episode counter, uploader pills, and batch selector.
 */
@Composable
fun GroupedAnimeCard(
    item: SearchResultItem.GroupedAnimeResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column {
            // Warm atmospheric banner with subtle gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .background(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            listOf(
                                Color(0xFF1E1A14),   // Warm dark
                                Color(0xFF2A2218),   // Deep amber tint
                                Color(0xFF1A1B20)    // Cool dark slate
                            )
                        )
                    )
            ) {
                // Season tag — top-left, integrated feel
                Surface(
                    color = PrimaryIndigo.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(bottomEnd = 8.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = "S${item.seasonNumber}",
                        style = AppTypography.caption.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = PrimaryIndigo,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                // Episode count — bottom-right, glass effect
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${item.totalEpisodes} EP",
                        style = AppTypography.caption.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.3.sp
                        ),
                        color = Color(0xFFE8DFD3)
                    )
                }

                // Title overlay in banner
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, bottom = 10.dp, end = 80.dp)
                ) {
                    Text(
                        text = item.title,
                        style = AppTypography.body.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            letterSpacing = (-0.2).sp
                        ),
                        color = Color(0xFFF0EBE3),
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            // Body section
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                // Metadata row: size, release count
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = item.formattedSize,
                        style = AppTypography.caption.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        ),
                        color = TextSecondary
                    )
                    Text(
                        text = " · ",
                        style = AppTypography.caption,
                        color = TextMuted
                    )
                    Text(
                        text = "${item.releases.size} releases",
                        style = AppTypography.caption.copy(fontSize = 11.sp),
                        color = TextMuted
                    )

                    Spacer(Modifier.weight(1f))

                    // Uploader pills — compact
                    if (item.availableUploaders.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            item.availableUploaders.take(2).forEach { uploader ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(DarkSurfaceVariant)
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = uploader,
                                        style = AppTypography.caption.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = TextSecondary
                                    )
                                }
                            }
                            if (item.availableUploaders.size > 2) {
                                Text(
                                    text = "+${item.availableUploaders.size - 2}",
                                    style = AppTypography.caption.copy(fontSize = 9.sp),
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Download CTA — warm accent, not too loud
                androidx.compose.material3.Button(
                    onClick = onClick,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = PrimaryIndigo.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.4f)),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = PrimaryIndigo
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Select Episodes (${item.totalEpisodes})",
                        style = AppTypography.caption.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryIndigo,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ItemSubtitle(item: SearchResultItem) {
    when (item) {
        is SearchResultItem.AnimeResult -> {
            Text(
                text = "${item.coveredEpisodes}/${item.totalEpisodes} episodes available",
                style = AppTypography.caption,
                color = TextSecondary
            )
        }
        is SearchResultItem.EpisodeResult -> {
            Text(
                text = "Episode ${item.episodeNumber} • ${if (item.isInLibrary) "In Library" else "Missing"}",
                style = AppTypography.caption,
                color = if (item.isInLibrary) AppSemanticColors.Success else TextMuted
            )
        }
        is SearchResultItem.ReleaseResult -> {
            Text(
                text = "${item.resolution} • ${item.codec} • ${item.sizeFormatted} • ${item.seeders} seeds • By ${item.uploader}",
                style = AppTypography.caption,
                color = TextMuted
            )
        }
        is SearchResultItem.LibraryResult -> {
            Text(
                text = "File: ${item.filePath}",
                style = AppTypography.caption,
                color = AppSemanticColors.Success,
                maxLines = 1
            )
        }
        is SearchResultItem.DownloadResult -> {
            Text(
                text = "${item.taskState} (${item.progressPercent}%)",
                style = AppTypography.caption,
                color = AppSemanticColors.Info
            )
        }
        else -> Unit
    }
}

@Composable
private fun SearchSourceBadge(source: SearchSource) {
    val (label, color) = when (source) {
        SearchSource.Local -> "Local" to AppSemanticColors.Success
        SearchSource.Provider -> "Nyaa" to PrimaryIndigo
        SearchSource.Hybrid -> "Synced" to AppSemanticColors.Info
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        Text(label, style = AppTypography.caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = color)
    }
}

@Composable
private fun ReasonChip(reason: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(2.dp))
            .background(DarkCardBorder)
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        Text(reason, style = AppTypography.caption.copy(fontSize = 9.sp), color = TextMuted)
    }
}
