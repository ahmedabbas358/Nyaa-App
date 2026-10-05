package com.aniflow.core.ui.components

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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aniflow.core.ui.theme.AccentCyan
import com.aniflow.core.ui.theme.DarkBackground
import com.aniflow.core.ui.theme.DarkCardBorder
import com.aniflow.core.ui.theme.DarkSurface
import com.aniflow.core.ui.theme.DarkSurfaceVariant
import com.aniflow.core.ui.theme.PrimaryIndigo
import com.aniflow.core.ui.theme.SuccessGreen
import com.aniflow.core.ui.theme.TextMuted
import com.aniflow.core.ui.theme.TextPrimary
import com.aniflow.core.ui.theme.TextSecondary
import com.aniflow.core.ui.util.TorrentAppDescriptor
import com.aniflow.core.ui.util.TorrentClientBridge

/**
 * Universal External Torrent App Dispatcher Modal.
 *
 * Automatically detects installed torrent apps (1DM, Flud, LibreTorrent, FDM, μTorrent)
 * and allows users to dispatch single releases or batches seamlessly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TorrentClientDispatchModal(
    batchTitle: String,
    items: List<Pair<String, String>>, // title to (magnet or torrentUrl)
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val installedApps = remember { TorrentClientBridge.getInstalledTorrentClients(context) }

    var selectedAppPackage by remember {
        mutableStateOf(installedApps.firstOrNull()?.packageName)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkBackground,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(DarkCardBorder)
            )
        },
        modifier = modifier.fillMaxHeight(0.85f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "External Download Manager",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "$batchTitle (${items.size} item${if (items.size > 1) "s" else ""})",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Detected Apps Section
            Text(
                text = if (installedApps.isNotEmpty()) "Detected Download Apps on Device:" else "No dedicated torrent client detected:",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )

            Spacer(Modifier.height(8.dp))

            if (installedApps.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(installedApps) { app ->
                        val isSelected = selectedAppPackage == app.packageName
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) PrimaryIndigo.copy(alpha = 0.2f) else DarkSurfaceVariant)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) PrimaryIndigo else DarkCardBorder,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedAppPackage = app.packageName }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Download,
                                    contentDescription = null,
                                    tint = if (isSelected) PrimaryIndigo else TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = app.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) PrimaryIndigo else TextPrimary
                                )
                            }
                        }
                    }
                }
            } else {
                Surface(
                    color = DarkSurfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Install 1DM, LibreTorrent, or Flud from Google Play for the best torrent experience.",
                        fontSize = 11.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Primary Launch CTA Button
            val selectedApp = installedApps.find { it.packageName == selectedAppPackage }
            Button(
                onClick = {
                    if (items.size == 1) {
                        val (title, uri) = items.first()
                        TorrentClientBridge.openInExternalTorrentClient(
                            context = context,
                            magnetUri = uri,
                            title = title,
                            targetPackage = selectedAppPackage
                        )
                    } else {
                        TorrentClientBridge.dispatchBatchToClient(
                            context = context,
                            items = items,
                            targetPackage = selectedAppPackage,
                            batchTitle = batchTitle
                        )
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = when {
                        selectedApp != null && items.size > 1 -> "Send ${items.size} Episodes to ${selectedApp.displayName}"
                        selectedApp != null -> "Open in ${selectedApp.displayName}"
                        items.size > 1 -> "Send Batch to Downloader (${items.size} items)"
                        else -> "Open in Torrent App"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(10.dp))

            // Secondary Quick Actions (Copy Magnets + Export .txt + Share Sheet)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val magnets = items.map { it.second }.filter { it.isNotBlank() }
                        TorrentClientBridge.copyBatchMagnets(context, magnets)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Copy Links", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = {
                        TorrentClientBridge.exportBatchMagnetsToTextFile(
                            context = context,
                            batchTitle = batchTitle,
                            items = items
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Save .txt", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = {
                        val magnets = items.joinToString("\n") { it.second }
                        TorrentClientBridge.shareText(context, batchTitle, magnets)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Share", fontSize = 11.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            // If multiple items, show episode quick launch list
            if (items.size > 1) {
                Text(
                    text = "Quick Send Individual Episodes:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted
                )
                Spacer(Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(items) { (epTitle, epUri) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(DarkSurface)
                                .border(1.dp, DarkCardBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = epTitle,
                                fontSize = 11.sp,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(PrimaryIndigo.copy(alpha = 0.15f))
                                    .clickable {
                                        TorrentClientBridge.openMagnetInApp(
                                            context = context,
                                            magnetUri = epUri,
                                            targetPackage = selectedAppPackage,
                                            title = epTitle
                                        )
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Send",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PrimaryIndigo
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
