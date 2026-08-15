package io.github.spearchucker667.veniceforge.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Venice typography hierarchy based on official brand guidance.
 *
 * Official reference typefaces:
 * - Display / Headline: Canela
 * - Supporting Sans: Aeonik
 * - Technical / Mono: Aeonik Fono
 *
 * Font Binary Status:
 * Commercial font binaries are not bundled without explicit licensing.
 * The hierarchy below uses standard system font families (Serif, SansSerif, Monospace)
 * configured with the brand's exact scale, weight, and line-height proportions.
 */
object VeniceTypography {
    val DisplayFontFamily = FontFamily.Serif       // Target: Canela
    val SansFontFamily = FontFamily.SansSerif      // Target: Aeonik
    val MonoFontFamily = FontFamily.Monospace      // Target: Aeonik Fono

    val Typography = Typography(
        displayLarge = TextStyle(
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 56.sp,
            lineHeight = 60.sp,
            letterSpacing = (-0.5).sp,
        ),
        displayMedium = TextStyle(
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 44.sp,
            lineHeight = 48.sp,
            letterSpacing = (-0.5).sp,
        ),
        displaySmall = TextStyle(
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 36.sp,
            lineHeight = 40.sp,
        ),
        headlineLarge = TextStyle(
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 32.sp,
            lineHeight = 36.sp,
        ),
        headlineMedium = TextStyle(
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            lineHeight = 32.sp,
        ),
        headlineSmall = TextStyle(
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 28.sp,
        ),
        titleLarge = TextStyle(
            fontFamily = SansFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp,
            lineHeight = 26.sp,
        ),
        titleMedium = TextStyle(
            fontFamily = SansFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.15.sp,
        ),
        titleSmall = TextStyle(
            fontFamily = SansFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
        ),
        bodyLarge = TextStyle(
            fontFamily = SansFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = SansFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp,
        ),
        bodySmall = TextStyle(
            fontFamily = SansFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp,
        ),
        labelLarge = TextStyle(
            fontFamily = SansFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
        ),
        labelMedium = TextStyle(
            fontFamily = MonoFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp,
        ),
        labelSmall = TextStyle(
            fontFamily = MonoFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.5.sp,
        ),
    )
}
