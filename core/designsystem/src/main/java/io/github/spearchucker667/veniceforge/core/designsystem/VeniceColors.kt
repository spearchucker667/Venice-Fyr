package io.github.spearchucker667.veniceforge.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Official Venice.ai brand tokens and foundations as specified in the Venice Brand Kit (DESIGN.md).
 */
object VeniceColors {
    // Primary Accents (used sparingly for CTAs, active states, and focus indicators; never backgrounds or logo ink)
    val VenetianBlueLight = Color(0xFF3C8FDD)
    val VenetianBlueDark = Color(0xFF125DA3)

    // Foundations
    val DeepBlue = Color(0xFF0E2942)       // Primary dark ink/text/logo on light surfaces
    val MidnightBlue = Color(0xFF0A121A)   // Primary dark-mode background
    val OffWhite = Color(0xFFF7F5ED)       // Primary light-mode background & light ink/logo on dark surfaces

    // Secondary Accents
    val SeaLight = Color(0xFFB3D0EB)
    val MidSea = Color(0xFF29526C)
    val SeaDark = Color(0xFF080F16)
    val StuccoLight = Color(0xFF6E6B5F)
    val StuccoMid = Color(0xFFBEA989)
    val StuccoDark = Color(0xFF1C1714)
    val NeutralLight = Color(0xFFFFFEFA)
    val NeutralGray = Color(0xFF6E7176)
    val NeutralDark = Color(0xFF151F28)

    // Semantic Status Colors (accessible & distinct from brand accents)
    val Success = Color(0xFF2E7D32)
    val Error = Color(0xFFD32F2F)
    val Warning = Color(0xFFED6C02)
    val Info = VenetianBlueLight

    /**
     * Accessible Venice dark color scheme based on Midnight Blue and Neutral Dark foundations.
     */
    val DarkColorScheme: ColorScheme = darkColorScheme(
        primary = VenetianBlueDark,
        onPrimary = OffWhite,
        primaryContainer = MidSea,
        onPrimaryContainer = SeaLight,
        secondary = MidSea,
        onSecondary = OffWhite,
        secondaryContainer = NeutralDark,
        onSecondaryContainer = SeaLight,
        tertiary = SeaLight,
        onTertiary = MidnightBlue,
        background = MidnightBlue,
        onBackground = OffWhite,
        surface = NeutralDark,
        onSurface = OffWhite,
        surfaceVariant = MidnightBlue,
        onSurfaceVariant = SeaLight,
        outline = NeutralGray,
        outlineVariant = DeepBlue,
        error = Error,
        onError = OffWhite,
        errorContainer = Color(0xFF5C0000),
        onErrorContainer = Color(0xFFFFDAD6),
    )

    /**
     * Accessible Venice light color scheme based on Off White and Neutral Light foundations.
     */
    val LightColorScheme: ColorScheme = lightColorScheme(
        primary = VenetianBlueLight,
        onPrimary = MidnightBlue, // High contrast text on light Venetian Blue control surfaces
        primaryContainer = SeaLight,
        onPrimaryContainer = DeepBlue,
        secondary = MidSea,
        onSecondary = OffWhite,
        secondaryContainer = OffWhite,
        onSecondaryContainer = DeepBlue,
        tertiary = MidSea,
        onTertiary = OffWhite,
        background = OffWhite,
        onBackground = DeepBlue,
        surface = NeutralLight,
        onSurface = DeepBlue,
        surfaceVariant = OffWhite,
        onSurfaceVariant = StuccoLight,
        outline = NeutralGray,
        outlineVariant = StuccoLight,
        error = Error,
        onError = NeutralLight,
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
    )
}
