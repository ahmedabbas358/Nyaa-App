package com.aniflow.feature.downloads.ui

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.aniflow.feature.downloads.model.DownloadEngineBadge
import com.aniflow.feature.downloads.model.DownloadPriorityUi
import com.aniflow.feature.downloads.model.DownloadStateUi
import com.aniflow.feature.downloads.model.DownloadTaskUiModel

/**
 * DownloadTaskCard (Sections 6, 7, 8, 9, 10, 11, 12, 70, 71, 97).
 * High-density, professional FDM/1DM-style task card with state-specific controls and accessible semantics.
 */
@Composable
fun DownloadTaskCard(
    task: DownloadTaskUiModel,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit,
    onToggleSelect: (Boolean) -> Unit = {},
    onPause: () -> Unit = {},
    onResume: () -> Unit = {},
    onCancel: () -> Unit = {},
    onRetry: () -> Unit = {},
    onOpen: () -> Unit = {},
    onOpenFolder: () -> Unit = {},
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {},
    onChangePriority: (DownloadPriorityUi) -> Unit = {},
    onRemoveHistory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showOverflowMenu by remember { mutableStateOf(false) }

    val accessibilityDesc = "${task.title}, ${task.state.displayName}, ${task.progressPercent} percent, ${task.speedFormatted}"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { contentDescription = accessibilityDesc },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(AppShapes.sm),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (task.isSelectedForBulk) PrimaryIndigo else DarkCardBorder
        )
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            // Header Row: Checkbox / Engine Badge / Priority / Title / More Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isSelectionMode) {
                        Checkbox(
                            checked = task.isSelectedForBulk,
                            onCheckedChange = onToggleSelect,
                            colors = CheckboxDefaults.colors(
                                checkedColor = PrimaryIndigo,
                                uncheckedColor = TextMuted
                            ),
                            modifier = Modifier.padding(end = AppSpacing.xs)
                        )
                    }

                    // Engine Badge (Section 70)
                    EngineBadgeChip(badge = task.engineBadge)
                    Spacer(Modifier.width(6.dp))

                    // Priority Badge if non-normal (Section 29)
                    if (task.priority != DownloadPriorityUi.Normal) {
                        PriorityBadgeChip(priority = task.priority)
                        Spacer(Modifier.width(6.dp))
                    }

                    // Title
                    Column {
                        Text(
                            text = task.title,
                            style = AppTypography.body.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                            color = TextPrimary,
                            maxLines = 1
                        )
                        if (task.animeTitle.isNotBlank() && task.animeTitle != task.title) {
                            Text(
                                text = task.animeTitle,
                                style = AppTypography.caption.copy(fontSize = 11.sp),
                                color = TextMuted,
                                maxLines = 1
                            )
                        }
                    }
                }

                Box {
                    IconButton(onClick = { showOverflowMenu = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More Actions", tint = TextMuted)
                    }

                    // Overflow Context Menu (Section 82, 83)
                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false },
                        modifier = Modifier.background(DarkSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("View Details") },
                            onClick = {
                                showOverflowMenu = false
                                onClick()
                            }
                        )
                        if (task.state.isTerminal) {
                            DropdownMenuItem(
                                text = { Text("Open File Location") },
                                onClick = {
                                    showOverflowMenu = false
                                    onOpenFolder()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Remove from List", color = AppSemanticColors.Error) },
                                onClick = {
                                    showOverflowMenu = false
                                    onRemoveHistory()
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Set Priority: High") },
                                onClick = {
                                    showOverflowMenu = false
                                    onChangePriority(DownloadPriorityUi.High)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Set Priority: Low") },
                                onClick = {
                                    showOverflowMenu = false
                                    onChangePriority(DownloadPriorityUi.Low)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Cancel Task", color = AppSemanticColors.Error) },
                                onClick = {
                                    showOverflowMenu = false
                                    onCancel()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(AppSpacing.sm))

            // State-Specific Middle Section
            when (task.state) {
                DownloadStateUi.Downloading, DownloadStateUi.Starting, DownloadStateUi.Retrying, DownloadStateUi.Verifying, DownloadStateUi.Moving -> {
                    ActiveTaskLayout(task = task, onPause = onPause, onCancel = onCancel)
                }
                DownloadStateUi.Queued -> {
                    QueuedTaskLayout(task = task, onPause = onPause, onMoveUp = onMoveUp, onMoveDown = onMoveDown)
                }
                DownloadStateUi.Waiting -> {
                    WaitingTaskLayout(task = task, onPause = onPause, onCancel = onCancel)
                }
                DownloadStateUi.Paused -> {
                    PausedTaskLayout(task = task, onResume = onResume, onCancel = onCancel)
                }
                DownloadStateUi.Completed -> {
                    CompletedTaskLayout(task = task, onOpen = onOpen, onOpenFolder = onOpenFolder)
                }
                DownloadStateUi.Failed -> {
                    FailedTaskLayout(task = task, onRetry = onRetry, onCancel = onCancel)
                }
                DownloadStateUi.Cancelled -> {
                    CancelledTaskLayout(task = task, onRetry = onRetry, onRemove = onRemoveHistory)
                }
            }
        }
    }
}

@Composable
private fun ActiveTaskLayout(
    task: DownloadTaskUiModel,
    onPause: () -> Unit,
    onCancel: () -> Unit
) {
    Column {
        LinearProgressIndicator(
            progress = { (task.progressPercent.toFloat() / 100f).coerceIn(0f, 1f) },
            color = PrimaryIndigo,
            trackColor = DarkCardBorder,
            strokeCap = StrokeCap.Round,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
        )
        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${task.downloadedBytesFormatted} / ${task.totalBytesFormatted} (${task.progressPercent}%)",
                    style = AppTypography.bodySecondary.copy(fontSize = 12.sp),
                    color = TextPrimary
                )
                Text(
                    text = "${task.speedFormatted} • ETA ${task.etaFormatted}",
                    style = AppTypography.caption.copy(fontSize = 11.sp),
                    color = AppSemanticColors.Info
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onPause, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Pause, contentDescription = "Pause", tint = PrimaryIndigo)
                }
                IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel", tint = TextMuted)
                }
            }
        }
    }
}

