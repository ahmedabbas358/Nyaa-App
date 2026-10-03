package com.aniflow.feature.release

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aniflow.core.ui.theme.AppSemanticColors
import com.aniflow.core.ui.theme.DarkBackground
import com.aniflow.core.ui.theme.DarkCardBorder
import com.aniflow.core.ui.theme.DarkSurface
import com.aniflow.core.ui.theme.PrimaryIndigo
import com.aniflow.core.ui.theme.TextMuted
import com.aniflow.core.ui.theme.TextPrimary
import com.aniflow.core.ui.theme.TextSecondary
import com.aniflow.domain.anime.AnimeStatus
import com.aniflow.domain.anime.MediaType
import com.aniflow.domain.coverage.AnimeCoverage
import com.aniflow.domain.coverage.SeasonCoverage

import com.aniflow.core.ui.components.AniEmptyState
import androidx.compose.material.icons.filled.Movie

/**
 * Step 20 — Anime Detail UI State (Section 31, 32, 82).
 * Zero hardcoded production data.
 */
data class AnimeDetailUiState(
    val animeId: String = "",
    val canonicalTitle: String = "",
    val alternativeTitle: String? = null,
    val year: Int? = null,
    val mediaType: MediaType = MediaType.Series,
    val status: AnimeStatus = AnimeStatus.Ongoing,
    val isFavorite: Boolean = false,
    val pendingReviewCount: Int = 0,
    val coverage: AnimeCoverage? = null,
    val seasons: List<SeasonSummaryUiModel> = emptyList()
)

