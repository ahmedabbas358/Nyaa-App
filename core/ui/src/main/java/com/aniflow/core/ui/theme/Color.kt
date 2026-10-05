package com.aniflow.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * AniFlow Color Palette v2 — Warm, Human-crafted Design.
 *
 * Design philosophy:
 * - Warm amber-gold accent paired with cool slate backgrounds
 * - Multi-tiered dark surfaces with subtle blue undertones (not pure black)
 * - Organic feel: warmer neutrals, softer contrasts, no harsh neon
 * - Light theme with cream/ivory warmth instead of clinical white
 */

// Primary Accent — Warm Amber-Gold (distinctive, not generic)
val AccentBlueViolet = Color(0xFFD4A857)        // Warm amber-gold primary
val AccentBlueVioletHover = Color(0xFFC49A4B)    // Hover: slightly deeper
val AccentBlueVioletSubtle = Color(0xFF2A2419)   // Subtle dark tint
val AccentBlueVioletLight = Color(0xFFBF8F3A)    // Light theme primary
val AccentBlueVioletSubtleLight = Color(0xFFFBF5E9) // Light subtle bg

// Legacy alias compatibility so other screens compile seamlessly
val PrimaryIndigo = AccentBlueViolet
val PrimaryIndigoVariant = Color(0xFFC49A4B)
val SecondaryTeal = Color(0xFF5BA89D)            // Muted sage-teal
val AccentCyan = Color(0xFF6BA3BE)               // Dusty sky blue

// Dark Theme Surfaces — warm undertones, no pure black
val DarkBackground = Color(0xFF111318)           // Very dark slate with warmth
val DarkSurface = Color(0xFF181B22)              // Base card surface
val DarkSurfaceElevated = Color(0xFF1F232D)      // Elevated cards/sheets
val DarkSurfaceVariant = Color(0xFF262B38)       // Chips, inset areas
val DarkCardBorder = Color(0xFF2E3444)           // Structural divider
val DarkCardBorderSubtle = Color(0xFF232834)

// Light Theme Surfaces — warm ivory, not clinical white
val LightBackground = Color(0xFFF8F6F2)          // Warm off-white
val LightSurface = Color(0xFFFFFEFB)             // Cream white
val LightSurfaceElevated = Color(0xFFFFFEFB)
val LightSurfaceVariant = Color(0xFFF0ECE5)      // Warm light grey
val LightCardBorder = Color(0xFFE2DDD4)          // Warm border
val LightCardBorderSubtle = Color(0xFFEBE7E0)

// Status Colors — slightly desaturated for elegance
val SuccessGreen = Color(0xFF3DA87A)             // Sage green
val WarningAmber = Color(0xFFE8A84C)             // Warm amber
val ErrorRose = Color(0xFFD4574B)                // Brick red (not neon)
val InfoBlue = Color(0xFF5B9FCA)                 // Dusty blue

// Quality Badges — muted, cohesive palette
val Badge1080p = Color(0xFFD4A857)               // Gold (matches primary)
val Badge720p = Color(0xFF6B8EC4)                // Steel blue
val Badge4K = Color(0xFF9B7AC4)                  // Dusty purple
val BadgeHevc = Color(0xFF4D9E7D)                // Muted emerald
val BadgeAv1 = Color(0xFF5B9FCA)                 // Dusty sky
val BadgeTrusted = Color(0xFF4D9E7D)             // Same as HEVC for coherence
val BadgeRemake = Color(0xFFCB7644)              // Terracotta

// Text Colors - Dark — warmer, not blue-grey
val TextPrimary = Color(0xFFF0EBE3)              // Warm off-white
val TextSecondary = Color(0xFF9E978C)            // Warm grey
val TextMuted = Color(0xFF6B6560)                // Muted warm grey

// Text Colors - Light — deep ink, not pure black
val TextPrimaryLight = Color(0xFF1A1714)         // Deep warm black
val TextSecondaryLight = Color(0xFF56504A)        // Warm charcoal
val TextMutedLight = Color(0xFF8A8279)           // Muted warm

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
