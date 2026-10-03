package com.aniflow.feature.collections

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.sp
import com.aniflow.core.ui.theme.TextSecondary
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.aniflow.core.ui.model.CollectionUiModel
import com.aniflow.core.ui.theme.AppSemanticColors
import com.aniflow.core.ui.theme.AppShapes
import com.aniflow.core.ui.theme.AppSpacing
import com.aniflow.core.ui.theme.AppTypography
import com.aniflow.core.ui.theme.DarkBackground
import com.aniflow.core.ui.theme.DarkCardBorder
import com.aniflow.core.ui.theme.DarkSurface
import com.aniflow.core.ui.theme.PrimaryIndigo
import com.aniflow.core.ui.theme.TextPrimary
import com.aniflow.core.ui.components.AniAppBar
import com.aniflow.core.ui.components.AniEmptyState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CollectionsUiState(
    val collections: List<CollectionUiModel> = emptyList(),
    val isLoading: Boolean = false
)

class CollectionsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CollectionsUiState())
    val uiState: StateFlow<CollectionsUiState> = _uiState.asStateFlow()
}

/**
 * CollectionsScreen (Sections 54, 76, 77, 78).
 * Manages user manual collections and automated Smart Collections.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionsScreen(
    viewModel: CollectionsViewModel = CollectionsViewModel(),
    onCollectionClick: (String) -> Unit = {},
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            AniAppBar(
                title = "Collections",
                subtitle = "Curated anime lists",
                onBack = onBack
            )
        },
        containerColor = DarkBackground,
        modifier = modifier
    ) { paddingValues ->
        if (state.collections.isEmpty()) {
            AniEmptyState(
                title = "No collections created",
                description = "Create collections from Anime Details to group favorite franchises, sagas, or themes together.",
                icon = Icons.Default.FolderSpecial,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = AppSpacing.md,
                    end = AppSpacing.md,
                    bottom = AppSpacing.xxxl,
                    top = AppSpacing.sm
                ),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
            items(state.collections, key = { it.id }) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = AppShapes.medium,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCollectionClick(item.id) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(AppSpacing.md)
                    ) {
                        Icon(
                            imageVector = if (item.isSmartCollection) Icons.Default.AutoAwesome else Icons.Default.FolderSpecial,
                            contentDescription = null,
                            tint = if (item.isSmartCollection) AppSemanticColors.Accent else PrimaryIndigo,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(AppSpacing.md))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(item.name, style = AppTypography.title, color = TextPrimary)
                                if (item.isSmartCollection) {
                                    Spacer(modifier = Modifier.width(AppSpacing.xs))
                                    Text("SMART", color = AppSemanticColors.Accent, fontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                }
                            }
                            item.description?.let {
                                Text(it, style = AppTypography.caption, color = TextSecondary, maxLines = 1)
                            }
                            Text(
                                "${item.downloadedCount} / ${item.itemCount} items downloaded",
                                style = AppTypography.caption,
                                color = AppSemanticColors.Success
                            )
                        }
                    }
                }
            }
        }
    }
}
}