data class SeasonSummaryUiModel(
    val seasonId: String,
    val title: String,
    val number: Int?,
    val availableCount: Int,
    val expectedCount: Int?,
    val percentage: Float?,
    val coverageSummaryText: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeDetailScreen(
    uiState: AnimeDetailUiState,
    onBackClick: () -> Unit,
    onSeasonClick: (seasonId: String) -> Unit,
    onSearchReleasesClick: (query: String) -> Unit,
    onDownloadMissingClick: (animeId: String) -> Unit,
    onToggleFavoriteClick: () -> Unit,
    onPreferencesClick: () -> Unit,
    onReviewQueueClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.canonicalTitle.isNotBlank()) uiState.canonicalTitle else "Anime Details",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    if (uiState.canonicalTitle.isNotBlank()) {
                        IconButton(onClick = onToggleFavoriteClick) {
                            Icon(
                                imageVector = if (uiState.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (uiState.isFavorite) "Favorited" else "Favorite",
                                tint = if (uiState.isFavorite) Color(0xFFEF4444) else TextSecondary
                            )
                        }
                        IconButton(onClick = onPreferencesClick) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Preferences",
                                tint = TextSecondary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        }
    ) { padding ->
        if (uiState.canonicalTitle.isBlank() && uiState.seasons.isEmpty()) {
            AniEmptyState(
                title = "Anime not found",
                description = "The requested anime entity could not be found in your library or local index.",
                icon = Icons.Default.Movie,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Preferences",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Anime Header (Section 32)
            item {
                AnimeHeaderSection(
                    uiState = uiState,
                    onSearchReleasesClick = { onSearchReleasesClick(uiState.canonicalTitle) },
                    onDownloadMissingClick = { onDownloadMissingClick(uiState.animeId) }
                )
            }

            // 2. Review Queue Alert Banner (Section 64, 93)
            if (uiState.pendingReviewCount > 0) {
                item {
                    ReviewQueueBanner(
                        pendingCount = uiState.pendingReviewCount,
                        onClick = onReviewQueueClick
                    )
                }
            }

            // 3. Coverage Statistics Overview (Section 81, 82)
            item {
                AnimeCoverageOverviewSection(uiState = uiState)
            }

            // 4. Seasons Header
            item {
                Text(
                    text = "Seasons",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // 5. Season List (Section 34)
            items(uiState.seasons, key = { it.seasonId }) { season ->
                SeasonRowItem(
                    season = season,
                    onClick = { onSeasonClick(season.seasonId) }
                )
            }
        }
    }
}

@Composable
private fun AnimeHeaderSection(
    uiState: AnimeDetailUiState,
    onSearchReleasesClick: () -> Unit,
    onDownloadMissingClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Titles
            Text(
                text = uiState.canonicalTitle,
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )

            if (!uiState.alternativeTitle.isNullOrBlank()) {
                Text(
                    text = uiState.alternativeTitle,
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }

            // Badges row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = PrimaryIndigo.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = uiState.mediaType.name,
                        color = PrimaryIndigo,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    color = when (uiState.status) {
                        AnimeStatus.Ongoing -> AppSemanticColors.Info.copy(alpha = 0.15f)
                        AnimeStatus.Completed -> AppSemanticColors.Success.copy(alpha = 0.15f)
                        AnimeStatus.Hiatus -> AppSemanticColors.Warning.copy(alpha = 0.15f)
                        else -> TextMuted.copy(alpha = 0.15f)
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = uiState.status.name,
                        color = when (uiState.status) {
                            AnimeStatus.Ongoing -> AppSemanticColors.Info
                            AnimeStatus.Completed -> AppSemanticColors.Success
                            AnimeStatus.Hiatus -> AppSemanticColors.Warning
                            else -> TextSecondary
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (uiState.year != null) {
                    Text(
                        text = uiState.year.toString(),
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action buttons (Section 33)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSearchReleasesClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Search", fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = onDownloadMissingClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Missing", color = TextPrimary, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun ReviewQueueBanner(
    pendingCount: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = AppSemanticColors.Warning.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppSemanticColors.Warning.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Review Required",
                tint = AppSemanticColors.Warning,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$pendingCount release${if (pendingCount > 1) "s" else ""} need review",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Ambiguous titles or seasons detected",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = AppSemanticColors.Warning
            )
        }
    }
}

@Composable
private fun AnimeCoverageOverviewSection(uiState: AnimeDetailUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Coverage Status",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            val cov = uiState.coverage
            val summaryText = when {
                cov != null && cov.totalExpected != null ->
                    "${cov.totalAvailable} / ${cov.totalExpected} Available (${(cov.totalAvailable.toFloat() / cov.totalExpected * 100).toInt()}%)"
                cov != null ->
                    "${cov.totalAvailable} Episodes Found"
                else -> "Coverage Calculating..."
            }

            Text(
                text = summaryText,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            // Accessible linear bar if expected count is known (Section 81, 83)
            if (cov?.totalExpected != null && cov.totalExpected > 0) {
                LinearProgressIndicator(
                    progress = { (cov.totalAvailable.toFloat() / cov.totalExpected).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = AppSemanticColors.Success,
                    trackColor = DarkBackground,
                    strokeCap = StrokeCap.Round
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatBadge(label = "Seasons", value = uiState.seasons.size.toString())
                StatBadge(label = "Available", value = cov?.totalAvailable?.toString() ?: "—")
                StatBadge(label = "Downloading", value = cov?.totalDownloading?.toString() ?: "0")
                StatBadge(label = "Review", value = cov?.totalReview?.toString() ?: "0")
            }
        }
    }
}

@Composable
private fun StatBadge(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(text = label, color = TextMuted, fontSize = 11.sp)
    }
}

@Composable
private fun SeasonRowItem(
    season: SeasonSummaryUiModel,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = DarkSurface,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = season.title,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = season.coverageSummaryText,
                    color = TextSecondary,
                    fontSize = 13.sp
                )

                // Visual progress bar if percentage is known (Section 81, 83)
                if (season.percentage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { season.percentage.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = AppSemanticColors.Success,
                        trackColor = DarkBackground,
                        strokeCap = StrokeCap.Round
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View Season",
                tint = TextMuted
            )
        }
    }
}
