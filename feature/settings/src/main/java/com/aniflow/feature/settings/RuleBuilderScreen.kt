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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.aniflow.core.ui.theme.TextPrimary
import com.aniflow.core.ui.theme.TextSecondary
import com.aniflow.domain.model.aggregate.organization.RuleScope

data class VisualConditionItem(
    val field: String,
    val operator: String,
    val value: String
)

/**
 * RuleBuilderScreen (Sections 14, 15, 16, 17, 40, 50).
 * Visual Rule Builder allowing intuitive construction of complex conditions (IF, AND, OR, THEN)
 * without exposing raw code or JSON ASTs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleBuilderScreen(
    onBack: () -> Unit = {},
    onSaveRule: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var ruleName by remember { mutableStateOf("Auto-Download One Piece 1080p") }
    var selectedScope by remember { mutableStateOf(RuleScope.Anime) }
    var priority by remember { mutableIntStateOf(70) }

    val conditions = remember {
        mutableStateListOf(
            VisualConditionItem("Anime", "is", "One Piece"),
            VisualConditionItem("Resolution", "is", "1080p"),
            VisualConditionItem("Codec", "is", "HEVC")
        )
    }

    val actions = remember {
        mutableStateListOf("Queue Download", "Apply Profile: Anime 1080p")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Visual Rule Builder", style = AppTypography.headline, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    Button(
                        onClick = onSaveRule,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        shape = AppShapes.pill,
                        modifier = Modifier.padding(end = AppSpacing.sm)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save Rule")
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
            // Rule Name
            item {
                OutlinedTextField(
                    value = ruleName,
                    onValueChange = { ruleName = it },
                    label = { Text("Rule Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryIndigo,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = AppShapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Scope Selector
            item {
                Column {
                    Text("Rule Scope", style = AppTypography.title, color = TextPrimary)
                    Spacer(modifier = Modifier.height(AppSpacing.xs))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                        items(RuleScope.values()) { scope ->
                            FilterChip(
                                selected = selectedScope == scope,
                                onClick = { selectedScope = scope },
                                label = { Text(scope.name) }
                            )
                        }
                    }
                }
            }

            // IF Block (Conditions)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = AppShapes.medium,
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(AppSpacing.md)) {
                        Text(
                            text = "IF (Match All Conditions)",
                            style = AppTypography.title,
                            color = PrimaryIndigo,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(AppSpacing.sm))

                        conditions.forEachIndexed { index, cond ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = DarkCardBorder,
                                        shape = AppShapes.small
                                    ) {
                                        Text(
                                            text = if (index == 0) "WHERE" else "AND",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextSecondary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${cond.field} ${cond.operator} \"${cond.value}\"",
                                        style = AppTypography.body,
                                        color = TextPrimary
                                    )
                                }
                                IconButton(
                                    onClick = { conditions.removeAt(index) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = AppSemanticColors.Error, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(AppSpacing.sm))
                        Button(
                            onClick = { conditions.add(VisualConditionItem("Seeders", ">=", "5")) },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkCardBorder),
                            shape = AppShapes.small
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Condition", fontSize = 12.sp)
                        }
                    }
                }
            }

            // THEN Block (Actions)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = AppShapes.medium,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppSemanticColors.Success.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(AppSpacing.md)) {
                        Text(
                            text = "THEN (Execute Actions)",
                            style = AppTypography.title,
                            color = AppSemanticColors.Success,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(AppSpacing.sm))

                        actions.forEachIndexed { index, action ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Text("• $action", style = AppTypography.body, color = TextPrimary)
                                IconButton(
                                    onClick = { actions.removeAt(index) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = AppSemanticColors.Error, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(AppSpacing.sm))
                        Button(
                            onClick = { actions.add("Notify User") },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkCardBorder),
                            shape = AppShapes.small
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Action", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
