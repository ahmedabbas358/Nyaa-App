package com.aniflow.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class ThemeMode {
    System,
    Light,
    Dark
}

private val LocalAniFlowColorScheme = staticCompositionLocalOf { DarkAniFlowColorScheme }

private val MaterialDarkColorScheme = darkColorScheme(
    primary = AccentBlueViolet,
    onPrimary = Color.White,
    primaryContainer = PrimaryIndigoVariant,
    onPrimaryContainer = Color.White,
    secondary = SecondaryTeal,
    onSecondary = Color.Black,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkCardBorder,
    error = ErrorRose,
    onError = Color.White
)

private val MaterialLightColorScheme = lightColorScheme(
    primary = AccentBlueVioletLight,
    onPrimary = Color.White,
    primaryContainer = PrimaryIndigoVariant,
    onPrimaryContainer = Color.White,
    secondary = SecondaryTeal,
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = LightCardBorder,
    error = ErrorRose,
    onError = Color.White
)

/**
 * Central theme provider for AniFlow.
 * Supports System, Light, and Dark modes with custom AniFlowTokens & Material 3 color mapping.
 */
@Composable
fun AniFlowTheme(
    themeMode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Dark -> true
        ThemeMode.Light -> false
    }

    val aniColors = if (isDark) DarkAniFlowColorScheme else LightAniFlowColorScheme
    val materialColors = if (isDark) MaterialDarkColorScheme else MaterialLightColorScheme

    CompositionLocalProvider(
        LocalAniFlowColorScheme provides aniColors
    ) {
        MaterialTheme(
            colorScheme = materialColors,
            content = content
        )
    }
}

@Composable
fun AniFlowTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    AniFlowTheme(
        themeMode = if (darkTheme) ThemeMode.Dark else ThemeMode.Light,
        content = content
    )
}

/**
 * Global accessor for AniFlow design system tokens.
 */
object AniFlowTheme {
    val colors: AniFlowColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalAniFlowColorScheme.current

    val typography: AppTypography
        get() = AppTypography

    val spacing: AppSpacing
        get() = AppSpacing

    val shapes: AppShapes
        get() = AppShapes

    val elevation: AppElevation
        get() = AppElevation

    val motion: AppMotion
        get() = AppMotion
}
