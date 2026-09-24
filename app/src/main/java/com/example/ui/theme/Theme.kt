package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val RaqeemDarkColorScheme = darkColorScheme(
    primary = AmberGold80,
    onPrimary = Color(0xFF241800),
    primaryContainer = AmberGoldContainerDark,
    onPrimaryContainer = Color(0xFFFFDF9E),
    secondary = Color(0xFFB4C5D6),
    onSecondary = Color(0xFF1E3140),
    secondaryContainer = Color(0xFF354857),
    onSecondaryContainer = Color(0xFFD0E1F2),
    tertiary = Color(0xFFC7B797),
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = Color(0xFF2C3440)
)

private val RaqeemLightColorScheme = lightColorScheme(
    primary = AmberGold40,
    onPrimary = Color.White,
    primaryContainer = AmberGoldContainerLight,
    onPrimaryContainer = Color(0xFF2A1C00),
    secondary = Color(0xFF4C5F70),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD0E1F2),
    onSecondaryContainer = Color(0xFF081C2B),
    tertiary = Color(0xFF675B3E),
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = Color(0xFFE2DCD1)
)

@Composable
fun RaqeemTheme(
    darkTheme: Boolean = true, // Default to true as per product specification
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) RaqeemDarkColorScheme else RaqeemLightColorScheme

    // Mandatory Arabic RTL by design
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

// For test backwards compatibility
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    RaqeemTheme(darkTheme = darkTheme, content = content)
}
