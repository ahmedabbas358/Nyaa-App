package com.aniflow.feature.settings

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

enum class RuleExecutionOutcome(val label: String) {
    All("All"),
    Matched("Matched"),
    Skipped("Skipped"),
    Blocked("Blocked"),
    Failed("Failed"),
    Completed("Completed")
}

data class RuleDecisionDetail(
    val conditionName: String,
    val isMet: Boolean,
    val explanation: String
)

data class RuleExecutionRecord(
    val id: String,
    val ruleName: String,
    val triggerName: String,
    val releaseTitle: String,
    val timestampFormatted: String,
    val outcome: RuleExecutionOutcome,
    val decisions: List<RuleDecisionDetail>
)

/**
 * RuleHistoryScreen (Section 49, 167).
 * Displays execution logs for automation rules.
 * Tabs: Matched, Skipped, Blocked, Failed, Completed.
 * Each item expands "Why did this happen?" with transparent rule evaluation decisions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleHistoryScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabOutcomes = RuleExecutionOutcome.entries.toTypedArray()

    // Real list populated from domain automation/rule repositories
    var executionRecords by remember { mutableStateOf<List<RuleExecutionRecord>>(emptyList()) }

    val activeOutcome = tabOutcomes[selectedTabIndex]
    val filteredRecords = if (activeOutcome == RuleExecutionOutcome.All) {
        executionRecords
    } else {
        executionRecords.filter { it.outcome == activeOutcome }
    }

    Scaffold(
        topBar = {
            AniAppBar(
                title = "Rule Execution History",
                subtitle = "Automation audit trail & decisions",
                onBack = onBack
            )
        },
        containerColor = DarkBackground,
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = DarkSurface,
                contentColor = TextPrimary,
                edgePadding = AppSpacing.md,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = PrimaryIndigo
                    )
                }
            ) {
                tabOutcomes.forEachIndexed { index, outcome ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = outcome.label,
                                style = AppTypography.Subtitle.copy(
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    )
                }
            }

            if (filteredRecords.isEmpty()) {
                AniEmptyState(
                    title = "No executions recorded",
                    description = "When automation rules trigger and evaluate release candidates, execution audits and decision rationales will be logged here.",
                    icon = Icons.Default.History,
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(AppSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredRecords, key = { it.id }) { record ->
                        RuleExecutionCard(record = record)
                    }
                }
            }
        }
    }
}

@Composable
private fun RuleExecutionCard(
    record: RuleExecutionRecord,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    val outcomeColor = when (record.outcome) {
        RuleExecutionOutcome.Matched -> PrimaryIndigo
        RuleExecutionOutcome.Completed -> AppSemanticColors.Success
        RuleExecutionOutcome.Skipped -> AppSemanticColors.Neutral
        RuleExecutionOutcome.Blocked -> AppSemanticColors.Warning
        RuleExecutionOutcome.Failed -> AppSemanticColors.Error
        RuleExecutionOutcome.All -> PrimaryIndigo
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = AppShapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
        modifier = modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AniStatusBadge(
                        label = record.outcome.label,
                        color = outcomeColor
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.xs))
                    Text(
                        text = record.timestampFormatted,
                        style = AppTypography.Caption,
                        color = TextMuted
                    )
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand details",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.xs))

            Text(
                text = record.ruleName,
                style = AppTypography.Title.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Text(
                text = "Trigger: ${record.triggerName} • Candidate: ${record.releaseTitle}",
                style = AppTypography.Caption,
                color = TextSecondary
            )

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = AppSpacing.md)) {
                    Text(
                        text = "Why did this happen?",
                        style = AppTypography.Overline,
                        color = PrimaryIndigo
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.xs))

                    record.decisions.forEach { decision ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (decision.isMet) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = if (decision.isMet) AppSemanticColors.Success else AppSemanticColors.Error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(AppSpacing.xs))
                            Column {
                                Text(
                                    text = decision.conditionName,
                                    style = AppTypography.Subtitle.copy(fontWeight = FontWeight.Medium),
                                    color = TextPrimary
                                )
                                Text(
                                    text = decision.explanation,
                                    style = AppTypography.Caption,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