@Composable
private fun QueuedTaskLayout(
    task: DownloadTaskUiModel,
    onPause: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Position #${task.queuePosition ?: 1} in Queue",
                style = AppTypography.body.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                color = TextSecondary
            )
            Text(
                text = "Size: ${task.totalBytesFormatted} • Priority: ${task.priority.displayName}",
                style = AppTypography.caption,
                color = TextMuted
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            IconButton(onClick = onMoveUp, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", tint = TextMuted, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onMoveDown, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", tint = TextMuted, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onPause, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Pause, contentDescription = "Pause", tint = TextMuted, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun WaitingTaskLayout(
    task: DownloadTaskUiModel,
    onPause: () -> Unit,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = AppSemanticColors.Warning, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Column {
                Text(
                    text = task.waitingReason?.displayName ?: "Waiting for resources",
                    style = AppTypography.bodySecondary.copy(fontSize = 12.sp),
                    color = AppSemanticColors.Warning
                )
                Text("Size: ${task.totalBytesFormatted}", style = AppTypography.caption, color = TextMuted)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onPause, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Pause, contentDescription = "Pause", tint = TextMuted)
            }
            IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = TextMuted)
            }
        }
    }
}

@Composable
private fun PausedTaskLayout(
    task: DownloadTaskUiModel,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    Column {
        LinearProgressIndicator(
            progress = { (task.progressPercent.toFloat() / 100f).coerceIn(0f, 1f) },
            color = TextMuted,
            trackColor = DarkCardBorder,
            strokeCap = StrokeCap.Round,
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
        )
        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Paused • ${task.downloadedBytesFormatted} / ${task.totalBytesFormatted} (${task.progressPercent}%)",
                style = AppTypography.caption,
                color = TextMuted
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onResume, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = PrimaryIndigo)
                }
                IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel", tint = TextMuted)
                }
            }
        }
    }
}

@Composable
private fun CompletedTaskLayout(
    task: DownloadTaskUiModel,
    onOpen: () -> Unit,
    onOpenFolder: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AppSemanticColors.Success, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Column {
                Text(
                    text = "Completed • ${task.totalBytesFormatted}",
                    style = AppTypography.bodySecondary.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    color = AppSemanticColors.Success
                )
                Text(
                    text = "Saved in: ${task.destinationPath}",
                    style = AppTypography.caption,
                    color = TextMuted,
                    maxLines = 1
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onOpen, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.OpenInNew, contentDescription = "Open", tint = PrimaryIndigo)
            }
            IconButton(onClick = onOpenFolder, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Folder, contentDescription = "Folder", tint = TextMuted)
            }
        }
    }
}

@Composable
private fun FailedTaskLayout(
    task: DownloadTaskUiModel,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = AppSemanticColors.Error, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Column {
                Text(
                    text = "Failed: ${task.errorMessage ?: "Unknown error"}",
                    style = AppTypography.bodySecondary.copy(fontSize = 12.sp),
                    color = AppSemanticColors.Error,
                    maxLines = 1
                )
                Text("Click to view details or retry", style = AppTypography.caption, color = TextMuted)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onRetry, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = PrimaryIndigo)
            }
            IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = TextMuted)
            }
        }
    }
}

@Composable
private fun CancelledTaskLayout(
    task: DownloadTaskUiModel,
    onRetry: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Cancelled by user", style = AppTypography.caption, color = TextMuted)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onRetry, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = "Restart", tint = TextMuted)
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Remove", tint = TextMuted)
            }
        }
    }
}

@Composable
private fun EngineBadgeChip(badge: DownloadEngineBadge) {
    val color = if (badge == DownloadEngineBadge.Torrent) PrimaryIndigo else AppSemanticColors.Info
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        Text(badge.displayName, style = AppTypography.caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = color)
    }
}

@Composable
private fun PriorityBadgeChip(priority: DownloadPriorityUi) {
    val color = when (priority) {
        DownloadPriorityUi.Highest -> AppSemanticColors.Error
        DownloadPriorityUi.High -> AppSemanticColors.Warning
        else -> TextMuted
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        Text(priority.displayName, style = AppTypography.caption.copy(fontSize = 10.sp), color = color)
    }
}
