package com.aniflow.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Spacing design tokens (Section 11, 16).
 * Standardized 4dp base grid tokens.
 */
object AppSpacing {
    val xxs: Dp = 4.dp
    val xs: Dp = 8.dp
    val sm: Dp = 12.dp
    val md: Dp = 16.dp
    val lg: Dp = 20.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp
    val xxxl: Dp = 40.dp
    val huge: Dp = 48.dp
    val massive: Dp = 64.dp

    // Exact scale from Section 7
    val s4: Dp = 4.dp
    val s8: Dp = 8.dp
    val s12: Dp = 12.dp
    val s16: Dp = 16.dp
    val s20: Dp = 20.dp
    val s24: Dp = 24.dp
    val s32: Dp = 32.dp
    val s40: Dp = 40.dp
    val s48: Dp = 48.dp
    val s64: Dp = 64.dp
}

/**
 * Shape tokens (Section 8).
 * Cohesive radius scale.
 */
object AppShapes {
    val small = RoundedCornerShape(6.dp)
    val medium = RoundedCornerShape(10.dp)
    val large = RoundedCornerShape(16.dp)
    val extraLarge = RoundedCornerShape(24.dp)
    val pill = RoundedCornerShape(999.dp)
}

/**
 * Elevation tokens.
 */
object AppElevation {
    val none: Dp = 0.dp
    val card: Dp = 1.dp
    val sheet: Dp = 8.dp
    val dialog: Dp = 16.dp
}

/**
 * Semantic status colors (Section 5, 124).
 */
object AppSemanticColors {
    val Success = Color(0xFF10B981)   // Completed / downloaded
    val Warning = Color(0xFFF59E0B)   // Low storage / fallback / warning
    val Error = Color(0xFFF43F5E)     // Failed / critical error
    val Info = Color(0xFF38BDF8)      // Active download / stream
    val Neutral = Color(0xFF64748B)   // Queued / idle / paused
    val Accent = Color(0xFF6366F1)    // Primary interactive
    val Secondary = Color(0xFF14B8A6) // Auxiliary interactive
}

/**
 * AppTypography hierarchy (Section 6).
 * Required hierarchy:
 * - Display
 * - Large Title
 * - Title
 * - Section Title
 * - Subtitle
 * - Body
 * - Body Small
 * - Caption
 * - Metadata
 * - Overline
 */
object AppTypography {
    // Official hierarchy (Section 6)
    val Display = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.75).sp
    )

    val LargeTitle = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.4).sp
    )

    val Title = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.2).sp
    )

    val SectionTitle = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.1).sp
    )

    val Subtitle = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )

    val Body = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )

    val BodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = TextSecondary
    )

    val Caption = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        color = TextMuted
    )

    val Metadata = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.3.sp
    )

    val Overline = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        letterSpacing = 1.0.sp
    )

    // Aliases and components compatibility
    val displayLarge = Display
    val headline = LargeTitle
    val title = Title
    val titleMedium = SectionTitle
    val subheadline = Subtitle
    val body = Body
    val bodySmall = BodySmall
    val bodySecondary = BodySmall
    val label = Subtitle
    val caption = Caption

    // Dedicated numeric styles for speeds, ETAs, and file sizes (Section 6, 15)
    val numericSpeed = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        letterSpacing = 0.5.sp
    )

    val numericEta = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.3.sp
    )

    val numericSize = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp
    )
}
