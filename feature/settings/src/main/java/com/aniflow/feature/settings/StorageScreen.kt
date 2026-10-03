package com.aniflow.feature.settings

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
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
import com.aniflow.core.ui.components.AniAppBar
import com.aniflow.core.ui.components.AniEmptyState
import com.aniflow.core.ui.model.StorageUiModel

/**
 * StorageScreen (Sections 87, 88).
 * Manages storage volumes (Internal, SD Card, USB), live health bars, permissions, and directory migration.
 * Zero hardcoded production data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageScreen(
    storages: List<StorageUiModel> = emptyList(),
    onBack: () -> Unit = {},
    onAddLocationClick: () -> Unit = {},
    onRescanClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            AniAppBar(
                title = "Storage Management",
                subtitle = "Storage roots, usage breakdown, and volume health",
                onBack = onBack,
                actions = {
                    IconButton(onClick = onAddLocationClick) {
                        Icon(Icons.Default.Add, contentDescription = "Add Storage Location", tint = PrimaryIndigo)
                    }
                }
            )
        },
        containerColor = DarkBackground,
        modifier = modifier
    ) { paddingValues ->
        if (storages.isEmpty()) {
            AniEmptyState(
                title = "No storage locations configured",
                description = "Configure an internal directory or external storage location to save downloads and index media.",
                icon = Icons.Default.SdCard,
                actionLabel = "Add Storage Location",
                onAction = onAddLocationClick,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = AppSpacing.md,
                    end = AppSpacing.md,
                    bottom = AppSpacing.xxxl,
                    top = AppSpacing.sm
                ),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
            items(storages, key = { it.locationId }) { storage ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = AppShapes.medium,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(AppSpacing.md)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (storage.typeName == "SD Card") Icons.Default.SdCard else Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = PrimaryIndigo,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(AppSpacing.sm))
                                Column {
                                    Text(storage.displayName, style = AppTypography.title, color = TextPrimary)
                                    Text("${storage.typeName} • ${storage.statusText}", style = AppTypography.caption, color = storage.statusColor)
                                }
                            }

                            IconButton(onClick = { onRescanClick(storage.locationId) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Refresh, contentDescription = "Rescan", tint = TextSecondary)
                            }
                        }

                        Spacer(modifier = Modifier.height(AppSpacing.md))

                        // Storage Usage Bar
                        val usedFraction = (100 - storage.freePercentage).toFloat() / 100f
                        LinearProgressIndicator(
                            progress = { usedFraction },
                            color = PrimaryIndigo,
                            trackColor = DarkCardBorder,
                            strokeCap = StrokeCap.Round,
                            modifier = Modifier.fillMaxWidth().height(8.dp)
                        )

                        Spacer(modifier = Modifier.height(AppSpacing.xs))

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${storage.freeBytesFormatted} free of ${storage.totalBytesFormatted}", style = AppTypography.caption, color = TextSecondary)
                            Text("${storage.freePercentage}% free", style = AppTypography.numericSize, color = TextMuted)
                        }
                    }
                }
            }
        }
    }
}
}

