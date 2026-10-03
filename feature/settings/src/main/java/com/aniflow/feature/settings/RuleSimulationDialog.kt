package com.aniflow.feature.settings

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.aniflow.domain.controlplane.service.CandidateReleaseContext
import com.aniflow.domain.rules.simulation.RuleSimulator
import com.aniflow.domain.valueobject.Resolution
import com.aniflow.domain.valueobject.VideoCodec

/**
 * RuleSimulationDialog (Sections 30, 31, 32).
 * Interactive modal that tests candidate rules against sample releases,
 * rendering step-by-step condition traces and dry-run safety previews without mutating queue/storage.
 */
@Composable
fun RuleSimulationDialog(
    rule: AdvancedRule,
    candidates: List<CandidateReleaseContext> = emptyList(),
    onDismiss: () -> Unit
) {
    var selectedCandidateIndex by remember { mutableStateOf(0) }
    val simulator = remember { RuleSimulator() }

    val report = remember(rule, selectedCandidateIndex, candidates) {
        if (candidates.isNotEmpty()) {
            simulator.simulate(rule, candidates[selectedCandidateIndex.coerceIn(0, candidates.size - 1)])
        } else null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = PrimaryIndigo)
                Spacer(modifier = Modifier.width(AppSpacing.xs))
                Text("Rule Dry Run & Simulation", style = AppTypography.headline, color = TextPrimary)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppSpacing.xs)
            ) {
                if (candidates.isEmpty()) {
                    Text(
                        text = "No candidate releases available for simulation. Search or open an anime release to evaluate this rule in real-time.",
                        style = AppTypography.body,
                        color = TextMuted
                    )
                } else {
                    Text(
                        text = "Testing '${rule.name}' on candidate release (Zero mutation):",
                        style = AppTypography.caption,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(AppSpacing.sm))

                    // Candidate picker
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        candidates.forEachIndexed { index, candidate ->
                            val isSelected = (index == selectedCandidateIndex)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSelected) PrimaryIndigo.copy(alpha = 0.2f) else DarkBackground, shape = AppShapes.pill)
                                    .border(1.dp, if (isSelected) PrimaryIndigo else DarkCardBorder, shape = AppShapes.pill)
                                    .clickable { selectedCandidateIndex = index }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Candidate ${index + 1}",
                                    style = AppTypography.caption,
                                    color = if (isSelected) PrimaryIndigo else TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(AppSpacing.sm))

                    // Release title display
                    Text(
                        text = candidates[selectedCandidateIndex.coerceIn(0, candidates.size - 1)].title,
                        style = AppTypography.caption,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .background(DarkBackground, shape = AppShapes.badge)
                            .padding(AppSpacing.sm)
                            .fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(AppSpacing.md))

                    report?.let { rep ->
                        // Condition step results
                        Text("Evaluated Conditions:", style = AppTypography.subheadline, color = TextSecondary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(AppSpacing.xs))

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            rep.conditionSteps.forEach { step ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = if (step.matched) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                        contentDescription = null,
                                        tint = if (step.matched) AppSemanticColors.success else AppSemanticColors.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(AppSpacing.xs))
                                    Text(
                                        text = "${step.conditionDescription} (Expected: ${step.expectedValue}, Actual: ${step.actualValue})",
                                        style = AppTypography.caption,
                                        color = if (step.matched) TextPrimary else TextMuted
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(AppSpacing.md))

                        // Safety Gate & Verdict
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkBackground),
                            shape = AppShapes.card,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(AppSpacing.sm)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Security,
                                        contentDescription = null,
                                        tint = if (rep.safetyPreview.passed) AppSemanticColors.success else AppSemanticColors.warning,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        if (rep.safetyPreview.passed) "Safety Gate: PASSED" else "Safety Gate: BLOCKED",
                                        style = AppTypography.caption,
                                        fontWeight = FontWeight.Bold,
                                        color = if (rep.safetyPreview.passed) AppSemanticColors.success else AppSemanticColors.warning
                                    )
                                }
                                Text(
                                    text = rep.explanation,
                                    style = AppTypography.caption,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
            ) {
                Text("Close", color = Color.White)
            }
        },
        containerColor = DarkSurface
    )
}
