package io.github.spearchucker667.veniceforge.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Venice Fyr theme providing the official Venice.ai color schemes, typography, and shape hierarchy.
 */
@Composable
fun VeniceForgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) {
        VeniceColors.DarkColorScheme
    } else {
        VeniceColors.LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VeniceTypography.Typography,
        content = content,
    )
}
