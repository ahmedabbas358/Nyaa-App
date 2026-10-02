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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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

/**
 * DeveloperModeScreen (Sections 90, 175).
 * Technical diagnostic dashboard for inspection of Provider state, Room caches,
 * parser benchmarks, and low-level logs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperModeScreen(
    onBack: () -> Unit = {},
    onResetCachesClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Developer Diagnostics", style = AppTypography.headline, color = TextPrimary) },
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
        LazyColumn(
            contentPadding = PaddingValues(
                start = AppSpacing.md,
                end = AppSpacing.md,
                bottom = AppSpacing.xxxl,
                top = AppSpacing.sm
            ),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 1. Provider Health Diagnostic
            item {
                DiagnosticCard(
                    title = "Provider Subsystem",
                    icon = Icons.Default.Code,
                    items = listOf(
                        "Active Provider: Nyaa.si (v2 Adapter)" to AppSemanticColors.Success,
                        "Latency: 284 ms" to TextSecondary,
                        "HTTP Status: 200 OK" to AppSemanticColors.Success,
                        "Parser Engine: Tokenizer & Multi-regex v2.4" to TextSecondary
                    )
                )
            }

            // 2. Database & Room Diagnostics
            item {
                DiagnosticCard(
                    title = "Room Database Index",
                    icon = Icons.Default.Storage,
                    items = listOf(
                        "Database Version: 1 (SQLite 3.42)" to TextSecondary,
                        "Library Items Indexed: 24" to TextSecondary,
                        "Library Files Indexed: 142" to TextSecondary,
                        "Provider Cache Entries: 68 (Valid TTL)" to TextSecondary
                    )
                )
            }

            // 3. Download Runtime Diagnostics
            item {
                DiagnosticCard(
                    title = "Download Runtime Engine",
                    icon = Icons.Default.BugReport,
                    items = listOf(
                        "Foreground Service: Active (UIDT Bound)" to AppSemanticColors.Success,
                        "HTTP Engine: OkHttp Segmented Stream (Max 8)" to TextSecondary,
                        "Torrent Swarm: DHT Ready" to TextSecondary,
                        "Active Locks: None" to TextSecondary
                    )
                )
            }

            // 4. Quick Actions
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onResetCachesClick,
                        colors = ButtonDefaults.buttonColors(containerColor = DarkCardBorder),
                        shape = AppShapes.medium,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear Cache", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {},
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        shape = AppShapes.medium,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Export Logs", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    items: List<Pair<String, androidx.compose.ui.graphics.Color>>
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = AppShapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(AppSpacing.xs))
                Text(title, style = AppTypography.title, color = TextPrimary)
            }
            Spacer(modifier = Modifier.height(AppSpacing.sm))
            items.forEach { (text, color) ->
                Text(
                    text = "• $text",
                    style = AppTypography.caption.copy(fontSize = 12.sp),
                    color = color,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}
