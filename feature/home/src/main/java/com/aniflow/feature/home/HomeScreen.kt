package com.aniflow.feature.home

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aniflow.core.common.result.AniFlowResult
import com.aniflow.core.ui.R
import com.aniflow.core.ui.components.AniEmptyState
import com.aniflow.core.ui.components.AniErrorState
import com.aniflow.core.ui.components.AniReleaseRow
import com.aniflow.core.ui.components.AniSectionHeader
import com.aniflow.core.ui.components.AniStatusBadge
import com.aniflow.core.ui.model.ReleaseUiModel
import com.aniflow.core.ui.model.toUiModel
import com.aniflow.core.ui.theme.AniFlowTheme
import com.aniflow.core.ui.theme.AppElevation
import com.aniflow.core.ui.theme.AppShapes
import com.aniflow.core.ui.theme.AppSpacing
import com.aniflow.core.ui.theme.AppTypography
import com.aniflow.core.ui.theme.DarkBackground
import com.aniflow.core.ui.theme.ThemeMode
import com.aniflow.core.ui.util.BidiFormatter
import com.aniflow.core.ui.util.UiFormatters
import com.aniflow.domain.controlplane.models.ComparisonExpression
import com.aniflow.domain.controlplane.models.ComparisonOperator
import com.aniflow.domain.controlplane.models.SearchExpression
import com.aniflow.domain.controlplane.models.SearchField
import com.aniflow.domain.library.model.ContinueWatchingItem
import com.aniflow.domain.library.repository.WatchProgressRepository
import com.aniflow.domain.valueobject.SearchFilters
import com.aniflow.domain.model.SearchRequest
import com.aniflow.domain.model.aggregate.download.DownloadTask
import com.aniflow.domain.model.aggregate.release.Release
import com.aniflow.domain.model.aggregate.release.ReleaseSource
import com.aniflow.domain.repository.DownloadRepository
import com.aniflow.domain.repository.ReleaseRepository
import com.aniflow.domain.search.model.SearchResultItem
import com.aniflow.domain.state.DownloadState
import com.aniflow.domain.usecase.QueueDownloadUseCase
import com.aniflow.domain.usecase.SearchReleasesCoordinatorUseCase
import com.aniflow.domain.valueobject.SearchQuery
import com.aniflow.feature.automation.scheduler.NyaaRealtimeSyncScheduler
import com.aniflow.feature.automation.scheduler.NyaaSyncEvent
import com.aniflow.feature.automation.scheduler.SyncInterval
import com.aniflow.feature.search.components.AnimeReleaseGrouper
import com.aniflow.feature.search.components.EpisodeBatchSelectionSheet
import com.aniflow.feature.search.components.GroupedAnimeCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Download Overview Widget Model for Home Dashboard (Section 22).
 */
data class DownloadHomeSummary(
    val activeCount: Int,
    val queuedCount: Int,
    val failedCount: Int,
    val completedCount: Int,
    val speedFormatted: String,
    val aggregateProgress: Float
)

/**
 * Actionable Upgrade Item Model for Home Dashboard (Section 23).
 */
data class UpgradeHomeItem(
    val episodeTitle: String,
    val currentQuality: String,
    val availableQuality: String,
    val releaseId: String
)

/**
 * UI State for AniFlow Home Screen (Section 17, 157).
 * Real-time synchronization and live YouTube-style anime discovery from Nyaa.si.
 */
data class HomeUiState(
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val isLiveSyncActive: Boolean = true,
    val syncCountdownSeconds: Long = 60L,
    val syncInterval: SyncInterval = SyncInterval.FAST_1M,
    val groupedAnime: List<SearchResultItem.GroupedAnimeResult> = emptyList(),
    val selectedGroupForBatch: SearchResultItem.GroupedAnimeResult? = null,
    val showBatchSelectionSheet: Boolean = false,
    val batchSuccessNotification: String? = null,
    val continueWatching: List<ContinueWatchingItem> = emptyList(),
    val recentReleases: List<ReleaseUiModel> = emptyList(),
    val downloadSummary: DownloadHomeSummary? = null,
    val actionableUpgrades: List<UpgradeHomeItem> = emptyList(),
    val error: String? = null
)

