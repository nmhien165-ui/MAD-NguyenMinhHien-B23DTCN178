package com.example.btl.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF5352B8), onPrimary = Color.White,
    primaryContainer = Color(0xFFE9E7FF), onPrimaryContainer = Color(0xFF302C73),
    secondary = Color(0xFF256D60), secondaryContainer = Color(0xFFD6F1E8),
    onSecondaryContainer = Color(0xFF154F43),
    background = Color(0xFFF7F8FC), surface = Color(0xFFFCFCFF),
    onBackground = Color(0xFF20253D), onSurface = Color(0xFF20253D),
    surfaceVariant = Color(0xFFEEEDF5), onSurfaceVariant = Color(0xFF626579),
    outline = Color(0xFF858699), outlineVariant = Color(0xFFDEDFEA)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC5C0FF), primaryContainer = Color(0xFF373178),
    secondary = Color(0xFF9FD5C5), secondaryContainer = Color(0xFF174F43),
    background = Color(0xFF131522), surface = Color(0xFF1A1D2E),
    surfaceVariant = Color(0xFF313345), onSurfaceVariant = Color(0xFFC6C5D7)
)

@Composable
fun BTLTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography, content = content)
}
