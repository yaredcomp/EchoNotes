package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Dark Palette Tokens (Google Stitch Ultra-Dark)
val StitchBackgroundDark = Color(0xFF0D0E15)
val StitchSurfaceDark = Color(0xFF171824)
val StitchSurfaceVariantDark = Color(0xFF1F2130)
val StitchCardOutlineDark = Color(0xFF292C3E)
val StitchOutlineVariantDark = Color(0xFF212332)
val StitchTextPrimaryDark = Color(0xFFFFFFFF)
val StitchTextSecondaryDark = Color(0xFF9EA1B5)
val StitchTextMutedDark = Color(0xFF686B80)

// Light Palette Tokens (Modern Stitch Clean Light)
val StitchBackgroundLight = Color(0xFFF6F8FD)
val StitchSurfaceLight = Color(0xFFFFFFFF)
val StitchSurfaceVariantLight = Color(0xFFEFF2F9)
val StitchCardOutlineLight = Color(0xFFE2E6F0)
val StitchOutlineVariantLight = Color(0xFFE8ECF5)
val StitchTextPrimaryLight = Color(0xFF121420)
val StitchTextSecondaryLight = Color(0xFF5B6077)
val StitchTextMutedLight = Color(0xFF868B9E)

// Dynamic Composable Color Accessors
val StitchBackground: Color
    @Composable get() = if (ThemeManager.isDarkTheme) StitchBackgroundDark else StitchBackgroundLight

val StitchSurface: Color
    @Composable get() = if (ThemeManager.isDarkTheme) StitchSurfaceDark else StitchSurfaceLight

val StitchSurfaceVariant: Color
    @Composable get() = if (ThemeManager.isDarkTheme) StitchSurfaceVariantDark else StitchSurfaceVariantLight

val StitchCardOutline: Color
    @Composable get() = if (ThemeManager.isDarkTheme) StitchCardOutlineDark else StitchCardOutlineLight

val StitchOutlineVariant: Color
    @Composable get() = if (ThemeManager.isDarkTheme) StitchOutlineVariantDark else StitchOutlineVariantLight

val StitchTextPrimary: Color
    @Composable get() = if (ThemeManager.isDarkTheme) StitchTextPrimaryDark else StitchTextPrimaryLight

val StitchTextSecondary: Color
    @Composable get() = if (ThemeManager.isDarkTheme) StitchTextSecondaryDark else StitchTextSecondaryLight

val StitchTextMuted: Color
    @Composable get() = if (ThemeManager.isDarkTheme) StitchTextMutedDark else StitchTextMutedLight

// Vibrant Brand Accents
val StitchPurple = Color(0xFF6332F6)
val StitchPurpleGradientEnd = Color(0xFF7E48FD)
val StitchPurpleContainer = Color(0xFF251A4D)
val StitchPurpleText = Color(0xFFB998FF)

val StitchCyan = Color(0xFF00E5FF)
val StitchCyanContainer = Color(0xFF0C2C38)
val StitchCyanText = Color(0xFF5CE7FF)

val StitchGreen = Color(0xFF00E676)
val StitchGreenContainer = Color(0xFF0C3322)

val StitchRed = Color(0xFFFF5252)
val StitchRedContainer = Color(0xFF33141E)

// Backward compatibility tokens
val PrimaryViolet = StitchPurple
val PrimaryVioletLight = StitchPurpleGradientEnd
val PrimaryContainerViolet = StitchPurpleContainer
val AccentSparkle = StitchPurpleText
val SecondaryTeal = StitchCyan
val WaveformActive = StitchCyan
val WaveformInactive = Color(0xFF2D3044)
