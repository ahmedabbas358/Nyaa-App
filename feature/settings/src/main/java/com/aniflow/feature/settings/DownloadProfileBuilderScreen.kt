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
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.aniflow.domain.controlplane.models.FallbackTier
import com.aniflow.domain.controlplane.models.SelectionStrategy

/**
 * DownloadProfileBuilderScreen (Sections 6, 7, 8, 9, 49).
 * Gradual profile editor with Basic, Advanced, and Expert tiers, strategy weights,
 * and multi-level fallback sequences.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadProfileBuilderScreen(
    onBack: () -> Unit = {},
    onSaveProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var profileName by remember { mutableStateOf("Anime 1080p HEVC") }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Basic", "Weights & Strategy", "Fallback Tiers")

    var selectedStrategy by remember { mutableStateOf(SelectionStrategy.Balanced) }
    var resolutionWeight by remember { mutableIntStateOf(30) }
    var codecWeight by remember { mutableIntStateOf(20) }
    var uploaderWeight by remember { mutableIntStateOf(15) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile Builder", style = AppTypography.headline, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    Button(
                        onClick = onSaveProfile,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        shape = AppShapes.pill,
                        modifier = Modifier.padding(end = AppSpacing.sm)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save")
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
            // Profile Name Input
            OutlinedTextField(
                value = profileName,
                onValueChange = { profileName = it },
                label = { Text("Profile Name") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryIndigo,
                    unfocusedBorderColor = DarkCardBorder,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = AppShapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs)
            )

            // Step Navigation Tabs
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

            when (selectedTab) {
                0 -> BasicProfileTab()
                1 -> WeightsAndStrategyTab(
                    strategy = selectedStrategy,
                    onStrategyChange = { selectedStrategy = it },
                    resWeight = resolutionWeight,
                    onResWeightChange = { resolutionWeight = it },
                    codecWeight = codecWeight,
                    onCodecWeightChange = { codecWeight = it },
                    uploaderWeight = uploaderWeight,
                    onUploaderWeightChange = { uploaderWeight = it }
                )
                2 -> FallbackTiersTab()
            }
        }
    }
}

@Composable
private fun BasicProfileTab() {
    LazyColumn(
        contentPadding = PaddingValues(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = AppShapes.medium,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(AppSpacing.md)) {
                    Text("Target Quality", style = AppTypography.title, color = TextPrimary)
                    Spacer(modifier = Modifier.height(AppSpacing.xs))
                    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                        FilterChip(selected = true, onClick = {}, label = { Text("1080p") })
                        FilterChip(selected = false, onClick = {}, label = { Text("720p") })
                        FilterChip(selected = false, onClick = {}, label = { Text("4K") })
                    }
                    Spacer(modifier = Modifier.height(AppSpacing.sm))
                    Text("Video Codec", style = AppTypography.title, color = TextPrimary)
                    Spacer(modifier = Modifier.height(AppSpacing.xs))
                    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                        FilterChip(selected = true, onClick = {}, label = { Text("HEVC / x265") })
                        FilterChip(selected = false, onClick = {}, label = { Text("H.264 / AVC") })
                        FilterChip(selected = false, onClick = {}, label = { Text("AV1") })
                    }
                }
            }
        }
    }
}

@Composable
private fun WeightsAndStrategyTab(
    strategy: SelectionStrategy,
    onStrategyChange: (SelectionStrategy) -> Unit,
    resWeight: Int,
    onResWeightChange: (Int) -> Unit,
    codecWeight: Int,
    onCodecWeightChange: (Int) -> Unit,
    uploaderWeight: Int,
    onUploaderWeightChange: (Int) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = AppShapes.medium,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(AppSpacing.md)) {
                    Text("Selection Strategy", style = AppTypography.title, color = TextPrimary)
                    Spacer(modifier = Modifier.height(AppSpacing.xs))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                        items(SelectionStrategy.values()) { strat ->
                            FilterChip(
                                selected = strategy == strat,
                                onClick = { onStrategyChange(strat) },
                                label = { Text(strat.name) }
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = AppShapes.medium,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(AppSpacing.md)) {
                    Text("Score Weights (Expert Mode)", style = AppTypography.title, color = TextPrimary)
                    Spacer(modifier = Modifier.height(AppSpacing.sm))

                    Text("Resolution Weight: $resWeight", style = AppTypography.bodySecondary)
                    Slider(
                        value = resWeight.toFloat(),
                        onValueChange = { onResWeightChange(it.toInt()) },
                        valueRange = 0f..50f,
                        colors = SliderDefaults.colors(thumbColor = PrimaryIndigo, activeTrackColor = PrimaryIndigo)
                    )

                    Text("Codec Weight: $codecWeight", style = AppTypography.bodySecondary)
                    Slider(
                        value = codecWeight.toFloat(),
                        onValueChange = { onCodecWeightChange(it.toInt()) },
                        valueRange = 0f..50f,
                        colors = SliderDefaults.colors(thumbColor = PrimaryIndigo, activeTrackColor = PrimaryIndigo)
                    )

                    Text("Uploader Weight: $uploaderWeight", style = AppTypography.bodySecondary)
                    Slider(
                        value = uploaderWeight.toFloat(),
                        onValueChange = { onUploaderWeightChange(it.toInt()) },
                        valueRange = 0f..50f,
                        colors = SliderDefaults.colors(thumbColor = PrimaryIndigo, activeTrackColor = PrimaryIndigo)
                    )
                }
            }
        }
    }
}

@Composable
private fun FallbackTiersTab() {
    val tiers = listOf(
        FallbackTier(1, "Tier 1: Preferred Encode", null, null, false, false, null, 5, "1080p HEVC + Preferred Uploader"),
        FallbackTier(2, "Tier 2: Any Uploader", null, null, true, false, null, 3, "1080p HEVC with any uploader"),
        FallbackTier(3, "Tier 3: Standard Codec", null, null, true, true, null, 2, "1080p H.264 fallback"),
        FallbackTier(4, "Tier 4: Compatible", null, null, true, true, null, 1, "Any compatible file with seeds")
    )

    LazyColumn(
        contentPadding = PaddingValues(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        items(tiers) { tier ->
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = AppShapes.medium,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(AppSpacing.md)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(tier.name, style = AppTypography.title, color = TextPrimary)
                        Text("Min Seeds: ${tier.minSeeders}", style = AppTypography.caption, color = AppSemanticColors.Success)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(tier.explanation, style = AppTypography.bodySecondary)
                }
            }
        }
    }
}