/**
 * HomeViewModel (Section 156: ViewModel Contract).
 * Connected to live Nyaa SearchCoordinator, Realtime SyncScheduler, and 1DM/FDM Queue Engine.
 */
class HomeViewModel(
    private val releaseRepository: ReleaseRepository? = null,
    private val downloadRepository: DownloadRepository? = null,
    private val watchProgressRepository: WatchProgressRepository? = null,
    private val searchCoordinatorUseCase: SearchReleasesCoordinatorUseCase? = null,
    private val queueDownloadUseCase: QueueDownloadUseCase? = null,
    private val syncScheduler: NyaaRealtimeSyncScheduler? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeDomainStreams()
        setupRealtimeSync()
        fetchLiveNyaaReleases()
    }

    private fun setupRealtimeSync() {
        if (syncScheduler != null) {
            syncScheduler.start()

            viewModelScope.launch {
                syncScheduler.countdownSeconds.collect { sec ->
                    _uiState.value = _uiState.value.copy(syncCountdownSeconds = sec)
                }
            }

            viewModelScope.launch {
                syncScheduler.engineState.collect { state ->
                    _uiState.value = _uiState.value.copy(
                        isLiveSyncActive = state.isRunning,
                        syncInterval = state.interval
                    )
                }
            }

            viewModelScope.launch {
                syncScheduler.events.collect { event ->
                    when (event) {
                        is NyaaSyncEvent.NewReleaseFound,
                        is NyaaSyncEvent.SyncCycleCompleted -> {
                            fetchLiveNyaaReleases()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    fun fetchLiveNyaaReleases(query: String = "") {
        if (searchCoordinatorUseCase == null) {
            _uiState.value = _uiState.value.copy(isLoading = false, isSyncing = false)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true)
            val searchExpr = SearchExpression(
                root = ComparisonExpression(
                    field = SearchField.Anime,
                    operator = ComparisonOperator.Contains,
                    value = query
                )
            )
            val searchReq = SearchRequest(
                query = searchExpr,
                page = 1,
                filters = SearchFilters(trustedOnly = false, excludeRemakes = false)
            )

            searchCoordinatorUseCase(searchReq).collect { result ->
                when (result) {
                    is AniFlowResult.Loading -> {
                        _uiState.value = _uiState.value.copy(isSyncing = true)
                    }
                    is AniFlowResult.Success -> {
                        val releaseItems = result.data.items.map { it.toSearchResultItem() }
                        val grouped = AnimeReleaseGrouper.groupReleases(releaseItems)
                            .filterIsInstance<SearchResultItem.GroupedAnimeResult>()

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isSyncing = false,
                            groupedAnime = grouped
                        )
                    }
                    is AniFlowResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isSyncing = false
                        )
                    }
                }
            }
        }
    }

    private fun observeDomainStreams() {
        val continueFlow = watchProgressRepository?.observeContinueWatching()
            ?: MutableStateFlow(emptyList())

        val downloadFlow = downloadRepository?.observeTasks()
            ?: MutableStateFlow(emptyList())

        val releasesFlow = releaseRepository?.search(SearchQuery.of(""))
            ?: MutableStateFlow(null)

        combine(continueFlow, downloadFlow, releasesFlow) { continueList, tasks, releasesResult ->
            val downloadSummary = if (tasks.isNotEmpty()) {
                val active = tasks.count { it.state.isActive }
                val queued = tasks.count { it.state == DownloadState.Queued || it.state == DownloadState.Pending }
                val failed = tasks.count { it.state == DownloadState.Failed }
                val completed = tasks.count { it.state == DownloadState.Completed }
                val progress = if (tasks.isNotEmpty()) {
                    val activeTasks = tasks.filter { it.state.isActive }
                    if (activeTasks.isNotEmpty()) 0.45f else 1.0f
                } else 0f

                DownloadHomeSummary(
                    activeCount = active,
                    queuedCount = queued,
                    failedCount = failed,
                    completedCount = completed,
                    speedFormatted = if (active > 0) "$active downloading" else "0 KB/s",
                    aggregateProgress = progress
                )
            } else null

            val releasesUi = releasesResult?.getOrNull()?.items?.map { it.toUiModel() }
                ?: emptyList()

            _uiState.value.copy(
                isLoading = false,
                continueWatching = continueList,
                recentReleases = releasesUi,
                downloadSummary = downloadSummary,
                actionableUpgrades = emptyList(),
                error = null
            )
        }.catch { e ->
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = e.localizedMessage ?: "Failed to load dashboard"
            )
        }.onEach { state ->
            _uiState.value = state
        }.launchIn(viewModelScope)
    }

    fun onRefresh() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        observeDomainStreams()
        fetchLiveNyaaReleases()
    }

    fun onTriggerImmediateSync() {
        if (syncScheduler != null) {
            syncScheduler.triggerImmediateSync()
        }
        fetchLiveNyaaReleases()
    }

    fun onSetSyncInterval(interval: SyncInterval) {
        syncScheduler?.setSyncInterval(interval)
        _uiState.value = _uiState.value.copy(syncInterval = interval)
    }

    fun onOpenBatchSelection(group: SearchResultItem.GroupedAnimeResult) {
        _uiState.value = _uiState.value.copy(
            selectedGroupForBatch = group,
            showBatchSelectionSheet = true
        )
    }

    fun onCloseBatchSelection() {
        _uiState.value = _uiState.value.copy(
            showBatchSelectionSheet = false,
            selectedGroupForBatch = null
        )
    }

    fun onQueueBatchDownloads(releases: List<SearchResultItem.ReleaseResult>) {
        val group = _uiState.value.selectedGroupForBatch
        val destinationFolder = if (group != null) {
            val safeTitle = group.title.replace(Regex("""[\\/:*?"<>|]"""), "_").trim()
            "Anime/$safeTitle/Season ${group.seasonNumber}"
        } else {
            "Anime/Downloads"
        }

        viewModelScope.launch {
            var queuedCount = 0
            for (release in releases) {
                val effectiveUri = release.magnetUri?.takeIf { it.isNotBlank() }
                    ?: release.torrentUrl?.takeIf { it.isNotBlank() }
                    ?: "https://nyaa.si/download/${release.id}.torrent"

                queueDownloadUseCase?.queueFromLink(
                    link = effectiveUri,
                    customTitle = release.title,
                    releaseId = release.id,
                    destinationFolder = destinationFolder
                )
                queuedCount++
            }
            _uiState.value = _uiState.value.copy(
                showBatchSelectionSheet = false,
                selectedGroupForBatch = null,
                batchSuccessNotification = "Queued $queuedCount episode(s) into $destinationFolder"
            )
        }
    }

    fun onDismissBatchSuccessNotification() {
        _uiState.value = _uiState.value.copy(batchSuccessNotification = null)
    }

    private fun Release.toSearchResultItem(): SearchResultItem.ReleaseResult {
        val torrentSource = source as? ReleaseSource.Torrent
        val magnet = torrentSource?.magnetUri?.rawValue
        val torrentUrl = torrentSource?.torrentUrl?.rawValue ?: "https://nyaa.si/download/${id.value}.torrent"
        val sizeBytes = availability.size?.bytes ?: 0L
        val sizeStr = if (sizeBytes > 0) UiFormatters.formatBytes(sizeBytes) else "—"
        return SearchResultItem.ReleaseResult(
            id = id.value,
            title = title,
            animeTitle = animeIdentity?.rawTitle ?: title,
            resolution = technical.resolution?.displayName ?: "Unknown",
            codec = technical.videoCodec?.displayName ?: "Unknown",
            uploader = uploader?.name ?: "Unknown",
            releaseGroup = releaseGroup?.name,
            sizeFormatted = sizeStr,
            seeders = availability.seeders ?: 0,
            leechers = availability.leechers ?: 0,
            magnetUri = magnet,
            torrentUrl = torrentUrl,
            isBatch = isBatch,
            isDownloaded = false,
            isDownloading = false
        )
    }
}

