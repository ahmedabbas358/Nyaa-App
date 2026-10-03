package com.aniflow.feature.search

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

enum class SearchHistoryPeriod {
    Today,
    Yesterday,
    Earlier
}

data class SearchHistoryItemUiModel(
    val id: String,
    val query: String,
    val timestampFormatted: String,
    val period: SearchHistoryPeriod,
    val scope: String = "Universal"
)

/**
 * SearchHistoryScreen (Section 30).
 * Sections: Today, Yesterday, Earlier.
 * Each entry: Query, Timestamp, Scope.
 * Actions: Search Again, Save Search, Delete.
 * Global action: Clear History.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchHistoryScreen(
    onBack: () -> Unit = {},
    onSearchAgain: (String) -> Unit = {},
    onSaveSearch: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var historyItems by remember { mutableStateOf<List<SearchHistoryItemUiModel>>(emptyList()) }
    var showClearConfirmation by remember { mutableStateOf(false) }

    val groupedItems = historyItems.groupBy { it.period }

    Scaffold(
        topBar = {
            AniAppBar(
                title = "Search History",
                subtitle = "Recent queries and expressions",
                onBack = onBack,
                actions = {
                    if (historyItems.isNotEmpty()) {
                        IconButton(onClick = { showClearConfirmation = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear All History",
                                tint = AppSemanticColors.Error
                            )
                        }
                    }
                }
            )
        },
        containerColor = DarkBackground,
        modifier = modifier
    ) { padding ->
        if (historyItems.isEmpty()) {
            AniEmptyState(
                title = "No search history",
                description = "Your past search queries across universal, local, and provider scopes will appear here.",
                icon = Icons.Default.History,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(AppSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                SearchHistoryPeriod.entries.forEach { period ->
                    val itemsForPeriod = groupedItems[period]
                    if (!itemsForPeriod.isNullOrEmpty()) {
                        item(key = "header_${period.name}") {
                            Text(
                                text = period.name,
                                style = AppTypography.SectionTitle,
                                color = TextPrimary,
                                modifier = Modifier.padding(vertical = AppSpacing.xs)
                            )
                        }

                        items(itemsForPeriod, key = { it.id }) { item ->
                            SearchHistoryRow(
                                item = item,
                                onClick = { onSearchAgain(item.query) },
                                onSave = { onSaveSearch(item.query) },
                                onDelete = {
                                    historyItems = historyItems.filter { it.id != item.id }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showClearConfirmation) {
        AniDialog(
            title = "Clear Search History?",
            message = "This will remove all saved queries from search history. Saved searches and filters will remain intact.",
            confirmText = "Clear History",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                historyItems = emptyList()
                showClearConfirmation = false
            },
            onDismiss = { showClearConfirmation = false }
        )
    }
}

@Composable
private fun SearchHistoryRow(
    item: SearchHistoryItemUiModel,
    onClick: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = AppShapes.small,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(AppSpacing.sm))
                Column {
                    Text(
                        text = item.query,
                        style = AppTypography.Subtitle.copy(fontWeight = FontWeight.Medium),
                        color = TextPrimary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.timestampFormatted,
                            style = AppTypography.Caption,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.width(AppSpacing.xs))
                        Text(
                            text = "• ${item.scope}",
                            style = AppTypography.Caption,
                            color = PrimaryIndigo
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onSave) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = "Save Search",
                        tint = TextSecondary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove item",
                        tint = TextMuted
                    )
                }
            }
        }
    }
}
