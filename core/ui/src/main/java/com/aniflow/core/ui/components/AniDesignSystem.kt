package com.aniflow.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aniflow.core.ui.theme.AppSemanticColors
import com.aniflow.core.ui.theme.AppShapes
import com.aniflow.core.ui.theme.AppSpacing
import com.aniflow.core.ui.theme.AppTypography
import com.aniflow.core.ui.theme.DarkBackground
import com.aniflow.core.ui.theme.DarkCardBorder
import com.aniflow.core.ui.theme.DarkSurface
import com.aniflow.core.ui.theme.DarkSurfaceVariant
import com.aniflow.core.ui.theme.PrimaryIndigo
import com.aniflow.core.ui.theme.TextMuted
import com.aniflow.core.ui.theme.TextPrimary
import com.aniflow.core.ui.model.ReleaseUiModel
import com.aniflow.core.ui.theme.TextSecondary

/**
 * Standard AniFlow App Bar (Section 122).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AniAppBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = AppTypography.headline,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = AppTypography.caption,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Navigate back",
                        tint = TextPrimary
                    )
                }
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = DarkBackground,
            titleContentColor = TextPrimary
        ),
        modifier = modifier
    )
}

/**
 * Standard AniFlow Search Bar (Section 13, 122).
 */
@Composable
fun AniSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    placeholder: String = "Search anime, episodes, releases...",
    onFilterClick: (() -> Unit)? = null,
    activeFilterCount: Int = 0,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder, style = AppTypography.body, color = TextMuted) },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary)
        },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search", tint = TextSecondary)
                    }
                }
                if (onFilterClick != null) {
                    IconButton(onClick = onFilterClick) {
                        Box {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = "Filter options",
                                tint = if (activeFilterCount > 0) PrimaryIndigo else TextSecondary
                            )
                            if (activeFilterCount > 0) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .align(Alignment.TopEnd)
                                        .background(PrimaryIndigo, shape = CircleShape)
                                ) {
                                    Text(
                                        text = "$activeFilterCount",
                                        style = AppTypography.caption.copy(fontSize = 9.sp),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = DarkSurface,
            unfocusedContainerColor = DarkSurface,
            focusedBorderColor = PrimaryIndigo,
            unfocusedBorderColor = DarkCardBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = PrimaryIndigo
        ),
        shape = AppShapes.medium,
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * Editorial 2:3 Aspect Ratio Anime Poster (Section 10, 122).
 */
@Composable
fun AniPoster(
    title: String,
    progressFraction: Float? = null,
    badgeText: String? = null,
    badgeColor: Color = PrimaryIndigo,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = AppShapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
        modifier = modifier
            .aspectRatio(2f / 3f)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Stylized placeholder artwork when image loading is deferred
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkSurface)
                    .padding(AppSpacing.sm)
            ) {
                Text(
                    text = title.take(2).uppercase(),
                    style = AppTypography.displayLarge.copy(fontSize = 32.sp),
                    color = PrimaryIndigo.copy(alpha = 0.35f),
                    fontWeight = FontWeight.Black
                )
            }

            // Top Badge
            if (badgeText != null) {
                Box(
                    modifier = Modifier
                        .padding(AppSpacing.xs)
                        .background(badgeColor, shape = AppShapes.small)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = AppTypography.caption.copy(fontSize = 10.sp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Bottom Progress Bar for Continue Watching
            if (progressFraction != null && progressFraction > 0f) {
                LinearProgressIndicator(
                    progress = { progressFraction },
                    color = PrimaryIndigo,
                    trackColor = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter)
                )
            }
        }
    }
}

/**
 * 16:9 Landscape Media Card for Episodes and Player Thumbnails (Section 10, 123).
 */
