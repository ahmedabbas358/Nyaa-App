package com.aniflow.feature.settings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aniflow.core.ui.components.AniConfirmationDialog
import com.aniflow.core.ui.components.AniSectionHeader
import com.aniflow.core.ui.components.AniSettingRow
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
import com.aniflow.domain.enums.NetworkType
import com.aniflow.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val maxConcurrentDownloads: Int = 3,
    val maxSegmentsPerDownload: Int = 4,
    val networkRule: NetworkType = NetworkType.AnyNetwork,
    val downloadDirectory: String = "Downloads/AniFlow",
    val libraryDirectory: String = "Movies/Anime",
    val themeMode: String = "System",
    val wifiOnly: Boolean = false,
    val autoPlayNext: Boolean = true,
    val defaultAudioLanguage: String = "Japanese",
    val defaultSubtitleLanguage: String = "English",
    val lowStorageThresholdGb: Int = 5,
    val searchHistoryRetentionDays: Int = 30
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observeSettings()
    }

    private fun observeSettings() {
        if (settingsRepository == null) return
        viewModelScope.launch {
            settingsRepository.observeMaxConcurrentDownloads().collect {
                _uiState.value = _uiState.value.copy(maxConcurrentDownloads = it)
            }
        }
        viewModelScope.launch {
            settingsRepository.observeDownloadDirectory().collect {
                _uiState.value = _uiState.value.copy(downloadDirectory = it)
            }
        }
    }

    fun setMaxConcurrentDownloads(count: Int) {
        viewModelScope.launch {
            settingsRepository?.setMaxConcurrentDownloads(count)
        }
    }

    fun setWifiOnly(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(wifiOnly = enabled)
    }

    fun setAutoPlayNext(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoPlayNext = enabled)
    }

    fun setThemeMode(mode: String) {
        _uiState.value = _uiState.value.copy(themeMode = mode)
    }
}

