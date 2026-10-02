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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.mutableStateListOf
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

enum class PreferenceAttitude {
    Preferred,
    Neutral,
    Avoid,
    Blocked
}

data class UploaderUiItem(
    val name: String,
    val releaseCount: Int,
    val averageSizeMb: Int,
    val knownCodec: String,
    var attitude: PreferenceAttitude
)

data class ReleaseGroupUiItem(
    val name: String,
    val releasesCount: Int,
    val scope: String, // Global, Anime X, etc.
    var attitude: PreferenceAttitude
)

/**
 * UploaderGroupPreferencesScreen (Sections 10, 11).
 * Allows users to set granular preferences (Preferred, Neutral, Avoid, Blocked)
 * for uploaders and release groups with scoped overrides and rich metadata.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploaderGroupPreferencesScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Uploaders", "Release Groups")

    val uploaders = remember {
        mutableStateListOf(
            UploaderUiItem("Erai-raws", 420, 1400, "1080p HEVC", PreferenceAttitude.Preferred),
            UploaderUiItem("SubsPlease", 380, 1350, "1080p AVC", PreferenceAttitude.Preferred),
            UploaderUiItem("Judas", 150, 450, "720p HEVC", PreferenceAttitude.Neutral),
            UploaderUiItem("SpamUploader", 12, 8500, "Fake 4K", PreferenceAttitude.Blocked)
        )
    }

    val groups = remember {
        mutableStateListOf(
            ReleaseGroupUiItem("Erai-raws", 420, "Global", PreferenceAttitude.Preferred),
            ReleaseGroupUiItem("SubsPlease", 380, "Global", PreferenceAttitude.Preferred),
            ReleaseGroupUiItem("Commie", 85, "Anime: Monogatari", PreferenceAttitude.Preferred),
            ReleaseGroupUiItem("HorribleSubs (Archive)", 900, "Global", PreferenceAttitude.Neutral)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Uploaders & Release Groups", style = AppTypography.headline, color = TextPrimary) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkSurface,
                contentColor = PrimaryIndigo,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = PrimaryIndigo
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(
                    start = AppSpacing.md,
                    end = AppSpacing.md,
                    bottom = AppSpacing.xxxl,
                    top = AppSpacing.md
                ),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                if (selectedTab == 0) {
                    items(uploaders) { uploader ->
                        UploaderPreferenceCard(
                            uploader = uploader,
                            onAttitudeChange = { newAttitude ->
                                val index = uploaders.indexOf(uploader)
                                if (index != -1) {
                                    uploaders[index] = uploader.copy(attitude = newAttitude)
                                }
                            }
                        )
                    }
                } else {
                    items(groups) { group ->
                        ReleaseGroupPreferenceCard(
                            group = group,
                            onAttitudeChange = { newAttitude ->
                                val index = groups.indexOf(group)
                                if (index != -1) {
                                    groups[index] = group.copy(attitude = newAttitude)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UploaderPreferenceCard(
    uploader: UploaderUiItem,
    onAttitudeChange: (PreferenceAttitude) -> Unit
) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(AppSpacing.xs))
                    Text(uploader.name, style = AppTypography.title, color = TextPrimary, fontWeight = FontWeight.Bold)
                }
                AttitudeBadge(uploader.attitude)
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${uploader.releaseCount} releases • Avg ~${uploader.averageSizeMb} MB • Typical: ${uploader.knownCodec}",
                style = AppTypography.caption,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                PreferenceAttitude.values().forEach { att ->
                    FilterChip(
                        selected = uploader.attitude == att,
                        onClick = { onAttitudeChange(att) },
                        label = { Text(att.name, fontSize = 11.sp) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReleaseGroupPreferenceCard(
    group: ReleaseGroupUiItem,
    onAttitudeChange: (PreferenceAttitude) -> Unit
) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Group, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(AppSpacing.xs))
                    Text(group.name, style = AppTypography.title, color = TextPrimary, fontWeight = FontWeight.Bold)
                }
                AttitudeBadge(group.attitude)
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Scope: ${group.scope} • ${group.releasesCount} known releases",
                style = AppTypography.caption,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                PreferenceAttitude.values().forEach { att ->
                    FilterChip(
                        selected = group.attitude == att,
                        onClick = { onAttitudeChange(att) },
                        label = { Text(att.name, fontSize = 11.sp) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AttitudeBadge(attitude: PreferenceAttitude) {
    val (color, label) = when (attitude) {
        PreferenceAttitude.Preferred -> AppSemanticColors.Success to "Preferred"
        PreferenceAttitude.Neutral -> TextSecondary to "Neutral"
        PreferenceAttitude.Avoid -> AppSemanticColors.Warning to "Avoid"
        PreferenceAttitude.Blocked -> AppSemanticColors.Error to "Blocked"
    }

    Text(
        text = label,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
    )
}
