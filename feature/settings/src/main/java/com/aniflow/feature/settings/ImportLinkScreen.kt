package com.aniflow.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
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
 * ImportLinkScreen (Sections 127, 128, 163, 164).
 * Handles external share entries and manual paste for Magnet URIs, Torrent files, and Nyaa URLs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportLinkScreen(
    initialLink: String = "",
    onBack: () -> Unit = {},
    onAddToQueue: (link: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var linkInput by remember { mutableStateOf(initialLink) }

    val linkType = when {
        linkInput.startsWith("magnet:?", ignoreCase = true) -> "Magnet URI"
        linkInput.endsWith(".torrent", ignoreCase = true) -> "Torrent File URL"
        linkInput.contains("nyaa.si/view/", ignoreCase = true) -> "Nyaa Release URL"
        linkInput.isBlank() -> null
        else -> "Direct Media URL"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import Media Link", style = AppTypography.headline, color = TextPrimary) },
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
                .padding(AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            OutlinedTextField(
                value = linkInput,
                onValueChange = { linkInput = it },
                label = { Text("Paste Magnet URI, Torrent, or Nyaa link") },
                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = PrimaryIndigo) },
                placeholder = { Text("magnet:?xt=urn:btih:...", color = TextMuted) },
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryIndigo,
                    unfocusedBorderColor = DarkCardBorder,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = AppShapes.medium,
                modifier = Modifier.fillMaxWidth()
            )

            if (linkType != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = AppShapes.medium,
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(AppSpacing.md)) {
                        Text("Detected Format", style = AppTypography.caption, color = PrimaryIndigo)
                        Text(linkType, style = AppTypography.title, color = TextPrimary)
                        Spacer(modifier = Modifier.height(AppSpacing.xs))
                        Text("Destination: Anime/Downloads", style = AppTypography.bodySecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { onAddToQueue(linkInput) },
                enabled = linkInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryIndigo,
                    disabledContainerColor = DarkCardBorder
                ),
                shape = AppShapes.pill,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add to Download Queue")
            }
        }
    }
}
