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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aniflow.core.ui.components.AniAppBar
import com.aniflow.core.ui.components.AniPrimaryButton
import com.aniflow.core.ui.components.AniSecondaryButton
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
import com.aniflow.domain.storage.model.StorageRole

data class StorageSetupLocationItem(
    val id: String,
    val path: String,
    val totalSpaceFormatted: String,
    val freeSpaceFormatted: String,
    val freeFraction: Float,
    val selectedRole: StorageRole = StorageRole.Both
)

/**
 * StorageSetupScreen (Section 16).
 * First-run / storage configuration screen to choose library and download locations,
 * select SAF directories, and designate storage roles (Library, Downloads, Both).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageSetupScreen(
    onContinue: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    onPickDirectory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var locations by remember {
        mutableStateOf(
            listOf(
                StorageSetupLocationItem(
                    id = "internal-storage",
                    path = "/storage/emulated/0/AniFlow",
                    totalSpaceFormatted = "128 GB",
                    freeSpaceFormatted = "42.5 GB",
                    freeFraction = 0.33f,
                    selectedRole = StorageRole.Both
                )
            )
        )
    }

    Scaffold(
        topBar = {
            AniAppBar(
                title = "Choose Library Location",
                subtitle = "Storage configuration",
                onBack = onBack
            )
        },
        containerColor = DarkBackground,
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(AppSpacing.md),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = AppShapes.medium,
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(AppSpacing.md),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = PrimaryIndigo,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(AppSpacing.sm))
                            Column {
                                Text(
                                    text = "Storage Roots & Roles",
                                    style = AppTypography.Subtitle.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Designate locations for storing your anime collection and active downloads. You can use separate paths for Library and Downloads or keep both together.",
                                    style = AppTypography.BodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Configured Storage Locations",
                        style = AppTypography.SectionTitle,
                        color = TextPrimary
                    )
                }

                items(locations, key = { it.id }) { loc ->
                    LocationCard(
                        location = loc,
                        onRoleSelected = { newRole ->
                            locations = locations.map {
                                if (it.id == loc.id) it.copy(selectedRole = newRole) else it
                            }
                        }
                    )
                }

                item {
                    OutlinedButton(
                        onClick = onPickDirectory,
                        shape = AppShapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = PrimaryIndigo,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(AppSpacing.xs))
                        Text(
                            text = "Add Custom Location (SAF Picker)",
                            style = AppTypography.Subtitle,
                            color = PrimaryIndigo
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            AniPrimaryButton(
                text = "Confirm & Continue",
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun LocationCard(
    location: StorageSetupLocationItem,
    onRoleSelected: (StorageRole) -> Unit,
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = PrimaryIndigo,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.xs))
                    Text(
                        text = location.path,
                        style = AppTypography.Subtitle.copy(fontWeight = FontWeight.SemiBold),
                        color = TextPrimary
                    )
                }
                AniStatusBadge(
                    label = "${location.freeSpaceFormatted} free",
                    color = AppSemanticColors.Success
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            LinearProgressIndicator(
                progress = { 1f - location.freeFraction },
                color = PrimaryIndigo,
                trackColor = DarkBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
            )

            Spacer(modifier = Modifier.height(AppSpacing.md))

            Text(
                text = "Assigned Role",
                style = AppTypography.Overline,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(AppSpacing.xs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RoleChip(
                    role = StorageRole.Library,
                    label = "Library",
                    isSelected = location.selectedRole == StorageRole.Library,
                    onClick = { onRoleSelected(StorageRole.Library) }
                )
                RoleChip(
                    role = StorageRole.Downloads,
                    label = "Downloads",
                    isSelected = location.selectedRole == StorageRole.Downloads,
                    onClick = { onRoleSelected(StorageRole.Downloads) }
                )
                RoleChip(
                    role = StorageRole.Both,
                    label = "Both",
                    isSelected = location.selectedRole == StorageRole.Both,
                    onClick = { onRoleSelected(StorageRole.Both) }
                )
            }
        }
    }
}

@Composable
private fun RoleChip(
    role: StorageRole,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) PrimaryIndigo.copy(alpha = 0.15f) else DarkBackground
    val border = if (isSelected) PrimaryIndigo else DarkCardBorder
    val textCol = if (isSelected) PrimaryIndigo else TextSecondary

    Box(
        modifier = modifier
            .border(1.dp, border, AppShapes.small)
            .background(bg, AppShapes.small)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = AppTypography.Caption.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
            color = textCol
        )
    }
}
