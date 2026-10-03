package com.aniflow.feature.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.aniflow.domain.profile.model.ProfileTemplateType

/**
 * OnboardingScreen (Sections 4, 5).
 * Lightweight, 6-step setup flow for first-time launch:
 * 1. Welcome
 * 2. Storage Location
 * 3. Preferred Language
 * 4. Default Profile
 * 5. Network / Download Policy
 * 6. Ready!
 * Users can skip advanced settings at any step.
 */
@Composable
fun OnboardingScreen(
    onFinishOnboarding: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 6

    var selectedStorage by remember { mutableStateOf("Internal Storage (/storage/emulated/0/AniFlow)") }
    var selectedLanguage by remember { mutableStateOf("English (LTR)") }
    var selectedProfile by remember { mutableStateOf(ProfileTemplateType.Balanced) }
    var wifiOnly by remember { mutableStateOf(true) }

    Scaffold(
        containerColor = DarkBackground,
        modifier = modifier
    ) { padding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(AppSpacing.lg)
        ) {
            // Header progress indicator
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Setup Step ${currentStep + 1} of $totalSteps",
                        style = AppTypography.caption,
                        color = TextMuted
                    )
                    if (currentStep < totalSteps - 1) {
                        TextButton(onClick = onFinishOnboarding) {
                            Text("Skip Setup", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(AppSpacing.xs))
                LinearProgressIndicator(
                    progress = { (currentStep + 1).toFloat() / totalSteps.toFloat() },
                    color = PrimaryIndigo,
                    trackColor = DarkSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                )
            }

            // Step Content
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "OnboardingStep"
                ) { step ->
                    when (step) {
                        0 -> StepWelcome()
                        1 -> StepStorage(selectedStorage) { selectedStorage = it }
                        2 -> StepLanguage(selectedLanguage) { selectedLanguage = it }
                        3 -> StepProfile(selectedProfile) { selectedProfile = it }
                        4 -> StepNetwork(wifiOnly) { wifiOnly = it }
                        5 -> StepReady()
                    }
                }
            }

            // Footer navigation buttons
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (currentStep > 0) {
                    TextButton(onClick = { currentStep-- }) {
                        Text("Back", color = TextSecondary)
                    }
                } else {
                    Spacer(modifier = Modifier.width(40.dp))
                }

                Button(
                    onClick = {
                        if (currentStep < totalSteps - 1) {
                            currentStep++
                        } else {
                            onFinishOnboarding()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    shape = AppShapes.pill,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(
                        text = if (currentStep == totalSteps - 1) "Get Started" else "Continue",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.xs))
                    Icon(
                        imageVector = if (currentStep == totalSteps - 1) Icons.Default.CheckCircle else Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StepWelcome() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(72.dp)
                .background(PrimaryIndigo.copy(alpha = 0.15f), shape = AppShapes.pill)
        ) {
            Icon(Icons.Default.Speed, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(36.dp))
        }
        Spacer(modifier = Modifier.height(AppSpacing.md))
        Text("Welcome to AniFlow", style = AppTypography.displayLarge, color = TextPrimary, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        Text(
            "Your high-performance anime discovery, intelligent selection, automated downloading, and playback platform.",
            style = AppTypography.body,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StepStorage(selected: String, onSelect: (String) -> Unit) {
    val options = listOf(
        "Internal Fast Storage (/AniFlow)",
        "Removable SD Card (/storage/sdcard1/AniFlow)",
        "Scoped SAF Custom Folder"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Folder, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        Text("Storage Location", style = AppTypography.headline, color = TextPrimary)
        Text("Choose where downloaded media will be stored and organized.", style = AppTypography.caption, color = TextMuted)
        Spacer(modifier = Modifier.height(AppSpacing.md))

        options.forEach { option ->
            val isSelected = selected == option
            SelectionOptionCard(title = option, isSelected = isSelected, onClick = { onSelect(option) })
            Spacer(modifier = Modifier.height(AppSpacing.xs))
        }
    }
}

@Composable
private fun StepLanguage(selected: String, onSelect: (String) -> Unit) {
    val languages = listOf("English (LTR)", "العربية (RTL)", "日本語 (Japanese)")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Language, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        Text("Preferred Language", style = AppTypography.headline, color = TextPrimary)
        Text("Interface language and default subtitle priority.", style = AppTypography.caption, color = TextMuted)
        Spacer(modifier = Modifier.height(AppSpacing.md))

        languages.forEach { lang ->
            SelectionOptionCard(title = lang, isSelected = selected == lang, onClick = { onSelect(lang) })
            Spacer(modifier = Modifier.height(AppSpacing.xs))
        }
    }
}

@Composable
private fun StepProfile(selected: ProfileTemplateType, onSelect: (ProfileTemplateType) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Settings, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        Text("Default Profile", style = AppTypography.headline, color = TextPrimary)
        Text("Sets quality targets, preferred codecs, and size limits.", style = AppTypography.caption, color = TextMuted)
        Spacer(modifier = Modifier.height(AppSpacing.md))

        ProfileTemplateType.values().take(4).forEach { template ->
            SelectionOptionCard(
                title = template.name,
                subtitle = when (template) {
                    ProfileTemplateType.Balanced -> "1080p HEVC, 500MB-2.5GB (Recommended)"
                    ProfileTemplateType.HighQuality -> "1080p/4K BluRay, Lossless Audio"
                    ProfileTemplateType.SmallSize -> "720p/Mini, < 700MB compact"
                    ProfileTemplateType.Archive -> "Full Season Batches, BluRay"
                    else -> ""
                },
                isSelected = selected == template,
                onClick = { onSelect(template) }
            )
            Spacer(modifier = Modifier.height(AppSpacing.xs))
        }
    }
}

@Composable
private fun StepNetwork(wifiOnly: Boolean, onToggle: (Boolean) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        Text("Download Policy", style = AppTypography.headline, color = TextPrimary)
        Text("Protect your mobile data plan and battery.", style = AppTypography.caption, color = TextMuted)
        Spacer(modifier = Modifier.height(AppSpacing.md))

        SelectionOptionCard(
            title = "Wi-Fi Only (Recommended)",
            subtitle = "Downloads will only start or resume on unmetered Wi-Fi connections.",
            isSelected = wifiOnly,
            onClick = { onToggle(true) }
        )
        Spacer(modifier = Modifier.height(AppSpacing.xs))
        SelectionOptionCard(
            title = "Any Network",
            subtitle = "Allow cellular downloads with metered warning prompts.",
            isSelected = !wifiOnly,
            onClick = { onToggle(false) }
        )
    }
}

@Composable
private fun StepReady() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AppSemanticColors.Success, modifier = Modifier.size(72.dp))
        Spacer(modifier = Modifier.height(AppSpacing.md))
        Text("You're All Set!", style = AppTypography.displayLarge, color = TextPrimary)
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        Text(
            "AniFlow is ready. You can discover anime, set up automated releases, or import existing files from your library.",
            style = AppTypography.body,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SelectionOptionCard(
    title: String,
    subtitle: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (isSelected) DarkSurface else DarkBackground),
        shape = AppShapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) PrimaryIndigo else DarkCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(AppSpacing.md)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .border(2.dp, if (isSelected) PrimaryIndigo else DarkCardBorder, shape = AppShapes.pill)
                    .background(if (isSelected) PrimaryIndigo else Color.Transparent, shape = AppShapes.pill)
            )
            Spacer(modifier = Modifier.width(AppSpacing.sm))
            Column {
                Text(title, style = AppTypography.subheadline, fontWeight = FontWeight.Bold, color = TextPrimary)
                subtitle?.let {
                    Text(it, style = AppTypography.caption, color = TextMuted)
                }
            }
        }
    }
}
