package com.vishalnursery.cashmemo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object NurseryColors {
    val Maroon = Color(0xFF8B1527)
    val MaroonDark = Color(0xFF5E0B19)
    val MaroonLight = Color(0xFFFDF2F4)
    val BotanicalGreen = Color(0xFF1B6B3A)
    val BotanicalGreenLight = Color(0xFFE6F4EA)
    val GoldAccent = Color(0xFFC98A16)
    val CreamBackground = Color(0xFFF8F5F0)
    val PaperSurface = Color(0xFFFFFDF9)
    val DueRed = Color(0xFFC62828)
    val DueRedContainer = Color(0xFFFFEBEE)
    val InkDark = Color(0xFF1F1A1C)
    val MutedText = Color(0xFF6B5F63)
    val OutlineSoft = Color(0xFFE2D9D0)
}

private val LightColorScheme = lightColorScheme(
    primary = NurseryColors.Maroon,
    onPrimary = Color.White,
    primaryContainer = NurseryColors.MaroonLight,
    onPrimaryContainer = NurseryColors.MaroonDark,
    secondary = NurseryColors.BotanicalGreen,
    onSecondary = Color.White,
    secondaryContainer = NurseryColors.BotanicalGreenLight,
    onSecondaryContainer = Color(0xFF0E4021),
    tertiary = NurseryColors.GoldAccent,
    onTertiary = Color.White,
    background = NurseryColors.CreamBackground,
    onBackground = NurseryColors.InkDark,
    surface = NurseryColors.PaperSurface,
    onSurface = NurseryColors.InkDark,
    surfaceVariant = Color(0xFFF1ECE4),
    onSurfaceVariant = NurseryColors.MutedText,
    outline = NurseryColors.OutlineSoft,
    error = NurseryColors.DueRed,
    errorContainer = NurseryColors.DueRedContainer
)

@Composable
fun CashmemoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
