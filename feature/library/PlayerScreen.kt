package com.aniflow.feature.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.aniflow.domain.player.model.AudioTrackInfo
import com.aniflow.domain.player.model.PlaybackItem
import com.aniflow.domain.player.model.SubtitleTrackInfo
import com.aniflow.domain.valueobject.AudioChannels
import com.aniflow.domain.valueobject.LanguageCode
import kotlinx.coroutines.delay

/**
 * PlayerScreen (Sections 13, 14, 15, 16, 17, 18, 20).
 * Apple/Linear-grade media playback interface with clean typography,
 * responsive gesture controls, subtitle delay/styling, audio switcher, and auto-next episode prompt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    mediaId: LibraryMediaId,
    onBack: () -> Unit = {},
    onDownloadNextEpisode: (Double) -> Unit = {},
    onSearchReleases: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Simulated PlaybackItem for demonstration
    val sampleItem = remember(mediaId) {
        PlaybackItem(
            mediaId = mediaId,
            animeId = com.aniflow.domain.identity.AnimeId("one_piece"),
            animeTitle = "One Piece",
            seasonNumber = 1,
            episodeNumber = 1090.0,
            episodeTitle = "The Hero's Return! Garp's Fist Strikes!",
            mediaFilePath = "/storage/emulated/0/AniFlow/One Piece/One.Piece.E1090.mkv",
            durationMs = 24 * 60 * 1000L,
            initialPositionMs = 8 * 60 * 1000L,
            audioTracks = listOf(
                AudioTrackInfo("a1", "Japanese (Default)", LanguageCode.JAPANESE, AudioChannels.Stereo, isSelected = true),
                AudioTrackInfo("a2", "English Dub", LanguageCode.ENGLISH, AudioChannels.Stereo, isSelected = false)
            ),
            subtitleTracks = listOf(
                SubtitleTrackInfo("s1", "English (Full)", LanguageCode.ENGLISH, isSelected = true),
                SubtitleTrackInfo("s2", "العربية (Arabic)", LanguageCode.ARABIC, isSelected = false)
            )
        )
    }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(sampleItem.initialPositionMs) }
    val durationMs = sampleItem.durationMs
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var controlsVisible by remember { mutableStateOf(true) }

    // Sheets & Dialogs
    var showAudioSheet by remember { mutableStateOf(false) }
    var showSubtitleSheet by remember { mutableStateOf(false) }
    var showNextEpisodeDialog by remember { mutableStateOf(false) }
    var subtitleDelayMs by remember { mutableLongStateOf(0L) }
    var subtitleFontSize by remember { mutableFloatStateOf(18f) }

    // Auto-hide controls after 4 seconds
    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying) {
            delay(4000)
            controlsVisible = false
        }
    }

    // Progress tick simulation
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(1000)
            if (currentPositionMs < durationMs) {
                currentPositionMs += (1000 * playbackSpeed).toLong()
            } else {
                isPlaying = false
                showNextEpisodeDialog = true // Trigger Next Episode evaluation (Section 18)
            }
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
        // Video Viewport Area (Dark surface representation)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = "${sampleItem.animeTitle} — Episode ${sampleItem.episodeNumber.toInt()}",
                style = AppTypography.headline.copy(fontSize = 20.sp),
                color = Color.White.copy(alpha = 0.4f)
            )
        }

        // Subtitle Overlay (Section 15)
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (controlsVisible) 96.dp else 40.dp)
        ) {
            Text(
                text = "We're setting sail for the next adventure!",
                style = AppTypography.subheadline.copy(
                    fontSize = subtitleFontSize.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), shape = AppShapes.badge)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        // Player Controls HUD
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
                        Text(sampleItem.animeTitle, style = AppTypography.subheadline, fontWeight = FontWeight.Bold, color = Color.White)
                        sampleItem.episodeTitle?.let {
                            Text(it, style = AppTypography.caption, color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                    Row {
                        IconButton(onClick = { showAudioSheet = true }) {
                            Icon(Icons.Default.Audiotrack, contentDescription = "Audio Track", tint = Color.White)
                        }
                        IconButton(onClick = { showSubtitleSheet = true }) {
                            Icon(Icons.Default.Subtitles, contentDescription = "Subtitles", tint = Color.White)
                        }
                    }
                }

                // Center Action Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = { currentPositionMs = (currentPositionMs - 10000).coerceAtLeast(0) }) {
                        Icon(Icons.Default.Replay10, contentDescription = "Rewind 10s", tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                    Spacer(modifier = Modifier.width(AppSpacing.lg))
                    IconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier
                            .size(64.dp)
                            .background(PrimaryIndigo, shape = AppShapes.pill)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(AppSpacing.lg))
                    IconButton(onClick = { currentPositionMs = (currentPositionMs + 10000).coerceAtMost(durationMs) }) {
                        Icon(Icons.Default.Forward10, contentDescription = "Forward 10s", tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                }

                // Bottom Timeline & Scrubbing Controls
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(formatDuration(currentPositionMs), style = AppTypography.caption, color = Color.White)
                        Text(formatDuration(durationMs), style = AppTypography.caption, color = Color.White.copy(alpha = 0.7f))
                    }
                    Slider(
                        value = if (durationMs > 0) currentPositionMs.toFloat() / durationMs.toFloat() else 0f,
                        onValueChange = { fraction ->
                            currentPositionMs = (fraction * durationMs).toLong()
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = PrimaryIndigo,
                            activeTrackColor = PrimaryIndigo,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Secondary Bottom Controls
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = {
                                playbackSpeed = when (playbackSpeed) {
                                    1.0f -> 1.25f
                                    1.25f -> 1.5f
                                    1.5f -> 2.0f
                                    2.0f -> 0.75f
                                    else -> 1.0f
                                }
                            }) {
                                Text("${playbackSpeed}x", color = Color.White, fontWeight = FontWeight.Bold)
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

    // Audio Track Modal Sheet (Section 16)
    if (showAudioSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAudioSheet = false },
            containerColor = DarkSurface
        ) {
            Column(modifier = Modifier.padding(AppSpacing.md)) {
                Text("Audio Tracks", style = AppTypography.headline, color = TextPrimary)
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                sampleItem.audioTracks.forEach { track ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (track.isSelected) PrimaryIndigo.copy(alpha = 0.2f) else DarkBackground),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { showAudioSheet = false }
                    ) {
                        Row(modifier = Modifier.padding(AppSpacing.md)) {
                            Text(track.label, style = AppTypography.body, color = TextPrimary, fontWeight = if (track.isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
    }

    // Subtitle Customization Modal Sheet (Section 15)
    if (showSubtitleSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSubtitleSheet = false },
            containerColor = DarkSurface
        ) {
            Column(modifier = Modifier.padding(AppSpacing.md)) {
                Text("Subtitle Options", style = AppTypography.headline, color = TextPrimary)
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                sampleItem.subtitleTracks.forEach { sub ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (sub.isSelected) PrimaryIndigo.copy(alpha = 0.2f) else DarkBackground),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { showSubtitleSheet = false }
                    ) {
                        Row(modifier = Modifier.padding(AppSpacing.md)) {
                            Text(sub.label, style = AppTypography.body, color = TextPrimary, fontWeight = if (sub.isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppSpacing.md))
                Text("Font Size: ${subtitleFontSize.toInt()}sp", style = AppTypography.caption, color = TextSecondary)
                Slider(
                    value = subtitleFontSize,
                    onValueChange = { subtitleFontSize = it },
                    valueRange = 12f..32f,
                    colors = SliderDefaults.colors(thumbColor = PrimaryIndigo, activeTrackColor = PrimaryIndigo)
                )

                Text("Subtitle Delay: ${subtitleDelayMs}ms", style = AppTypography.caption, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                    Button(onClick = { subtitleDelayMs -= 250 }, colors = ButtonDefaults.buttonColors(containerColor = DarkBackground)) {
                        Text("-250ms", color = TextPrimary)
                    }
                    Button(onClick = { subtitleDelayMs = 0 }, colors = ButtonDefaults.buttonColors(containerColor = DarkBackground)) {
                        Text("Reset", color = TextPrimary)
                    }
                    Button(onClick = { subtitleDelayMs += 250 }, colors = ButtonDefaults.buttonColors(containerColor = DarkBackground)) {
                        Text("+250ms", color = TextPrimary)
                    }
                }
            }
        }
    }

    // Auto Next Episode Prompt (Section 18)
    if (showNextEpisodeDialog) {
        AlertDialog(
            onDismissRequest = { showNextEpisodeDialog = false },
            title = { Text("Next Episode", style = AppTypography.headline, color = TextPrimary) },
            text = {
                Text(
                    "Episode ${sampleItem.episodeNumber.toInt() + 1} is not yet downloaded in your local library. How would you like to proceed?",
                    style = AppTypography.body,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showNextEpisodeDialog = false
                        onDownloadNextEpisode(sampleItem.episodeNumber + 1)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) {
                    Text("Auto-Download", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showNextEpisodeDialog = false
                        onSearchReleases("${sampleItem.animeTitle} ${sampleItem.episodeNumber.toInt() + 1}")
                    }
                ) {
                    Text("Search Releases", color = PrimaryIndigo)
                }
            },
            containerColor = DarkSurface
        )
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