@Composable
fun AniLandscapeMediaCard(
    title: String,
    subtitle: String? = null,
    durationText: String? = null,
    progressFraction: Float? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = AppShapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                // Duration overlay badge
                if (durationText != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(AppSpacing.xs)
                            .background(Color.Black.copy(alpha = 0.75f), shape = AppShapes.small)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = durationText,
                            style = AppTypography.caption.copy(fontSize = 11.sp),
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Progress Indicator
                if (progressFraction != null && progressFraction > 0f) {
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        color = PrimaryIndigo,
                        trackColor = Color.Black.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.BottomCenter)
                    )
                }
            }

            Column(modifier = Modifier.padding(AppSpacing.sm)) {
                Text(
                    text = title,
                    style = AppTypography.subheadline,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = AppTypography.caption,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Semantic Status Badge (Section 124).
 */
@Composable
fun AniStatusBadge(
    label: String,
    color: Color = PrimaryIndigo,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = AppShapes.small,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Text(
            text = label,
            color = color,
            style = AppTypography.caption.copy(fontSize = 11.sp),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

/**
 * Compact Two-Column Metadata Row (Section 39, 125, 133).
 */
@Composable
fun AniMetadataRow(
    property: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Text(
            text = property,
            style = AppTypography.caption,
            color = TextMuted
        )
        Text(
            text = value,
            style = AppTypography.bodySmall,
            color = TextPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Standardized Section Header (Section 129).
 */
@Composable
fun AniSectionHeader(
    title: String,
    subtitle: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.xs)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = AppTypography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = AppTypography.caption,
                    color = TextMuted
                )
            }
        }
        if (actionText != null && onActionClick != null) {
            TextButton(
                onClick = onActionClick,
                contentPadding = PaddingValues(horizontal = AppSpacing.sm, vertical = 0.dp)
            ) {
                Text(
                    text = actionText,
                    style = AppTypography.bodySmall,
                    color = PrimaryIndigo,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Content-First Empty State Component (Section 102, 122).
 */
@Composable
fun AniEmptyState(
    title: String,
    message: String = "",
    icon: ImageVector = Icons.Default.Info,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    description: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val actualMessage = if (message.isNotBlank()) message else description.orEmpty()
    val actualActionText = actionText ?: actionLabel
    val actualOnAction = onActionClick ?: onAction

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(AppSpacing.xl)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .background(DarkSurface, shape = CircleShape)
                .border(1.dp, DarkCardBorder, shape = CircleShape)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(AppSpacing.md))
        Text(
            text = title,
            style = AppTypography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(AppSpacing.xs))
        Text(
            text = actualMessage,
            style = AppTypography.bodySmall,
            color = TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = AppSpacing.md)
        )
        if (actualActionText != null && actualOnAction != null) {
            Spacer(modifier = Modifier.height(AppSpacing.md))
            Button(
                onClick = actualOnAction,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                shape = AppShapes.pill
            ) {
                Text(text = actualActionText, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}

/**
 * Explanatory Error State Component (Section 101, 122).
 * Formulated with "What happened? Why? What can I do?"
 */
@Composable
fun AniErrorState(
    title: String,
    reason: String = "",
    onRetry: () -> Unit = {},
    retryText: String = "Retry Operation",
    secondaryActionText: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    message: String? = null
) {
    val actualReason = if (reason.isNotBlank()) reason else message.orEmpty()

    Card(
        colors = CardDefaults.cardColors(containerColor = AppSemanticColors.Error.copy(alpha = 0.1f)),
        shape = AppShapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, AppSemanticColors.Error.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(AppSpacing.md)
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = AppSemanticColors.Error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(AppSpacing.xs))
                Text(
                    text = title,
                    style = AppTypography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppSemanticColors.Error
                )
            }
            Spacer(modifier = Modifier.height(AppSpacing.xs))
            Text(
                text = actualReason,
                style = AppTypography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(AppSpacing.md))
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = AppSemanticColors.Error),
                    shape = AppShapes.pill
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(retryText, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                if (secondaryActionText != null && onSecondaryAction != null) {
                    OutlinedButton(
                        onClick = onSecondaryAction,
                        shape = AppShapes.pill
                    ) {
                        Text(secondaryActionText, color = TextPrimary)
                    }
                }
            }
        }
    }
}

/**
 * Professional Settings Row Component (Section 128).
 */
@Composable
fun AniSettingRow(
    title: String,
    description: String? = null,
    currentValue: String? = null,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = AppSpacing.sm, horizontal = AppSpacing.md)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(AppSpacing.md))
            }
            Column {
                Text(
                    text = title,
                    style = AppTypography.body,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                if (description != null) {
                    Text(
                        text = description,
                        style = AppTypography.caption,
                        color = TextMuted
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (currentValue != null) {
                Text(
                    text = currentValue,
                    style = AppTypography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(end = AppSpacing.xs)
                )
            }
            if (trailingContent != null) {
                trailingContent()
            } else if (onClick != null) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Standardized Primary Button (Section 122).
 */
@Composable
fun AniPrimaryButton(
    text: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryIndigo,
            disabledContainerColor = DarkSurface
        ),
        shape = AppShapes.pill,
        modifier = modifier.height(44.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(AppSpacing.xs))
        }
        Text(text = text, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

/**
 * Standardized Secondary Button (Section 122).
 */
@Composable
fun AniSecondaryButton(
    text: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = AppShapes.pill,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
        modifier = modifier.height(44.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(AppSpacing.xs))
        }
        Text(text = text, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}

/**
 * Standardized Icon Button (Section 122).
 */
@Composable
fun AniIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = TextSecondary,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(40.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Standardized Filter Chip Component (Section 28, 122).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AniFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    onRemove: (() -> Unit)? = null,
    count: Int? = null,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    style = AppTypography.caption.copy(fontSize = 12.sp),
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
                if (count != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "($count)",
                        style = AppTypography.caption.copy(fontSize = 11.sp),
                        color = if (selected) Color.White else TextMuted
                    )
                }
            }
        },
        trailingIcon = if (selected && onRemove != null) {
            {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove $label filter",
                    modifier = Modifier
                        .size(14.dp)
                        .clickable(onClick = onRemove)
                )
            }
        } else null,
        shape = AppShapes.pill,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = DarkSurface,
            labelColor = TextSecondary,
            selectedContainerColor = PrimaryIndigo,
            selectedLabelColor = Color.White
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = DarkCardBorder,
            selectedBorderColor = PrimaryIndigo,
            enabled = true,
            selected = selected
        ),
        modifier = modifier
    )
}

