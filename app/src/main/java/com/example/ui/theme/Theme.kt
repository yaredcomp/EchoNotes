package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val StitchDarkColorScheme = darkColorScheme(
    primary = StitchPurple,
    onPrimary = Color.White,
    primaryContainer = StitchPurpleContainer,
    onPrimaryContainer = StitchPurpleText,
    secondary = StitchCyan,
    onSecondary = Color.Black,
    secondaryContainer = StitchCyanContainer,
    onSecondaryContainer = StitchCyanText,
    tertiary = StitchPurpleGradientEnd,
    onTertiary = Color.White,
    background = StitchBackgroundDark,
    onBackground = StitchTextPrimaryDark,
    surface = StitchSurfaceDark,
    onSurface = StitchTextPrimaryDark,
    surfaceVariant = StitchSurfaceVariantDark,
    onSurfaceVariant = StitchTextSecondaryDark,
    outline = StitchCardOutlineDark,
    outlineVariant = StitchOutlineVariantDark
)

private val StitchLightColorScheme = lightColorScheme(
    primary = StitchPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFECE6FE),
    onPrimaryContainer = StitchPurple,
    secondary = Color(0xFF007A8A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6F8FF),
    onSecondaryContainer = Color(0xFF004953),
    tertiary = StitchPurpleGradientEnd,
    onTertiary = Color.White,
    background = StitchBackgroundLight,
    onBackground = StitchTextPrimaryLight,
    surface = StitchSurfaceLight,
    onSurface = StitchTextPrimaryLight,
    surfaceVariant = StitchSurfaceVariantLight,
    onSurfaceVariant = StitchTextSecondaryLight,
    outline = StitchCardOutlineLight,
    outlineVariant = StitchOutlineVariantLight
)

@Composable
fun EchoNotesTheme(
    darkTheme: Boolean = ThemeManager.isDarkTheme,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) StitchDarkColorScheme else StitchLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = ThemeManager.isDarkTheme,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    EchoNotesTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
