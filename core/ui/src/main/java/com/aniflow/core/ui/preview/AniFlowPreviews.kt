package com.aniflow.core.ui.preview

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.aniflow.core.ui.components.AniConfirmationDialog
import com.aniflow.core.ui.components.AniDownloadCard
import com.aniflow.core.ui.components.AniEmptyState
import com.aniflow.core.ui.components.AniEpisodeRow
import com.aniflow.core.ui.components.AniErrorState
import com.aniflow.core.ui.components.AniFilterChip
import com.aniflow.core.ui.components.AniLandscapeMediaCard
import com.aniflow.core.ui.components.AniPoster
import com.aniflow.core.ui.components.AniPrimaryButton
import com.aniflow.core.ui.components.AniProgressBar
import com.aniflow.core.ui.components.AniReleaseRow
import com.aniflow.core.ui.components.AniSecondaryButton
import com.aniflow.core.ui.components.AniSectionHeader
import com.aniflow.core.ui.components.AniSettingRow
import com.aniflow.core.ui.components.AniStatusBadge
import com.aniflow.core.ui.theme.AniFlowTheme
import com.aniflow.core.ui.theme.AppSemanticColors
import com.aniflow.core.ui.theme.AppSpacing
import com.aniflow.core.ui.theme.DarkBackground
import com.aniflow.core.ui.theme.PrimaryIndigo

/**
 * Preview harness validating design system components under all required configurations:
 * 1. Dark Mode (Default)
 * 2. Light Mode
 * 3. Arabic / RTL Mode
 * 4. Tablet Form Factor (1024dp x 768dp)
 * 5. Stress Test: Long Titles & Massive Metadata
 */
@Composable
fun PreviewComponentSuite(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.lg)
    ) {
        AniSectionHeader(
            title = "Release Rows & Cards",
            subtitle = "Technical hierarchy with multi-line title protection",
            actionText = "See All",
            onActionClick = {}
        )

        AniReleaseRow(
            title = "[SubsPlease] One Piece - 1050 (1080p) [16A463B8] Multiple Subtitles Softsubs Batch",
            resolution = "1080p",
            codec = "HEVC",
            source = "WEB-DL",
            sizeFormatted = "1.37 GB",
            seeds = 142,
            peers = 8,
            uploader = "SubsPlease",
            isPreferred = true,
            onClick = {},
            onDownloadClick = {}
        )

        AniEpisodeRow(
            episodeNumber = 12,
            title = "The Dawn of Adventure in the Grand Line - Facing the Warlords of the Sea",
            durationFormatted = "24m",
            statusBadge = "Downloaded",
            statusColor = AppSemanticColors.Success,
            progressFraction = 0.85f,
            onClick = {},
            onActionClick = {}
        )

        AniDownloadCard(
            title = "Bleach: Thousand-Year Blood War - S02E14 [1080p HEVC Dual-Audio]",
            statusText = "Downloading (4 segments)",
            statusColor = PrimaryIndigo,
            progressFraction = 0.62f,
            speedFormatted = "8.4 MB/s",
            etaFormatted = "2m 14s",
            onPauseResume = {},
            onCancel = {},
            onClick = {}
        )

        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            AniPoster(
                title = "Frieren: Beyond Journey's End",
                badgeText = "NEW EP",
                progressFraction = 0.45f,
                modifier = Modifier.width(110.dp)
            )
            AniLandscapeMediaCard(
                title = "Episode 28 - Journey to Ende",
                subtitle = "Season 1 Finale",
                durationText = "24:18",
                progressFraction = 0.90f,
                onClick = {},
                modifier = Modifier.weight(1f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            AniFilterChip(label = "1080p", selected = true, onClick = {}, onRemove = {}, count = 48)
            AniFilterChip(label = "HEVC", selected = true, onClick = {}, onRemove = {}, count = 32)
            AniFilterChip(label = "English Subs", selected = false, onClick = {})
        }

        AniEmptyState(
            title = "No downloads active",
            message = "Search for an anime release to queue downloads with smart quality profiles.",
            actionText = "Open Universal Search",
            onActionClick = {}
        )

        AniErrorState(
            title = "Download Paused Automatically",
            reason = "Free storage on /storage/emulated/0 (3.2 GB) is below configured reserve safety threshold (5.0 GB).",
            onRetry = {},
            retryText = "Resume Anyway",
            secondaryActionText = "Manage Storage",
            onSecondaryAction = {}
        )
    }
}

// 1. Dark Theme Preview
@Preview(name = "Dark Theme", showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
fun PreviewDarkTheme() {
    AniFlowTheme(darkTheme = true) {
        Surface(color = DarkBackground) {
            PreviewComponentSuite()
        }
    }
}

// 2. Light Theme Preview
@Preview(name = "Light Theme", showBackground = true, backgroundColor = 0xFFF8FAFC)
@Composable
fun PreviewLightTheme() {
    AniFlowTheme(darkTheme = false) {
        Surface(color = androidx.compose.ui.graphics.Color(0xFFF8FAFC)) {
            PreviewComponentSuite()
        }
    }
}

// 3. Arabic / RTL Preview (Section 110)
@Preview(name = "Arabic RTL", locale = "ar", showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
fun PreviewArabicRtl() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AniFlowTheme(darkTheme = true) {
            Surface(color = DarkBackground) {
                PreviewComponentSuite()
            }
        }
    }
}

// 4. Tablet / Large Screen Preview (Section 106, 107)
@Preview(
    name = "Tablet Expanded (1024x768)",
    device = Devices.TABLET,
    showBackground = true,
    backgroundColor = 0xFF0F172A
)
@Composable
fun PreviewTabletLayout() {
    AniFlowTheme(darkTheme = true) {
        Surface(color = DarkBackground) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Simulated Tablet Nav Rail
                Column(
                    modifier = Modifier
                        .width(72.dp)
                        .background(com.aniflow.core.ui.theme.DarkSurface)
                        .padding(AppSpacing.sm)
                ) {
                    AniStatusBadge(label = "AF", color = PrimaryIndigo)
                }
                // Content Pane
                PreviewComponentSuite(modifier = Modifier.weight(1f))
            }
        }
    }
}