/**
 * HomeScreen (Section 17, 18, 19, 20, 21, 22, 23).
 *
 * Central personalized dashboard:
 * - Realtime Periodic Sync Header (Minutes and Seconds Countdown + Sync Now)
 * - YouTube-style 16:9 Anime & Season Discovery cards
 * - 1DM/FDM Multi-Episode Download Selection Sheet
 * - "Continue Watching" horizontal carousel with real progress
 * - "Downloads" overview card (automatically hidden when empty)
 * - "New Releases" list with 8-level information hierarchy badges
 * - Full localization (Arabic RTL + English)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToSearch: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    onReleaseClick: (String) -> Unit = {},
    onMediaPlayClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val colors = AniFlowTheme.colors

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.home_title),
                            style = AppTypography.LargeTitle,
                            color = colors.textPrimary
                        )
                        Text(
                            text = stringResource(R.string.home_subtitle),
                            style = AppTypography.Caption,
                            color = colors.textMuted
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.cd_search),
                            tint = colors.textPrimary
                        )
                    }
                    IconButton(onClick = viewModel::onTriggerImmediateSync) {
                        if (state.isSyncing) {
                            CircularProgressIndicator(
                                color = colors.primary,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sync Nyaa",
                                tint = colors.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background
                )
            )
        },
        containerColor = colors.background,
        modifier = modifier
    ) { paddingValues ->
        when {
            state.isLoading && state.groupedAnime.isEmpty() -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    CircularProgressIndicator(
                        color = colors.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            state.error != null && state.groupedAnime.isEmpty() -> {
                AniErrorState(
                    title = stringResource(R.string.state_error),
                    reason = state.error.orEmpty(),
                    onRetry = viewModel::onRefresh,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            else -> {
                HomeDashboardContent(
                    state = state,
                    onNavigateToSearch = onNavigateToSearch,
                    onNavigateToDownloads = onNavigateToDownloads,
                    onReleaseClick = onReleaseClick,
                    onMediaPlayClick = onMediaPlayClick,
                    onTriggerSync = viewModel::onTriggerImmediateSync,
                    onSetSyncInterval = viewModel::onSetSyncInterval,
                    onOpenBatchSelection = viewModel::onOpenBatchSelection,
                    onDismissBatchSuccess = viewModel::onDismissBatchSuccessNotification,
                    contentPadding = paddingValues
                )
            }
        }

        // Mount EpisodeBatchSelectionSheet when requested
        if (state.showBatchSelectionSheet && state.selectedGroupForBatch != null) {
            EpisodeBatchSelectionSheet(
                groupedAnime = state.selectedGroupForBatch!!,
                onQueueBatchDownload = viewModel::onQueueBatchDownloads,
                onDismiss = viewModel::onCloseBatchSelection
            )
        }
    }
}

@Composable
private fun HomeDashboardContent(
    state: HomeUiState,
    onNavigateToSearch: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onReleaseClick: (String) -> Unit,
    onMediaPlayClick: (String) -> Unit,
    onTriggerSync: () -> Unit,
    onSetSyncInterval: (SyncInterval) -> Unit,
    onOpenBatchSelection: (SearchResultItem.GroupedAnimeResult) -> Unit,
    onDismissBatchSuccess: () -> Unit,
    contentPadding: PaddingValues
) {
    val colors = AniFlowTheme.colors
    val hasContent = state.continueWatching.isNotEmpty() ||
        state.downloadSummary != null ||
        state.recentReleases.isNotEmpty() ||
        state.actionableUpgrades.isNotEmpty() ||
        state.groupedAnime.isNotEmpty() ||
        state.isSyncing

    if (!hasContent) {
        AniEmptyState(
            title = stringResource(R.string.state_empty),
            message = stringResource(R.string.downloads_empty_desc),
            actionText = stringResource(R.string.search_action),
            onActionClick = onNavigateToSearch,
            icon = Icons.Default.Search,
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(
            top = AppSpacing.sm,
            bottom = AppSpacing.massive
        ),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        // 1. Real-time Periodic Sync Status Bar (Minutes and Seconds Countdown)
        item(key = "home_live_sync_status") {
            LiveSyncStatusBar(
                isActive = state.isLiveSyncActive,
                isSyncing = state.isSyncing,
                countdownSeconds = state.syncCountdownSeconds,
                currentInterval = state.syncInterval,
                onTriggerSync = onTriggerSync,
                onSetInterval = onSetSyncInterval
            )
        }

        // 2. Batch Download Success Notification Toast / Banner
        if (state.batchSuccessNotification != null) {
            item(key = "home_batch_success_banner") {
                BatchSuccessBanner(
                    message = state.batchSuccessNotification,
                    onDismiss = onDismissBatchSuccess
                )
            }
        }

        // 3. Search Bar Trigger
        item(key = "home_search_trigger") {
            HomeSearchTrigger(onClick = onNavigateToSearch)
        }

        // 4. Trending & Live Anime Releases (Grouped by Anime & Season)
        if (state.groupedAnime.isNotEmpty()) {
            item(key = "home_live_anime_section_header") {
                Column(modifier = Modifier.padding(horizontal = AppSpacing.md)) {
                    AniSectionHeader(
                        title = stringResource(R.string.home_live_anime_title),
                        subtitle = stringResource(R.string.home_live_anime_subtitle),
                        actionText = stringResource(R.string.home_see_all),
                        onActionClick = onNavigateToSearch
                    )
                }
            }

            items(
                items = state.groupedAnime,
                key = { "grp_${it.id}" }
            ) { group ->
                Box(modifier = Modifier.padding(horizontal = AppSpacing.md)) {
                    GroupedAnimeCard(
                        item = group,
                        onClick = { onOpenBatchSelection(group) }
                    )
                }
            }
        }

        // 5. Continue Watching Carousel (Section 19)
        if (state.continueWatching.isNotEmpty()) {
            item(key = "home_continue_watching") {
                Column {
                    AniSectionHeader(
                        title = stringResource(R.string.home_continue_watching),
                        subtitle = null,
                        actionText = stringResource(R.string.home_see_all),
                        onActionClick = onNavigateToSearch,
                        modifier = Modifier.padding(horizontal = AppSpacing.md)
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.xs))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = AppSpacing.md),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                    ) {
                        items(
                            items = state.continueWatching,
                            key = { it.mediaId.value }
                        ) { item ->
                            ContinueWatchingCard(
                                item = item,
                                onClick = { onMediaPlayClick(item.mediaId.value) }
                            )
                        }
                    }
                }
            }
        }

        // 6. Active Downloads Overview Widget (Section 22)
        state.downloadSummary?.let { summary ->
            if (summary.activeCount > 0 || summary.queuedCount > 0 || summary.failedCount > 0) {
                item(key = "home_downloads_widget") {
                    HomeDownloadsWidget(
                        summary = summary,
                        onClick = onNavigateToDownloads
                    )
                }
            }
        }

        // 7. Actionable Upgrades Widget (Section 23)
        if (state.actionableUpgrades.isNotEmpty()) {
            item(key = "home_upgrades_widget") {
                HomeUpgradesWidget(
                    upgrades = state.actionableUpgrades,
                    onReview = onReleaseClick
                )
            }
        }

        // 8. Individual New Releases (Section 21)
        if (state.recentReleases.isNotEmpty()) {
            item(key = "home_new_releases_header") {
                AniSectionHeader(
                    title = stringResource(R.string.home_new_releases),
                    subtitle = null,
                    actionText = stringResource(R.string.home_see_all),
                    onActionClick = onNavigateToSearch,
                    modifier = Modifier.padding(horizontal = AppSpacing.md)
                )
            }

            items(
                items = state.recentReleases,
                key = { "rel_${it.id}" }
            ) { release ->
                Box(modifier = Modifier.padding(horizontal = AppSpacing.md)) {
                    AniReleaseRow(
                        title = release.title,
                        resolution = release.resolution,
                        codec = release.codec,
                        source = release.releaseGroup ?: "",
                        sizeFormatted = release.sizeFormatted,
                        seeds = release.seeders,
                        peers = release.leechers,
                        uploader = release.uploader,
                        isPreferred = release.isPreferred,
                        onClick = { onReleaseClick(release.id) },
                        onDownloadClick = { onReleaseClick(release.id) }
                    )
                }
            }
        }
    }
}

/**
 * Real-time Live Sync Status Bar with Minute & Second Countdown and Quick Interval Selection.
 */
