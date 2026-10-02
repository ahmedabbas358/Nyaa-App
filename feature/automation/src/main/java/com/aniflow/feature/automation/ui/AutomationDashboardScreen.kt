package com.aniflow.feature.automation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.aniflow.core.ui.theme.DarkSurfaceVariant
import com.aniflow.core.ui.theme.PrimaryIndigo
import com.aniflow.core.ui.theme.TextMuted
import com.aniflow.core.ui.theme.TextPrimary
import com.aniflow.core.ui.theme.TextSecondary
import com.aniflow.domain.automation.model.AutomationAction
import com.aniflow.domain.automation.model.AutomationDryRunResult
import com.aniflow.domain.automation.model.AutomationExecution
import com.aniflow.domain.automation.model.AutomationExecutionResult
import com.aniflow.domain.automation.model.AutomationExecutionState
import com.aniflow.domain.automation.model.AutomationRule
import com.aniflow.domain.automation.model.AutomationTrigger
import com.aniflow.domain.automation.model.ReviewItem
import com.aniflow.domain.automation.model.SafetyDecision
import com.aniflow.domain.identity.AutomationExecutionId
import com.aniflow.domain.identity.AutomationRuleId
import com.aniflow.domain.identity.ReviewItemId
import java.time.Instant

/**
 * AutomationDashboardScreen (Sections 106, 107, 108, 110, 111, 112, 149, 150).
 * Displays Active Rules, Upcoming Schedules, Review Queue, Audit Logs, and Global Kill Switch.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationDashboardScreen(
    onBack: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenReviewQueue: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var globalAutomationEnabled by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedExecutionForDetail by remember { mutableStateOf<AutomationExecution?>(null) }
    var simulationResult by remember { mutableStateOf<AutomationDryRunResult?>(null) }
    var showSimulationSheet by remember { mutableStateOf(false) }

    val rules = remember {
        mutableStateListOf(
            AutomationRule(
                id = AutomationRuleId("rule_1"),
                name = "Auto-Download One Piece (1080p HEVC)",
                enabled = true,
                trigger = AutomationTrigger.NewRelease("One Piece"),
                actions = listOf(AutomationAction.QueueDownload),
                priority = 100
            ),
            AutomationRule(
                id = AutomationRuleId("rule_2"),
                name = "Notify on Jujutsu Kaisen Weekly Episodes",
                enabled = true,
                trigger = AutomationTrigger.EpisodeAvailable(
                    animeId = com.aniflow.domain.identity.AnimeId("jjk"),
                    episodeNumber = 24.0
                ),
                actions = listOf(AutomationAction.Notify("New episode ready")),
                priority = 80
            ),
            AutomationRule(
                id = AutomationRuleId("rule_3"),
                name = "Auto-Upgrade 720p Library items to 1080p",
                enabled = false,
                trigger = AutomationTrigger.LibraryChanged,
                actions = listOf(AutomationAction.CreateDownloadPlan),
                priority = 50
            )
        )
    }

    val pendingReviews = remember {
        listOf(
            ReviewItem(
                id = ReviewItemId("rev_1"),
                executionId = AutomationExecutionId("exec_101"),
                issue = "Ambiguous Episode Numbering",
                candidateReleaseTitle = "[SubsPlease] Frieren - S01E28.5 SP (1080p) [ABCD]",
                reason = "Parser confidence 0.65 is below auto-action threshold",
                recommendedAction = "Review and map manually to Special episode"
            ),
            ReviewItem(
                id = ReviewItemId("rev_2"),
                executionId = AutomationExecutionId("exec_102"),
                issue = "Large Batch Exceeds 10 GB",
                candidateReleaseTitle = "[Erai-raws] Bleach TYBW Part 2 (01-13) [1080p][HEVC][Multi-Sub]",
                reason = "Batch size 22.4 GB requires explicit user confirmation",
                recommendedAction = "Confirm download plan"
            )
        )
    }

    val executionHistory = remember {
        listOf(
            AutomationExecution(
                id = AutomationExecutionId("exec_99"),
                ruleId = AutomationRuleId("rule_1"),
                trigger = AutomationTrigger.NewRelease("One Piece #1110"),
                targetIdentity = "One Piece - Episode 1110",
                state = AutomationExecutionState.Completed,
                startedAt = Instant.now().minusSeconds(7200),
                completedAt = Instant.now().minusSeconds(7180),
                decision = SafetyDecision.Allowed,
                result = AutomationExecutionResult.DownloadQueued("plan_99", listOf("task_99")),
                explainabilityLog = listOf(
                    "Rule 'Auto-Download One Piece' triggered by NewRelease",
                    "Conditions verified: 1080p HEVC matched",
                    "Selection engine picked: [SubsPlease] One Piece - 1110 (1080p)",
                    "Safety Gate checked: Storage 124GB available (> 1.4GB required + margin)",
                    "Safety Gate checked: Network WiFi connected",
                    "Safety Gate checked: Duplicate check clean, no active task",
                    "Download plan created and enqueued successfully"
                )
            ),
            AutomationExecution(
                id = AutomationExecutionId("exec_98"),
                ruleId = AutomationRuleId("rule_1"),
                trigger = AutomationTrigger.NewRelease("One Piece #1109"),
                targetIdentity = "One Piece - Episode 1109",
                state = AutomationExecutionState.Blocked,
                startedAt = Instant.now().minusSeconds(14400),
                completedAt = Instant.now().minusSeconds(14390),
                decision = SafetyDecision.Blocked,
                result = AutomationExecutionResult.Blocked(
                    reason = com.aniflow.domain.automation.model.SafetyBlockReason.DuplicateTaskExists,
                    detail = "An identical episode task is already downloading"
                ),
                explainabilityLog = listOf(
                    "Rule 'Auto-Download One Piece' triggered",
                    "Duplicate check failed: active task 'task_98' for Episode 1109 already exists",
                    "Action blocked to prevent redundant bandwidth consumption"
                )
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Automation Engine", style = AppTypography.headline, color = TextPrimary)
                        Text(
                            text = if (globalAutomationEnabled) "Active & Monitoring" else "ALL AUTOMATION PAUSED",
                            style = AppTypography.caption,
                            color = if (globalAutomationEnabled) AppSemanticColors.Success else AppSemanticColors.Error
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // Dry-run simulation (Section 66, 67, 68)
                        simulationResult = AutomationDryRunResult(
                            matchesCount = 5,
                            wouldSelectCount = 2,
                            wouldDownloadCount = 1,
                            wouldReviewCount = 1,
                            wouldSkipCount = 3,
                            estimatedSizeBytes = 3L * 1024 * 1024 * 1024,
                            actionsPreview = listOf(
                                "Would queue: One Piece #1111 (1.4 GB)",
                                "Would send to Review: Bleach Batch (18.2 GB)",
                                "Would skip: 3 low seed releases"
                            )
                        )
                        showSimulationSheet = true
                    }) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Dry Run Simulation", tint = PrimaryIndigo)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Master Kill Switch Banner (Section 112)
            MasterKillSwitchBanner(
                isEnabled = globalAutomationEnabled,
                onToggle = { globalAutomationEnabled = it }
            )

            // Review Queue Alert Banner (Section 54, 55, 110)
            if (pendingReviews.isNotEmpty()) {
                ReviewQueueBanner(
                    pendingCount = pendingReviews.size,
                    onClick = onOpenReviewQueue
                )
            }

            // Stats row (Section 116, 169)
            AutomationStatsOverviewRow(
                activeRulesCount = rules.count { it.enabled },
                todayDownloads = 4,
                todayStorageGb = 5.6f,
                maxStorageGb = 50f
            )

            // Navigation Tabs
            val tabs = listOf("Rules (${rules.size})", "History (${executionHistory.size})")
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkSurface,
                contentColor = PrimaryIndigo,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = PrimaryIndigo
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                style = AppTypography.body,
                                color = if (selectedTab == index) PrimaryIndigo else TextMuted
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    // Active Rules List (Section 107)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(AppSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
                    ) {
                        items(rules) { rule ->
                            AutomationRuleCard(
                                rule = rule,
                                onToggle = { enabled ->
                                    val idx = rules.indexOf(rule)
                                    if (idx != -1) {
                                        rules[idx] = rule.copy(enabled = enabled)
                                    }
                                }
                            )
                        }
                    }
                }
                1 -> {
                    // History List with explainability triggers (Section 50, 52, 53)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(AppSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
                    ) {
                        items(executionHistory) { execution ->
                            AutomationExecutionCard(
                                execution = execution,
                                onClick = { selectedExecutionForDetail = execution }
                            )
                        }
                    }
                }
            }
        }
    }

    // Explainability Sheet (Section 51, 52, 53, 105)
    selectedExecutionForDetail?.let { execution ->
        ExplainabilityBottomSheet(
            execution = execution,
            onDismiss = { selectedExecutionForDetail = null }
        )
    }

    // Dry Run Simulation Sheet (Section 66, 67, 68, 127)
    if (showSimulationSheet && simulationResult != null) {
        SimulationResultBottomSheet(
            result = simulationResult!!,
            onDismiss = { showSimulationSheet = false }
        )
    }
}

@Composable
private fun MasterKillSwitchBanner(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) DarkSurfaceVariant else AppSemanticColors.Error.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(AppShapes.sm),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isEnabled) DarkCardBorder else AppSemanticColors.Error
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isEnabled) "Global Automation Active" else "Global Automation PAUSED",
                    style = AppTypography.body.copy(fontWeight = FontWeight.Bold),
                    color = if (isEnabled) TextPrimary else AppSemanticColors.Error
                )
                Text(
                    text = if (isEnabled) "Rules evaluate triggers and schedules normally." else "All background triggers & auto-downloads are stopped.",
                    style = AppTypography.caption,
                    color = TextMuted
                )
            }
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PrimaryIndigo,
                    checkedTrackColor = PrimaryIndigo.copy(alpha = 0.5f),
                    uncheckedThumbColor = AppSemanticColors.Error,
                    uncheckedTrackColor = DarkBackground
                )
            )
        }
    }
}

@Composable
private fun ReviewQueueBanner(
    pendingCount: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs)
            .clickable(onClick = onClick),
        color = AppSemanticColors.Warning.copy(alpha = 0.15f),
        shape = RoundedCornerShape(AppShapes.sm),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppSemanticColors.Warning)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = AppSemanticColors.Warning)
                Column {
                    Text(
                        text = "$pendingCount Items Need Your Review",
                        style = AppTypography.body.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "Ambiguous titles or large batches held safely.",
                        style = AppTypography.caption,
                        color = TextMuted
                    )
                }
            }
            Text("Review >", style = AppTypography.caption.copy(fontWeight = FontWeight.Bold), color = AppSemanticColors.Warning)
        }
    }
}

@Composable
private fun AutomationStatsOverviewRow(
    activeRulesCount: Int,
    todayDownloads: Int,
    todayStorageGb: Float,
    maxStorageGb: Float
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        StatCard(modifier = Modifier.weight(1f), label = "Active Rules", value = "$activeRulesCount")
        StatCard(modifier = Modifier.weight(1f), label = "Downloads Today", value = "$todayDownloads / 20")
        StatCard(modifier = Modifier.weight(1.2f), label = "Storage Today", value = String.format("%.1f / %.0f GB", todayStorageGb, maxStorageGb))
    }
}

@Composable
private fun StatCard(modifier: Modifier = Modifier, label: String, value: String) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(AppShapes.sm),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(modifier = Modifier.padding(AppSpacing.sm)) {
            Text(label, style = AppTypography.caption, color = TextMuted, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(value, style = AppTypography.body.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
        }
    }
}

@Composable
private fun AutomationRuleCard(
    rule: AutomationRule,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(AppShapes.md),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(rule.name, style = AppTypography.headline.copy(fontSize = 16.sp), color = TextPrimary)
                    Spacer(Modifier.height(2.dp))
                    Text("Priority ${rule.priority} • Cooldown ${rule.cooldown.scope}", style = AppTypography.caption, color = TextMuted)
                }
                Switch(
                    checked = rule.enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(checkedThumbColor = PrimaryIndigo, checkedTrackColor = PrimaryIndigo.copy(alpha = 0.5f))
                )
            }
            Spacer(Modifier.height(AppSpacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                BadgeChip(label = "Trigger: ${rule.trigger::class.simpleName}", color = PrimaryIndigo)
                BadgeChip(label = "Action: ${rule.actions.firstOrNull()?.let { it::class.simpleName } ?: "Queue"}", color = AppSemanticColors.Success)
            }
        }
    }
}

@Composable
private fun AutomationExecutionCard(
    execution: AutomationExecution,
    onClick: () -> Unit
) {
    val statusColor = when (execution.state) {
        AutomationExecutionState.Completed -> AppSemanticColors.Success
        AutomationExecutionState.Blocked -> AppSemanticColors.Warning
        AutomationExecutionState.Failed -> AppSemanticColors.Error
        else -> TextMuted
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(AppShapes.sm),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(execution.targetIdentity, style = AppTypography.body.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Status: ${execution.state.name} • ${execution.result?.let { it::class.simpleName } ?: ""}",
                    style = AppTypography.caption,
                    color = statusColor
                )
            }
            Icon(Icons.Default.Info, contentDescription = "Explainability Detail", tint = TextMuted)
        }
    }
}

@Composable
private fun BadgeChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(label, style = AppTypography.caption.copy(fontSize = 11.sp), color = color)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExplainabilityBottomSheet(
    execution: AutomationExecution,
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
        ) {
            Text("Decision Trace & Explainability", style = AppTypography.headline, color = TextPrimary)
            Spacer(Modifier.height(AppSpacing.xs))
            Text("Target: ${execution.targetIdentity}", style = AppTypography.body, color = PrimaryIndigo)
            Spacer(Modifier.height(AppSpacing.md))

            Text("Trace Log:", style = AppTypography.caption.copy(fontWeight = FontWeight.Bold), color = TextMuted)
            Spacer(Modifier.height(AppSpacing.xs))

            execution.explainabilityLog.forEach { step ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AppSemanticColors.Success, modifier = Modifier.size(16.dp))
                    Text(step, style = AppTypography.body.copy(fontSize = 13.sp), color = TextSecondary)
                }
            }
            Spacer(Modifier.height(AppSpacing.lg))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
            ) {
                Text("Close")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimulationResultBottomSheet(
    result: AutomationDryRunResult,
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
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PrimaryIndigo)
                Text("Dry Run Simulation Result", style = AppTypography.headline, color = TextPrimary)
            }
            Spacer(Modifier.height(AppSpacing.sm))
            Text(
                "Simulated without modifying library, files, or queue side-effects.",
                style = AppTypography.caption,
                color = TextMuted
            )
            Spacer(Modifier.height(AppSpacing.md))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Matches found: ${result.matchesCount}", style = AppTypography.body, color = TextSecondary)
                Text("Would Download: ${result.wouldDownloadCount}", style = AppTypography.body, color = AppSemanticColors.Success)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Needs Review: ${result.wouldReviewCount}", style = AppTypography.body, color = AppSemanticColors.Warning)
                Text("Would Skip: ${result.wouldSkipCount}", style = AppTypography.body, color = TextMuted)
            }
            Spacer(Modifier.height(AppSpacing.md))
            Text("Actions Preview:", style = AppTypography.caption.copy(fontWeight = FontWeight.Bold), color = TextMuted)
            Spacer(Modifier.height(AppSpacing.xs))
            result.actionsPreview.forEach { action ->
                Text("• $action", style = AppTypography.body.copy(fontSize = 13.sp), color = TextSecondary)
            }

            Spacer(Modifier.height(AppSpacing.lg))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
            ) {
                Text("Done")
            }
        }
    }
}
