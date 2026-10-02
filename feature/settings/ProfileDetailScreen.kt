package com.aniflow.feature.settings

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.aniflow.domain.profile.model.BatchMode
import com.aniflow.domain.profile.model.DubPreference
import com.aniflow.domain.profile.model.NetworkPolicyMode
import com.aniflow.domain.profile.model.SubtitleRequirement
import com.aniflow.domain.profile.model.UserProfile
import com.aniflow.domain.profile.template.ProfileTemplates
import com.aniflow.domain.profile.validator.ProfileValidator
import com.aniflow.domain.valueobject.LanguageCode
import com.aniflow.domain.valueobject.Resolution
import com.aniflow.domain.valueobject.VideoCodec
import kotlinx.coroutines.launch

/**
 * ProfileDetailScreen (Sections 38, 39, 9, 10, 11, 12, 13, 14, 15, 16, 17).
 * Inspects and edits fine-grained ProfilePreferences, separating Hard Constraints from Soft Preferences.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDetailScreen(
    profileId: ProfileId,
    onBack: () -> Unit = {},
    onSaveProfile: (UserProfile) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // State initialized from existing or template profile
    var profile by remember {
        mutableStateOf(ProfileTemplates.createBalanced(profileId, isDefault = false))
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("General", "Quality & Codec", "Audio & Subs", "Policies")

    val validationResult = remember(profile) {
        ProfileValidator.validate(profile)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(profile.name, style = AppTypography.headline, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (validationResult.isValid) {
                                onSaveProfile(profile)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Profile '${profile.name}' saved successfully")
                                }
                            } else {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Cannot save: ${validationResult.violations.first().message}")
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        shape = AppShapes.pill,
                        modifier = Modifier.padding(end = AppSpacing.sm)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DarkBackground,
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Contradiction warnings if any exist
            if (!validationResult.isValid) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppSemanticColors.error.copy(alpha = 0.15f))
                        .padding(AppSpacing.sm)
                ) {
                    Text(
                        text = "Invalid Configuration: ${validationResult.violations.joinToString { it.message }}",
                        style = AppTypography.caption,
                        color = AppSemanticColors.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkBackground,
                contentColor = PrimaryIndigo,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = PrimaryIndigo
                    )
                }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                style = AppTypography.bodySmall,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) PrimaryIndigo else TextSecondary
                            )
                        }
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(AppSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
                modifier = Modifier.fillMaxSize()
            ) {
                when (selectedTab) {
                    0 -> {
                        // General Info
                        item {
                            OutlinedTextField(
                                value = profile.name,
                                onValueChange = { profile = profile.copy(name = it) },
                                label = { Text("Profile Name") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryIndigo,
                                    unfocusedBorderColor = DarkCardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = profile.description ?: "",
                                onValueChange = { profile = profile.copy(description = it) },
                                label = { Text("Description") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryIndigo,
                                    unfocusedBorderColor = DarkCardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                shape = AppShapes.card,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(AppSpacing.md)
                                ) {
                                    Column {
                                        Text("Enabled", style = AppTypography.subheadline, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text("Active for selection & download engines", style = AppTypography.caption, color = TextMuted)
                                    }
                                    Switch(
                                        checked = profile.enabled,
                                        onCheckedChange = { profile = profile.copy(enabled = it) },
                                        colors = SwitchDefaults.colors(checkedThumbColor = PrimaryIndigo)
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        // Quality & Codec
                        item {
                            Text("Resolution Preferences", style = AppTypography.subheadline, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.height(AppSpacing.xs))
                            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                                listOf(Resolution.R1080p, Resolution.R720p, Resolution.R2160p).forEach { res ->
                                    val isPreferred = profile.preferences.resolution.preferred == res
                                    FilterChip(
                                        selected = isPreferred,
                                        onClick = {
                                            val currentRes = profile.preferences.resolution
                                            profile = profile.copy(
                                                preferences = profile.preferences.copy(
                                                    resolution = currentRes.copy(preferred = if (isPreferred) null else res)
                                                )
                                            )
                                        },
                                        label = { Text(res.displayName) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = PrimaryIndigo,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                        item {
                            Text("Preferred Codec", style = AppTypography.subheadline, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.height(AppSpacing.xs))
                            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                                listOf(VideoCodec.HEVC, VideoCodec.AVC, VideoCodec.AV1).forEach { cdc ->
                                    val isPreferred = profile.preferences.codec.preferred == cdc
                                    FilterChip(
                                        selected = isPreferred,
                                        onClick = {
                                            val currentCodec = profile.preferences.codec
                                            profile = profile.copy(
                                                preferences = profile.preferences.copy(
                                                    codec = currentCodec.copy(preferred = if (isPreferred) null else cdc)
                                                )
                                            )
                                        },
                                        label = { Text(cdc.displayName) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = PrimaryIndigo,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                        item {
                            Text("Batch Preference", style = AppTypography.subheadline, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.height(AppSpacing.xs))
                            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                                BatchMode.values().forEach { mode ->
                                    val isSelected = profile.preferences.batch.mode == mode
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            profile = profile.copy(
                                                preferences = profile.preferences.copy(
                                                    batch = profile.preferences.batch.copy(mode = mode)
                                                )
                                            )
                                        },
                                        label = { Text(mode.name) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = PrimaryIndigo,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                    2 -> {
                        // Audio & Subtitles
                        item {
                            Text("Audio Language", style = AppTypography.subheadline, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.height(AppSpacing.xs))
                            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                                listOf(LanguageCode.JAPANESE, LanguageCode.ENGLISH, LanguageCode.MULTI).forEach { lang ->
                                    val isSelected = profile.preferences.audio.preferredLanguage == lang
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            profile = profile.copy(
                                                preferences = profile.preferences.copy(
                                                    audio = profile.preferences.audio.copy(preferredLanguage = lang)
                                                )
                                            )
                                        },
                                        label = { Text(lang.code.uppercase()) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = PrimaryIndigo,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                        item {
                            Text("Subtitle Requirement", style = AppTypography.subheadline, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.height(AppSpacing.xs))
                            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                                SubtitleRequirement.values().forEach { req ->
                                    val isSelected = profile.preferences.subtitles.requirement == req
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            profile = profile.copy(
                                                preferences = profile.preferences.copy(
                                                    subtitles = profile.preferences.subtitles.copy(requirement = req)
                                                )
                                            )
                                        },
                                        label = { Text(req.name) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = PrimaryIndigo,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                    3 -> {
                        // Network & Storage Policies
                        item {
                            Text("Network Policy", style = AppTypography.subheadline, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.height(AppSpacing.xs))
                            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                                listOf(NetworkPolicyMode.WiFiOnly, NetworkPolicyMode.AnyNetwork).forEach { mode ->
                                    val isSelected = profile.networkPolicy.mode == mode
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            profile = profile.copy(
                                                networkPolicy = profile.networkPolicy.copy(mode = mode)
                                            )
                                        },
                                        label = { Text(mode.name) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = PrimaryIndigo,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                shape = AppShapes.card,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(AppSpacing.md)
                                ) {
                                    Column {
                                        Text("Auto-Download", style = AppTypography.subheadline, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text("Queue approved releases automatically", style = AppTypography.caption, color = TextMuted)
                                    }
                                    Switch(
                                        checked = profile.automationPolicy.autoDownload,
                                        onCheckedChange = {
                                            profile = profile.copy(
                                                automationPolicy = profile.automationPolicy.copy(autoDownload = it)
                                            )
                                        },
                                        colors = SwitchDefaults.colors(checkedThumbColor = PrimaryIndigo)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
