package com.aniflow.feature.search.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aniflow.core.ui.components.ReleaseCardDensity
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

/**
 * Top Search Header with dynamic action icons and IME integration.
 */
@Composable
fun SearchHeaderBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onClearQuery: () -> Unit,
    density: ReleaseCardDensity,
    onToggleDensity: () -> Unit,
    onOpenFilterSheet: () -> Unit,
    onOpenSortSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search releases, anime, or uploader...", color = TextMuted) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = PrimaryIndigo)
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClearQuery) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = TextMuted)
                    }
                }
            },
            singleLine = true,
            shape = AppShapes.roundedMedium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryIndigo,
                unfocusedBorderColor = DarkCardBorder,
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearchSubmit() })
        )

        Spacer(modifier = Modifier.height(AppSpacing.xs))

        // Quick filter and mode toggles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                modifier = Modifier.weight(1f)
            ) {
                item {
                    IconButton(onClick = onOpenFilterSheet) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filters", tint = TextSecondary)
                    }
                }
                item {
                    IconButton(onClick = onOpenSortSheet) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort", tint = TextSecondary)
                    }
                }
            }

            IconButton(onClick = onToggleDensity) {
                Icon(
                    imageVector = if (density == ReleaseCardDensity.Comfortable) Icons.Default.ViewList else Icons.Default.GridView,
                    contentDescription = "Toggle view density",
                    tint = TextSecondary
                )
            }
        }
    }
}

/**
 * Banner notifying if data is cached or offline (Section 60, 61).
 */
@Composable
fun FreshnessBanner(
    isFromCache: Boolean,
    freshnessText: String?,
    modifier: Modifier = Modifier
) {
    if (freshnessText != null || isFromCache) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
            color = if (isFromCache) AppSemanticColors.warning.copy(alpha = 0.15f) else PrimaryIndigo.copy(alpha = 0.15f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = if (isFromCache) AppSemanticColors.warning else PrimaryIndigo,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = freshnessText ?: if (isFromCache) "Displaying cached offline results" else "Updated",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Filter Bottom Sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchFiltersSheet(
    isTrustedOnly: Boolean,
    selectedResolution: String?,
    onToggleTrusted: (Boolean) -> Unit,
    onSelectResolution: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.lg)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Search Filters", style = AppTypography.headingMedium, color = TextPrimary)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            Text("Uploader Trust Level", style = AppTypography.bodySmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(AppSpacing.xs))
            FilterChip(
                selected = isTrustedOnly,
                onClick = { onToggleTrusted(!isTrustedOnly) },
                label = { Text("Trusted Uploaders Only") },
                leadingIcon = if (isTrustedOnly) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryIndigo,
                    selectedLabelColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(AppSpacing.md))

            Text("Resolution", style = AppTypography.bodySmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(AppSpacing.xs))
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                listOf("1080p", "720p", "4k").forEach { res ->
                    FilterChip(
                        selected = selectedResolution == res,
                        onClick = { onSelectResolution(res) },
                        label = { Text(res) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryIndigo,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.xl))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
            ) {
                Text("Apply Filters", color = Color.White)
            }
        }
    }
}

/**
 * Sort Bottom Sheet (Section 81).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchSortSheet(
    currentSort: String,
    onSelectSort: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sortOptions = listOf(
        "Date" to "Release Date (Chronological)",
        "Seeds" to "Seeders (Healthiest Swarm)",
        "Downloads" to "Downloads (Most Popular)",
        "Size" to "File Size",
        "Comments" to "Comments Count"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.lg)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sort Releases", style = AppTypography.headingMedium, color = TextPrimary)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            sortOptions.forEach { (key, label) ->
                val isSelected = currentSort.equals(key, ignoreCase = true)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            onSelectSort(key)
                            onDismiss()
                        },
                    color = if (isSelected) PrimaryIndigo.copy(alpha = 0.15f) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) PrimaryIndigo else TextPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryIndigo)
                        }
                    }
                }
            }
        }
    }
}
