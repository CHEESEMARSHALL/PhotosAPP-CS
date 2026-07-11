package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFC107),       // Glowing Amber Gold for sync actions and highlights
    secondary = Color(0xFF2196F3),     // Electric Blue for connection indicator and download cues
    tertiary = Color(0xFF4CAF50),      // Grass Green for fully cached and synced states
    background = Color(0xFF0A0C10),    // Ultimate Obsidian dark background
    surface = Color(0xFF12161F),       // Layered carbon grey for cards
    onPrimary = Color(0xFF0F0F14),
    onSecondary = Color(0xFFFFFFFF),
    onBackground = Color(0xFFE2E8F0),  // Crisp slate white text
    onSurface = Color(0xFFE2E8F0)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFF57C00),       // Warm safety orange
    secondary = Color(0xFF1976D2),
    tertiary = Color(0xFF388E3C),
    background = Color(0xFFF8FAFC),    // Crisp off-white
    surface = Color(0xFFFFFFFF),
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force secure dark theme as preferred standard!
    dynamicColor: Boolean = false, // Use our gorgeous custom premium styles exclusively
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
