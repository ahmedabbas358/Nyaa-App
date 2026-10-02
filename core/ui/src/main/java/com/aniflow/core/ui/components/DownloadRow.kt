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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aniflow.core.ui.model.DownloadStatusUi
import com.aniflow.core.ui.model.DownloadUiModel
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

/**
 * DownloadRow (Sections 59, 61, 62, 63, 64).
 * Dense FDM/1DM-style active download row showing progress, smooth speed, ETA, and quick actions.
 */
@Composable
fun DownloadRow(
    download: DownloadUiModel,
    onClick: () -> Unit = {},
    onPauseResumeToggle: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val progressFraction = (download.progressPercent.toFloat() / 100f).coerceIn(0f, 1f)

    val progressColor = when (download.status) {
        DownloadStatusUi.Active -> PrimaryIndigo
        DownloadStatusUi.Completed -> AppSemanticColors.Success
        DownloadStatusUi.Paused -> AppSemanticColors.Neutral
        DownloadStatusUi.Failed -> AppSemanticColors.Error
        DownloadStatusUi.Queued -> AppSemanticColors.Warning
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = AppShapes.medium,
        border = BorderStroke(1.dp, DarkCardBorder),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            // Title and Action Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = AppSpacing.sm)) {
                    Text(
                        text = download.title,
                        style = AppTypography.title,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${download.engineType} • ${download.status.name}",
                        style = AppTypography.caption,
                        color = TextSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (download.status) {
                        DownloadStatusUi.Active -> {
                            IconButton(onClick = onPauseResumeToggle, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Pause, contentDescription = "Pause", tint = TextPrimary)
                            }
                        }
                        DownloadStatusUi.Paused, DownloadStatusUi.Queued -> {
                            IconButton(onClick = onPauseResumeToggle, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = PrimaryIndigo)
                            }
                        }
                        DownloadStatusUi.Failed -> {
                            IconButton(onClick = onPauseResumeToggle, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = AppSemanticColors.Error)
                            }
                        }
                        DownloadStatusUi.Completed -> {}
                    }

                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onMoreClick, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            // Smooth linear progress bar
            LinearProgressIndicator(
                progress = { progressFraction },
                color = progressColor,
                trackColor = DarkCardBorder,
                strokeCap = StrokeCap.Round,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            )

            Spacer(modifier = Modifier.height(AppSpacing.xs))

            // Metrics: Percent, Downloaded/Total, Speed, ETA
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${download.progressPercent}% • ${download.downloadedBytesFormatted} / ${download.totalBytesFormatted}",
                    style = AppTypography.numericSize,
                    color = TextSecondary
                )

                if (download.status == DownloadStatusUi.Active) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = download.speedFormatted,
                            style = AppTypography.numericSpeed,
                            color = AppSemanticColors.Info
                        )
                        Spacer(modifier = Modifier.width(AppSpacing.xs))
                        Text(
                            text = "ETA ${download.etaFormatted}",
                            style = AppTypography.numericEta,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}
