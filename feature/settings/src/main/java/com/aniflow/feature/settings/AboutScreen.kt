package com.aniflow.feature.settings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aniflow.core.ui.components.AniDataRow
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

/**
 * AboutScreen (Section 97).
 * Minimal, premium product identity screen detailing:
 * - App Identity & Version
 * - Build & Engine specifications
 * - Open Source & Architecture Notices
 * - Privacy Principles (Local-first, zero telemetry tracking)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About AniFlow", style = AppTypography.Title, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Navigate back", tint = TextPrimary)
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
            // Hero Brand Header
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = AppSpacing.xl)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .background(PrimaryIndigo, shape = CircleShape)
                    ) {
                        Text(
                            text = "AF",
                            style = AppTypography.Display.copy(fontSize = 28.sp),
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(AppSpacing.md))
                    Text(
                        text = "AniFlow",
                        style = AppTypography.LargeTitle,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Release Intelligence & Private Media Platform",
                        style = AppTypography.BodySmall,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.xs))
                    Text(
                        text = "Version 1.0.0 (Build 2026.10)",
                        style = AppTypography.Caption,
                        color = PrimaryIndigo,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Technical Runtime Specifications
            item {
                Column {
                    AniSectionHeader(
                        title = "Runtime Environment",
                        subtitle = "Native subsystem architectures"
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = AppShapes.medium,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(AppSpacing.sm)) {
                            AniDataRow(property = "UI Framework", value = "Jetpack Compose + Material 3")
                            AniDataRow(property = "Torrent Engine", value = "libtorrent4j v2.1.0-native")
                            AniDataRow(property = "HTTP Engine", value = "Multi-segment OkHttp3 Coordinator")
                            AniDataRow(property = "Playback Engine", value = "Media3 ExoPlayer v1.4.1")
                            AniDataRow(property = "Persistence", value = "SQLite FTS4 + Encrypted Metadata")
                            AniDataRow(property = "Target Architecture", value = "arm64-v8a / x86_64")
                        }
                    }
                }
            }

            // Privacy & Open Standards
            item {
                Column {
                    AniSectionHeader(
                        title = "Privacy & Open Standards",
                        subtitle = "Zero telemetry, local-first computing"
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = AppShapes.medium,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            AniSettingRow(
                                title = "Local-First Architecture",
                                description = "All search history, profiles, downloads, and progress stay strictly on-device",
                                icon = Icons.Default.PrivacyTip
                            )
                            AniSettingRow(
                                title = "Open Source Licenses",
                                description = "Apache 2.0, MIT, and BSD notices for incorporated libraries",
                                icon = Icons.Default.Code
                            )
                            AniSettingRow(
                                title = "No Cloud Telemetry",
                                description = "No tracking trackers, advertisements, or external logging",
                                icon = Icons.Default.Security
                            )
                        }
                    }
                }
            }
        }
    }
}