/**
 * Centralized Download & Playback Progress Bar (Section 126, 127).
 * Supports linear percentage, speed, ETA, and state indicators.
 */
@Composable
fun AniProgressBar(
    progress: Float,
    speedFormatted: String? = null,
    etaFormatted: String? = null,
    statusText: String? = null,
    statusColor: Color = PrimaryIndigo,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            color = statusColor,
            trackColor = DarkSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(AppShapes.pill)
        )
        if (speedFormatted != null || etaFormatted != null || statusText != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (statusText != null) {
                        Text(
                            text = statusText,
                            style = AppTypography.caption,
                            color = statusColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(AppSpacing.xs))
                    }
                    Text(
                        text = "${(progress.coerceIn(0f, 1f) * 100).toInt()}%",
                        style = AppTypography.numericSpeed.copy(fontSize = 11.sp),
                        color = TextPrimary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (speedFormatted != null) {
                        Text(
                            text = speedFormatted,
                            style = AppTypography.numericSpeed.copy(fontSize = 11.sp),
                            color = TextSecondary
                        )
                    }
                    if (etaFormatted != null) {
                        Spacer(modifier = Modifier.width(AppSpacing.xs))
                        Text(
                            text = "ETA: $etaFormatted",
                            style = AppTypography.numericEta.copy(fontSize = 11.sp),
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}

/**
 * Standard Confirmation Dialog for destructive or sensitive operations (Section 99).
 */
@Composable
fun AniConfirmationDialog(
    title: String,
    message: String,
    impactWarning: String? = null,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = AppTypography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDestructive) AppSemanticColors.Error else TextPrimary
            )
        },
        text = {
            Column {
                Text(
                    text = message,
                    style = AppTypography.body,
                    color = TextSecondary
                )
                if (impactWarning != null) {
                    Spacer(modifier = Modifier.height(AppSpacing.sm))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isDestructive) AppSemanticColors.Error else AppSemanticColors.Warning,
                            modifier = Modifier.size(16.dp).padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = impactWarning,
                            style = AppTypography.caption,
                            color = if (isDestructive) AppSemanticColors.Error else AppSemanticColors.Warning,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDestructive) AppSemanticColors.Error else PrimaryIndigo
                ),
                shape = AppShapes.pill
            ) {
                Text(text = confirmText, fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = dismissText, color = TextSecondary)
            }
        },
        containerColor = DarkSurface,
        shape = AppShapes.large
    )
}

