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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aniflow.core.ui.model.ReleaseUiModel
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

enum class ReleaseCardDensity {
    Comfortable,
    Compact
}

/**
 * ReleaseCard (Sections 36, 37, 38, 122, 123, 151, 152).
 * Adaptive release card supporting Comfortable, Compact, Multi-select, and Preferred states.
 */
@Composable
fun ReleaseCard(
    release: ReleaseUiModel,
    density: ReleaseCardDensity = ReleaseCardDensity.Comfortable,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onSelectToggle: () -> Unit = {},
    onClick: () -> Unit = {},
    onDownloadClick: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val borderColor = when {
        isSelected -> PrimaryIndigo
        release.isPreferred -> AppSemanticColors.Accent.copy(alpha = 0.8f)
        else -> DarkCardBorder
    }

    val containerColor = if (isSelected) {
        PrimaryIndigo.copy(alpha = 0.12f)
    } else {
        DarkSurface
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = AppShapes.medium,
        border = BorderStroke(if (isSelected || release.isPreferred) 1.5.dp else 1.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                if (isSelectionMode) onSelectToggle() else onClick()
            }
    ) {
        if (density == ReleaseCardDensity.Compact) {
            CompactReleaseContent(
                release = release,
                isSelected = isSelected,
                isSelectionMode = isSelectionMode,
                onSelectToggle = onSelectToggle,
                onDownloadClick = onDownloadClick
            )
        } else {
            ComfortableReleaseContent(
                release = release,
                isSelected = isSelected,
                isSelectionMode = isSelectionMode,
                onSelectToggle = onSelectToggle,
                onDownloadClick = onDownloadClick,
                onMoreClick = onMoreClick
            )
        }
    }
}

@Composable
private fun ComfortableReleaseContent(
    release: ReleaseUiModel,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onSelectToggle: () -> Unit,
    onDownloadClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Column(modifier = Modifier.padding(AppSpacing.md)) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isSelectionMode) {
                IconButton(
                    onClick = onSelectToggle,
                    modifier = Modifier.size(24.dp).padding(end = AppSpacing.xs)
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircleOutline,
                        contentDescription = "Select",
                        tint = if (isSelected) PrimaryIndigo else TextSecondary
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                if (release.isPreferred) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = AppSpacing.xxs)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = AppSemanticColors.Warning,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "SMART PREFERRED",
                            color = AppSemanticColors.Warning,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = release.title,
                    style = AppTypography.title,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onMoreClick, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(AppSpacing.sm))

        // Metadata badges row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
            modifier = Modifier.fillMaxWidth()
        ) {
            QualityBadge(text = release.resolution)
            QualityBadge(text = release.codec, color = AppSemanticColors.Success)
            if (release.isTrusted) {
                QualityBadge(text = "Trusted", color = AppSemanticColors.Success)
            }
            Text(
                text = "• ${release.uploader}",
                style = AppTypography.caption,
                color = TextSecondary,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.height(AppSpacing.sm))

        // Stats row: Size, Seeds, Leechers, Date, Download CTA
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
                Text(
                    text = release.sizeFormatted,
                    style = AppTypography.numericSize,
                    color = TextPrimary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Seeders",
                        tint = AppSemanticColors.Success,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${release.seeders}",
                        style = AppTypography.numericSpeed,
                        color = AppSemanticColors.Success
                    )
                }
                Text(
                    text = release.publishedDateFormatted,
                    style = AppTypography.caption,
                    color = TextMuted
                )
            }

            IconButton(
                onClick = onDownloadClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Download Release",
                    tint = PrimaryIndigo
                )
            }
        }
    }
}

@Composable
private fun CompactReleaseContent(
    release: ReleaseUiModel,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onSelectToggle: () -> Unit,
    onDownloadClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm)
    ) {
        if (isSelectionMode) {
            IconButton(
                onClick = onSelectToggle,
                modifier = Modifier.size(24.dp).padding(end = AppSpacing.xs)
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircleOutline,
                    contentDescription = "Select",
                    tint = if (isSelected) PrimaryIndigo else TextSecondary
                )
            }
        }

        Column(modifier = Modifier.weight(1f).padding(end = AppSpacing.sm)) {
            Text(
                text = release.title,
                style = AppTypography.body,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                Text(
                    text = "${release.resolution} • ${release.sizeFormatted} • ${release.uploader}",
                    style = AppTypography.caption,
                    color = TextSecondary
                )
                Text(
                    text = "▲ ${release.seeders}",
                    style = AppTypography.numericSpeed,
                    color = AppSemanticColors.Success
                )
            }
        }

        IconButton(onClick = onDownloadClick, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "Download",
                tint = PrimaryIndigo,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
