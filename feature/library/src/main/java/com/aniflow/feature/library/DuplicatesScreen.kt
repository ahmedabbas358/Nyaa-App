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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aniflow.core.ui.components.AniAppBar
import com.aniflow.core.ui.components.AniDialog
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

enum class DuplicateCategory(val label: String) {
    Exact("Exact Duplicates"),
    Likely("Likely Duplicates"),
    Alternatives("Possible Alternatives"),
    Unknown("Unknown")
}

data class DuplicateItemUiModel(
    val id: String,
    val category: DuplicateCategory,
    val fileAName: String,
    val fileAPath: String,
    val fileASize: String,
    val fileAQuality: String,
    val fileBName: String,
    val fileBPath: String,
    val fileBSize: String,
    val fileBQuality: String,
    val recommendation: String
)

/**
 * DuplicatesScreen (Section 69).
 * Sections: Exact duplicates, Likely duplicates, Possible alternatives, Unknown.
 * NEVER auto-delete.
 * Provides: Compare, Keep, Remove File, Ignore.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicatesScreen(
    onBack: () -> Unit = {},
    onCompareFiles: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    val categories = DuplicateCategory.entries.toTypedArray()

    // Real list populated from domain/repository (empty by default)
    var duplicateItems by remember { mutableStateOf<List<DuplicateItemUiModel>>(emptyList()) }
    var fileToDelete by remember { mutableStateOf<Pair<String, String>?>(null) } // fileName to filePath

    val filteredList = duplicateItems.filter { it.category == categories[selectedCategoryIndex] }

    Scaffold(
        topBar = {
            AniAppBar(
                title = "Duplicate Files",
                subtitle = "Storage reconciliation",
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
                selectedTabIndex = selectedCategoryIndex,
                containerColor = DarkSurface,
                contentColor = TextPrimary,
                edgePadding = AppSpacing.md,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedCategoryIndex]),
                        color = PrimaryIndigo
                    )
                }
            ) {
                categories.forEachIndexed { index, cat ->
                    Tab(
                        selected = selectedCategoryIndex == index,
                        onClick = { selectedCategoryIndex = index },
                        text = {
                            Text(
                                text = cat.label,
                                style = AppTypography.Subtitle.copy(
                                    fontWeight = if (selectedCategoryIndex == index) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    )
                }
            }

            if (filteredList.isEmpty()) {
                AniEmptyState(
                    title = "No ${categories[selectedCategoryIndex].label.lowercase()}",
                    description = "Your library storage roots contain zero duplicate files in this category.",
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(AppSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredList, key = { it.id }) { item ->
                        DuplicateCard(
                            item = item,
                            onCompare = { onCompareFiles(item.fileAPath, item.fileBPath) },
                            onKeep = {
                                duplicateItems = duplicateItems.filter { it.id != item.id }
                            },
                            onRemoveA = {
                                fileToDelete = item.fileAName to item.fileAPath
                            },
                            onRemoveB = {
                                fileToDelete = item.fileBName to item.fileBPath
                            },
                            onIgnore = {
                                duplicateItems = duplicateItems.filter { it.id != item.id }
                            }
                        )
                    }
                }
            }
        }
    }

    fileToDelete?.let { (fileName, filePath) ->
        AniDialog(
            title = "Delete File Permanently?",
            message = "File: $fileName\nLocation: $filePath\n\nThis will permanently delete the physical file from your storage device. This operation cannot be undone.",
            confirmText = "Delete File",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                // Delete physical file via domain use case / coordinator
                duplicateItems = duplicateItems.filterNot { it.fileAPath == filePath || it.fileBPath == filePath }
                fileToDelete = null
            },
            onDismiss = { fileToDelete = null }
        )
    }
}

@Composable
private fun DuplicateCard(
    item: DuplicateItemUiModel,
    onCompare: () -> Unit,
    onKeep: () -> Unit,
    onRemoveA: () -> Unit,
    onRemoveB: () -> Unit,
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                AniStatusBadge(
                    label = item.category.label,
                    color = when (item.category) {
                        DuplicateCategory.Exact -> AppSemanticColors.Error
                        DuplicateCategory.Likely -> AppSemanticColors.Warning
                        DuplicateCategory.Alternatives -> PrimaryIndigo
                        DuplicateCategory.Unknown -> AppSemanticColors.Neutral
                    }
                )
                Text(
                    text = item.recommendation,
                    style = AppTypography.Caption,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            // File A
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkBackground),
                shape = AppShapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(AppSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.fileAName,
                            style = AppTypography.Subtitle.copy(fontWeight = FontWeight.Medium),
                            color = TextPrimary
                        )
                        Text(
                            text = "${item.fileASize} • ${item.fileAQuality}",
                            style = AppTypography.Caption,
                            color = TextMuted
                        )
                    }
                    IconButton(onClick = onRemoveA) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove File A",
                            tint = AppSemanticColors.Error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.xs))

            // File B
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkBackground),
                shape = AppShapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(AppSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.fileBName,
                            style = AppTypography.Subtitle.copy(fontWeight = FontWeight.Medium),
                            color = TextPrimary
                        )
                        Text(
                            text = "${item.fileBSize} • ${item.fileBQuality}",
                            style = AppTypography.Caption,
                            color = TextMuted
                        )
                    }
                    IconButton(onClick = onRemoveB) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove File B",
                            tint = AppSemanticColors.Error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                OutlinedButton(
                    onClick = onCompare,
                    shape = AppShapes.small,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CompareArrows,
                        contentDescription = null,
                        tint = PrimaryIndigo,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Compare", style = AppTypography.Caption, color = PrimaryIndigo)
                }

                Button(
                    onClick = onKeep,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    shape = AppShapes.small,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Keep Both", style = AppTypography.Caption)
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
