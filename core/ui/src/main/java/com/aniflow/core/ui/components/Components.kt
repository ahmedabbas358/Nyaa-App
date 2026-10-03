package com.aniflow.core.ui.components

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
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aniflow.core.ui.model.DownloadUiModel
import com.aniflow.core.ui.model.ReleaseUiModel
import com.aniflow.core.ui.model.toUiModel
import com.aniflow.core.ui.theme.Badge1080p
import com.aniflow.core.ui.theme.DarkCardBorder
import com.aniflow.core.ui.theme.DarkSurface
import com.aniflow.core.ui.theme.ErrorRose
import com.aniflow.core.ui.theme.InfoBlue
import com.aniflow.core.ui.theme.PrimaryIndigo
import com.aniflow.core.ui.theme.SuccessGreen
import com.aniflow.core.ui.theme.TextMuted
import com.aniflow.core.ui.theme.TextSecondary
import com.aniflow.core.ui.theme.WarningAmber
import com.aniflow.domain.model.aggregate.release.Release

/**
 * Episode status enum for presentation layer badges.
 */
enum class EpisodeStatus {
    Downloaded,
    Downloading,
    Queued,
    Missing,
    Failed,
    Available,
    Selected,
    Duplicate
}

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
 * Domain-aggregate ReleaseCard overload delegating directly to presentation ReleaseCard.
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
    ReleaseCard(
        release = release.toUiModel(),
        isSelected = isSelected,
        isSelectionMode = onSelectToggle != null,
        onSelectToggle = { onSelectToggle?.invoke(!isSelected) },
        onClick = onClick,
        onDownloadClick = onDownloadClick,
        modifier = modifier
    )
}

/**
 * Download item adapter delegating directly to DownloadRow.
 */
@Composable
fun DownloadItem(
    download: DownloadUiModel,
    onPauseResume: () -> Unit = {},
    onCancel: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    DownloadRow(
        download = download,
        onClick = {},
        onPauseResumeToggle = onPauseResume,
        onMoreClick = onCancel,
        modifier = modifier
    )
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