/**
 * Standard Two-Column Technical Property/Value Row (Section 133).
 */
@Composable
fun AniDataRow(
    property: String,
    value: String,
    isHighlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = AppSpacing.sm)
    ) {
        Text(
            text = property,
            style = AppTypography.caption,
            color = TextMuted
        )
        Text(
            text = value,
            style = AppTypography.Metadata,
            color = if (isHighlighted) PrimaryIndigo else TextPrimary,
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * Standard AniReleaseRow (Section 39, 125, 132).
 */
@Composable
fun AniReleaseRow(
    title: String = "",
    resolution: String = "",
    codec: String = "",
    source: String = "",
    sizeFormatted: String = "",
    seeds: Int = 0,
    peers: Int = 0,
    uploader: String = "",
    isPreferred: Boolean = false,
    onClick: () -> Unit = {},
    onDownloadClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    release: ReleaseUiModel? = null
) {
    val actualTitle = release?.title ?: title
    val actualResolution = release?.resolution ?: resolution
    val actualCodec = release?.codec ?: codec
    val actualSource = release?.releaseGroup ?: source
    val actualSizeFormatted = release?.sizeFormatted ?: sizeFormatted
    val actualSeeds = release?.seeders ?: seeds
    val actualPeers = release?.leechers ?: peers
    val actualUploader = release?.uploader ?: uploader
    val actualIsPreferred = release?.isPreferred ?: isPreferred

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (actualIsPreferred) PrimaryIndigo.copy(alpha = 0.08f) else DarkSurface
        ),
        shape = AppShapes.medium,
        border = androidx.compose.foundation.BorderStroke(
            if (actualIsPreferred) 1.5.dp else 1.dp,
            if (actualIsPreferred) PrimaryIndigo else DarkCardBorder
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (actualIsPreferred) {
                        AniStatusBadge(
                            label = "SMART PREFERRED",
                            color = AppSemanticColors.Warning,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    Text(
                        text = actualTitle,
                        style = AppTypography.Title.copy(fontSize = 15.sp),
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onDownloadClick) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download release",
                        tint = PrimaryIndigo
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.xs))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                AniStatusBadge(label = actualResolution, color = PrimaryIndigo)
                AniStatusBadge(label = actualCodec, color = AppSemanticColors.Success)
                AniStatusBadge(label = actualSource, color = AppSemanticColors.Info)
                Text(
                    text = "• $actualUploader",
                    style = AppTypography.caption,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.xs))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = actualSizeFormatted,
                    style = AppTypography.numericSize,
                    color = TextPrimary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Seeders",
                            tint = AppSemanticColors.Success,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "$actualSeeds",
                            style = AppTypography.numericSpeed,
                            color = AppSemanticColors.Success
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Peers",
                            tint = AppSemanticColors.Info,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "$actualPeers",
                            style = AppTypography.numericSpeed,
                            color = AppSemanticColors.Info
                        )
                    }
                }
            }
        }
    }
}

