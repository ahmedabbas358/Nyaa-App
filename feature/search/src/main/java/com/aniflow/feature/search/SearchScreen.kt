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
 * Universal SearchScreen (Sections 7, 15, 23, 28, 30, 31, 32, 40, 41).
 * High-performance Discovery interface uniting Local FTS and Remote Providers with:
 * - Smart suggestions and recency ranking
 * - Advanced filter builder with AST translation
 * - Multi-release comparison modal
 * - Multi-source state transparency (Local vs Nyaa vs Offline fallback)
 * - Adaptive layout for Phone and Tablet landscape
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
        "Universal" to SearchScope.Universal,
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
                            placeholder = { Text("Search anime, episodes, uploader, library…", style = AppTypography.caption) },
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
                            shape = RoundedCornerShape(AppShapes.sm),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryIndigo,
                                unfocusedBorderColor = DarkCardBorder,
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        )
                    },
                    actions = {
                        IconButton(onClick = { viewModel.onEvent(SearchUiEvent.SetFilterSheetVisible(true)) }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Advanced Filters", tint = PrimaryIndigo)
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
                // Scope Filter Tabs (Section 2)
                TabRow(
                    selectedTabIndex = selectedScopeIndex,
                    containerColor = DarkSurface,
                    contentColor = PrimaryIndigo,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedScopeIndex]),
                            color = PrimaryIndigo
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
                                    style = AppTypography.body.copy(fontSize = 12.sp),
                                    fontWeight = if (selectedScopeIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedScopeIndex == index) PrimaryIndigo else TextMuted
                                )
                            }
                        )
                    }
                }

                // YouTube-style Filter Chips Row (Auto-Batch Grouping & Uploader Selection)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.md, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        FilterChip(
                            selected = uiState.isGroupedView,
                            onClick = { viewModel.onEvent(SearchUiEvent.ToggleGroupedView) },
                            label = { Text("📦 Grouped Anime", style = AppTypography.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryIndigo.copy(alpha = 0.25f),
                                selectedLabelColor = PrimaryIndigo,
                                containerColor = DarkSurface,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = uiState.isGroupedView,
                                selectedBorderColor = PrimaryIndigo,
                                borderColor = DarkCardBorder
                            ),
                            shape = RoundedCornerShape(AppShapes.sm)
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
                            label = { Text(uploaderName, style = AppTypography.caption.copy(fontSize = 11.sp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryIndigo.copy(alpha = 0.25f),
                                selectedLabelColor = PrimaryIndigo,
                                containerColor = DarkSurface,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                selectedBorderColor = PrimaryIndigo,
                                borderColor = DarkCardBorder
                            ),
                            shape = RoundedCornerShape(AppShapes.sm)
                        )
                    }
                }

                // Offline Notice Banner (Section 28)
                if (uiState.isOffline) {
                    Surface(
                        color = AppSemanticColors.Warning.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppSemanticColors.Warning),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppSpacing.sm)
                    ) {
                        Row(
                            modifier = Modifier.padding(AppSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.WifiOff, contentDescription = null, tint = AppSemanticColors.Warning, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Offline Mode: Showing local library and cache results.", style = AppTypography.caption, color = TextPrimary)
                        }
                    }
                }

                // Content View: Suggestions vs Results
                if (uiState.status is SearchStatus.Suggesting || uiState.query.isBlank()) {
                    // Smart Suggestions & Recent Searches View (Sections 8, 9, 10)
                    SearchSuggestionsView(
                        suggestions = uiState.suggestions,
                        recentHistory = uiState.history,
                        onSelectSuggestion = { viewModel.onEvent(SearchUiEvent.SelectSuggestion(it)) },
                        onDeleteHistoryEntry = { viewModel.onEvent(SearchUiEvent.DeleteHistoryEntry(it)) },
                        onClearAllHistory = { viewModel.onEvent(SearchUiEvent.ClearAllHistory) },
                        onConvertToSavedSearch = { viewModel.onEvent(SearchUiEvent.ConvertToSavedSearch(it)) }
                    )
                } else if (uiState.status is SearchStatus.SearchingLocal || uiState.status is SearchStatus.SearchingProvider) {
                    // Loading State
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = PrimaryIndigo, modifier = Modifier.size(36.dp))
                            Spacer(Modifier.height(AppSpacing.md))
                            Text("Searching local & provider indices…", style = AppTypography.caption, color = TextMuted)
                        }
                    }
                } else if (uiState.status is SearchStatus.Empty) {
                    // Empty Results State
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No Results Found", style = AppTypography.headline.copy(fontSize = 16.sp), color = TextPrimary)
                            Spacer(Modifier.height(4.dp))
                            Text("Try adjusting your spelling or relaxing filters.", style = AppTypography.caption, color = TextMuted)
                        }
                    }
                } else {
                    // Search Results List (Section 16, 30, 31)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(AppSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
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

                    // Floating Comparison Action Bar (Section 32)
                    if (uiState.selectedForCompareIds.size >= 2) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AppSpacing.md),
                            color = DarkSurfaceVariant,
                            shape = RoundedCornerShape(AppShapes.sm),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryIndigo)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(AppSpacing.sm),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${uiState.selectedForCompareIds.size} releases selected",
                                    style = AppTypography.body.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                    color = PrimaryIndigo
                                )
                                Button(
                                    onClick = { viewModel.onEvent(SearchUiEvent.OpenCompareSheet) },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                                    shape = RoundedCornerShape(AppShapes.sm)
                                ) {
                                    Icon(Icons.Default.CompareArrows, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Compare Releases", style = AppTypography.caption)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Advanced Filter Builder Modal (Section 23, 24, 25)
        if (uiState.showFilterSheet) {
            AdvancedFilterBuilderSheet(
                initialSelection = uiState.filterSelection,
                onApply = { viewModel.onEvent(SearchUiEvent.ApplyFilters(it)) },
                onSaveAsPreset = { viewModel.onEvent(SearchUiEvent.SaveAsPreset(it)) },
                onDismiss = { viewModel.onEvent(SearchUiEvent.SetFilterSheetVisible(false)) }
            )
        }

        // Release Comparison Modal (Section 32)
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

        // Smart Episode Batch Selection Modal (YouTube & 1DM Style)
        if (uiState.showBatchSelectionSheet && uiState.selectedGroupForBatch != null) {
            EpisodeBatchSelectionSheet(
                groupedAnime = uiState.selectedGroupForBatch!!,
                onQueueBatchDownload = { releases ->
                    viewModel.onEvent(SearchUiEvent.BatchQueueDownloads(releases))
                },
                onDismiss = { viewModel.onEvent(SearchUiEvent.CloseBatchSelectionSheet) }
            )
        }

        // Batch Queue Success Notification Toast
        uiState.batchSuccessNotification?.let { msg ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AppSpacing.md),
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    color = AppSemanticColors.Success,
                    shape = RoundedCornerShape(AppShapes.sm),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(text = msg, style = AppTypography.body.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold), color = Color.White)
                        Spacer(Modifier.width(12.dp))
                        IconButton(
                            onClick = { viewModel.onEvent(SearchUiEvent.DismissBatchSuccessNotification) },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Dismiss", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}
