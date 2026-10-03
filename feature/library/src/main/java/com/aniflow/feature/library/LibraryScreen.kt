package com.aniflow.feature.library

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
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
import com.aniflow.domain.identity.AnimeId
import com.aniflow.core.ui.components.AniEmptyState
import com.aniflow.core.ui.components.AniLoadingState
import com.aniflow.domain.identity.EpisodeId
import com.aniflow.domain.identity.LibraryFileId
import com.aniflow.domain.identity.LibraryItemId
import com.aniflow.domain.identity.SeasonId
import com.aniflow.domain.identity.StorageId
import com.aniflow.domain.library.model.DuplicateMediaType
import com.aniflow.domain.library.model.DuplicateResolutionRecommendation
import com.aniflow.domain.library.model.LibraryItemType
import com.aniflow.domain.repository.LibraryRepository
import com.aniflow.domain.storage.model.StorageLocation
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class AnimeLibraryCardUiModel(
    val id: String,
    val title: String,
    val seasonCount: Int,
    val totalEpisodes: Int,
    val availableEpisodes: Int,
    val formattedTotalSize: String,
    val lastAddedFormatted: String,
    val upgradeAvailableCount: Int = 0
) {
    val coveragePercent: Float
        get() = if (totalEpisodes > 0) (availableEpisodes.toFloat() / totalEpisodes.toFloat()) * 100f else 0f
}

data class EpisodeLibraryCardUiModel(
    val episodeId: String,
    val episodeNumber: String,
    val title: String,
    val resolution: String,
    val codec: String,
    val formattedSize: String,
    val isLocalAvailable: Boolean,
    val upgradeCandidateTitle: String? = null
)

data class UnidentifiedFileUiModel(
    val fileId: String,
    val fileName: String,
    val relativePath: String,
    val formattedSize: String
)

data class DuplicateFileUiModel(
    val fileAId: String,
    val fileAName: String,
    val fileBId: String,
    val fileBName: String,
    val formattedSize: String,
    val duplicateType: String
)

