package com.aniflow.feature.search.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aniflow.core.ui.theme.AppSemanticColors
import com.aniflow.core.ui.theme.AppShapes
import com.aniflow.core.ui.theme.AppSpacing
import com.aniflow.core.ui.theme.AppTypography
import com.aniflow.core.ui.theme.DarkCardBorder
import com.aniflow.core.ui.theme.DarkSurface
import com.aniflow.core.ui.theme.PrimaryIndigo
import com.aniflow.core.ui.theme.TextMuted
import com.aniflow.core.ui.theme.TextPrimary
import com.aniflow.core.ui.theme.TextSecondary
import com.aniflow.domain.search.model.SearchHistoryEntry
import com.aniflow.domain.search.model.SearchSuggestion
import com.aniflow.domain.search.model.SearchSuggestionType

/**
 * SearchSuggestionsView (Sections 8, 9, 10).
 * Displays deterministic, ranked smart suggestions and recent search history.
 */
@Composable
fun SearchSuggestionsView(
    suggestions: List<SearchSuggestion>,
    recentHistory: List<SearchHistoryEntry>,
    onSelectSuggestion: (String) -> Unit,
    onDeleteHistoryEntry: (String) -> Unit,
    onClearAllHistory: () -> Unit,
    onConvertToSavedSearch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        // 1. Live Suggestions if typing (Section 8, 9)
        if (suggestions.isNotEmpty()) {
            item {
                Text(
                    text = "Suggestions",
                    style = AppTypography.caption.copy(fontWeight = FontWeight.Bold),
                    color = PrimaryIndigo
                )
            }
            items(suggestions, key = { it.id }) { suggestion ->
                SuggestionRow(
                    suggestion = suggestion,
                    onClick = { onSelectSuggestion(suggestion.text) }
                )
            }
        }

        // 2. Recent Search History (Section 10)
        if (recentHistory.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Searches",
                        style = AppTypography.caption.copy(fontWeight = FontWeight.Bold),
                        color = PrimaryIndigo
                    )
                    TextButton(onClick = onClearAllHistory) {
                        Text("Clear All", style = AppTypography.caption, color = TextMuted)
                    }
                }
            }

            items(recentHistory, key = { it.id.value }) { entry ->
                HistoryRow(
                    entry = entry,
                    onClick = { onSelectSuggestion(entry.query) },
                    onDelete = { onDeleteHistoryEntry(entry.id.value) },
                    onSaveSearch = { onConvertToSavedSearch(entry.query) }
                )
            }
        }
    }
}

@Composable
private fun SuggestionRow(
    suggestion: SearchSuggestion,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(AppShapes.sm),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val icon = when (suggestion.type) {
                    SearchSuggestionType.RecentSearch -> Icons.Default.History
                    SearchSuggestionType.PopularEntity -> Icons.Default.TrendingUp
                    SearchSuggestionType.AnimeTitle -> Icons.Default.Movie
                    SearchSuggestionType.Uploader -> Icons.Default.Person
                    SearchSuggestionType.ReleaseGroup -> Icons.Default.Person
                    SearchSuggestionType.SavedSearch -> Icons.Default.BookmarkBorder
                    SearchSuggestionType.FilterSuggestion -> Icons.Default.Search
                }
                Icon(icon, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(AppSpacing.sm))
                Column {
                    Text(suggestion.text, style = AppTypography.body.copy(fontSize = 13.sp), color = TextPrimary)
                    suggestion.subtitle?.let {
                        Text(it, style = AppTypography.caption.copy(fontSize = 11.sp), color = TextMuted)
                    }
                }
            }
            if (suggestion.isPinned) {
                Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = PrimaryIndigo, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun HistoryRow(
    entry: SearchHistoryEntry,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onSaveSearch: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(AppShapes.sm),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.History, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(AppSpacing.sm))
                Column {
                    Text(entry.query, style = AppTypography.body.copy(fontSize = 13.sp), color = TextPrimary, maxLines = 1)
                    entry.filtersSummary?.let {
                        Text(it, style = AppTypography.caption.copy(fontSize = 11.sp), color = TextMuted)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(onClick = onSaveSearch, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.BookmarkBorder, contentDescription = "Save as search", tint = PrimaryIndigo, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = TextMuted, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
