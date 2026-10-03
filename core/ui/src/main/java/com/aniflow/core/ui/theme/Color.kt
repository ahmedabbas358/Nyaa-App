package com.aniflow.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * AniFlow Refined Palette (Section 5: Visual Identity).
 *
 * Restrained, content-first aesthetic:
 * - Single subdued deep blue-violet accent used deliberately.
 * - Multi-tiered dark surface hierarchy (no pure black OLED smearing).
 * - Clear, high-contrast light surface hierarchy.
 * - Semantic status colors tailored for high readability.
 */

// Accent - Deep Blue-Violet (Restrained, Editorial)
val AccentBlueViolet = Color(0xFF505CB8)
val AccentBlueVioletHover = Color(0xFF4551A6)
val AccentBlueVioletSubtle = Color(0xFF1E2342)
val AccentBlueVioletLight = Color(0xFF4854B0)
val AccentBlueVioletSubtleLight = Color(0xFFEEF0FA)

// Legacy alias compatibility so other screens compile seamlessly
val PrimaryIndigo = AccentBlueViolet
val PrimaryIndigoVariant = Color(0xFF424DA1)
val SecondaryTeal = Color(0xFF0D9488)
val AccentCyan = Color(0xFF0284C7)

// Dark Theme Surfaces
val DarkBackground = Color(0xFF0B0F17)      // Deep charcoal/obsidian
val DarkSurface = Color(0xFF121824)         // Base elevated surface
val DarkSurfaceElevated = Color(0xFF1A2234) // Cards, sheets, dialogs
val DarkSurfaceVariant = Color(0xFF232D42)  // Chips, inset wells
val DarkCardBorder = Color(0xFF28344B)      // Clean structural divider (1dp)
val DarkCardBorderSubtle = Color(0xFF1D2637)

// Light Theme Surfaces
val LightBackground = Color(0xFFF7F8FA)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceElevated = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEEF1F6)
val LightCardBorder = Color(0xFFDCE1EA)
val LightCardBorderSubtle = Color(0xFFE9ECF2)

// Status Colors (Subtle & High Contrast)
val SuccessGreen = Color(0xFF10B981)        // Emerald 500
val WarningAmber = Color(0xFFF59E0B)        // Amber 500
val ErrorRose = Color(0xFFEF4444)           // Red 500
val InfoBlue = Color(0xFF38BDF8)            // Sky 400

// Quality Badges
val Badge1080p = Color(0xFF505CB8)
val Badge720p = Color(0xFF2563EB)
val Badge4K = Color(0xFF7C3AED)
val BadgeHevc = Color(0xFF059669)
val BadgeAv1 = Color(0xFF0284C7)
val BadgeTrusted = Color(0xFF16A34A)
val BadgeRemake = Color(0xFFEA580C)

// Text Colors - Dark
val TextPrimary = Color(0xFFF1F5F9)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

// Text Colors - Light
val TextPrimaryLight = Color(0xFF0F172A)
val TextSecondaryLight = Color(0xFF475569)
val TextMutedLight = Color(0xFF8290A4)

/**
 * Immutable Color Scheme for AniFlow's Design System.
 */
@Immutable
data class AniFlowColorScheme(
    val primary: Color,
    val primaryVariant: Color,
    val primarySubtle: Color,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceVariant: Color,
    val border: Color,
    val borderSubtle: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val success: Color = SuccessGreen,
    val warning: Color = WarningAmber,
    val error: Color = ErrorRose,
    val info: Color = InfoBlue,
    val isDark: Boolean = true
)

val DarkAniFlowColorScheme = AniFlowColorScheme(
    primary = AccentBlueViolet,
    primaryVariant = PrimaryIndigoVariant,
    primarySubtle = AccentBlueVioletSubtle,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceElevated = DarkSurfaceElevated,
    surfaceVariant = DarkSurfaceVariant,
    border = DarkCardBorder,
    borderSubtle = DarkCardBorderSubtle,
    textPrimary = TextPrimary,
    textSecondary = TextSecondary,
    textMuted = TextMuted,
    isDark = true
)

val LightAniFlowColorScheme = AniFlowColorScheme(
    primary = AccentBlueVioletLight,
    primaryVariant = PrimaryIndigoVariant,
    primarySubtle = AccentBlueVioletSubtleLight,
    background = LightBackground,
    surface = LightSurface,
    surfaceElevated = LightSurfaceElevated,
    surfaceVariant = LightSurfaceVariant,
    border = LightCardBorder,
    borderSubtle = LightCardBorderSubtle,
    textPrimary = TextPrimaryLight,
    textSecondary = TextSecondaryLight,
    textMuted = TextMutedLight,
    isDark = false
)
