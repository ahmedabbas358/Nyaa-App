package com.aniflow.feature.automation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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

/**
 * AutomationSettingsScreen (Section 111, 118, 119, 120, 121, 170, 171, 174).
 * Controls daily rate limits, storage quotas, auto-upgrade permission, and safe defaults.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationSettingsScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var allowAutoDownload by remember { mutableStateOf(false) }
    var allowAutoUpgrade by remember { mutableStateOf(false) }
    var autoDeleteOldVersion by remember { mutableStateOf(false) }
    var wiFiOnly by remember { mutableStateOf(true) }
    var maxDownloadsPerDay by remember { mutableFloatStateOf(20f) }
    var maxStoragePerDayGb by remember { mutableFloatStateOf(50f) }
    var askConfirmationSizeGb by remember { mutableFloatStateOf(10f) }
    var syncIntervalSeconds by remember { mutableFloatStateOf(60f) }
    var autoOrganizeFolders by remember { mutableStateOf(true) }
    var selectedUploaderPreference by remember { mutableStateOf("Judas (1080p HEVC)") }
    var notifyOnEpisodeDrop by remember { mutableStateOf(true) }

    val syncIntervalLabel = when (syncIntervalSeconds.toInt()) {
        30 -> "30 Seconds (Ultra-Fast Realtime)"
        60 -> "60 Seconds (1 Minute)"
        120 -> "2 Minutes"
        300 -> "5 Minutes"
        else -> "${syncIntervalSeconds.toInt()}s"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Automation Limits & Safety", style = AppTypography.headline, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            item {
                SectionHeader("Permissions & Consent (Safe by Default)")
            }

            item {
                ToggleSettingCard(
                    title = "Automatic Downloads",
                    subtitle = "Allow matching releases to queue downloads without confirmation.",
                    checked = allowAutoDownload,
                    onCheckedChange = { allowAutoDownload = it }
                )
            }

            item {
                ToggleSettingCard(
                    title = "Automatic Quality Upgrades",
                    subtitle = "Upgrade existing library episodes when a strictly superior release appears (e.g. 1080p BDRip over 720p WEB).",
                    checked = allowAutoUpgrade,
                    onCheckedChange = { allowAutoUpgrade = it }
                )
            }

            if (allowAutoUpgrade) {
                item {
                    ToggleSettingCard(
                        title = "Auto-Delete Replaced File",
                        subtitle = "Delete old version after new release downloads and passes checksum verification. (Default: OFF, keep original).",
                        checked = autoDeleteOldVersion,
                        onCheckedChange = { autoDeleteOldVersion = it }
                    )
                }
            }

            item {
                ToggleSettingCard(
                    title = "Wi-Fi Only Execution",
                    subtitle = "Never run background searches or downloads over mobile data.",
                    checked = wiFiOnly,
                    onCheckedChange = { wiFiOnly = it }
                )
            }

            item {
                SectionHeader("Rate Limits & Daily Budgets")
            }

            item {
                SliderSettingCard(
                    title = "Max Automatic Downloads / Day",
                    subtitle = "Hard ceiling on files queued automatically per 24 hours.",
                    value = maxDownloadsPerDay,
                    valueRange = 5f..100f,
                    steps = 18,
                    valueDisplay = "${maxDownloadsPerDay.toInt()} files/day",
                    onValueChange = { maxDownloadsPerDay = it }
                )
            }

            item {
                SliderSettingCard(
                    title = "Daily Automatic Storage Budget",
                    subtitle = "Total download size permitted per 24 hours.",
                    value = maxStoragePerDayGb,
                    valueRange = 10f..200f,
                    steps = 18,
                    valueDisplay = "${maxStoragePerDayGb.toInt()} GB/day",
                    onValueChange = { maxStoragePerDayGb = it }
                )
            }

            item {
                SliderSettingCard(
                    title = "Confirmation Required Above",
                    subtitle = "Any single release or batch larger than this size will be sent to the Review Queue.",
                    value = askConfirmationSizeGb,
                    valueRange = 2f..30f,
                    steps = 13,
                    valueDisplay = "${askConfirmationSizeGb.toInt()} GB",
                    onValueChange = { askConfirmationSizeGb = it }
                )
            }

            item {
                SectionHeader("Real-Time Periodic Sync (Nyaa.si Live Tracker)")
            }

            item {
                SliderSettingCard(
                    title = "Periodic Sync Frequency",
                    subtitle = "How often AniFlow polls Nyaa.si feeds for new releases (sub-minute to minutes).",
                    value = syncIntervalSeconds,
                    valueRange = 30f..300f,
                    steps = 4,
                    valueDisplay = syncIntervalLabel,
                    onValueChange = { syncIntervalSeconds = it }
                )
            }

            item {
                ToggleSettingCard(
                    title = "New Episode Notification",
                    subtitle = "Send high-priority notifications with instant 'Download' action when a tracked anime drops.",
                    checked = notifyOnEpisodeDrop,
                    onCheckedChange = { notifyOnEpisodeDrop = it }
                )
            }

            item {
                SectionHeader("Uploader Intelligence & Preference")
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(AppShapes.sm),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Column(modifier = Modifier.padding(AppSpacing.md)) {
                        Text(
                            text = "Preferred Release Group / Uploader",
                            style = AppTypography.headline.copy(fontSize = 15.sp),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "When multiple uploaders release the same episode, AniFlow prioritizes your preference based on file size and video encoding.",
                            style = AppTypography.caption,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(AppSpacing.sm))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("Judas (1080p HEVC)", "SubsPlease", "Erai-raws", "EMBER").forEach { uploader ->
                                val isSelected = selectedUploaderPreference == uploader
                                androidx.compose.material3.FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedUploaderPreference = uploader },
                                    label = { Text(uploader, style = AppTypography.caption) },
                                    colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryIndigo,
                                        selectedLabelColor = Color.White,
                                        containerColor = DarkBackground,
                                        labelColor = TextSecondary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader("Automated Directory & Storage Organization")
            }

            item {
                ToggleSettingCard(
                    title = "Auto-Organize Anime Folders",
                    subtitle = "Automatically route downloads to Anime/<Title>/Season <N>/ with clean standardized file names.",
                    checked = autoOrganizeFolders,
                    onCheckedChange = { autoOrganizeFolders = it }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = AppTypography.caption.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
        color = PrimaryIndigo,
        modifier = Modifier.padding(top = AppSpacing.sm, bottom = 4.dp)
    )
}

@Composable
private fun ToggleSettingCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(AppShapes.sm),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = AppTypography.body.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = AppTypography.caption, color = TextMuted)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedThumbColor = PrimaryIndigo, checkedTrackColor = PrimaryIndigo.copy(alpha = 0.5f))
            )
        }
    }
}

@Composable
private fun SliderSettingCard(
    title: String,
    subtitle: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueDisplay: String,
    onValueChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(AppShapes.sm),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = AppTypography.body.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                Text(valueDisplay, style = AppTypography.body.copy(fontWeight = FontWeight.Bold), color = PrimaryIndigo)
            }
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = AppTypography.caption, color = TextMuted)
            Spacer(Modifier.height(AppSpacing.sm))
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                colors = SliderDefaults.colors(
                    thumbColor = PrimaryIndigo,
                    activeTrackColor = PrimaryIndigo,
                    inactiveTrackColor = DarkBackground
                )
            )
        }
    }
}
