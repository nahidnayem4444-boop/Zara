package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = ZaraWhite,
    onPrimary = ZaraDarkBg,
    primaryContainer = ZaraCharcoal,
    onPrimaryContainer = ZaraWhite,
    secondary = ZaraMuted,
    onSecondary = ZaraWhite,
    background = ZaraDarkBg,
    onBackground = ZaraWhite,
    surface = ZaraDarkSurface,
    onSurface = ZaraWhite,
    outline = ZaraDarkBorder,
    error = ZaraAccentRed
)

private val LightColorScheme = lightColorScheme(
    primary = ZaraBlack,
    onPrimary = ZaraWhite,
    primaryContainer = ZaraSurfaceLight,
    onPrimaryContainer = ZaraBlack,
    secondary = ZaraSlate,
    onSecondary = ZaraWhite,
    background = ZaraWhite,
    onBackground = ZaraBlack,
    surface = ZaraWhite,
    onSurface = ZaraBlack,
    outline = ZaraBorder,
    error = ZaraAccentRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
