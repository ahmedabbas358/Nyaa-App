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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.aniflow.domain.controlplane.models.BandwidthScheduleWindow
import com.aniflow.domain.controlplane.models.NetworkPolicyType
import java.time.DayOfWeek

/**
 * BandwidthSchedulerScreen (Sections 27, 28).
 * Manages Network Policy constraints (WiFiOnly, MeteredAllowed, etc.) and time-windowed
 * download speed schedules (e.g. Unlimited off-peak, capped daytime).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BandwidthSchedulerScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedNetworkPolicy by remember { mutableStateOf(NetworkPolicyType.WiFiOnly) }
    var pauseOnMetered by remember { mutableStateOf(true) }
    var pauseOnLowBattery by remember { mutableStateOf(true) }
    var globalSpeedLimitMb by remember { mutableFloatStateOf(0f) } // 0 = unlimited

    val scheduleWindows = remember {
        mutableStateListOf(
            BandwidthScheduleWindow(
                id = "win-1",
                name = "Night Owl (Off-peak)",
                startHour = 0,
                endHour = 7,
                maxDownloadSpeedBytesPerSec = null, // Unlimited
                isEnabled = true
            ),
            BandwidthScheduleWindow(
                id = "win-2",
                name = "Working Hours Capped",
                startHour = 7,
                endHour = 18,
                maxDownloadSpeedBytesPerSec = 10L * 1024 * 1024, // 10 MB/s
                isEnabled = true
            ),
            BandwidthScheduleWindow(
                id = "win-3",
                name = "Evening Priority",
                startHour = 18,
                endHour = 24,
                maxDownloadSpeedBytesPerSec = 25L * 1024 * 1024, // 25 MB/s
                isEnabled = true
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Network & Bandwidth Policies", style = AppTypography.headline, color = TextPrimary) },
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
            // Section 1: Network Policy Selector
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = AppShapes.medium,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(AppSpacing.md)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(AppSpacing.xs))
                            Text("Network Policy", style = AppTypography.title, color = TextPrimary)
                        }
                        Spacer(modifier = Modifier.height(AppSpacing.sm))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                            items(NetworkPolicyType.values()) { policy ->
                                FilterChip(
                                    selected = selectedNetworkPolicy == policy,
                                    onClick = { selectedNetworkPolicy = policy },
                                    label = { Text(policy.name) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(AppSpacing.md))

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text("Pause on Metered Connections", style = AppTypography.body, color = TextPrimary)
                                Text("Prevents cellular data overages", style = AppTypography.caption, color = TextMuted)
                            }
                            Switch(
                                checked = pauseOnMetered,
                                onCheckedChange = { pauseOnMetered = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = PrimaryIndigo)
                            )
                        }

                        Spacer(modifier = Modifier.height(AppSpacing.sm))

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text("Pause on Low Battery (<15%)", style = AppTypography.body, color = TextPrimary)
                                Text("Conserves battery when unplugged", style = AppTypography.caption, color = TextMuted)
                            }
                            Switch(
                                checked = pauseOnLowBattery,
                                onCheckedChange = { pauseOnLowBattery = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = PrimaryIndigo)
                            )
                        }
                    }
                }
            }

            // Section 2: Global Bandwidth Limit
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = AppShapes.medium,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(AppSpacing.md)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(AppSpacing.xs))
                            Text("Global Download Speed Limit", style = AppTypography.title, color = TextPrimary)
                        }
                        Spacer(modifier = Modifier.height(AppSpacing.xs))
                        Text(
                            text = if (globalSpeedLimitMb == 0f) "Unlimited Speed" else "${globalSpeedLimitMb.toInt()} MB/s",
                            style = AppTypography.bodySecondary,
                            color = if (globalSpeedLimitMb == 0f) AppSemanticColors.Success else PrimaryIndigo,
                            fontWeight = FontWeight.SemiBold
                        )
                        Slider(
                            value = globalSpeedLimitMb,
                            onValueChange = { globalSpeedLimitMb = it },
                            valueRange = 0f..50f,
                            steps = 9,
                            colors = SliderDefaults.colors(thumbColor = PrimaryIndigo, activeTrackColor = PrimaryIndigo)
                        )
                    }
                }
            }

            // Section 3: Time-Window Bandwidth Scheduler
            item {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Scheduled Bandwidth Windows", style = AppTypography.title, color = TextPrimary)
                    Button(
                        onClick = {
                            scheduleWindows.add(
                                BandwidthScheduleWindow(
                                    id = "win-${scheduleWindows.size + 1}",
                                    name = "Custom Window",
                                    startHour = 20,
                                    endHour = 23,
                                    maxDownloadSpeedBytesPerSec = 5L * 1024 * 1024,
                                    isEnabled = true
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkCardBorder),
                        shape = AppShapes.small
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Window", fontSize = 11.sp)
                    }
                }
            }

            items(scheduleWindows) { window ->
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
                            Column {
                                Text(window.name, style = AppTypography.title, color = TextPrimary)
                                Text(
                                    text = String.format("%02d:00 → %02d:00", window.startHour, window.endHour),
                                    style = AppTypography.caption,
                                    color = TextSecondary
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = window.maxDownloadSpeedBytesPerSec?.let { "${it / (1024 * 1024)} MB/s" } ?: "Unlimited",
                                    style = AppTypography.caption,
                                    color = if (window.maxDownloadSpeedBytesPerSec == null) AppSemanticColors.Success else PrimaryIndigo,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(AppSpacing.sm))
                                IconButton(
                                    onClick = { scheduleWindows.remove(window) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = AppSemanticColors.Error, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
