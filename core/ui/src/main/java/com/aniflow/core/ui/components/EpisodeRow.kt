package com.aniflow.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aniflow.core.ui.model.EpisodeUiModel
import com.aniflow.core.ui.theme.AppSemanticColors
import com.aniflow.core.ui.theme.AppShapes
import com.aniflow.core.ui.theme.AppSpacing
import com.aniflow.core.ui.theme.AppTypography
import com.aniflow.core.ui.theme.DarkCardBorder
import com.aniflow.core.ui.theme.DarkSurface
import com.aniflow.core.ui.theme.PrimaryIndigo
import com.aniflow.core.ui.theme.TextMuted
import com.aniflow.core.ui.theme.TextPrimary
import com.aniflow.core.ui.theme.TextSecondary
import com.aniflow.core.ui.util.UiFormatters

/**
 * EpisodeRow (Sections 44, 45, 46, 47, 48).
 * Displays episode number, status badge, preferred release summary, and "Why selected?" prompt.
 */
@Composable
fun EpisodeRow(
    episode: EpisodeUiModel,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onSelectToggle: () -> Unit = {},
    onClick: () -> Unit = {},
    onExplainClick: () -> Unit = {},
    onDownloadClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val epFormatted = UiFormatters.formatEpisodeNumber(episode.episodeNumber)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PrimaryIndigo.copy(alpha = 0.12f) else DarkSurface
        ),
        shape = AppShapes.medium,
        border = BorderStroke(1.dp, if (isSelected) PrimaryIndigo else DarkCardBorder),
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                if (isSelectionMode) onSelectToggle() else onClick()
            }
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isSelectionMode) {
                        IconButton(
                            onClick = onSelectToggle,
                            modifier = Modifier.size(24.dp).padding(end = AppSpacing.xs)
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircleOutline,
                                contentDescription = "Select Episode",
                                tint = if (isSelected) PrimaryIndigo else TextSecondary
                            )
                        }
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Episode $epFormatted",
                                style = AppTypography.title,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(AppSpacing.xs))
                            Surface(
                                color = episode.statusColor.copy(alpha = 0.15f),
                                shape = AppShapes.small
                            ) {
                                Text(
                                    text = episode.statusText,
                                    color = episode.statusColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (episode.title.isNotBlank() && !episode.title.startsWith("Episode")) {
                            Text(
                                text = episode.title,
                                style = AppTypography.caption,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                IconButton(onClick = onDownloadClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download Episode",
                        tint = PrimaryIndigo
                    )
                }
            }

            // Preferred release & "Why selected?" prompt
            if (episode.preferredRelease != null) {
                Spacer(modifier = Modifier.height(AppSpacing.xs))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Preferred: ${episode.preferredRelease.uploader} • ${episode.preferredRelease.resolution} • ${episode.preferredRelease.sizeFormatted}",
                            style = AppTypography.caption,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (episode.whySelectedReasons.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { onExplainClick() }
                                .padding(start = AppSpacing.xs)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Why selected?",
                                tint = AppSemanticColors.Accent,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "Why?",
                                color = AppSemanticColors.Accent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