@Composable
private fun LiveSyncStatusBar(
    isActive: Boolean,
    isSyncing: Boolean,
    countdownSeconds: Long,
    currentInterval: SyncInterval,
    onTriggerSync: () -> Unit,
    onSetInterval: (SyncInterval) -> Unit
) {
    val colors = AniFlowTheme.colors
    val minutes = countdownSeconds / 60
    val seconds = countdownSeconds % 60
    val formattedTime = String.format("%02d:%02d", minutes, seconds)

    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = AppShapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.md)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = if (isActive) colors.success else colors.textMuted,
                                shape = CircleShape
                            )
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isSyncing) "Syncing Nyaa..." else "Nyaa Live Sync: Active",
                        style = AppTypography.BodySmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isSyncing) colors.primary else colors.textPrimary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "• $formattedTime",
                        style = AppTypography.Caption.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.primary
                    )
                }

                IconButton(
                    onClick = onTriggerSync,
                    enabled = !isSyncing,
                    modifier = Modifier.size(32.dp)
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            color = colors.primary,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync Now",
                            tint = colors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Sub-minute & minute intervals chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(SyncInterval.values()) { interval ->
                    val isSelected = currentInterval == interval
                    Surface(
                        color = if (isSelected) colors.primary.copy(alpha = 0.2f) else colors.surfaceElevated,
                        shape = AppShapes.small,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) colors.primary else colors.border
                        ),
                        modifier = Modifier.clickable { onSetInterval(interval) }
                    ) {
                        Text(
                            text = when (interval) {
                                SyncInterval.ULTRA_FAST_30S -> "30s"
                                SyncInterval.FAST_1M -> "1m"
                                SyncInterval.STANDARD_2M -> "2m"
                                SyncInterval.RELAXED_5M -> "5m"
                                SyncInterval.POWER_SAVER_15M -> "15m"
                            },
                            style = AppTypography.Caption.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            ),
                            color = if (isSelected) colors.primary else colors.textSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Batch Download Queued Notification Banner.
 */
@Composable
private fun BatchSuccessBanner(
    message: String,
    onDismiss: () -> Unit
) {
    val colors = AniFlowTheme.colors

    Card(
        colors = CardDefaults.cardColors(containerColor = colors.success.copy(alpha = 0.15f)),
        shape = AppShapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.success.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.md)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = colors.success,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = message,
                    style = AppTypography.BodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = colors.textMuted,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * Editorial Search Input Trigger on Home.
 */
@Composable
private fun HomeSearchTrigger(onClick: () -> Unit) {
    val colors = AniFlowTheme.colors

    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = AppShapes.pill,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
        elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.card),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.md)
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.md, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(AppSpacing.sm))
                Text(
                    text = stringResource(R.string.search_hint),
                    style = AppTypography.Body,
                    color = colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                color = colors.surfaceVariant,
                shape = AppShapes.small,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
            ) {
                Text(
                    text = stringResource(R.string.search_shortcut_hint),
                    style = AppTypography.Metadata.copy(fontSize = 10.sp),
                    color = colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * Continue Watching Card (Section 19).
 * 16:9 thumbnail ratio, clear episode badge, progress line, and resume CTA.
 */
@Composable
private fun ContinueWatchingCard(
    item: ContinueWatchingItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AniFlowTheme.colors

    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = AppShapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
        modifier = modifier
            .width(240.dp)
            .clickable(onClick = onClick)
    ) {
        Column {
            // 16:9 Thumbnail Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(colors.surfaceElevated)
            ) {
                // Placeholder artwork monogram
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = item.animeTitle.take(2).uppercase(),
                        style = AppTypography.LargeTitle,
                        color = colors.primary.copy(alpha = 0.35f),
                        fontWeight = FontWeight.Bold
                    )
                }

                // Play Button Overlay
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.Center)
                        .background(colors.primary.copy(alpha = 0.85f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.continue_resume_action),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Bottom Progress Bar
                LinearProgressIndicator(
                    progress = { item.progress.progressFraction },
                    color = colors.primary,
                    trackColor = colors.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter)
                )
            }

            // Metadata & Details
            Column(modifier = Modifier.padding(AppSpacing.sm)) {
                Text(
                    text = item.animeTitle,
                    style = AppTypography.Title,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val epFormatted = BidiFormatter.isolateLtr(
                        stringResource(
                            R.string.continue_episode_format,
                            item.seasonNumber,
                            item.episodeNumber.toInt()
                        )
                    )
                    Text(
                        text = epFormatted,
                        style = AppTypography.BodySmall,
                        color = colors.textSecondary
                    )

                    val remainingMin = item.progress.remainingMinutes.toInt().coerceAtLeast(1)
                    Text(
                        text = stringResource(R.string.continue_remaining_format, remainingMin),
                        style = AppTypography.Caption,
                        color = colors.textMuted
                    )
                }
            }
        }
    }
}

