package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CustomDarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    secondary = CyberGreen,
    tertiary = CyberOrange,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = LightText,
    onSecondary = LightText,
    onTertiary = LightText,
    onBackground = LightText,
    onSurface = LightText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = LightText,
    error = CyberRed,
    onError = LightText
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CustomDarkColorScheme,
        typography = Typography,
        content = content
    )
}
