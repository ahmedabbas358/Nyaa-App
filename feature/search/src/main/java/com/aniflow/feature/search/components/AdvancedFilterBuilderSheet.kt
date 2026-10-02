package com.aniflow.feature.search.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.aniflow.core.ui.theme.DarkCardBorder
import com.aniflow.core.ui.theme.DarkSurface
import com.aniflow.core.ui.theme.PrimaryIndigo
import com.aniflow.core.ui.theme.TextMuted
import com.aniflow.core.ui.theme.TextPrimary
import com.aniflow.core.ui.theme.TextSecondary
import com.aniflow.domain.search.model.SearchPreset

data class AdvancedFilterSelection(
    val resolution: String? = null,
    val codec: String? = null,
    val audioLanguage: String? = null,
    val subtitleLanguage: String? = null,
    val isBatchOnly: Boolean = false,
    val isTrustedOnly: Boolean = false,
    val minSeeds: Int? = null,
    val source: String? = null
)

/**
 * AdvancedFilterBuilderSheet (Sections 23, 24, 25).
 * Multi-category visual filter builder with presets and structured AST mapping.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedFilterBuilderSheet(
    initialSelection: AdvancedFilterSelection = AdvancedFilterSelection(),
    onApply: (AdvancedFilterSelection) -> Unit,
    onSaveAsPreset: (AdvancedFilterSelection) -> Unit,
    onDismiss: () -> Unit
) {
    var selection by remember { mutableStateOf(initialSelection) }

    val resolutions = listOf("1080p", "720p", "4K / 2160p", "480p")
    val codecs = listOf("HEVC", "H.264", "AV1")
    val audioLangs = listOf("Japanese", "English", "Arabic", "Dual Audio")
    val subLangs = listOf("English", "Arabic", "Multi-Sub")
    val sources = listOf("BluRay", "Web-DL", "HDTV")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.lg)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FilterList, contentDescription = null, tint = PrimaryIndigo)
                    Spacer(Modifier.width(AppSpacing.sm))
                    Text("Advanced Filter Builder", style = AppTypography.headline, color = TextPrimary)
                }
                TextButton(onClick = { selection = AdvancedFilterSelection() }) {
                    Text("Reset", style = AppTypography.caption, color = AppSemanticColors.Error)
                }
            }

            Spacer(Modifier.height(AppSpacing.md))

            // Presets Row (Section 25)
            Text("Quick Presets", style = AppTypography.caption.copy(fontWeight = FontWeight.Bold), color = PrimaryIndigo)
            Spacer(Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                item {
                    PresetChip(name = "1080p HEVC") {
                        selection = selection.copy(resolution = "1080p", codec = "HEVC")
                    }
                }
                item {
                    PresetChip(name = "720p Small Size") {
                        selection = selection.copy(resolution = "720p", codec = "HEVC")
                    }
                }
                item {
                    PresetChip(name = "Anime Batches") {
                        selection = selection.copy(isBatchOnly = true)
                    }
                }
                item {
                    PresetChip(name = "Arabic Subs") {
                        selection = selection.copy(subtitleLanguage = "Arabic")
                    }
                }
            }

            Spacer(Modifier.height(AppSpacing.md))

            // 1. Resolution
            FilterCategorySection(title = "Resolution") {
                resolutions.forEach { res ->
                    val isSelected = selection.resolution == res
                    FilterChip(
                        selected = isSelected,
                        onClick = { selection = selection.copy(resolution = if (isSelected) null else res) },
                        label = { Text(res) }
                    )
                }
            }

            // 2. Video Codec
            FilterCategorySection(title = "Video Codec") {
                codecs.forEach { codec ->
                    val isSelected = selection.codec == codec
                    FilterChip(
                        selected = isSelected,
                        onClick = { selection = selection.copy(codec = if (isSelected) null else codec) },
                        label = { Text(codec) }
                    )
                }
            }

            // 3. Audio & Subtitles
            FilterCategorySection(title = "Audio Language") {
                audioLangs.forEach { lang ->
                    val isSelected = selection.audioLanguage == lang
                    FilterChip(
                        selected = isSelected,
                        onClick = { selection = selection.copy(audioLanguage = if (isSelected) null else lang) },
                        label = { Text(lang) }
                    )
                }
            }

            FilterCategorySection(title = "Subtitles") {
                subLangs.forEach { sub ->
                    val isSelected = selection.subtitleLanguage == sub
                    FilterChip(
                        selected = isSelected,
                        onClick = { selection = selection.copy(subtitleLanguage = if (isSelected) null else sub) },
                        label = { Text(sub) }
                    )
                }
            }

            // 4. Source & Torrent Health
            FilterCategorySection(title = "Source Format") {
                sources.forEach { src ->
                    val isSelected = selection.source == src
                    FilterChip(
                        selected = isSelected,
                        onClick = { selection = selection.copy(source = if (isSelected) null else src) },
                        label = { Text(src) }
                    )
                }
            }

            FilterCategorySection(title = "Availability & Health") {
                FilterChip(
                    selected = selection.isTrustedOnly,
                    onClick = { selection = selection.copy(isTrustedOnly = !selection.isTrustedOnly) },
                    label = { Text("Trusted Uploaders Only") }
                )
                FilterChip(
                    selected = selection.isBatchOnly,
                    onClick = { selection = selection.copy(isBatchOnly = !selection.isBatchOnly) },
                    label = { Text("Complete Batches Only") }
                )
                FilterChip(
                    selected = selection.minSeeds == 5,
                    onClick = { selection = selection.copy(minSeeds = if (selection.minSeeds == 5) null else 5) },
                    label = { Text("≥ 5 Seeds") }
                )
            }

            Spacer(Modifier.height(AppSpacing.lg))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                OutlinedButton(
                    onClick = { onSaveAsPreset(selection) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(AppShapes.sm)
                ) {
                    Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text("Save Preset")
                }
                Button(
                    onClick = {
                        onApply(selection)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    shape = RoundedCornerShape(AppShapes.sm)
                ) {
                    Text("Apply Filters")
                }
            }
        }
    }
}

@Composable
private fun FilterCategorySection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = AppSpacing.xs)) {
        Text(title, style = AppTypography.caption.copy(fontWeight = FontWeight.Bold), color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            item { content() }
        }
    }
}

@Composable
private fun PresetChip(name: String, onClick: () -> Unit) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = { Text(name, style = AppTypography.caption) },
        colors = FilterChipDefaults.filterChipColors(containerColor = DarkCardBorder)
    )
}
