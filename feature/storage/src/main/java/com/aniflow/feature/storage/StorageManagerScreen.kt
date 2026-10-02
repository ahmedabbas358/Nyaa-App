package com.aniflow.feature.storage

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import com.aniflow.domain.identity.StorageId
import com.aniflow.domain.storage.model.StorageAvailability
import com.aniflow.domain.storage.model.StorageLocationRoot
import com.aniflow.domain.storage.model.StorageRoot
import com.aniflow.domain.storage.model.StorageType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class StorageManagerUiState(
    val roots: List<StorageRoot> = listOf(
        StorageRoot(
            id = StorageId("internal_anime"),
            name = "Internal Anime Library",
            type = StorageType.AppPrivate,
            state = StorageAvailability.Available,
            location = StorageLocationRoot("/storage/emulated/0/AniFlow/Anime"),
            totalSpaceBytes = 128L * 1024 * 1024 * 1024,
            freeSpaceBytes = 42L * 1024 * 1024 * 1024,
            isDefault = true
        ),
        StorageRoot(
            id = StorageId("sdcard_archive"),
            name = "MicroSD Card Archive",
            type = StorageType.ExternalVolume,
            state = StorageAvailability.Available,
            location = StorageLocationRoot("/storage/0000-0000/AnimeArchive"),
            totalSpaceBytes = 256L * 1024 * 1024 * 1024,
            freeSpaceBytes = 118L * 1024 * 1024 * 1024,
            isDefault = false
        ),
        StorageRoot(
            id = StorageId("usb_offline"),
            name = "External USB Drive",
            type = StorageType.ExternalVolume,
            state = StorageAvailability.Offline,
            location = StorageLocationRoot("/storage/USB_DRIVE/Anime"),
            totalSpaceBytes = 1000L * 1024 * 1024 * 1024,
            freeSpaceBytes = 450L * 1024 * 1024 * 1024,
            isDefault = false
        )
    ),
    val isScanning: Boolean = false,
    val selectedRootId: StorageId? = null
)

class StorageManagerViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(StorageManagerUiState())
    val uiState: StateFlow<StorageManagerUiState> = _uiState.asStateFlow()

    fun onScanRoot(id: StorageId) {}
    fun onRepairAccess(id: StorageId) {}
    fun onSetDefault(id: StorageId) {}
    fun onAddRoot() {}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageManagerScreen(
    viewModel: StorageManagerViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onRootClick: (StorageId) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = PrimaryIndigo,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(AppSpacing.sm))
                        Text(
                            text = "Storage & Locations",
                            style = AppTypography.headlineMedium,
                            color = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onAddRoot() },
                containerColor = PrimaryIndigo,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Location")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            // Header Analytics overview banner
            item {
                StorageGlobalOverviewBanner(roots = state.roots)
            }

            item {
                Text(
                    text = "Registered Library Roots (${state.roots.size})",
                    style = AppTypography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = AppSpacing.xs)
                )
            }

            items(state.roots, key = { it.id.value }) { root ->
                StorageRootCard(
                    root = root,
                    onRootClick = { onRootClick(root.id) },
                    onScan = { viewModel.onScanRoot(root.id) },
                    onRepair = { viewModel.onRepairAccess(root.id) },
                    onSetDefault = { viewModel.onSetDefault(root.id) }
                )
            }
        }
    }
}

