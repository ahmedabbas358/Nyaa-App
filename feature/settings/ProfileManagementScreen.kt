package com.aniflow.feature.settings

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.aniflow.domain.identity.ProfileId
import com.aniflow.domain.profile.model.ProfileTemplateType
import com.aniflow.domain.profile.model.UserProfile
import com.aniflow.domain.profile.template.ProfileTemplates
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ProfileManagementScreen (Sections 2, 3, 4, 36, 37, 54).
 * Dedicated Control Plane screen allowing users to:
 * - View all profiles with active/default state indicators
 * - Switch active default profile with atomic guarantees
 * - Duplicate existing profiles with clean ID allocation
 * - Create new profiles from templates (Balanced, HighQuality, SmallSize, Archive, Mobile)
 * - Inspect or edit fine-grained profile details
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileManagementScreen(
    onBack: () -> Unit = {},
    onSelectProfile: (ProfileId) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val profiles = remember {
        mutableStateListOf(
            ProfileTemplates.createBalanced(ProfileId("prof_balanced"), isDefault = true),
            ProfileTemplates.createHighQuality(ProfileId("prof_hq"), isDefault = false),
            ProfileTemplates.createSmallSize(ProfileId("prof_small"), isDefault = false),
            ProfileTemplates.createArchive(ProfileId("prof_archive"), isDefault = false)
        )
    }

    var showTemplateDialog by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<UserProfile?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Download Profiles", style = AppTypography.headline, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    Button(
                        onClick = { showTemplateDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        shape = AppShapes.pill,
                        modifier = Modifier.padding(end = AppSpacing.sm)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Profile", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DarkBackground,
        modifier = modifier
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                Text(
                    text = "Control Plane Profiles define how AniFlow evaluates releases, resolves qualities, and routes downloads. Exactly one profile is designated as global default.",
                    style = AppTypography.caption,
                    color = TextMuted,
                    modifier = Modifier.padding(bottom = AppSpacing.xs)
                )
            }

            items(profiles, key = { it.id.value }) { profile ->
                ProfileCard(
                    profile = profile,
                    onClick = { onSelectProfile(profile.id) },
                    onSetDefault = {
                        // Atomic switch: exactly one default profile (Section 54)
                        for (i in profiles.indices) {
                            profiles[i] = profiles[i].copy(isDefault = (profiles[i].id == profile.id))
                        }
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("'${profile.name}' is now the default profile")
                        }
                    },
                    onDuplicate = {
                        val copy = profile.copy(
                            id = ProfileId("prof_${UUID.randomUUID().toString().take(8)}"),
                            name = "${profile.name} (Copy)",
                            isDefault = false,
                            version = 1
                        )
                        profiles.add(copy)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Duplicated '${profile.name}'")
                        }
                    },
                    onDelete = {
                        if (profile.isDefault) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Cannot delete default profile! Please set another default first.")
                            }
                        } else {
                            deleteCandidate = profile
                        }
                    }
                )
            }
        }
    }

    // Template selection dialog
    if (showTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showTemplateDialog = false },
            title = { Text("Choose Profile Template", style = AppTypography.headline, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                    Text(
                        "Templates serve as blueprints with pre-configured targets and constraints.",
                        style = AppTypography.bodySmall,
                        color = TextSecondary
                    )
                    ProfileTemplateType.values().forEach { type ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            shape = AppShapes.card,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newProf = ProfileTemplates.createFromTemplate(type, isDefault = false)
                                    profiles.add(newProf)
                                    showTemplateDialog = false
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Created '${newProf.name}' from ${type.name} template")
                                    }
                                }
                        ) {
                            Column(modifier = Modifier.padding(AppSpacing.sm)) {
                                Text(type.name, style = AppTypography.subheadline, color = TextPrimary, fontWeight = FontWeight.Bold)
                                Text(
                                    when (type) {
                                        ProfileTemplateType.Balanced -> "1080p HEVC, 500MB-2.5GB, Japanese Audio + English/Arabic subs"
                                        ProfileTemplateType.HighQuality -> "1080p/4K BluRay, Lossless Audio, Soft Subtitles, No Size Limit"
                                        ProfileTemplateType.SmallSize -> "720p/Mini-1080p, < 700MB, HEVC/AV1, Prefer Smaller"
                                        ProfileTemplateType.Archive -> "Full Season Batches, 1080p BluRay, Consistency First, 10GB Buffer"
                                        ProfileTemplateType.Mobile -> "720p, Wi-Fi Only, Low Battery & Low Storage Protections"
                                    },
                                    style = AppTypography.caption,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTemplateDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }

    // Delete confirmation dialog
    deleteCandidate?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Delete Profile?", style = AppTypography.headline, color = TextPrimary) },
            text = {
                Text(
                    "Are you sure you want to delete '${target.name}'? This action cannot be undone.",
                    style = AppTypography.body,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        profiles.removeAll { it.id == target.id }
                        deleteCandidate = null
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Deleted '${target.name}'")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppSemanticColors.error)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
private fun ProfileCard(
    profile: UserProfile,
    onClick: () -> Unit,
    onSetDefault: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = AppShapes.card,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (profile.isDefault) PrimaryIndigo else DarkCardBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = profile.name,
                        style = AppTypography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (profile.isDefault) {
                        Spacer(modifier = Modifier.width(AppSpacing.sm))
                        Box(
                            modifier = Modifier
                                .background(PrimaryIndigo.copy(alpha = 0.2f), shape = AppShapes.pill)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "DEFAULT",
                                style = AppTypography.caption,
                                color = PrimaryIndigo,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = TextSecondary)
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        if (!profile.isDefault) {
                            DropdownMenuItem(
                                text = { Text("Set as Default") },
                                onClick = {
                                    menuExpanded = false
                                    onSetDefault()
                                },
                                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Duplicate") },
                            onClick = {
                                menuExpanded = false
                                onDuplicate()
                            },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
                        )
                        if (!profile.isDefault) {
                            DropdownMenuItem(
                                text = { Text("Delete", color = AppSemanticColors.error) },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = AppSemanticColors.error) }
                            )
                        }
                    }
                }
            }

            profile.description?.let { desc ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(desc, style = AppTypography.bodySmall, color = TextMuted)
            }

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            // Preference summary chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                modifier = Modifier.fillMaxWidth()
            ) {
                SummaryBadge(profile.preferences.resolution.preferred?.displayName ?: "Any Res")
                SummaryBadge(profile.preferences.codec.preferred?.displayName ?: "Any Codec")
                SummaryBadge(profile.preferences.audio.preferredLanguage.code.uppercase())
                SummaryBadge(profile.networkPolicy.mode.name)
            }
        }
    }
}

@Composable
private fun SummaryBadge(text: String) {
    Box(
        modifier = Modifier
            .background(DarkBackground, shape = AppShapes.pill)
            .border(1.dp, DarkCardBorder, shape = AppShapes.pill)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(text = text, style = AppTypography.caption, color = TextSecondary, fontSize = 11.sp)
    }
}