/**
 * Standard AniEpisodeRow (Section 36, 122).
 */
@Composable
fun AniEpisodeRow(
    episodeNumber: Int,
    title: String,
    durationFormatted: String? = null,
    statusBadge: String? = null,
    statusColor: Color = PrimaryIndigo,
    progressFraction: Float? = null,
    onClick: () -> Unit,
    onActionClick: (() -> Unit)? = null,
    actionIcon: ImageVector = Icons.Default.PlayArrow,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = AppShapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.md)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .background(PrimaryIndigo.copy(alpha = 0.15f), shape = CircleShape)
                ) {
                    Text(
                        text = "$episodeNumber",
                        style = AppTypography.numericSpeed.copy(fontSize = 14.sp),
                        color = PrimaryIndigo,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(AppSpacing.md))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = AppTypography.Title.copy(fontSize = 15.sp),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        if (durationFormatted != null) {
                            Text(
                                text = durationFormatted,
                                style = AppTypography.caption,
                                color = TextMuted
                            )
                        }
                        if (statusBadge != null) {
                            AniStatusBadge(label = statusBadge, color = statusColor)
                        }
                    }
                }

                if (onActionClick != null) {
                    IconButton(onClick = onActionClick) {
                        Icon(
                            imageVector = actionIcon,
                            contentDescription = "Episode action",
                            tint = PrimaryIndigo
                        )
                    }
                }
            }

            if (progressFraction != null && progressFraction > 0f) {
                LinearProgressIndicator(
                    progress = { progressFraction.coerceIn(0f, 1f) },
                    color = PrimaryIndigo,
                    trackColor = DarkSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                )
            }
        }
    }
}

/**
 * Standard AniDownloadCard (Section 57, 131).
 */
@Composable
fun AniDownloadCard(
    title: String,
    statusText: String,
    statusColor: Color,
    progressFraction: Float,
    speedFormatted: String? = null,
    etaFormatted: String? = null,
    isPaused: Boolean = false,
    onPauseResume: () -> Unit,
    onCancel: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = AppShapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = AppTypography.Title.copy(fontSize = 15.sp),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    AniStatusBadge(
                        label = statusText,
                        color = statusColor,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPauseResume) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (isPaused) "Resume download" else "Pause download",
                            tint = PrimaryIndigo
                        )
                    }
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel download",
                            tint = TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            AniProgressBar(
                progress = progressFraction,
                speedFormatted = speedFormatted,
                etaFormatted = etaFormatted,
                statusColor = statusColor
            )
        }
    }
}

/**
 * Standard AniLoadingState (Section 103, 122).
 * Contextual loading presentation with calm indicator and descriptive text.
 */
@Composable
fun AniLoadingState(
    message: String = "Loading...",
    subtext: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(AppSpacing.xxl)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = PrimaryIndigo,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(AppSpacing.md))
            Text(
                text = message,
                style = AppTypography.Subtitle,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            if (subtext != null) {
                Spacer(modifier = Modifier.height(AppSpacing.xxs))
                Text(
                    text = subtext,
                    style = AppTypography.Caption,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Standard AniChip (Section 122).
 * Compact chip for tags, categories, or filters.
 */
@Composable
fun AniChip(
    label: String,
    isSelected: Boolean = false,
    leadingIcon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) PrimaryIndigo.copy(alpha = 0.2f) else DarkSurface
    val borderColor = if (isSelected) PrimaryIndigo else DarkCardBorder
    val contentColor = if (isSelected) PrimaryIndigo else TextSecondary

    Surface(
        color = backgroundColor,
        shape = AppShapes.small,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier
                        .size(12.dp)
                        .padding(end = 4.dp)
                )
            }
            Text(
                text = label,
                style = AppTypography.Caption.copy(fontWeight = FontWeight.Medium),
                color = contentColor
            )
        }
    }
}

