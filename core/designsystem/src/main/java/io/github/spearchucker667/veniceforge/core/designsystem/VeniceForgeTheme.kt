package io.github.spearchucker667.veniceforge.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF6A1A),
    secondary = Color(0xFFFFB38A),
    background = Color(0xFF080808),
    surface = Color(0xFF111111),
    surfaceVariant = Color(0xFF1A1A1A),
    onPrimary = Color.Black,
    onBackground = Color(0xFFF2F0EA),
    onSurface = Color(0xFFF2F0EA),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFFB83F00),
    secondary = Color(0xFF7C2D00),
    background = Color(0xFFFFFBF8),
    surface = Color.White,
)

@Composable
fun VeniceForgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