data class LibraryScreenUiState(
    val animeCards: List<AnimeLibraryCardUiModel> = emptyList(),
    val unidentifiedFiles: List<UnidentifiedFileUiModel> = emptyList(),
    val duplicateFiles: List<DuplicateFileUiModel> = emptyList(),
    val searchQuery: String = "",
    val isScanning: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

class LibraryViewModel(
    private val libraryRepository: LibraryRepository? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow(LibraryScreenUiState(isLoading = true))
    val uiState: StateFlow<LibraryScreenUiState> = _uiState.asStateFlow()

    init {
        observeLibraryItems()
    }

    private fun observeLibraryItems() {
        if (libraryRepository == null) {
            _uiState.value = _uiState.value.copy(isLoading = false)
            return
        }

        libraryRepository.observeItems()
            .onEach { items ->
                val animeItems = items.filter { it.mediaIdentity.animeId != null }
                val cards = animeItems.map { item ->
                    AnimeLibraryCardUiModel(
                        id = item.id.value,
                        title = item.mediaIdentity.canonicalTitle.ifBlank { "Untitled Anime" },
                        seasonCount = 1,
                        totalEpisodes = 0,
                        availableEpisodes = 0,
                        formattedTotalSize = "0 B",
                        lastAddedFormatted = "Recently",
                        upgradeAvailableCount = 0
                    )
                }
                val unidentified = items.filter { it.mediaIdentity.animeId == null }.map { item ->
                    UnidentifiedFileUiModel(
                        fileId = item.id.value,
                        fileName = item.mediaIdentity.canonicalTitle.ifBlank { "Unidentified File" },
                        relativePath = "library/${item.mediaIdentity.canonicalTitle.ifBlank { "file" }}",
                        formattedSize = "Unknown"
                    )
                }
                _uiState.value = _uiState.value.copy(
                    animeCards = cards,
                    unidentifiedFiles = unidentified,
                    isLoading = false,
                    error = null
                )
            }
            .catch { ex ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = ex.message ?: "Failed to observe library items"
                )
            }
            .launchIn(viewModelScope)
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun onScanRequested() {
        _uiState.value = _uiState.value.copy(isScanning = true)
    }

    fun onManualMap(fileId: String, animeTitle: String, season: Int, episode: Double) {}
    fun onRename(fileId: String, newName: String) {}
    fun onMove(fileId: String, targetStorageId: String) {}
    fun onDelete(fileId: String, deletePhysical: Boolean) {}
}

/**
 * LibraryScreen (Section 147, 148, 149, 150, 151, 152).
 * Tabs: Recently Added, Continue, Anime, Movies, Specials, Upgrades, Duplicates, Unidentified, Storage.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onAnimeClick: (String) -> Unit = {},
    onNavigateStorage: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(2) } // Default to "Anime" tab

    val tabs = listOf(
        "Recently Added",
        "Continue",
        "Anime",
        "Movies",
        "Specials",
        "Upgrades",
        "Duplicates",
        "Unidentified",
        "Storage"
    )

    // Modals
    var manualMapTarget by remember { mutableStateOf<UnidentifiedFileUiModel?>(null) }
    var deleteFileTarget by remember { mutableStateOf<Pair<String, String>?>(null) }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Movie, contentDescription = null, tint = PrimaryIndigo)
                        Spacer(modifier = Modifier.width(AppSpacing.sm))
                        Text(
                            text = "AniFlow Library",
                            style = AppTypography.headlineMedium,
                            color = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onScanRequested() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Scan Library", tint = PrimaryIndigo)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Scrollable section tabs (Section 147)
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkSurface,
                contentColor = PrimaryIndigo,
                edgePadding = AppSpacing.md,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = PrimaryIndigo
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = {
                            if (title == "Storage") {
                                onNavigateStorage()
                            } else {
                                selectedTab = index
                            }
                        },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = title,
                                    style = AppTypography.labelLarge,
                                    color = if (selectedTab == index) PrimaryIndigo else TextSecondary
                                )
                                if (title == "Unidentified" && state.unidentifiedFiles.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(AppSemanticColors.Warning, AppShapes.small)
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = state.unidentifiedFiles.size.toString(),
                                            style = AppTypography.labelSmall.copy(fontSize = 10.sp),
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }

            // Tab Content
            when (selectedTab) {
                0, 2 -> AnimeLibraryGrid(
                    animeCards = state.animeCards,
                    onAnimeClick = onAnimeClick
                )
                5 -> UpgradesSection(
                    animeCards = state.animeCards.filter { it.upgradeAvailableCount > 0 },
                    onAnimeClick = onAnimeClick
                )
                6 -> DuplicatesSection(
                    duplicates = state.duplicateFiles,
                    onResolve = {}
                )
                7 -> UnidentifiedSection(
                    files = state.unidentifiedFiles,
                    onMapClick = { manualMapTarget = it },
                    onDeleteClick = { deleteFileTarget = it.fileId to it.fileName }
                )
                else -> AnimeLibraryGrid(
                    animeCards = state.animeCards,
                    onAnimeClick = onAnimeClick
                )
            }
        }
    }

    // Manual Map Dialog (Section 39, 131)
    manualMapTarget?.let { file ->
        ManualMapModalDialog(
            file = file,
            onDismiss = { manualMapTarget = null },
            onConfirm = { anime, season, ep ->
                viewModel.onManualMap(file.fileId, anime, season, ep)
                manualMapTarget = null
            }
        )
    }

    // Delete Confirmation Dialog (Section 155)
    deleteFileTarget?.let { (fileId, fileName) ->
        DeleteConfirmationModalDialog(
            fileName = fileName,
            onDismiss = { deleteFileTarget = null },
            onRemoveIndexOnly = {
                viewModel.onDelete(fileId, deletePhysical = false)
                deleteFileTarget = null
            },
            onDeletePhysical = {
                viewModel.onDelete(fileId, deletePhysical = true)
                deleteFileTarget = null
            }
        )
    }
}

@Composable
fun AnimeLibraryGrid(
    animeCards: List<AnimeLibraryCardUiModel>,
    onAnimeClick: (String) -> Unit
) {
    if (animeCards.isEmpty()) {
        AniEmptyState(
            title = "No anime in library",
            description = "Downloaded and indexed anime will appear here organized by series and seasons.",
            icon = Icons.Default.Folder
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 280.dp),
            contentPadding = PaddingValues(AppSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
            modifier = Modifier.fillMaxSize()
        ) {
            items(animeCards, key = { it.id }) { anime ->
                AnimeLibraryCard(anime = anime, onClick = { onAnimeClick(anime.id) })
            }
        }
    }
}

/**
 * AnimeLibraryCard (Section 148).
 * Shows title, season count, coverage progress bar, total size, last added, upgrade count.
 */
@Composable
fun AnimeLibraryCard(
    anime: AnimeLibraryCardUiModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, DarkCardBorder, AppShapes.large),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = anime.title,
                        style = AppTypography.Title,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${anime.seasonCount} Seasons • ${anime.availableEpisodes}/${anime.totalEpisodes} Episodes",
                        style = AppTypography.BodySmall,
                        color = TextSecondary
                    )
                }

                if (anime.upgradeAvailableCount > 0) {
                    Box(
                        modifier = Modifier
                            .background(PrimaryIndigo.copy(alpha = 0.2f), AppShapes.small)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${anime.upgradeAvailableCount} UPGRADE",
                            style = AppTypography.Caption.copy(fontSize = 9.sp),
                            color = PrimaryIndigo,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // Episode Coverage progress bar (Section 100, 148)
            LinearProgressIndicator(
                progress = { anime.coveragePercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = if (anime.coveragePercent >= 100f) AppSemanticColors.Success else PrimaryIndigo,
                trackColor = DarkBackground,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(AppSpacing.xs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${anime.coveragePercent.toInt()}% Coverage",
                    style = AppTypography.Caption,
                    color = if (anime.coveragePercent >= 100f) AppSemanticColors.Success else TextMuted
                )
                Text(
                    text = anime.formattedTotalSize,
                    style = AppTypography.Caption,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun UpgradesSection(
    animeCards: List<AnimeLibraryCardUiModel>,
    onAnimeClick: (String) -> Unit
) {
    if (animeCards.isEmpty()) {
        AniEmptyState(
            title = "No upgrades available",
            description = "All indexed episodes meet or exceed your active download profile quality targets.",
            icon = Icons.Default.Upgrade
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, PrimaryIndigo.copy(alpha = 0.3f), AppShapes.medium),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Row(modifier = Modifier.padding(AppSpacing.md), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Upgrade, contentDescription = null, tint = PrimaryIndigo)
                        Spacer(modifier = Modifier.width(AppSpacing.sm))
                        Text(
                            "Higher quality releases available for local episodes adhering to user download profile.",
                            style = AppTypography.BodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            items(animeCards) { anime ->
                AnimeLibraryCard(anime = anime, onClick = { onAnimeClick(anime.id) })
            }
        }
    }
}

@Composable
fun DuplicatesSection(
    duplicates: List<DuplicateFileUiModel>,
    onResolve: (DuplicateFileUiModel) -> Unit
) {
    if (duplicates.isEmpty()) {
        AniEmptyState(
            title = "No duplicate files found",
            description = "Your storage roots contain zero duplicate media files or redundant releases.",
            icon = Icons.Default.Folder
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            items(duplicates) { dup ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DarkCardBorder, AppShapes.medium),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(AppSpacing.md)) {
                        Text(dup.duplicateType, style = AppTypography.Subtitle, color = PrimaryIndigo, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(AppSpacing.xs))
                        Text("File A: ${dup.fileAName}", style = AppTypography.BodySmall, color = TextPrimary)
                        Text("File B: ${dup.fileBName}", style = AppTypography.BodySmall, color = TextMuted)
                        Spacer(modifier = Modifier.height(AppSpacing.sm))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            OutlinedButton(onClick = { onResolve(dup) }) {
                                Text("Compare & Keep")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UnidentifiedSection(
    files: List<UnidentifiedFileUiModel>,
    onMapClick: (UnidentifiedFileUiModel) -> Unit,
    onDeleteClick: (UnidentifiedFileUiModel) -> Unit
) {
    if (files.isEmpty()) {
        AniEmptyState(
            title = "No unidentified media",
            description = "All media files in storage roots have been identified and mapped to library series.",
            icon = Icons.Default.HelpOutline
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AppSemanticColors.Warning.copy(alpha = 0.4f), AppShapes.medium),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Row(modifier = Modifier.padding(AppSpacing.md), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.HelpOutline, contentDescription = null, tint = AppSemanticColors.Warning)
                        Spacer(modifier = Modifier.width(AppSpacing.sm))
                        Text(
                            "Ambiguous files where anime identity could not be confidently determined. Map them manually to include them in coverage.",
                            style = AppTypography.BodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            items(files) { file ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DarkCardBorder, AppShapes.medium),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppSpacing.md),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(file.fileName, style = AppTypography.Body, color = TextPrimary)
                            Text(file.relativePath, style = AppTypography.BodySmall, color = TextMuted)
                        }

                        Row {
                            Button(
                                onClick = { onMapClick(file) },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text("Map", style = AppTypography.Subtitle)
                            }

                            Spacer(modifier = Modifier.width(AppSpacing.xs))

                            IconButton(onClick = { onDeleteClick(file) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Manual mapping modal dialog (Section 39, 40, 131).
 */
@Composable
fun ManualMapModalDialog(
    file: UnidentifiedFileUiModel,
    onDismiss: () -> Unit,
    onConfirm: (animeTitle: String, season: Int, episode: Double) -> Unit
) {
    var animeTitle by remember { mutableStateOf("") }
    var seasonNumber by remember { mutableStateOf("1") }
    var episodeNumber by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Map File to Anime", color = TextPrimary) },
        text = {
            Column {
                Text(file.fileName, style = AppTypography.bodySmall, color = TextMuted)
                Spacer(modifier = Modifier.height(AppSpacing.md))
                OutlinedTextField(
                    value = animeTitle,
                    onValueChange = { animeTitle = it },
                    label = { Text("Anime Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                    OutlinedTextField(
                        value = seasonNumber,
                        onValueChange = { seasonNumber = it },
                        label = { Text("Season") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = episodeNumber,
                        onValueChange = { episodeNumber = it },
                        label = { Text("Episode") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val s = seasonNumber.toIntOrNull() ?: 1
                    val ep = episodeNumber.toDoubleOrNull() ?: 1.0
                    onConfirm(animeTitle, s, ep)
                },
                enabled = animeTitle.isNotBlank()
            ) {
                Text("Confirm Mapping")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Section 155: Delete Confirmation Dialog.
 * Clearly differentiates "Remove from Library only" vs "Delete physical file".
 */
@Composable
fun DeleteConfirmationModalDialog(
    fileName: String,
    onDismiss: () -> Unit,
    onRemoveIndexOnly: () -> Unit,
    onDeletePhysical: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete / Remove File", color = TextPrimary) },
        text = {
            Column {
                Text("Select action for: $fileName", style = AppTypography.bodyMedium, color = TextSecondary)
                Spacer(modifier = Modifier.height(AppSpacing.md))
                Text(
                    "• Remove from Library only: Removes database index record; physical file on device is kept safe.",
                    style = AppTypography.bodySmall,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(AppSpacing.xs))
                Text(
                    "• Delete physical file: Permanently erases the file from storage disk.",
                    style = AppTypography.bodySmall,
                    color = AppSemanticColors.Error
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDeletePhysical,
                colors = ButtonDefaults.buttonColors(containerColor = AppSemanticColors.Error)
            ) {
                Text("Delete Physical File")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onRemoveIndexOnly) {
                Text("Remove from Library Only")
            }
        }
    )
}
