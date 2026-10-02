package com.aniflow.feature.settings

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.aniflow.domain.controlplane.models.AdvancedRule
import com.aniflow.domain.controlplane.models.AdvancedRuleAction
import com.aniflow.domain.controlplane.models.AdvancedRuleCondition
import com.aniflow.domain.controlplane.models.ConditionNode
import com.aniflow.domain.identity.RuleId
import com.aniflow.domain.model.aggregate.organization.RuleScope
import com.aniflow.domain.rules.conflict.RuleConflict
import com.aniflow.domain.rules.conflict.RuleConflictDetector
import com.aniflow.domain.valueobject.Resolution
import com.aniflow.domain.valueobject.VideoCodec

/**
 * RuleListScreen (Sections 40, 41, 29).
 * Comprehensive rule dashboard listing rules, precedence levels, conflict detection,
 * and dry-run execution.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleListScreen(
    onBack: () -> Unit = {},
    onCreateNewRule: () -> Unit = {},
    onEditRule: (RuleId) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val sampleRules = remember {
        mutableStateListOf(
            AdvancedRule(
                id = RuleId("rule_one_piece"),
                name = "One Piece 1080p Auto-Queue",
                scope = RuleScope.Anime,
                root = ConditionNode(AdvancedRuleCondition.AnimeIs("One Piece")),
                actions = listOf(AdvancedRuleAction.QueueDownload),
                customPriority = 80,
                enabled = true
            ),
            AdvancedRule(
                id = RuleId("rule_hevc_filter"),
                name = "Prefer HEVC Encodings",
                scope = RuleScope.Global,
                root = ConditionNode(AdvancedRuleCondition.CodecIs(VideoCodec.HEVC)),
                actions = listOf(AdvancedRuleAction.Prefer(scoreBonus = 20)),
                customPriority = 50,
                enabled = true
            ),
            AdvancedRule(
                id = RuleId("rule_reject_low_res"),
                name = "Reject SD Content (< 720p)",
                scope = RuleScope.Global,
                root = ConditionNode(AdvancedRuleCondition.ResolutionIs(Resolution.R480p)),
                actions = listOf(AdvancedRuleAction.Reject("Resolution below 720p minimum standard")),
                customPriority = 90,
                enabled = true
            )
        )
    }

    var simulatingRule by remember { mutableStateOf<AdvancedRule?>(null) }
    val conflictDetector = remember { RuleConflictDetector() }
    val conflicts = remember(sampleRules.toList()) { conflictDetector.detectConflicts(sampleRules) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Automation Rules", style = AppTypography.headline, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    Button(
                        onClick = onCreateNewRule,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        shape = AppShapes.pill,
                        modifier = Modifier.padding(end = AppSpacing.sm)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Rule", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground,
        modifier = modifier
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Conflict warning banner if semantic conflicts are detected (Section 29)
            if (conflicts.isNotEmpty()) {
                item {
                    ConflictWarningBanner(conflicts)
                }
            }

            item {
                Text(
                    text = "Rules apply conditional logic (WHEN/IF/THEN) on top of profile preferences. Higher priority rules take precedence.",
                    style = AppTypography.caption,
                    color = TextMuted
                )
            }

            items(sampleRules, key = { it.id.value }) { rule ->
                RuleCard(
                    rule = rule,
                    onToggleEnabled = { enabled ->
                        val index = sampleRules.indexOfFirst { it.id == rule.id }
                        if (index != -1) {
                            sampleRules[index] = rule.copy(enabled = enabled)
                        }
                    },
                    onSimulate = { simulatingRule = rule },
                    onEdit = { onEditRule(rule.id) }
                )
            }
        }
    }

    // Dry Run dialog (Section 30)
    simulatingRule?.let { rule ->
        RuleSimulationDialog(
            rule = rule,
            onDismiss = { simulatingRule = null }
        )
    }
}

@Composable
private fun ConflictWarningBanner(conflicts: List<RuleConflict>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AppSemanticColors.warning.copy(alpha = 0.15f)),
        shape = AppShapes.card,
        border = androidx.compose.foundation.BorderStroke(1.dp, AppSemanticColors.warning.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(AppSpacing.md), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = AppSemanticColors.warning)
            Spacer(modifier = Modifier.width(AppSpacing.sm))
            Column {
                Text(
                    "Rule Conflicts Detected (${conflicts.size})",
                    style = AppTypography.subheadline,
                    fontWeight = FontWeight.Bold,
                    color = AppSemanticColors.warning
                )
                conflicts.forEach { conflict ->
                    Text(
                        conflict.description,
                        style = AppTypography.caption,
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RuleCard(
    rule: AdvancedRule,
    onToggleEnabled: (Boolean) -> Unit,
    onSimulate: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = AppShapes.card,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(rule.name, style = AppTypography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Scope: ${rule.scope.name} • Priority: ${rule.customPriority}", style = AppTypography.caption, color = TextMuted)
                }
                Switch(
                    checked = rule.enabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(checkedThumbColor = PrimaryIndigo, checkedTrackColor = PrimaryIndigo.copy(alpha = 0.5f))
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            // Actions display
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Actions: ${rule.actions.joinToString { it.javaClass.simpleName }}",
                    style = AppTypography.bodySmall,
                    color = TextSecondary
                )
                OutlinedButton(
                    onClick = onSimulate,
                    shape = AppShapes.pill,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryIndigo)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test Dry Run", fontSize = 12.sp, color = PrimaryIndigo)
                }
            }
        }
    }
}
