package com.aniflow.feature.settings

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInNew
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aniflow.core.ui.theme.AppSemanticColors
import com.aniflow.core.ui.theme.AppShapes
import com.aniflow.core.ui.theme.AppSpacing
import com.aniflow.core.ui.theme.AppTypography
import com.aniflow.core.ui.theme.DarkBackground
import com.aniflow.core.ui.theme.DarkCardBorder
import com.aniflow.core.ui.theme.DarkSurface
import com.aniflow.core.ui.theme.PrimaryIndigo
import com.aniflow.core.ui.theme.SuccessGreen
import com.aniflow.core.ui.theme.TextMuted
import com.aniflow.core.ui.theme.TextPrimary
import com.aniflow.core.ui.theme.TextSecondary
import com.aniflow.core.ui.util.TorrentClientBridge

/**
 * ImportLinkScreen (Sections 127, 128, 163, 164).
 * Handles external share entries and manual paste for Magnet URIs, Torrent files, and Nyaa URLs.
 * Supports direct download queueing, external client handoff, and .torrent file extraction.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportLinkScreen(
    initialLink: String = "",
    onBack: () -> Unit = {},
    onAddToQueue: (link: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var linkInput by remember { mutableStateOf(initialLink) }

    val cleanLink = linkInput.trim()
    val isMagnet = cleanLink.startsWith("magnet:?", ignoreCase = true)
    val isTorrentUrl = cleanLink.endsWith(".torrent", ignoreCase = true) || cleanLink.contains("nyaa.si/download/", ignoreCase = true)
    val isNyaaPage = cleanLink.contains("nyaa.si/view/", ignoreCase = true)

    val linkType = when {
        isMagnet -> "Magnet URI"
        isTorrentUrl -> "Torrent File URL"
        isNyaaPage -> "Nyaa.si Release Page"
        cleanLink.isBlank() -> null
        else -> "Direct Media URL"
    }

    val nyaaId = if (isNyaaPage) {
        cleanLink.substringAfter("nyaa.si/view/").substringBefore("?").substringBefore("/").takeIf { it.isNotBlank() }
    } else null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import Media Link", style = AppTypography.Title, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(AppSpacing.md)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            // Text input with paste & clear buttons
            OutlinedTextField(
                value = linkInput,
                onValueChange = { linkInput = it },
                label = { Text("Paste Magnet URI, Torrent, or Nyaa link") },
                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = PrimaryIndigo) },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (linkInput.isNotBlank()) {
                            IconButton(onClick = { linkInput = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
                            }
                        }
                        IconButton(onClick = {
                            val clipText = clipboardManager.getText()?.text
                            if (!clipText.isNullOrBlank()) {
                                linkInput = clipText.trim()
                                Toast.makeText(context, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = PrimaryIndigo)
                        }
                    }
                },
                placeholder = { Text("magnet:?xt=urn:btih:... or https://nyaa.si/...", color = TextMuted) },
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryIndigo,
                    unfocusedBorderColor = DarkCardBorder,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = AppShapes.medium,
                modifier = Modifier.fillMaxWidth()
            )

            // Detected Format Card
            if (linkType != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = AppShapes.medium,
                    border = BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(AppSpacing.md)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Detected Format", style = AppTypography.Caption, color = SuccessGreen)
                        }
                        Text(linkType, style = AppTypography.Title, color = TextPrimary)
                        Spacer(modifier = Modifier.height(AppSpacing.xs))
                        Text("Destination: Internal / Anime / Downloads", style = AppTypography.BodySmall)

                        if (nyaaId != null) {
                            Spacer(modifier = Modifier.height(AppSpacing.xs))
                            Text("Nyaa Item ID: #$nyaaId", style = AppTypography.Caption, color = PrimaryIndigo)
                        }
                    }
                }
            }

            // Quick Actions section
            if (cleanLink.isNotBlank()) {
                Text("Quick Actions", style = AppTypography.Title, color = TextPrimary)

                // External BitTorrent Client Button
                if (isMagnet || isTorrentUrl) {
                    OutlinedButton(
                        onClick = {
                            TorrentClientBridge.openInExternalTorrentClient(
                                context = context,
                                magnetUri = cleanLink,
                                title = "Imported Media"
                            )
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryIndigo),
                        border = BorderStroke(1.dp, PrimaryIndigo),
                        shape = AppShapes.pill,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open in External Client (1DM / LibreTorrent / Flud)")
                    }
                }

                // Download .torrent file
                if (isTorrentUrl || isNyaaPage) {
                    val torrentUrl = if (isTorrentUrl) cleanLink else "https://nyaa.si/download/$nyaaId.torrent"
                    OutlinedButton(
                        onClick = {
                            TorrentClientBridge.downloadTorrentFileDirectly(
                                context = context,
                                torrentUrl = torrentUrl,
                                title = if (nyaaId != null) "nyaa_$nyaaId" else "download"
                            )
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = BorderStroke(1.dp, DarkCardBorder),
                        shape = AppShapes.pill,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save .torrent File to Device")
                    }
                }

                // Open on Nyaa.si Web
                if (isNyaaPage || nyaaId != null) {
                    OutlinedButton(
                        onClick = {
                            TorrentClientBridge.openWebPage(context, cleanLink)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        border = BorderStroke(1.dp, DarkCardBorder),
                        shape = AppShapes.pill,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Nyaa Page in Browser")
                    }
                }

                // Copy to Clipboard
                OutlinedButton(
                    onClick = {
                        TorrentClientBridge.copyToClipboard(context, cleanLink, "Link", "Link copied to clipboard")
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    border = BorderStroke(1.dp, DarkCardBorder),
                    shape = AppShapes.pill,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copy Link to Clipboard")
                }
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(AppSpacing.md))

            // Primary Queue Download Button
            Button(
                onClick = {
                    val finalLink = if (isNyaaPage && nyaaId != null) {
                        // Use direct torrent download endpoint for Nyaa page
                        "https://nyaa.si/download/$nyaaId.torrent"
                    } else {
                        cleanLink
                    }
                    onAddToQueue(finalLink)
                },
                enabled = cleanLink.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryIndigo,
                    disabledContainerColor = DarkCardBorder
                ),
                shape = AppShapes.pill,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Download in AniFlow", style = AppTypography.Subtitle)
            }
        }
    }
}
