package com.aniflow.feature.search.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.aniflow.core.ui.theme.DarkBackground
import com.aniflow.core.ui.theme.DarkCardBorder
import com.aniflow.core.ui.theme.DarkSurface
import com.aniflow.core.ui.theme.PrimaryIndigo
import com.aniflow.core.ui.theme.TextMuted
import com.aniflow.core.ui.theme.TextPrimary
import com.aniflow.core.ui.theme.TextSecondary
import com.aniflow.domain.search.model.ReleaseComparisonItem
import com.aniflow.domain.search.model.ReleaseComparisonResult

/**
 * ReleaseComparisonSheet (Section 32).
 * Side-by-side release comparison table highlighting differences without bias.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReleaseComparisonSheet(
    comparisonResult: ReleaseComparisonResult,
    onSelectForDownload: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.lg)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CompareArrows, contentDescription = null, tint = PrimaryIndigo)
                    Spacer(Modifier.width(AppSpacing.sm))
                    Text(
                        text = "Compare Releases (${comparisonResult.items.size})",
                        style = AppTypography.headline,
                        color = TextPrimary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            Spacer(Modifier.height(AppSpacing.md))

            // Comparison Matrix with horizontal scroll
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
                comparisonResult.items.forEach { release ->
                    ComparisonColumn(
                        release = release,
                        differences = comparisonResult.differences,
                        onSelectForDownload = { onSelectForDownload(release.releaseId.value) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ComparisonColumn(
    release: ReleaseComparisonItem,
    differences: List<com.aniflow.domain.search.model.ComparisonDifference>,
    onSelectForDownload: () -> Unit
) {
    Card(
        modifier = Modifier.width(260.dp),
        colors = CardDefaults.cardColors(containerColor = DarkBackground),
        shape = RoundedCornerShape(AppShapes.sm),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            // Title & Uploader
            Text(
                text = release.title,
                style = AppTypography.body.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                color = TextPrimary,
                maxLines = 2
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "By ${release.uploader}",
                style = AppTypography.caption,
                color = PrimaryIndigo
            )

            Spacer(Modifier.height(AppSpacing.md))

            // Attribute Rows
            differences.forEach { diff ->
                val value = diff.valuesByReleaseId[release.releaseId.value] ?: "N/A"
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(diff.attributeName, style = AppTypography.caption.copy(fontSize = 10.sp), color = TextMuted)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = value,
                            style = AppTypography.body.copy(fontSize = 12.sp, fontWeight = if (diff.hasDifference) FontWeight.Bold else FontWeight.Normal),
                            color = if (diff.hasDifference) AppSemanticColors.Info else TextSecondary
                        )
                        if (diff.hasDifference) {
                            Spacer(Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(AppSemanticColors.Info.copy(alpha = 0.15f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text("Diff", style = AppTypography.caption.copy(fontSize = 9.sp), color = AppSemanticColors.Info)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(AppSpacing.md))

            Button(
                onClick = onSelectForDownload,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                shape = RoundedCornerShape(AppShapes.sm)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Select Release", style = AppTypography.caption)
            }
        }
    }
}
