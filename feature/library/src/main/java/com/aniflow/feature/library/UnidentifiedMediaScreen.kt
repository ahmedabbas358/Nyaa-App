package com.aniflow.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.unit.sp
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

data class UnidentifiedMediaItem(
    val id: String,
    val fileName: String,
    val filePath: String,
    val fileSizeFormatted: String,
    val resolution: String = "Unknown",
    val codec: String = "Unknown",
    val container: String = "MKV",
    val detectedAnimeGuess: String? = null,
    val detectedEpisodeGuess: Double? = null
)

/**
 * UnidentifiedMediaScreen (Section 70).
 * Displays unmapped media files in storage roots.
 * Actions: Identify, Map Anime, Map Season, Map Episode, Ignore, Delete.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnidentifiedMediaScreen(
    onBack: () -> Unit = {},
    onScanRequested: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var unmappedFiles by remember { mutableStateOf<List<UnidentifiedMediaItem>>(emptyList()) }
    var mappingItem by remember { mutableStateOf<UnidentifiedMediaItem?>(null) }
    var fileToDelete by remember { mutableStateOf<UnidentifiedMediaItem?>(null) }

    Scaffold(
        topBar = {
            AniAppBar(
                title = "Unidentified Media",
                subtitle = "Manual media mapping",
                onBack = onBack,
                actions = {
                    IconButton(onClick = onScanRequested) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Rescan Storage",
                            tint = PrimaryIndigo
                        )
                    }
                }
            )
        },
        containerColor = DarkBackground,
        modifier = modifier
    ) { padding ->
        if (unmappedFiles.isEmpty()) {
            AniEmptyState(
                title = "No unidentified media",
                description = "All media files across configured storage locations have been recognized and linked to your anime library.",
                icon = Icons.Default.CheckCircle,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(AppSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(unmappedFiles, key = { it.id }) { item ->
                    UnidentifiedMediaCard(
                        item = item,
                        onMap = { mappingItem = item },
                        onIgnore = {
                            unmappedFiles = unmappedFiles.filter { it.id != item.id }
                        },
                        onDelete = { fileToDelete = item }
                    )
                }
            }
        }
    }

    // Manual Mapping Dialog
    mappingItem?.let { item ->
        ManualMappingDialog(
            item = item,
            onDismiss = { mappingItem = null },
            onConfirm = { animeTitle, season, episode ->
                // Map item via domain coordinator/repository
                unmappedFiles = unmappedFiles.filter { it.id != item.id }
                mappingItem = null
            }
        )
    }

    // Delete Confirmation Dialog
    fileToDelete?.let { item ->
        AniDialog(
            title = "Delete File?",
            message = "Filename: ${item.fileName}\nPath: ${item.filePath}\nSize: ${item.fileSizeFormatted}\n\nThe file will be permanently deleted from physical storage.",
            confirmText = "Delete Permanently",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                unmappedFiles = unmappedFiles.filter { it.id != item.id }
                fileToDelete = null
            },
            onDismiss = { fileToDelete = null }
        )
    }
}

@Composable
private fun UnidentifiedMediaCard(
    item: UnidentifiedMediaItem,
    onMap: () -> Unit,
    onIgnore: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = AppShapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                AniStatusBadge(
                    label = "Unidentified",
                    color = AppSemanticColors.Warning
                )
                Text(
                    text = item.fileSizeFormatted,
                    style = AppTypography.Caption,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.xs))

            Text(
                text = item.fileName,
                style = AppTypography.Subtitle.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Text(
                text = item.filePath,
                style = AppTypography.Caption,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            // Metadata row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
                Text(
                    text = "Resolution: ${item.resolution}",
                    style = AppTypography.Caption,
                    color = TextSecondary
                )
                Text(
                    text = "Codec: ${item.codec}",
                    style = AppTypography.Caption,
                    color = TextSecondary
                )
                Text(
                    text = "Format: ${item.container}",
                    style = AppTypography.Caption,
                    color = TextSecondary
                )
            }

            if (item.detectedAnimeGuess != null) {
                Spacer(modifier = Modifier.height(AppSpacing.xs))
                Text(
                    text = "Suggested match: ${item.detectedAnimeGuess} (Ep ${item.detectedEpisodeGuess ?: 1})",
                    style = AppTypography.Caption,
                    color = PrimaryIndigo
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onMap,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    shape = AppShapes.small,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Identify & Map", style = AppTypography.Caption)
                }

                OutlinedButton(
                    onClick = onIgnore,
                    shape = AppShapes.small
                ) {
                    Text("Ignore", style = AppTypography.Caption, color = TextSecondary)
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete File",
                        tint = AppSemanticColors.Error
                    )
                }
            }
        }
    }
}

@Composable
private fun ManualMappingDialog(
    item: UnidentifiedMediaItem,
    onDismiss: () -> Unit,
    onConfirm: (animeTitle: String, season: Int, episode: Double) -> Unit
) {
    var animeTitle by remember { mutableStateOf(item.detectedAnimeGuess ?: "") }
    var seasonText by remember { mutableStateOf("1") }
    var episodeText by remember { mutableStateOf(item.detectedEpisodeGuess?.toString() ?: "1") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Map File to Anime",
                style = AppTypography.Title,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                Text(
                    text = "File: ${item.fileName}",
                    style = AppTypography.Caption,
                    color = TextMuted
                )

                OutlinedTextField(
                    value = animeTitle,
                    onValueChange = { animeTitle = it },
                    label = { Text("Anime Title") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryIndigo,
                        unfocusedBorderColor = DarkCardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                    OutlinedTextField(
                        value = seasonText,
                        onValueChange = { seasonText = it },
                        label = { Text("Season") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIndigo,
                            unfocusedBorderColor = DarkCardBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = episodeText,
                        onValueChange = { episodeText = it },
                        label = { Text("Episode") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIndigo,
                            unfocusedBorderColor = DarkCardBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val s = seasonText.toIntOrNull() ?: 1
                    val ep = episodeText.toDoubleOrNull() ?: 1.0
                    onConfirm(animeTitle, s, ep)
                },
                enabled = animeTitle.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                shape = AppShapes.small
            ) {
                Text("Save Mapping")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shape = AppShapes.small) {
                Text("Cancel", color = TextMuted)
            }
        },
        containerColor = DarkSurface,
        shape = AppShapes.large
    )
}
