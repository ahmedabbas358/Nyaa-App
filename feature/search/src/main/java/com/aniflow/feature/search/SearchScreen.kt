package com.aniflow.feature.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.aniflow.core.ui.theme.DarkSurfaceVariant
import com.aniflow.core.ui.theme.PrimaryIndigo
import com.aniflow.core.ui.theme.TextMuted
import com.aniflow.core.ui.theme.TextPrimary
import com.aniflow.core.ui.theme.TextSecondary
import com.aniflow.domain.search.model.SearchScope
import com.aniflow.domain.search.model.SearchResultItem
import com.aniflow.feature.search.components.AdvancedFilterBuilderSheet
import com.aniflow.feature.search.components.EpisodeBatchSelectionSheet
import com.aniflow.feature.search.components.ReleaseComparisonSheet
import com.aniflow.feature.search.components.SearchResultRow
import com.aniflow.feature.search.components.SearchSuggestionsView

/**
 * SearchScreen — Discovery interface for Nyaa releases.
 *
 * Features:
 * - Smart suggestions and recency ranking
 * - Advanced filter builder
 * - Multi-release comparison
 * - Batch episode download
 * - Adaptive layout (phone + tablet)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onAnimeClick: (String) -> Unit = {},
    onReleaseClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    val scopes = listOf(
        "All" to SearchScope.Universal,
        "Anime" to SearchScope.Anime(),
        "Library" to SearchScope.Library,
        "Downloads" to SearchScope.Downloads
    )
    var selectedScopeIndex by remember { mutableIntStateOf(0) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTabletLandscape = maxWidth > 840.dp

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = uiState.query,
                            onValueChange = { viewModel.onEvent(SearchUiEvent.QueryChanged(it)) },
                            placeholder = {
                                Text(
                                    "Search anime, episodes, uploader…",
                                    style = AppTypography.Caption,
                                    color = TextMuted
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = PrimaryIndigo)
                            },
                            trailingIcon = {
                                if (uiState.query.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onEvent(SearchUiEvent.ClearQuery) }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryIndigo,
                                unfocusedBorderColor = DarkCardBorder,
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        )
                    },
                    actions = {
                        IconButton(onClick = { viewModel.onEvent(SearchUiEvent.SetFilterSheetVisible(true)) }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filters", tint = TextSecondary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
                )
            },
            containerColor = DarkBackground
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Scope tabs
                TabRow(
                    selectedTabIndex = selectedScopeIndex,
                    containerColor = DarkSurface,
                    contentColor = PrimaryIndigo,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedScopeIndex]),
                            color = PrimaryIndigo,
                            height = 2.dp
                        )
                    }
                ) {
                    scopes.forEachIndexed { index, (title, scope) ->
                        Tab(
                            selected = selectedScopeIndex == index,
                            onClick = {
                                selectedScopeIndex = index
                                viewModel.onEvent(SearchUiEvent.ScopeChanged(scope))
                            },
                            text = {
                                Text(
                                    text = title,
                                    style = AppTypography.Caption.copy(fontSize = 12.sp),
                                    fontWeight = if (selectedScopeIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedScopeIndex == index) PrimaryIndigo else TextMuted
                                )
                            }
                        )
                    }
                }

                // Filter chips: Batch grouping + Uploader selection
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        FilterChip(
                            selected = uiState.isGroupedView,
                            onClick = { viewModel.onEvent(SearchUiEvent.ToggleGroupedView) },
                            label = {
                                Text(
                                    "Season Batches",
                                    style = AppTypography.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryIndigo.copy(alpha = 0.15f),
                                selectedLabelColor = PrimaryIndigo,
                                containerColor = DarkSurface,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = uiState.isGroupedView,
                                selectedBorderColor = PrimaryIndigo.copy(alpha = 0.4f),
                                borderColor = DarkCardBorder
                            ),
                            shape = RoundedCornerShape(6.dp)
                        )
                    }

                    val popularUploaders = listOf("All Uploaders", "SubsPlease", "Erai-raws", "EMBER", "Judas")
                    items(popularUploaders) { uploaderName ->
                        val isSelected = if (uploaderName == "All Uploaders") uiState.selectedUploaderFilter == null else uiState.selectedUploaderFilter == uploaderName
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.onEvent(SearchUiEvent.SetUploaderFilter(if (uploaderName == "All Uploaders") null else uploaderName))
                            },
                            label = {
                                Text(
                                    uploaderName,
                                    style = AppTypography.Caption.copy(fontSize = 11.sp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryIndigo.copy(alpha = 0.15f),
                                selectedLabelColor = PrimaryIndigo,
                                containerColor = DarkSurface,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                selectedBorderColor = PrimaryIndigo.copy(alpha = 0.4f),
                                borderColor = DarkCardBorder
                            ),
                            shape = RoundedCornerShape(6.dp)
                        )
                    }
                }

                // Offline banner
                if (uiState.isOffline) {
                    Surface(
                        color = AppSemanticColors.Warning.copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppSemanticColors.Warning.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.WifiOff, contentDescription = null, tint = AppSemanticColors.Warning, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Offline — showing cached results", style = AppTypography.Caption, color = TextSecondary)
                        }
                    }
                }

                // Content: Suggestions vs Results
                if (uiState.status is SearchStatus.Suggesting || uiState.query.isBlank()) {
                    SearchSuggestionsView(
                        suggestions = uiState.suggestions,
                        recentHistory = uiState.history,
                        onSelectSuggestion = { viewModel.onEvent(SearchUiEvent.SelectSuggestion(it)) },
                        onDeleteHistoryEntry = { viewModel.onEvent(SearchUiEvent.DeleteHistoryEntry(it)) },
                        onClearAllHistory = { viewModel.onEvent(SearchUiEvent.ClearAllHistory) },
                        onConvertToSavedSearch = { viewModel.onEvent(SearchUiEvent.ConvertToSavedSearch(it)) }
                    )
                } else if (uiState.status is SearchStatus.SearchingLocal || uiState.status is SearchStatus.SearchingProvider) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = PrimaryIndigo,
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 2.5.dp
                            )
                            Spacer(Modifier.height(12.dp))
                            Text("Searching…", style = AppTypography.Caption, color = TextMuted)
                        }
                    }
                } else if (uiState.status is SearchStatus.Empty) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No results", style = AppTypography.Title, color = TextPrimary)
                            Spacer(Modifier.height(4.dp))
                            Text("Try different keywords or relax filters", style = AppTypography.Caption, color = TextMuted)
                        }
                    }
                } else {
                    // Results list
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.results, key = { it.id }) { item ->
                            SearchResultRow(
                                item = item,
                                isSelectedForCompare = uiState.selectedForCompareIds.contains(item.id),
                                showCompareCheckbox = true,
                                onToggleCompare = { viewModel.onEvent(SearchUiEvent.ToggleCompareSelection(item.id)) },
                                onClick = {
                                    when (item) {
                                        is SearchResultItem.GroupedAnimeResult -> viewModel.onEvent(SearchUiEvent.OpenBatchSelectionSheet(item))
                                        is SearchResultItem.AnimeResult -> onAnimeClick(item.id)
                                        is SearchResultItem.ReleaseResult -> onReleaseClick(item.id)
                                        else -> Unit
                                    }
                                }
                            )
                        }
                    }

                    // Compare bar
                    if (uiState.selectedForCompareIds.size >= 2) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            color = DarkSurfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${uiState.selectedForCompareIds.size} selected",
                                    style = AppTypography.BodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = PrimaryIndigo
                                )
                                Button(
                                    onClick = { viewModel.onEvent(SearchUiEvent.OpenCompareSheet) },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.CompareArrows, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Compare", style = AppTypography.Caption.copy(fontWeight = FontWeight.SemiBold))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Advanced Filter Builder Modal
        if (uiState.showFilterSheet) {
            AdvancedFilterBuilderSheet(
                initialSelection = uiState.filterSelection,
                onApply = { viewModel.onEvent(SearchUiEvent.ApplyFilters(it)) },
                onSaveAsPreset = { viewModel.onEvent(SearchUiEvent.SaveAsPreset(it)) },
                onDismiss = { viewModel.onEvent(SearchUiEvent.SetFilterSheetVisible(false)) }
            )
        }

        // Release Comparison Modal
        if (uiState.showCompareSheet && uiState.comparisonResult != null) {
            ReleaseComparisonSheet(
                comparisonResult = uiState.comparisonResult!!,
                onSelectForDownload = { releaseId ->
                    viewModel.onEvent(SearchUiEvent.CloseCompareSheet)
                    onReleaseClick(releaseId)
                },
                onDismiss = { viewModel.onEvent(SearchUiEvent.CloseCompareSheet) }
            )
        }

        // Batch Selection Modal
        if (uiState.showBatchSelectionSheet && uiState.selectedGroupForBatch != null) {
            EpisodeBatchSelectionSheet(
                groupedAnime = uiState.selectedGroupForBatch!!,
                onQueueBatchDownload = { releases ->
                    viewModel.onEvent(SearchUiEvent.BatchQueueDownloads(releases))
                },
                onDismiss = { viewModel.onEvent(SearchUiEvent.CloseBatchSelectionSheet) }
            )
        }

        // Batch success toast
        uiState.batchSuccessNotification?.let { msg ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    color = AppSemanticColors.Success,
                    shape = RoundedCornerShape(8.dp),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = msg,
                            style = AppTypography.Caption.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White
                        )
                        Spacer(Modifier.width(12.dp))
                        IconButton(
                            onClick = { viewModel.onEvent(SearchUiEvent.DismissBatchSuccessNotification) },
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Dismiss", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}
