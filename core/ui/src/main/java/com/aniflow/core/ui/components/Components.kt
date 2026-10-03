package com.aniflow.core.ui.components

import com.aniflow.domain.state.DownloadState

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aniflow.core.ui.theme.Badge1080p
import com.aniflow.core.ui.theme.Badge4K
import com.aniflow.core.ui.theme.Badge720p
import com.aniflow.core.ui.theme.BadgeHevc
import com.aniflow.core.ui.theme.BadgeTrusted
import com.aniflow.core.ui.theme.DarkCardBorder
import com.aniflow.core.ui.theme.DarkSurface
import com.aniflow.core.ui.theme.ErrorRose
import com.aniflow.core.ui.theme.InfoBlue
import com.aniflow.core.ui.theme.PrimaryIndigo
import com.aniflow.core.ui.theme.SuccessGreen
import com.aniflow.core.ui.theme.TextMuted
import com.aniflow.core.ui.theme.TextSecondary
import com.aniflow.core.ui.theme.WarningAmber
import com.aniflow.domain.entity.DownloadTask
import com.aniflow.domain.entity.Release
import com.aniflow.domain.enums.DownloadState
import com.aniflow.domain.enums.EpisodeStatus

@Composable
fun QualityBadge(
    text: String,
    color: Color = Badge1080p,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun StatusBadge(
    status: EpisodeStatus,
    modifier: Modifier = Modifier
) {
    val (color, label) = when (status) {
        EpisodeStatus.Downloaded -> SuccessGreen to "Downloaded"
        EpisodeStatus.Downloading -> InfoBlue to "Downloading"
        EpisodeStatus.Queued -> WarningAmber to "Queued"
        EpisodeStatus.Missing -> Color(0xFF94A3B8) to "Missing"
        EpisodeStatus.Failed -> ErrorRose to "Failed"
        EpisodeStatus.Available -> PrimaryIndigo to "Available"
        EpisodeStatus.Selected -> PrimaryIndigo to "Selected"
        EpisodeStatus.Duplicate -> Color(0xFF64748B) to "Duplicate"
    }

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

/**
 * Release Card conforming to Section 63 (Search Result UI).
 */
@Composable
fun ReleaseCard(
    release: Release,
    onDownloadClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onSelectToggle: ((Boolean) -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) PrimaryIndigo else DarkCardBorder
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Title & Trust Badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (release.trusted) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Trusted",
                        tint = BadgeTrusted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = release.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Metadata Badges (1080p, HEVC, Dual Audio, Group)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                release.resolution?.let { res ->
                    val color = when (res.height) {
                        2160 -> Badge4K
                        1080 -> Badge1080p
                        else -> Badge720p
                    }
                    QualityBadge(text = res.standardTag, color = color)
                }

                release.codec?.let { cdc ->
                    QualityBadge(text = cdc.standardTag, color = BadgeHevc)
                }

                if (release.audio?.isDualAudio == true) {
                    QualityBadge(text = "Dual Audio", color = InfoBlue)
                }

                release.releaseGroup?.let { grp ->
                    Text(
                        text = grp,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom stats & Action
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Size, Seeds, Leeches
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = release.size.formatted,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Seeders",
                            tint = SuccessGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${release.seeders}",
                            color = SuccessGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Leechers",
                            tint = ErrorRose,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${release.leechers}",
                            color = ErrorRose,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Download Button
                IconButton(
                    onClick = onDownloadClick,
                    modifier = Modifier
                        .size(36.dp)
                        .background(PrimaryIndigo, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Download Item adhering to Section 54 (Download Item).
 */
@Composable
fun DownloadItem(
    task: DownloadTask,
    onPauseResume: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Title & Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = task.release.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                StatusBadge(
                    status = when (task.state) {
                        DownloadState.Downloading -> EpisodeStatus.Downloading
                        DownloadState.Completed -> EpisodeStatus.Downloaded
                        DownloadState.Queued -> EpisodeStatus.Queued
                        DownloadState.Failed -> EpisodeStatus.Failed
                        else -> EpisodeStatus.Available
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { task.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = when (task.state) {
                    DownloadState.Completed -> SuccessGreen
                    DownloadState.Failed -> ErrorRose
                    DownloadState.Paused -> WarningAmber
                    else -> PrimaryIndigo
                },
                trackColor = Color(0xFF334155)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Stats row (Downloaded/Total, Speed, ETA)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                val downloadedFormatted = com.aniflow.domain.valueobject.FileSize.fromBytes(task.downloadedBytes).formatted
                val totalFormatted = com.aniflow.domain.valueobject.FileSize.fromBytes(task.totalBytes).formatted
                val speedFormatted = "${com.aniflow.domain.valueobject.FileSize.fromBytes(task.speedBps).formatted}/s"

                Text(
                    text = "$downloadedFormatted / $totalFormatted • $speedFormatted",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onPauseResume,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (task.state == DownloadState.Downloading) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (task.state == DownloadState.Downloading) "Pause" else "Resume",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = ErrorRose
                        )
                    }
                }
            }
        }
    }
}

/**
 * Empty State conforming to Section 152.
 */
@Composable
fun EmptyState(
    title: String,
    message: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp)
    ) {
        Icon(
            imageVector = Icons.Default.HourglassEmpty,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (actionLabel != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
            ) {
                Text(text = actionLabel, color = Color.White)
            }
        }
    }
}
