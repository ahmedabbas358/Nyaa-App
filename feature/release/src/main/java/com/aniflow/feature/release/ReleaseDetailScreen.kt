package com.aniflow.feature.release

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import com.aniflow.core.ui.util.TorrentClientBridge
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aniflow.core.common.result.AniFlowResult
import com.aniflow.core.ui.components.QualityBadge
import com.aniflow.core.ui.theme.Badge1080p
import com.aniflow.core.ui.theme.BadgeHevc
import com.aniflow.core.ui.theme.BadgeTrusted
import com.aniflow.core.ui.theme.DarkCardBorder
import com.aniflow.core.ui.theme.DarkSurface
import com.aniflow.core.ui.theme.ErrorRose
import com.aniflow.core.ui.theme.InfoBlue
import com.aniflow.core.ui.theme.PrimaryIndigo
import com.aniflow.core.ui.theme.SuccessGreen
import com.aniflow.core.ui.theme.TextSecondary
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.model.aggregate.release.Release
import com.aniflow.domain.model.aggregate.release.ReleaseSource
import com.aniflow.domain.repository.ReleaseRepository
import com.aniflow.domain.usecase.GetReleaseDetailsUseCase
import com.aniflow.domain.usecase.QueueDownloadUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Release Detail UI State (Step 18 Sections 51, 52).
 */
data class ReleaseDetailUiState(
    val release: Release? = null,
    val isLoading: Boolean = false,
    val isFavorite: Boolean = false,
    val toastMessage: String? = null
)

/**
 * ViewModel managing lazy release details retrieval and user actions.
 */
class ReleaseDetailViewModel(
    private val releaseRepository: ReleaseRepository,
    private val queueDownloadUseCase: QueueDownloadUseCase,
    private val releaseId: String,
    private val getReleaseDetailsUseCase: GetReleaseDetailsUseCase? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReleaseDetailUiState(isLoading = true))
    val uiState: StateFlow<ReleaseDetailUiState> = _uiState.asStateFlow()

    init {
        loadRelease(forceRefresh = false)
    }

    fun loadRelease(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            if (getReleaseDetailsUseCase != null) {
                when (val result = getReleaseDetailsUseCase(ReleaseId(releaseId), forceRefresh)) {
                    is AniFlowResult.Success -> {
                        _uiState.value = _uiState.value.copy(release = result.data, isLoading = false)
                    }
                    is AniFlowResult.Error -> {
                        val fallback = releaseRepository.getById(ReleaseId(releaseId))
                        _uiState.value = _uiState.value.copy(release = fallback, isLoading = false)
                    }
                    is AniFlowResult.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true)
                    }
                }
            } else {
                val r = releaseRepository.getById(ReleaseId(releaseId))
                _uiState.value = _uiState.value.copy(release = r, isLoading = false)
            }
        }
    }

    fun refreshDetails() {
        loadRelease(forceRefresh = true)
    }

    fun toggleFavorite() {
        _uiState.value = _uiState.value.copy(isFavorite = !_uiState.value.isFavorite)
    }

    fun downloadRelease() {
        val rel = _uiState.value.release ?: return
        viewModelScope.launch {
            queueDownloadUseCase(rel, "Downloads/AniFlow/${rel.animeIdentity?.rawTitle ?: rel.title}/${rel.title}")
            _uiState.value = _uiState.value.copy(toastMessage = "Download queued successfully")
        }
    }
}

/**
 * ReleaseDetailScreen (Sections 51, 52, 53, 54, 55, 91).
 * Displays rich release metadata, swarm stats, torrent/magnet actions, and refresh.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReleaseDetailScreen(
    viewModel: ReleaseDetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val release = state.release

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Release Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshDetails() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Details")
                    }
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            imageVector = if (state.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (state.isFavorite) ErrorRose else Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.isLoading && release == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryIndigo)
                }
            } else if (release != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Title & Provider Tag
                    Text(
                        text = release.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QualityBadge(text = release.provider.name, color = PrimaryIndigo)
                        release.technical.resolution?.let { QualityBadge(text = it.displayName, color = Badge1080p) }
                        release.technical.videoCodec?.let { QualityBadge(text = it.displayName, color = BadgeHevc) }
                        release.uploader?.let { QualityBadge(text = it.name, color = BadgeTrusted) }
                    }

                    // 2. Swarm & Size Stats Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = BorderStroke(1.dp, DarkCardBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Size", color = TextSecondary, fontSize = 12.sp)
                                val sizeStr = release.availability.size?.let { "${it.bytes / (1024 * 1024)} MB" } ?: "Unknown"
                                Text(sizeStr, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Seeders", color = TextSecondary, fontSize = 12.sp)
                                Text("${release.availability.seeders ?: 0}", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Leechers", color = TextSecondary, fontSize = 12.sp)
                                Text("${release.availability.leechers ?: 0}", color = ErrorRose, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Downloads", color = TextSecondary, fontSize = 12.sp)
                                Text("${release.availability.completedDownloads ?: 0}", color = InfoBlue, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }

                    // 3. Metadata Rows
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = BorderStroke(1.dp, DarkCardBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            MetadataRow("Anime Title", release.animeIdentity?.rawTitle ?: release.title)
                            release.seasonHint?.let { MetadataRow("Season", "${it.numericValue ?: it}") }
                            release.episodeRange?.let { MetadataRow("Episode", it.toString()) }
                            release.releaseGroup?.let { MetadataRow("Release Group", it.name) }
                            release.uploader?.let { MetadataRow("Uploader", it.name) }
                            if (release.source is ReleaseSource.Torrent) {
                                val t = release.source as ReleaseSource.Torrent
                                MetadataRow("InfoHash", t.infoHash.hexString.take(16) + "...")
                            }
                        }
                    }

                    val context = LocalContext.current
                    val t = release.source as? ReleaseSource.Torrent
                    val magnetUri = t?.magnetUri?.rawValue
                    val torrentUrl = t?.torrentUrl?.rawValue ?: "https://nyaa.si/download/${release.providerReleaseId}.torrent"
                    val detailsWebUrl = "https://nyaa.si/view/${release.providerReleaseId}"

                    // 4. Advanced Torrent & External Actions
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (!magnetUri.isNullOrBlank()) {
                                        TorrentClientBridge.copyToClipboard(context, magnetUri)
                                    } else {
                                        TorrentClientBridge.copyToClipboard(context, detailsWebUrl, toastMessage = "Page link copied")
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy Magnet", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    TorrentClientBridge.openInExternalTorrentClient(
                                        context = context,
                                        magnetUri = magnetUri,
                                        torrentUrl = torrentUrl,
                                        title = release.title
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("External Client", fontSize = 12.sp)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    TorrentClientBridge.downloadTorrentFileDirectly(context, torrentUrl, release.title)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save .torrent", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    TorrentClientBridge.openWebPage(context, detailsWebUrl)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open on Nyaa", fontSize = 12.sp)
                            }
                        }
                    }

                    // 5. Main Action CTA: Download inside AniFlow
                    Button(
                        onClick = { viewModel.downloadRelease() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download in AniFlow", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextSecondary, fontSize = 13.sp)
        Text(text = value, fontWeight = FontWeight.Medium, fontSize = 13.sp)
    }
}
