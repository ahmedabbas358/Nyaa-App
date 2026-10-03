package com.aniflow.core.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Spacing design tokens (Section 7, Section 11, Section 16).
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
 * Cohesive radius scale. Restrained radii for technical layouts, rounded for media.
 */
object AppShapes {
    val small = RoundedCornerShape(6.dp)
    val medium = RoundedCornerShape(10.dp)
    val large = RoundedCornerShape(14.dp)
    val extraLarge = RoundedCornerShape(20.dp)
    val pill = RoundedCornerShape(999.dp)
    val card = medium
    val badge = small
    val roundedMedium = medium
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
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
 * Semantic status colors (Section 5, Section 124).
 */
object AppSemanticColors {
    val Success = SuccessGreen
    val Warning = WarningAmber
    val Error = ErrorRose
    val Info = InfoBlue
    val Neutral = Color(0xFF64748B)   // Queued / idle / paused
    val Accent = AccentBlueViolet     // Primary interactive
    val Secondary = SecondaryTeal    // Auxiliary interactive

    // Lower-case aliases for component compatibility
    val success = Success
    val warning = Warning
    val error = Error
    val info = Info
    val neutral = Neutral
    val accent = Accent
    val secondary = Secondary
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
    val Display = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.75).sp
    )

    val LargeTitle = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.4).sp
    )

    val Title = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.2).sp
    )

    val SectionTitle = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.1).sp
    )

    val Subtitle = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 19.sp
    )

    val Body = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )

    val BodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )

    val Caption = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )

    val Metadata = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.2.sp
    )

    val Overline = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        letterSpacing = 0.8.sp
    )

    // Aliases for legacy component compatibility
    @get:JvmName("getDisplayLargeAlias")
    val displayLarge = Display
    @get:JvmName("getHeadlineAlias")
    val headline = LargeTitle
    @get:JvmName("getTitleAlias")
    val title = Title
    @get:JvmName("getTitleMediumAlias")
    val titleMedium = SectionTitle
    @get:JvmName("getSubheadlineAlias")
    val subheadline = Subtitle
    @get:JvmName("getBodyAlias")
    val body = Body
    @get:JvmName("getBodySmallAlias")
    val bodySmall = BodySmall
    @get:JvmName("getBodySecondaryAlias")
    val bodySecondary = BodySmall
    @get:JvmName("getLabelAlias")
    val label = Subtitle
    @get:JvmName("getCaptionAlias")
    val caption = Caption
    @get:JvmName("getHeadingMediumAlias")
    val headingMedium = LargeTitle
    @get:JvmName("getHeadlineMediumAlias")
    val headlineMedium = LargeTitle
    @get:JvmName("getBodyMediumAlias")
    val bodyMedium = Body
    @get:JvmName("getLabelLargeAlias")
    val labelLarge = SectionTitle
    @get:JvmName("getLabelSmallAlias")
    val labelSmall = Caption
    @get:JvmName("getSubtitleAlias")
    val subtitle = Subtitle

    // Dedicated numeric styles for speeds, ETAs, and file sizes (Section 6, 15)
    val numericSpeed = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        letterSpacing = 0.3.sp
    )

    val numericEta = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        letterSpacing = 0.2.sp
    )

    val numericSize = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    )

    val numericProgress = numericSpeed
}

/**
 * Motion & Animation Tokens (Section 115).
 * Centralized motion specifications to ensure calm, predictable transitions without bouncing.
 */
object AppMotion {
    const val DurationShort = 150
    const val DurationStandard = 250
    const val DurationEmphasized = 400

    val EasingStandard: Easing = FastOutSlowInEasing
    val EasingEmphasized: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val EasingDecelerate: Easing = LinearOutSlowInEasing

    fun <T> shortTween(delayMillis: Int = 0) = tween<T>(
        durationMillis = DurationShort,
        delayMillis = delayMillis,
        easing = EasingStandard
    )

    fun <T> standardTween(delayMillis: Int = 0) = tween<T>(
        durationMillis = DurationStandard,
        delayMillis = delayMillis,
        easing = EasingStandard
    )

    fun <T> emphasizedTween(delayMillis: Int = 0) = tween<T>(
        durationMillis = DurationEmphasized,
        delayMillis = delayMillis,
        easing = EasingEmphasized
    )
}
