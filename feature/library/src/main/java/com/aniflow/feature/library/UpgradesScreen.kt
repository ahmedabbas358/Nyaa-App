package com.aniflow.feature.library

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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

data class UpgradeOpportunityUiModel(
    val episodeId: String,
    val animeTitle: String,
    val episodeTitle: String,
    val currentQuality: String,
    val currentSize: String,
    val availableQuality: String,
    val availableSize: String,
    val releaseGroup: String,
    val releaseId: String,
    val reason: String
)

/**
 * UpgradesScreen (Section 71).
 * Displays actionable quality upgrades for library episodes based on active download profile.
 * Actions: Review, Upgrade, Ignore.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpgradesScreen(
    onBack: () -> Unit = {},
    onReviewRelease: (String) -> Unit = {},
    onStartUpgrade: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var upgrades by remember { mutableStateOf<List<UpgradeOpportunityUiModel>>(emptyList()) }

    Scaffold(
        topBar = {
            AniAppBar(
                title = "Quality Upgrades",
                subtitle = "Profile recommendations",
                onBack = onBack
            )
        },
        containerColor = DarkBackground,
        modifier = modifier
    ) { padding ->
        if (upgrades.isEmpty()) {
            AniEmptyState(
                title = "No upgrades available",
                description = "All downloaded episodes match your preferred quality profile. New higher-quality releases will appear here automatically.",
                icon = Icons.Default.CheckCircle,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(AppSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = AppShapes.medium,
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(AppSpacing.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = PrimaryIndigo,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(AppSpacing.sm))
                            Column {
                                Text(
                                    text = "${upgrades.size} Upgradeable Episodes",
                                    style = AppTypography.Subtitle.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Releases found with better resolution, modern codec (HEVC/AV1), or dual audio.",
                                    style = AppTypography.Caption,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                items(upgrades, key = { it.episodeId }) { item ->
                    UpgradeCard(
                        item = item,
                        onReview = { onReviewRelease(item.releaseId) },
                        onUpgrade = { onStartUpgrade(item.releaseId) },
                        onIgnore = {
                            upgrades = upgrades.filter { it.episodeId != item.episodeId }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun UpgradeCard(
    item: UpgradeOpportunityUiModel,
    onReview: () -> Unit,
    onUpgrade: () -> Unit,
    onIgnore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = AppShapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            Text(
                text = item.animeTitle,
                style = AppTypography.Caption,
                color = PrimaryIndigo,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.episodeTitle,
                style = AppTypography.Title.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            // Comparison row: Current -> Available
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Current
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Current", style = AppTypography.Overline, color = TextMuted)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.currentQuality,
                        style = AppTypography.Subtitle.copy(fontWeight = FontWeight.Medium),
                        color = TextSecondary
                    )
                    Text(text = item.currentSize, style = AppTypography.Caption, color = TextMuted)
                }

                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = PrimaryIndigo,
                    modifier = Modifier.padding(horizontal = AppSpacing.sm)
                )

                // Available
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Available Upgrade", style = AppTypography.Overline, color = PrimaryIndigo)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.availableQuality,
                        style = AppTypography.Subtitle.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "${item.availableSize} • ${item.releaseGroup}",
                        style = AppTypography.Caption,
                        color = AppSemanticColors.Success
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            Text(
                text = "Reason: ${item.reason}",
                style = AppTypography.Caption,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(AppSpacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                Button(
                    onClick = onUpgrade,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    shape = AppShapes.small,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Upgrade", style = AppTypography.Caption)
                }

                OutlinedButton(
                    onClick = onReview,
                    shape = AppShapes.small,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Review", style = AppTypography.Caption, color = PrimaryIndigo)
                }

                TextButton(
                    onClick = onIgnore,
                    shape = AppShapes.small
                ) {
                    Text("Ignore", style = AppTypography.Caption, color = TextMuted)
                }
            }
        }
    }
}
