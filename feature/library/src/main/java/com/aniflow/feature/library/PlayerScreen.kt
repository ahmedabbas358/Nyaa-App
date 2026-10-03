package com.aniflow.feature.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aniflow.core.ui.components.AniEmptyState
import com.aniflow.core.ui.components.AniLoadingState
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
import com.aniflow.domain.identity.LibraryMediaId
import kotlinx.coroutines.delay

/**
 * PlayerScreen (Sections 72-80).
 * State-driven media player interface:
 * - Controls fade on idle (Section 73)
 * - 10s forward / rewind seek controls (Section 74)
 * - Audio Track Selection Bottom Sheet (Section 75)
 * - Subtitle Picker & Style Bottom Sheet (Section 76, 77)
 * - Playback Speed selector (Section 78)
 * - Player More Menu (Section 79)
 * - Playback Queue Sheet (Section 80)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    mediaId: LibraryMediaId,
    viewModel: PlayerViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onBack: () -> Unit = {},
    onDownloadNextEpisode: (Double) -> Unit = {},
    onSearchReleases: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    var controlsVisible by remember { mutableStateOf(true) }
    var showAudioSheet by remember { mutableStateOf(false) }
    var showSubtitleSheet by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showMoreMenuSheet by remember { mutableStateOf(false) }
    var showNextEpisodeDialog by remember { mutableStateOf(false) }

    // Auto-hide controls after 4 seconds of playback
    LaunchedEffect(controlsVisible, state.isPlaying) {
        if (controlsVisible && state.isPlaying) {
            delay(4000)
            controlsVisible = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { controlsVisible = !controlsVisible }
    ) {
        // Video Viewport Area
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            if (state.isLoading) {
                AniLoadingState(
                    message = "Loading media stream...",
                    subtext = state.currentItem?.animeTitle
                )
            } else if (state.currentItem != null) {
                Text(
                    text = "${state.currentItem?.animeTitle} — Episode ${state.currentItem?.episodeNumber?.toInt() ?: 1}",
                    style = AppTypography.LargeTitle,
                    color = Color.White.copy(alpha = 0.35f)
                )
            } else {
                Text(
                    text = "No active media stream",
                    style = AppTypography.Subtitle,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }

        // Subtitle Overlay (Section 76, 77)
        if (state.selectedSubtitleTrackId != null && state.currentItem != null) {
            Box(
                contentAlignment = Alignment.BottomCenter,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = if (controlsVisible) 100.dp else 40.dp)
            ) {
                Text(
                    text = "Subtitles active",
                    style = AppTypography.Subtitle.copy(
                        fontSize = state.preferences.subtitleStyle.fontSizeSp.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.65f), shape = AppShapes.small)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }

        // Player Controls HUD (Section 73)
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(AppSpacing.md)
            ) {
                // Top Bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.currentItem?.animeTitle ?: "Media Player",
                            style = AppTypography.Subtitle.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        state.currentItem?.episodeTitle?.let {
                            Text(it, style = AppTypography.Caption, color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                    Row {
                        IconButton(onClick = { showQueueSheet = true }) {
                            Icon(Icons.Default.QueueMusic, contentDescription = "Playback Queue", tint = Color.White)
                        }
                        IconButton(onClick = { showMoreMenuSheet = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More Options", tint = Color.White)
                        }
                    }
                }

                // Center Seek & Play Controls (Section 74)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = { viewModel.seekRelative(-10000L) }) {
                        Icon(
                            Icons.Default.Replay10,
                            contentDescription = "Rewind 10s",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(AppSpacing.xl))
                    IconButton(
                        onClick = { viewModel.togglePlayPause() },
                        modifier = Modifier
                            .size(64.dp)
                            .background(PrimaryIndigo, shape = AppShapes.pill)
                    ) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(AppSpacing.xl))
                    IconButton(onClick = { viewModel.seekRelative(10000L) }) {
                        Icon(
                            Icons.Default.Forward10,
                            contentDescription = "Forward 10s",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Bottom Timeline & Scrubbing (Section 127)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(formatDuration(state.positionMs), style = AppTypography.Caption, color = Color.White)
                        Text(formatDuration(state.durationMs), style = AppTypography.Caption, color = Color.White.copy(alpha = 0.7f))
                    }
                    Slider(
                        value = if (state.durationMs > 0) state.positionMs.toFloat() / state.durationMs.toFloat() else 0f,
                        onValueChange = { fraction ->
                            viewModel.seekTo((fraction * state.durationMs).toLong())
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = PrimaryIndigo,
                            activeTrackColor = PrimaryIndigo,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Secondary Action Controls
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { showSpeedSheet = true }) {
                                Text("${state.speed}x", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { showAudioSheet = true }) {
                                Icon(Icons.Default.Audiotrack, contentDescription = "Audio Tracks", tint = Color.White)
                            }
                            IconButton(onClick = { showSubtitleSheet = true }) {
                                Icon(Icons.Default.Subtitles, contentDescription = "Subtitles", tint = Color.White)
                            }
                        }

                        IconButton(onClick = { showNextEpisodeDialog = true }) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next Episode", tint = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Audio Tracks Bottom Sheet (Section 75)
    if (showAudioSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAudioSheet = false },
            containerColor = DarkSurface
        ) {
            Column(modifier = Modifier.padding(AppSpacing.md)) {
                Text("Audio Tracks", style = AppTypography.SectionTitle, color = TextPrimary)
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                if (state.audioTracks.isEmpty()) {
                    Text("Standard Default Audio Stream", style = AppTypography.Body, color = TextSecondary)
                } else {
                    state.audioTracks.forEach { track ->
                        val isSelected = track.id == state.selectedAudioTrackId || track.isSelected
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) PrimaryIndigo.copy(alpha = 0.2f) else DarkBackground
                            ),
                            shape = AppShapes.small,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    viewModel.setAudioTrack(track.id)
                                    showAudioSheet = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(AppSpacing.md),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = track.label,
                                    style = AppTypography.Body,
                                    color = TextPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryIndigo)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Subtitle Customization Sheet (Section 76, 77)
    if (showSubtitleSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSubtitleSheet = false },
            containerColor = DarkSurface
        ) {
            Column(modifier = Modifier.padding(AppSpacing.md)) {
                Text("Subtitles", style = AppTypography.SectionTitle, color = TextPrimary)
                Spacer(modifier = Modifier.height(AppSpacing.sm))

                // Off option
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (state.selectedSubtitleTrackId == null) PrimaryIndigo.copy(alpha = 0.2f) else DarkBackground
                    ),
                    shape = AppShapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            viewModel.setSubtitleTrack(null)
                            showSubtitleSheet = false
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppSpacing.md),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Off", style = AppTypography.Body, color = TextPrimary)
                        if (state.selectedSubtitleTrackId == null) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryIndigo)
                        }
                    }
                }

                state.subtitleTracks.forEach { sub ->
                    val isSelected = sub.id == state.selectedSubtitleTrackId
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) PrimaryIndigo.copy(alpha = 0.2f) else DarkBackground
                        ),
                        shape = AppShapes.small,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                viewModel.setSubtitleTrack(sub.id)
                                showSubtitleSheet = false
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AppSpacing.md),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(sub.label, style = AppTypography.Body, color = TextPrimary)
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryIndigo)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppSpacing.md))
                Text("Subtitle Style Presets", style = AppTypography.Subtitle, color = TextPrimary)
                Spacer(modifier = Modifier.height(AppSpacing.xs))
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                    listOf("Default" to 18, "Classic" to 16, "Large" to 24, "Minimal" to 14).forEach { (presetName, size) ->
                        OutlinedButton(
                            onClick = { viewModel.setSubtitleFontSize(size) },
                            shape = AppShapes.small,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(presetName, style = AppTypography.Caption)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppSpacing.md))
                Text("Sync Delay: ${state.preferences.subtitleStyle.delayMs}ms", style = AppTypography.Caption, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                    Button(
                        onClick = { viewModel.setSubtitleDelay(state.preferences.subtitleStyle.delayMs - 250) },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkBackground),
                        shape = AppShapes.small
                    ) {
                        Text("-250ms", color = TextPrimary)
                    }
                    Button(
                        onClick = { viewModel.setSubtitleDelay(0L) },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkBackground),
                        shape = AppShapes.small
                    ) {
                        Text("Reset", color = TextPrimary)
                    }
                    Button(
                        onClick = { viewModel.setSubtitleDelay(state.preferences.subtitleStyle.delayMs + 250) },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkBackground),
                        shape = AppShapes.small
                    ) {
                        Text("+250ms", color = TextPrimary)
                    }
                }
            }
        }
    }

    // Playback Speed Selector (Section 78)
    if (showSpeedSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSpeedSheet = false },
            containerColor = DarkSurface
        ) {
            Column(modifier = Modifier.padding(AppSpacing.md)) {
                Text("Playback Speed", style = AppTypography.SectionTitle, color = TextPrimary)
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (state.speed == speed) PrimaryIndigo.copy(alpha = 0.2f) else DarkBackground
                        ),
                        shape = AppShapes.small,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                viewModel.setSpeed(speed)
                                showSpeedSheet = false
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AppSpacing.md),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${speed}x", style = AppTypography.Body, color = TextPrimary)
                            if (state.speed == speed) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryIndigo)
                            }
                        }
                    }
                }
            }
        }
    }

    // Playback Queue Sheet (Section 80)
    if (showQueueSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQueueSheet = false },
            containerColor = DarkSurface
        ) {
            Column(modifier = Modifier.padding(AppSpacing.md)) {
                Text("Playback Queue", style = AppTypography.SectionTitle, color = TextPrimary)
                Spacer(modifier = Modifier.height(AppSpacing.sm))

                // Now Playing
                Text("Now Playing", style = AppTypography.Overline, color = PrimaryIndigo)
                Text(
                    text = state.currentItem?.let { "${it.animeTitle} - Ep ${it.episodeNumber.toInt()}" } ?: "Nothing playing",
                    style = AppTypography.Subtitle.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(AppSpacing.md))
                Text("Up Next", style = AppTypography.Overline, color = TextMuted)

                if (state.queue.upcomingItems.isEmpty()) {
                    Text("Queue is empty", style = AppTypography.BodySmall, color = TextMuted)
                } else {
                    LazyColumn(modifier = Modifier.height(200.dp)) {
                        itemsIndexed(state.queue.upcomingItems) { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = AppSpacing.xs),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${item.animeTitle} Ep ${item.episodeNumber.toInt()}",
                                        style = AppTypography.BodySmall,
                                        color = TextPrimary
                                    )
                                    item.episodeTitle?.let {
                                        Text(it, style = AppTypography.Caption, color = TextMuted)
                                    }
                                }
                                IconButton(onClick = { viewModel.removeFromQueue(index) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = TextMuted)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Player More Menu (Section 79)
    if (showMoreMenuSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMoreMenuSheet = false },
            containerColor = DarkSurface
        ) {
            Column(modifier = Modifier.padding(AppSpacing.md)) {
                Text("Player Settings", style = AppTypography.SectionTitle, color = TextPrimary)
                Spacer(modifier = Modifier.height(AppSpacing.sm))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showMoreMenuSheet = false }
                        .padding(vertical = AppSpacing.sm)
                ) {
                    Icon(Icons.Default.PictureInPicture, contentDescription = null, tint = PrimaryIndigo)
                    Spacer(modifier = Modifier.width(AppSpacing.md))
                    Text("Picture-in-Picture", style = AppTypography.Body, color = TextPrimary)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showMoreMenuSheet = false }
                        .padding(vertical = AppSpacing.sm)
                ) {
                    Icon(Icons.Default.AspectRatio, contentDescription = null, tint = PrimaryIndigo)
                    Spacer(modifier = Modifier.width(AppSpacing.md))
                    Text("Fit / Stretch Aspect Ratio", style = AppTypography.Body, color = TextPrimary)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showMoreMenuSheet = false
                            showAudioSheet = true
                        }
                        .padding(vertical = AppSpacing.sm)
                ) {
                    Icon(Icons.Default.Audiotrack, contentDescription = null, tint = PrimaryIndigo)
                    Spacer(modifier = Modifier.width(AppSpacing.md))
                    Text("Audio Configuration", style = AppTypography.Body, color = TextPrimary)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showMoreMenuSheet = false
                            showSubtitleSheet = true
                        }
                        .padding(vertical = AppSpacing.sm)
                ) {
                    Icon(Icons.Default.Subtitles, contentDescription = null, tint = PrimaryIndigo)
                    Spacer(modifier = Modifier.width(AppSpacing.md))
                    Text("Subtitle Formatting & Delay", style = AppTypography.Body, color = TextPrimary)
                }
            }
        }
    }

    // Next Episode Evaluation Dialog (Section 72, 80)
    if (showNextEpisodeDialog) {
        val nextEpNum = (state.currentItem?.episodeNumber ?: 0.0) + 1.0
        AlertDialog(
            onDismissRequest = { showNextEpisodeDialog = false },
            title = { Text("Next Episode", style = AppTypography.Title, color = TextPrimary) },
            text = {
                Text(
                    text = "Episode ${nextEpNum.toInt()} is the next episode in sequence. Would you like to play or find downloads for it?",
                    style = AppTypography.Body,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showNextEpisodeDialog = false
                        val played = viewModel.playNextEpisode()
                        if (!played) {
                            onDownloadNextEpisode(nextEpNum)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    shape = AppShapes.small
                ) {
                    Text("Play Next")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showNextEpisodeDialog = false
                        state.currentItem?.let {
                            onSearchReleases("${it.animeTitle} ${nextEpNum.toInt()}")
                        }
                    },
                    shape = AppShapes.small
                ) {
                    Text("Find Releases", color = PrimaryIndigo)
                }
            },
            containerColor = DarkSurface
        )
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
