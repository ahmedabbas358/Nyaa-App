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
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
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
import com.aniflow.domain.controlplane.models.AndExpression
import com.aniflow.domain.controlplane.models.ComparisonExpression
import com.aniflow.domain.controlplane.models.ComparisonOperator
import com.aniflow.domain.controlplane.models.SearchExpression
import com.aniflow.domain.controlplane.models.SearchField

data class UiSearchCriterion(
    val field: SearchField,
    val operator: ComparisonOperator,
    val value: String
)

/**
 * SearchBuilderScreen (Sections 12, 13, 53).
 * Visual multi-parameter query builder compiling into structured SearchExpression AST.
 * Supports granular conditions, query preview, and direct transition to SavedSearch or Automation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBuilderScreen(
    onBack: () -> Unit = {},
    onExecuteSearch: (SearchExpression) -> Unit = {},
    onSaveSearch: (SearchExpression, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var searchName by remember { mutableStateOf("") }
    var selectedProvider by remember { mutableStateOf("Nyaa.si") }

    val criteria = remember {
        mutableStateListOf(
            UiSearchCriterion(SearchField.Anime, ComparisonOperator.Equals, "")
        )
    }


    // Build the compiled AST expression
    val compiledExpression = remember(criteria.size, criteria.map { it.value }) {
        val nodes = criteria.map {
            ComparisonExpression(it.field, it.operator, it.value)
        }
        SearchExpression(
            root = if (nodes.size == 1) nodes.first() else AndExpression(nodes),
            name = searchName
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Advanced Search Builder", style = AppTypography.headline, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    Button(
                        onClick = { onSaveSearch(compiledExpression, searchName) },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkCardBorder),
                        shape = AppShapes.pill,
                        modifier = Modifier.padding(end = AppSpacing.xs)
                    ) {
                        Icon(Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save", fontSize = 12.sp)
                    }
                    Button(
                        onClick = { onExecuteSearch(compiledExpression) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        shape = AppShapes.pill,
                        modifier = Modifier.padding(end = AppSpacing.sm)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Search", fontSize = 12.sp)
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
            // Search Query Name & Provider
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = searchName,
                        onValueChange = { searchName = it },
                        label = { Text("Saved Search Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIndigo,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = AppShapes.medium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick Preset Chips
            item {
                Column {
                    Text("Target Provider", style = AppTypography.title, color = TextPrimary)
                    Spacer(modifier = Modifier.height(AppSpacing.xs))
                    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                        FilterChip(
                            selected = selectedProvider == "Nyaa.si",
                            onClick = { selectedProvider = "Nyaa.si" },
                            label = { Text("Nyaa.si (Anime)") }
                        )
                        FilterChip(
                            selected = selectedProvider == "All Providers",
                            onClick = { selectedProvider = "All Providers" },
                            label = { Text("All Providers") }
                        )
                    }
                }
            }

            // Criteria Builder Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = AppShapes.medium,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(AppSpacing.md)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Search Criteria (Match ALL)",
                                style = AppTypography.title,
                                color = PrimaryIndigo,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = {
                                    criteria.add(
                                        UiSearchCriterion(
                                            field = SearchField.Uploader,
                                            operator = ComparisonOperator.Equals,
                                            value = ""
                                        )
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkCardBorder),
                                shape = AppShapes.small
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Filter", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(AppSpacing.sm))

                        criteria.forEachIndexed { index, item ->
                            CriterionRow(
                                criterion = item,
                                isFirst = index == 0,
                                onUpdateValue = { newVal -> criteria[index] = item.copy(value = newVal) },
                                onDelete = { criteria.removeAt(index) }
                            )
                            if (index < criteria.size - 1) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }

            // Live AST Query Preview
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.7f)),
                    shape = AppShapes.medium,
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(AppSpacing.md)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = PrimaryIndigo.copy(alpha = 0.2f),
                                shape = AppShapes.small
                            ) {
                                Text(
                                    text = "COMPILED QUERY",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryIndigo,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Structured Domain AST", style = AppTypography.caption, color = TextMuted)
                        }
                        Spacer(modifier = Modifier.height(AppSpacing.xs))
                        Text(
                            text = compiledExpression.toQueryString(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = AppSemanticColors.Success,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CriterionRow(
    criterion: UiSearchCriterion,
    isFirst: Boolean,
    onUpdateValue: (String) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            color = DarkCardBorder,
            shape = AppShapes.small
        ) {
            Text(
                text = if (isFirst) "WHERE" else "AND",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }

        Surface(
            color = PrimaryIndigo.copy(alpha = 0.15f),
            shape = AppShapes.small
        ) {
            Text(
                text = criterion.field.name,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryIndigo,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        Text(
            text = when (criterion.operator) {
                ComparisonOperator.Equals -> "="
                ComparisonOperator.NotEquals -> "!="
                ComparisonOperator.GreaterThanOrEqual -> ">="
                ComparisonOperator.LessThanOrEqual -> "<="
                ComparisonOperator.Contains -> "contains"
                else -> "="
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary
        )

        OutlinedTextField(
            value = criterion.value,
            onValueChange = onUpdateValue,
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryIndigo,
                unfocusedBorderColor = DarkCardBorder,
                focusedContainerColor = DarkBackground,
                unfocusedContainerColor = DarkBackground,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = AppShapes.small,
            modifier = Modifier.weight(1f)
        )

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete criterion",
                tint = AppSemanticColors.Error,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