/**
 * Standard AniStat (Section 122).
 * Compact technical metric card for statistics, dashboards, and diagnostics.
 */
@Composable
fun AniStat(
    label: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    accentColor: Color = PrimaryIndigo,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = AppShapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = label,
                    style = AppTypography.Overline,
                    color = TextMuted
                )
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(AppSpacing.xs))
            Text(
                text = value,
                style = AppTypography.Title.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(AppSpacing.xxs))
                Text(
                    text = subtitle,
                    style = AppTypography.Caption,
                    color = TextSecondary
                )
            }
        }
    }
}

/**
 * Standard AniMediaCard (Section 122, 123).
 * Dual-ratio media card supporting poster (2:3) or landscape (16:9) formats.
 */
@Composable
fun AniMediaCard(
    title: String,
    subtitle: String? = null,
    badgeText: String? = null,
    badgeColor: Color = PrimaryIndigo,
    isLandscape: Boolean = false,
    progressFraction: Float? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val aspectRatio = if (isLandscape) 16f / 9f else 2f / 3f

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = AppShapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .background(DarkBackground)
            ) {
                if (badgeText != null) {
                    AniStatusBadge(
                        label = badgeText,
                        color = badgeColor,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(AppSpacing.xs)
                    )
                }
                if (progressFraction != null && progressFraction > 0f) {
                    LinearProgressIndicator(
                        progress = { progressFraction.coerceIn(0f, 1f) },
                        color = PrimaryIndigo,
                        trackColor = Color.Black.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.BottomCenter)
                    )
                }
            }
            Column(modifier = Modifier.padding(AppSpacing.sm)) {
                Text(
                    text = title,
                    style = AppTypography.Subtitle.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = AppTypography.Caption,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Standard AniFilterSection (Section 27, 122).
 * Grouped filter section with collapsible content and optional reset button.
 */
@Composable
fun AniFilterSection(
    title: String,
    onReset: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(vertical = AppSpacing.xs)
        ) {
            Text(
                text = title,
                style = AppTypography.SectionTitle,
                color = TextPrimary
            )
            if (onReset != null) {
                TextButton(
                    onClick = onReset,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "Reset",
                        style = AppTypography.Caption,
                        color = PrimaryIndigo
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(AppSpacing.xs))
        content()
    }
}

/**
 * Standard AniTimeline (Section 49, 122).
 * Timeline node for execution logs, automation history, or rule audits.
 */
@Composable
fun AniTimelineItem(
    title: String,
    timestampFormatted: String,
    description: String? = null,
    statusColor: Color = PrimaryIndigo,
    isLast: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(DarkCardBorder)
                )
            }
        }
        Spacer(modifier = Modifier.width(AppSpacing.sm))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else AppSpacing.md)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = AppTypography.Subtitle.copy(fontWeight = FontWeight.Medium),
                    color = TextPrimary
                )
                Text(
                    text = timestampFormatted,
                    style = AppTypography.Caption,
                    color = TextMuted
                )
            }
            if (description != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = AppTypography.BodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

/**
 * Standard AniDialog (Section 98, 99, 122).
 * High-clarity confirmation or informational dialog.
 */
@Composable
fun AniDialog(
    title: String,
    message: String,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = AppTypography.LargeTitle.copy(fontSize = 18.sp),
                color = TextPrimary
            )
        },
        text = {
            Text(
                text = message,
                style = AppTypography.Body,
                color = TextSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDestructive) AppSemanticColors.Error else PrimaryIndigo
                ),
                shape = AppShapes.small
            ) {
                Text(text = confirmText)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = AppShapes.small
            ) {
                Text(text = dismissText, color = TextMuted)
            }
        },
        containerColor = DarkSurface,
        shape = AppShapes.large
    )
}