/**
 * Hierarchical SettingsScreen (Sections 84 to 96).
 * Follows Stripe/Notion style of structured, readable settings with clear grouping.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: (() -> Unit)? = null,
    onNavigateToProfiles: () -> Unit = {},
    onNavigateToAutomation: () -> Unit = {},
    onNavigateToStorage: () -> Unit = {},
    onNavigateToDiagnostics: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = AppTypography.Title, color = TextPrimary) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Navigate back", tint = TextPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground,
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = AppSpacing.md,
                end = AppSpacing.md,
                top = AppSpacing.sm,
                bottom = AppSpacing.xxxl
            ),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.lg),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 1. Appearance Settings (Section 85)
            item {
                Column {
                    AniSectionHeader(title = "Appearance", subtitle = "Theme, accents, and visual display")
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = AppShapes.medium,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            AniSettingRow(
                                title = "Theme Mode",
                                description = "Select Light, Dark, or System surface theme",
                                currentValue = state.themeMode,
                                icon = Icons.Default.Palette,
                                onClick = {
                                    val nextMode = when (state.themeMode) {
                                        "System" -> "Dark"
                                        "Dark" -> "Light"
                                        else -> "System"
                                    }
                                    viewModel.setThemeMode(nextMode)
                                }
                            )
                            AniSettingRow(
                                title = "Accent Color",
                                description = "AniFlow Indigo (Restrained #6366F1)",
                                currentValue = "Indigo",
                                icon = Icons.Default.Palette
                            )
                        }
                    }
                }
            }

            // 2. Playback Settings (Section 86)
            item {
                Column {
                    AniSectionHeader(title = "Playback", subtitle = "Media3 video player preferences")
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = AppShapes.medium,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            AniSettingRow(
                                title = "Auto-play Next Episode",
                                description = "Automatically load next episode upon reaching 90% completion",
                                icon = Icons.Default.PlayCircle,
                                trailingContent = {
                                    Switch(
                                        checked = state.autoPlayNext,
                                        onCheckedChange = { viewModel.setAutoPlayNext(it) },
                                        colors = SwitchDefaults.colors(checkedThumbColor = PrimaryIndigo)
                                    )
                                }
                            )
                            AniSettingRow(
                                title = "Preferred Audio Language",
                                description = "Default audio track prioritization",
                                currentValue = state.defaultAudioLanguage,
                                icon = Icons.Default.PlayCircle
                            )
                            AniSettingRow(
                                title = "Preferred Subtitle Language",
                                description = "Default subtitle track prioritization",
                                currentValue = state.defaultSubtitleLanguage,
                                icon = Icons.Default.PlayCircle
                            )
                        }
                    }
                }
            }

            // 3. Download Engine & Bandwidth (Sections 87, 88)
            item {
                Column {
                    AniSectionHeader(title = "Downloads & Engine", subtitle = "Concurrency limits and bandwidth rules")
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = AppShapes.medium,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(AppSpacing.md)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Max Concurrent Tasks", style = AppTypography.Body, color = TextPrimary)
                                Text("${state.maxConcurrentDownloads}", style = AppTypography.numericSpeed, color = PrimaryIndigo)
                            }
                            Slider(
                                value = state.maxConcurrentDownloads.toFloat(),
                                onValueChange = { viewModel.setMaxConcurrentDownloads(it.toInt()) },
                                valueRange = 1f..6f,
                                steps = 4,
                                colors = SliderDefaults.colors(thumbColor = PrimaryIndigo, activeTrackColor = PrimaryIndigo)
                            )
                            Spacer(modifier = Modifier.height(AppSpacing.xs))
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("HTTP Segments per task", style = AppTypography.BodySmall, color = TextMuted)
                                Text("${state.maxSegmentsPerDownload}", style = AppTypography.Metadata, color = TextPrimary)
                            }
                        }
                    }
                }
            }

            // 4. Network Policy (Section 89)
            item {
                Column {
                    AniSectionHeader(title = "Network Policies", subtitle = "Metered connection and data limits")
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = AppShapes.medium,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            AniSettingRow(
                                title = "Wi-Fi Only",
                                description = "Pause all background downloads when on cellular data",
                                icon = Icons.Default.Wifi,
                                trailingContent = {
                                    Switch(
                                        checked = state.wifiOnly,
                                        onCheckedChange = { viewModel.setWifiOnly(it) },
                                        colors = SwitchDefaults.colors(checkedThumbColor = PrimaryIndigo)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // 5. System Shortcuts
            item {
                Column {
                    AniSectionHeader(title = "Subsystems & Diagnostics", subtitle = "Direct configuration entry points")
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = AppShapes.medium,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            AniSettingRow(
                                title = "Quality Profiles",
                                description = "Configure audio, codec, and resolution preference matrices",
                                icon = Icons.Default.Tune,
                                onClick = onNavigateToProfiles
                            )
                            AniSettingRow(
                                title = "Automation Rules",
                                description = "Conditional triggers and safety threshold controls",
                                icon = Icons.Default.AutoMode,
                                onClick = onNavigateToAutomation
                            )
                            AniSettingRow(
                                title = "Storage Disks",
                                description = "Manage root paths and low space preservation thresholds",
                                icon = Icons.Default.Storage,
                                onClick = onNavigateToStorage
                            )
                            AniSettingRow(
                                title = "System Diagnostics",
                                description = "Real-time health audits and engine telemetry export",
                                icon = Icons.Default.Healing,
                                onClick = onNavigateToDiagnostics
                            )
                            AniSettingRow(
                                title = "About AniFlow",
                                description = "Version details, open source notices, and architecture",
                                icon = Icons.Default.Info,
                                onClick = onNavigateToAbout
                            )
                        }
                    }
                }
            }
        }
    }

    if (showResetDialog) {
        AniConfirmationDialog(
            title = "Reset All Settings?",
            message = "This will restore default concurrency, network policies, and playback preferences.",
            impactWarning = "Active downloads and saved profiles will not be deleted.",
            confirmText = "Reset Settings",
            isDestructive = true,
            onConfirm = { showResetDialog = false },
            onDismiss = { showResetDialog = false }
        )
    }
}
