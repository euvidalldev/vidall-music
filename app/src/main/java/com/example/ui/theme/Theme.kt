package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AmberPrimary,
    onPrimary = Color.Black,
    secondary = AmberDark,
    onSecondary = Color.Black,
    background = CharcoalDark,
    onBackground = TextWhite,
    surface = CharcoalCard,
    onSurface = TextWhite,
    surfaceVariant = CharcoalLight,
    onSurfaceVariant = TextGray,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
