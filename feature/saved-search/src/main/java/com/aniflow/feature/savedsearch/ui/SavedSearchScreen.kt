package com.aniflow.feature.savedsearch.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.aniflow.domain.identity.SavedSearchId
import com.aniflow.domain.identity.SavedSearchRunId
import com.aniflow.domain.savedsearch.model.ProviderScope
import com.aniflow.domain.savedsearch.model.SavedSearch
import com.aniflow.domain.savedsearch.model.SavedSearchRun
import com.aniflow.domain.savedsearch.model.SearchExpression
import com.aniflow.domain.savedsearch.model.SearchFilters
import com.aniflow.domain.savedsearch.model.SearchRunStatus
import com.aniflow.domain.savedsearch.model.SearchSort
import java.time.Instant

/**
 * SavedSearchScreen (Sections 3, 4, 5, 6, 70, 108, 151).
 * Displays saved searches, execution actions (Run Now, Schedule, Automate), and run histories.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedSearchScreen(
    onBack: () -> Unit = {},
    onAutomate: (SavedSearch) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedSearchForHistory by remember { mutableStateOf<SavedSearch?>(null) }

    val savedSearches = remember {
        mutableStateListOf(
            SavedSearch(
                id = SavedSearchId("ss_1"),
                name = "One Piece 1080p HEVC Erai",
                query = SearchExpression.Structured(
                    animeTitle = "One Piece",
                    resolution = "1080p",
                    codec = "HEVC",
                    releaseGroup = "Erai-raws"
                ),
                providerScope = ProviderScope.NyaaOnly,
                filters = SearchFilters(minSeeds = 5),
                sort = SearchSort.DateDesc,
                enabled = true,
                createdAt = Instant.now().minusSeconds(86400 * 7),
                updatedAt = Instant.now()
            ),
            SavedSearch(
                id = SavedSearchId("ss_2"),
                name = "Jujutsu Kaisen Season 2 SubsPlease",
                query = SearchExpression.Structured(
                    animeTitle = "Jujutsu Kaisen",
                    season = 2,
                    releaseGroup = "SubsPlease"
                ),
                providerScope = ProviderScope.Global,
                filters = SearchFilters(trustedOnly = true),
                sort = SearchSort.DateDesc,
                enabled = true,
                createdAt = Instant.now().minusSeconds(86400 * 14),
                updatedAt = Instant.now()
            ),
            SavedSearch(
                id = SavedSearchId("ss_3"),
                name = "Bleach TYBW BDRip Batches",
                query = SearchExpression.Raw("Bleach Thousand-Year Blood War BDRip 1080p"),
                providerScope = ProviderScope.NyaaOnly,
                filters = SearchFilters(minSeeds = 2),
                sort = SearchSort.SeedersDesc,
                enabled = false,
                createdAt = Instant.now().minusSeconds(86400 * 30),
                updatedAt = Instant.now()
            )
        )
    }

    val sampleRuns = remember {
        listOf(
            SavedSearchRun(
                id = SavedSearchRunId("run_1"),
                savedSearchId = SavedSearchId("ss_1"),
                startedAt = Instant.now().minusSeconds(3600),
                completedAt = Instant.now().minusSeconds(3580),
                resultCount = 18,
                newReleaseCount = 1,
                status = SearchRunStatus.Success
            ),
            SavedSearchRun(
                id = SavedSearchRunId("run_2"),
                savedSearchId = SavedSearchId("ss_1"),
                startedAt = Instant.now().minusSeconds(86400),
                completedAt = Instant.now().minusSeconds(86382),
                resultCount = 17,
                newReleaseCount = 0,
                status = SearchRunStatus.Success
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved Searches", style = AppTypography.headline, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            items(savedSearches, key = { it.id.value }) { search ->
                SavedSearchCard(
                    savedSearch = search,
                    onToggleEnabled = { enabled ->
                        val idx = savedSearches.indexOf(search)
                        if (idx != -1) {
                            savedSearches[idx] = search.copy(enabled = enabled)
                        }
                    },
                    onRunNow = { /* Triggers instant search */ },
                    onAutomate = { onAutomate(search) },
                    onViewHistory = { selectedSearchForHistory = search },
                    onDelete = { savedSearches.remove(search) }
                )
            }
        }
    }

    selectedSearchForHistory?.let { search ->
        SavedSearchHistoryBottomSheet(
            search = search,
            runs = sampleRuns,
            onDismiss = { selectedSearchForHistory = null }
        )
    }
}

@Composable
private fun SavedSearchCard(
    savedSearch: SavedSearch,
    onToggleEnabled: (Boolean) -> Unit,
    onRunNow: () -> Unit,
    onAutomate: () -> Unit,
    onViewHistory: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(AppShapes.md),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(savedSearch.name, style = AppTypography.headline.copy(fontSize = 16.sp), color = TextPrimary)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Query: ${savedSearch.query.toQueryString()}",
                        style = AppTypography.caption,
                        color = PrimaryIndigo
                    )
                }
                Switch(
                    checked = savedSearch.enabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(checkedThumbColor = PrimaryIndigo, checkedTrackColor = PrimaryIndigo.copy(alpha = 0.5f))
                )
            }

            Spacer(Modifier.height(AppSpacing.sm))

            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                SearchBadgeChip(label = savedSearch.providerScope.name, color = PrimaryIndigo)
                SearchBadgeChip(label = "Sort: ${savedSearch.sort.name}", color = TextMuted)
                if (savedSearch.filters.trustedOnly) {
                    SearchBadgeChip(label = "Trusted Only", color = AppSemanticColors.Success)
                }
            }

            Spacer(Modifier.height(AppSpacing.md))

            // Action row (Run Now, Schedule, Automate, History)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                OutlinedButton(
                    onClick = onRunNow,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(AppShapes.sm)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Run", style = AppTypography.caption)
                }
                OutlinedButton(
                    onClick = onAutomate,
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(AppShapes.sm)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryIndigo)
                    Spacer(Modifier.width(4.dp))
                    Text("Automate", style = AppTypography.caption)
                }
                OutlinedButton(
                    onClick = onViewHistory,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(AppShapes.sm)
                ) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("History", style = AppTypography.caption)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AppSemanticColors.Error)
                }
            }
        }
    }
}

@Composable
private fun SearchBadgeChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(label, style = AppTypography.caption.copy(fontSize = 11.sp), color = color)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavedSearchHistoryBottomSheet(
    search: SavedSearch,
    runs: List<SavedSearchRun>,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.lg)
        ) {
            Text("Execution History: ${search.name}", style = AppTypography.headline, color = TextPrimary)
            Spacer(Modifier.height(AppSpacing.md))

            if (runs.isEmpty()) {
                Text("No recorded runs for this search.", style = AppTypography.body, color = TextMuted)
            } else {
                runs.forEach { run ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkBackground),
                        shape = RoundedCornerShape(AppShapes.sm),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AppSpacing.sm),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Status: ${run.status.name} • ${run.resultCount} results",
                                    style = AppTypography.body.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                    color = if (run.status == SearchRunStatus.Success) AppSemanticColors.Success else AppSemanticColors.Error
                                )
                                Text(
                                    text = "Discovered: ${run.newReleaseCount} new releases",
                                    style = AppTypography.caption,
                                    color = TextMuted
                                )
                            }
                            Text("1h ago", style = AppTypography.caption, color = TextMuted)
                        }
                    }
                }
            }

            Spacer(Modifier.height(AppSpacing.lg))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
            ) {
                Text("Close")
            }
        }
    }
}