@Composable
fun StorageGlobalOverviewBanner(roots: List<StorageRoot>) {
    val totalBytes = roots.sumOf { it.totalSpaceBytes }
    val freeBytes = roots.sumOf { it.freeSpaceBytes }
    val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)
    val usedPercent = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()) * 100f else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkCardBorder, AppShapes.large),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Combined Storage",
                    style = AppTypography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${formatBytes(usedBytes)} / ${formatBytes(totalBytes)}",
                    style = AppTypography.bodySmall,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            LinearProgressIndicator(
                progress = { usedPercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = if (usedPercent > 90f) AppSemanticColors.Error else PrimaryIndigo,
                trackColor = DarkBackground,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Free: ${formatBytes(freeBytes)}",
                    style = AppTypography.labelMedium,
                    color = AppSemanticColors.Success
                )
                Text(
                    text = "${usedPercent.toInt()}% Used",
                    style = AppTypography.labelMedium,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
fun StorageRootCard(
    root: StorageRoot,
    onRootClick: () -> Unit,
    onScan: () -> Unit,
    onRepair: () -> Unit,
    onSetDefault: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onRootClick() }
            .border(1.dp, if (root.isDefault) PrimaryIndigo.copy(alpha = 0.5f) else DarkCardBorder, AppShapes.medium),
        shape = AppShapes.medium,
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = when (root.type) {
                            StorageType.ExternalVolume -> Icons.Default.Storage
                            StorageType.UserSelectedFolder -> Icons.Default.Folder
                            else -> Icons.Default.Folder
                        },
                        contentDescription = null,
                        tint = PrimaryIndigo,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.sm))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = root.name,
                                style = AppTypography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (root.isDefault) {
                                Spacer(modifier = Modifier.width(AppSpacing.xs))
                                Box(
                                    modifier = Modifier
                                        .background(PrimaryIndigo.copy(alpha = 0.2f), AppShapes.small)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "DEFAULT",
                                        style = AppTypography.labelSmall.copy(fontSize = 9.sp),
                                        color = PrimaryIndigo,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Text(
                            text = root.location.rawUriOrPath,
                            style = AppTypography.bodySmall,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Availability badge (Section 11, 85, 137)
                StorageAvailabilityBadge(availability = root.state)
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // Capacity indicator
            LinearProgressIndicator(
                progress = { root.usedPercentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = when {
                    root.state == StorageAvailability.Offline -> TextMuted
                    root.usedPercentage > 90f -> AppSemanticColors.Error
                    else -> PrimaryIndigo
                },
                trackColor = DarkBackground,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(AppSpacing.xs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${formatBytes(root.usedSpaceBytes)} / ${formatBytes(root.totalSpaceBytes)}",
                    style = AppTypography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = "${formatBytes(root.freeSpaceBytes)} free",
                    style = AppTypography.bodySmall,
                    color = if (root.freeSpaceBytes < 2L * 1024 * 1024 * 1024) AppSemanticColors.Error else TextMuted
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            // Actions row (Section 12, 13)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (root.state == StorageAvailability.PermissionRequired) {
                    Button(
                        onClick = onRepair,
                        colors = ButtonDefaults.buttonColors(containerColor = AppSemanticColors.Warning),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Grant Permission", style = AppTypography.labelMedium)
                    }
                    Spacer(modifier = Modifier.width(AppSpacing.sm))
                }

                OutlinedButton(
                    onClick = onScan,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Scan", style = AppTypography.labelMedium)
                }

                if (!root.isDefault) {
                    Spacer(modifier = Modifier.width(AppSpacing.xs))
                    IconButton(onClick = onSetDefault, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Star, contentDescription = "Set Default", tint = TextMuted)
                    }
                }
            }
        }
    }
}

@Composable
fun StorageAvailabilityBadge(availability: StorageAvailability) {
    val (color, icon, label) = when (availability) {
        StorageAvailability.Available -> Triple(AppSemanticColors.Success, Icons.Default.CheckCircle, "Available")
        StorageAvailability.Offline -> Triple(AppSemanticColors.Warning, Icons.Default.CloudOff, "Offline")
        StorageAvailability.PermissionRequired -> Triple(AppSemanticColors.Error, Icons.Default.Security, "Access Required")
        StorageAvailability.ReadOnly -> Triple(TextMuted, Icons.Default.Build, "Read-Only")
        StorageAvailability.Full -> Triple(AppSemanticColors.Error, Icons.Default.Error, "Storage Full")
        else -> Triple(TextMuted, Icons.Default.Folder, "Unknown")
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), AppShapes.small)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = AppTypography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
    val pre = "KMGTPE"[exp - 1]
    return String.format("%.1f %sB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
}
