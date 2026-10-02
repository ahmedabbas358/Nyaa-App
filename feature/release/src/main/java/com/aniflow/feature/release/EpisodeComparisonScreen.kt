package com.aniflow.feature.release

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.aniflow.domain.usecase.ReleaseComparisonAttribute

/**
 * Step 20 — Episode Release Comparison UI State (Section 41).
 */
data class ComparisonReleaseHeader(
    val releaseId: String,
    val title: String,
    val releaseGroup: String?,
    val isRecommended: Boolean = false
)

data class EpisodeComparisonUiState(
    val releases: List<ComparisonReleaseHeader> = emptyList(),
    val attributes: List<ReleaseComparisonAttribute> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeComparisonScreen(
    uiState: EpisodeComparisonUiState,
    onBackClick: () -> Unit,
    onSelectRelease: (releaseId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val horizontalScrollState = rememberScrollState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Compare Releases (${uiState.releases.size})",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .horizontalScroll(horizontalScrollState)
                .padding(16.dp)
        ) {
            // Header Row (Releases columns)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Label placeholder
                Box(modifier = Modifier.width(130.dp)) {
                    Text(
                        text = "Attribute",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                uiState.releases.forEach { release ->
                    ComparisonReleaseCard(
                        release = release,
                        onSelect = { onSelectRelease(release.releaseId) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Attribute Rows (Section 41: Resolution, Codec, Source, Audio, Subs, Size, Seeds, Group, etc.)
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.attributes) { attribute ->
                    AttributeComparisonRow(
                        attribute = attribute,
                        columnCount = uiState.releases.size
                    )
                }
            }
        }
    }
}

@Composable
private fun ComparisonReleaseCard(
    release: ComparisonReleaseHeader,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier.width(220.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (release.isRecommended) PrimaryIndigo else DarkCardBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (release.isRecommended) {
                Surface(
                    color = PrimaryIndigo.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "Recommended",
                        color = PrimaryIndigo,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = release.title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2
            )

            if (release.releaseGroup != null) {
                Text(
                    text = release.releaseGroup,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Button(
                onClick = onSelect,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Select", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun AttributeComparisonRow(
    attribute: ReleaseComparisonAttribute,
    columnCount: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DarkSurface,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Attribute name
            Box(modifier = Modifier.width(130.dp)) {
                Text(
                    text = attribute.name,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Values for each release
            attribute.values.take(columnCount).forEach { value ->
                Box(
                    modifier = Modifier.width(220.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = value,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        maxLines = 2
                    )
                }
            }
        }
    }
}
