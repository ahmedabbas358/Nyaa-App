package com.aniflow.feature.settings

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.aniflow.domain.controlplane.models.AutomationAuditLog
import com.aniflow.domain.controlplane.models.AutomationSafetyLevel

/**
 * AutomationDashboardScreen (Sections 21, 23, 51).
 * Displays active automation status, dry-run simulation triggers, and an explainable audit trail.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationDashboardScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var hasRunSimulation by remember { mutableStateOf(false) }

    val recentLogs = listOf(
        AutomationAuditLog(
            id = "log-1",
            triggerDescription = "NewReleaseDetected(One Piece)",
            winningRuleName = "Auto-Download One Piece 1080p",
            actionsTaken = listOf("QueueDownload", "ApplyProfile: 1080p HEVC"),
            outcome = "PLAN_GENERATED",
            wasBlockedBySafety = false,
            explanation = "Matched preferred uploader Erai-raws"
        ),
        AutomationAuditLog(
            id = "log-2",
            triggerDescription = "ScheduledInterval(30 min)",
            winningRuleName = null,
            actionsTaken = emptyList(),
            outcome = "NO_ACTION",
            wasBlockedBySafety = false,
            explanation = "No missing episodes found"
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Automation Control Plane", style = AppTypography.headline, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground,
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = AppSpacing.md,
                end = AppSpacing.md,
                bottom = AppSpacing.xxxl,
                top = AppSpacing.sm
            ),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 1. Status Overview Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = AppShapes.medium,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(AppSpacing.md)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(AppSpacing.xs))
                            Text("Automation Health", style = AppTypography.title, color = TextPrimary)
                        }
                        Spacer(modifier = Modifier.height(AppSpacing.sm))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Active Rules: 4", style = AppTypography.bodySecondary)
                            Text("Executed Today: 12", style = AppTypography.bodySecondary)
                            Text("Blocked: 0", style = AppTypography.bodySecondary)
                        }
                        Spacer(modifier = Modifier.height(AppSpacing.xs))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = AppSemanticColors.Success, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Safety Gate: Require Confirmation (>10GB)", style = AppTypography.caption, color = AppSemanticColors.Success)
                        }
                    }
                }
            }

            // 2. Simulation Runner CTA
            item {
                Button(
                    onClick = { hasRunSimulation = true },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    shape = AppShapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Run Dry-Run Simulation")
                }
            }

            // 3. Simulation Result (Section 23)
            if (hasRunSimulation) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PrimaryIndigo.copy(alpha = 0.1f)),
                        shape = AppShapes.medium,
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryIndigo),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(AppSpacing.md)) {
                            Text("Simulation Result", style = AppTypography.title, color = PrimaryIndigo, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            Spacer(modifier = Modifier.height(AppSpacing.xs))
                            Text("12 candidate releases evaluated", style = AppTypography.bodySecondary)
                            Text("• 8 eligible releases matched", style = AppTypography.caption, color = AppSemanticColors.Success)
                            Text("• 4 rejected (Seeders < 5)", style = AppTypography.caption, color = AppSemanticColors.Error)
                            Text("Selected: 5 releases (~6.8 GB)", style = AppTypography.body, color = TextPrimary)
                            Text("Destination: Anime/One Piece/Season 01", style = AppTypography.caption, color = TextMuted)
                        }
                    }
                }
            }

            // 4. Audit Log Trail (Section 42, 43)
            item {
                Text("Automation Audit Trail", style = AppTypography.headline, color = TextPrimary)
            }

            items(recentLogs) { log ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = AppShapes.medium,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(AppSpacing.md)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(log.triggerDescription, style = AppTypography.title.copy(fontSize = 13.sp), color = TextPrimary)
                            Text(log.outcome, style = AppTypography.caption, color = if (log.wasBlockedBySafety) AppSemanticColors.Error else AppSemanticColors.Success)
                        }
                        log.winningRuleName?.let {
                            Text("Winning Rule: $it", style = AppTypography.caption, color = PrimaryIndigo)
                        }
                        Text(log.explanation, style = AppTypography.bodySecondary, color = TextMuted)
                    }
                }
            }
        }
    }
}
