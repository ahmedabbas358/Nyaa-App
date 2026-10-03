package com.aniflow.feature.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aniflow.core.ui.components.AniEmptyState
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

data class DownloadPlanUiModel(
    val title: String = "",
    val fileCount: Int = 0,
    val totalSizeBytes: Long = 0L,
    val totalSizeFormatted: String = "0 B",
    val preferredCount: Int = 0,
    val fallbackCount: Int = 0,
    val warningCount: Int = 0,
    val requiredSpaceFormatted: String = "0 B",
    val reservedSpaceFormatted: String = "0 B",
    val safetyMarginFormatted: String = "0 B",
    val availableSpaceFormatted: String = "0 B",
    val remainingSpaceAfterFormatted: String = "0 B",
    val hasSufficientSpace: Boolean = true,
    val networkConstraint: String = "Wi-Fi Only (Unmetered)",
    val concurrencyLimit: Int = 3,
    val destinationPath: String = ""
)

/**
 * DownloadPlanScreen (Sections 146, 147, 148).
 * Final review screen before persisting and enqueuing downloads.
 * Previews storage requirements, safety margins, network rules, and destination paths.
 * Enforces rule: Downloads never start directly from preview without persistence!
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadPlanScreen(
    plan: DownloadPlanUiModel = DownloadPlanUiModel(),
    onBack: () -> Unit = {},
    onChangeStorageClick: () -> Unit = {},
    onStartDownloadsClick: () -> Unit = {},
    onQueueOnlyClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Download Plan Review", style = AppTypography.headline, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        bottomBar = {
            if (plan.fileCount > 0 && plan.title.isNotBlank()) {
                Surface(
                    color = DarkSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppSpacing.md)
                    ) {
                        if (!plan.hasSufficientSpace) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = AppSpacing.sm)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = AppSemanticColors.Error, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Insufficient storage on destination volume",
                                    color = AppSemanticColors.Error,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            androidx.compose.material3.OutlinedButton(
                                onClick = onQueueOnlyClick,
                                shape = AppShapes.pill,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                            ) {
                                Text(
                                    text = "Queue Only",
                                    style = AppTypography.body,
                                    color = TextPrimary
                                )
                            }

                            Button(
                                onClick = onStartDownloadsClick,
                                enabled = plan.hasSufficientSpace,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryIndigo,
                                    disabledContainerColor = DarkCardBorder
                                ),
                                shape = AppShapes.pill,
                                modifier = Modifier
                                    .weight(2f)
                                    .height(50.dp)
                            ) {
                                Text(
                                    text = "Start Now (${plan.totalSizeFormatted})",
                                    style = AppTypography.title,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = DarkBackground,
        modifier = modifier
    ) { paddingValues ->
        if (plan.fileCount == 0 || plan.title.isBlank()) {
            AniEmptyState(
                title = "No download plan",
                description = "Select releases or episodes to generate a persistent download plan.",
                icon = Icons.Default.Folder,
                actionLabel = "Back",
                onActionClick = onBack,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(AppSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
            // 1. Plan Summary Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = AppShapes.medium,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(AppSpacing.md)) {
                    Text(text = plan.title, style = AppTypography.title, color = TextPrimary)
                    Text(
                        text = "${plan.fileCount} items queued for download",
                        style = AppTypography.caption,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.sm))
                    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                        Text("✓ ${plan.preferredCount} preferred", color = AppSemanticColors.Success, fontSize = 12.sp)
                        Text("▲ ${plan.fallbackCount} fallback", color = AppSemanticColors.Warning, fontSize = 12.sp)
                        if (plan.warningCount > 0) {
                            Text("! ${plan.warningCount} warning", color = AppSemanticColors.Error, fontSize = 12.sp)
                        }
                    }
                }
            }

            // 2. Storage Preview Card (Section 54, 108)
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = AppShapes.medium,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(AppSpacing.md)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(AppSpacing.xs))
                        Text("Storage Space Preview", style = AppTypography.title, color = TextPrimary)
                    }
                    Spacer(modifier = Modifier.height(AppSpacing.sm))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Required Space:", style = AppTypography.bodySecondary)
                        Text(plan.requiredSpaceFormatted, style = AppTypography.numericSize, color = TextPrimary)
                    }
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text("Available Space:", style = AppTypography.bodySecondary)
                        Text(plan.availableSpaceFormatted, style = AppTypography.numericSize, color = AppSemanticColors.Success)
                    }
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Remaining After:", style = AppTypography.bodySecondary)
                        Text(plan.remainingSpaceAfterFormatted, style = AppTypography.numericSize, color = TextSecondary)
                    }
                }
            }

            // 3. Destination Folder Preview Card (Section 56)
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = AppShapes.medium,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(AppSpacing.md)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(AppSpacing.sm))
                        Column {
                            Text("Download Target", style = AppTypography.title, color = TextPrimary)
                            Text(plan.destinationPath, style = AppTypography.caption, color = TextSecondary)
                        }
                    }
                    Button(
                        onClick = onChangeStorageClick,
                        colors = ButtonDefaults.buttonColors(containerColor = DarkCardBorder),
                        shape = AppShapes.small
                    ) {
                        Text("Change", fontSize = 12.sp)
                    }
                }
            }

            // 4. Network Preview Card (Section 55)
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = AppShapes.medium,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(AppSpacing.md)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Wifi, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(AppSpacing.xs))
                        Text("Network & Concurrency", style = AppTypography.title, color = TextPrimary)
                    }
                    Spacer(modifier = Modifier.height(AppSpacing.sm))
                    Text("Policy: ${plan.networkConstraint}", style = AppTypography.bodySecondary)
                    Text("Parallel Limit: ${plan.concurrencyLimit} active downloads", style = AppTypography.bodySecondary)
                }
            }
        }
    }
}
}