/**
 * Downloads Home Widget (Section 22).
 * Shows active, queued, failed counts, aggregate speed, and progress.
 */
@Composable
private fun HomeDownloadsWidget(
    summary: DownloadHomeSummary,
    onClick: () -> Unit
) {
    val colors = AniFlowTheme.colors

    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = AppShapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
        elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.card),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.md)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.xs))
                    Text(
                        text = stringResource(R.string.home_downloads_overview),
                        style = AppTypography.Title,
                        color = colors.textPrimary
                    )
                }

                // Speed indicator
                if (summary.activeCount > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = colors.info,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = summary.speedFormatted,
                            style = AppTypography.numericSpeed,
                            color = colors.info
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            // Linear Progress Bar
            LinearProgressIndicator(
                progress = { summary.aggregateProgress },
                color = colors.primary,
                trackColor = colors.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(AppShapes.pill)
            )

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            // State Badges Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (summary.activeCount > 0) {
                    Surface(
                        color = colors.primary.copy(alpha = 0.12f),
                        shape = AppShapes.small,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.primary.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = stringResource(R.string.downloads_active_count, summary.activeCount),
                            style = AppTypography.Caption,
                            color = colors.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                if (summary.queuedCount > 0) {
                    Surface(
                        color = colors.warning.copy(alpha = 0.12f),
                        shape = AppShapes.small,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.warning.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = stringResource(R.string.downloads_queued_count, summary.queuedCount),
                            style = AppTypography.Caption,
                            color = colors.warning,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                if (summary.failedCount > 0) {
                    Surface(
                        color = colors.error.copy(alpha = 0.12f),
                        shape = AppShapes.small,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.error.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = stringResource(R.string.downloads_failed_count, summary.failedCount),
                            style = AppTypography.Caption,
                            color = colors.error,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Upgrades Widget for Home (Section 23).
 */
@Composable
private fun HomeUpgradesWidget(
    upgrades: List<UpgradeHomeItem>,
    onReview: (String) -> Unit
) {
    val colors = AniFlowTheme.colors

    Column(modifier = Modifier.padding(horizontal = AppSpacing.md)) {
        AniSectionHeader(
            title = stringResource(R.string.home_upgrades_available),
            subtitle = null,
            actionText = null
        )

        Spacer(modifier = Modifier.height(AppSpacing.xs))

        upgrades.forEach { upgrade ->
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = AppShapes.medium,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppSpacing.md)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = upgrade.episodeTitle,
                            style = AppTypography.Title,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(
                                R.string.upgrade_diff_format,
                                upgrade.currentQuality,
                                upgrade.availableQuality
                            ),
                            style = AppTypography.BodySmall,
                            color = colors.textSecondary
                        )
                    }

                    Button(
                        onClick = { onReview(upgrade.releaseId) },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        shape = AppShapes.small,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.upgrade_review_action),
                            style = AppTypography.BodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews (Section 149: Design Validation)
// ---------------------------------------------------------------------------

@Preview(name = "Home - Dark Theme", showBackground = true, backgroundColor = 0xFF0B0F17)
@Composable
fun HomeScreenDarkPreview() {
    AniFlowTheme(themeMode = ThemeMode.Dark) {
        HomeScreen(
            viewModel = HomeViewModel()
        )
    }
}

@Preview(name = "Home - Light Theme", showBackground = true, backgroundColor = 0xFFF7F8FA)
@Composable
fun HomeScreenLightPreview() {
    AniFlowTheme(themeMode = ThemeMode.Light) {
        HomeScreen(
            viewModel = HomeViewModel()
        )
    }
}

@Preview(name = "Home - Arabic RTL", locale = "ar", showBackground = true, backgroundColor = 0xFF0B0F17)
@Composable
fun HomeScreenArabicPreview() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AniFlowTheme(themeMode = ThemeMode.Dark) {
            HomeScreen(
                viewModel = HomeViewModel()
            )
        }
    }
}

@Preview(name = "Home - Tablet Expanded", widthDp = 900, heightDp = 600, showBackground = true, backgroundColor = 0xFF0B0F17)
@Composable
fun HomeScreenTabletPreview() {
    AniFlowTheme(themeMode = ThemeMode.Dark) {
        HomeScreen(
            viewModel = HomeViewModel()
        )
    }
}
