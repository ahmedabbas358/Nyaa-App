package com.aniflow.feature.downloads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.sp
import com.aniflow.core.ui.theme.AppSpacing
import com.aniflow.core.ui.theme.AppTypography
import com.aniflow.core.ui.theme.DarkBackground
import com.aniflow.core.ui.theme.PrimaryIndigo
import com.aniflow.core.ui.theme.TextMuted
import com.aniflow.core.ui.theme.TextPrimary
import com.aniflow.feature.downloads.ui.DownloadDetailsScreen

/**
 * DownloadDetailScreen route entrypoint.
 * Retrieves real task from DownloadsViewModel by taskId.
 * No fake sample tasks or fabricated segment lists.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadDetailScreen(
    taskId: String,
    viewModel: DownloadsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val task = uiState.rawTasks.firstOrNull { it.id.value == taskId }

    if (task != null) {
        DownloadDetailsScreen(
            task = task,
            onBack = onBack,
            modifier = modifier
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Download Details", style = AppTypography.headline, color = TextPrimary) },
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
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(AppSpacing.xl)
                ) {
                    Text(
                        text = "Task Not Found",
                        style = AppTypography.headline.copy(fontSize = 18.sp),
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(AppSpacing.xs))
                    Text(
                        text = "The requested download task with ID '$taskId' does not exist or has been removed.",
                        style = AppTypography.body,
                        color = TextMuted
                    )
                    Spacer(Modifier.height(AppSpacing.md))
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                    ) {
                        Text("Return to Downloads")
                    }
                }
            }
        }
    }
}
