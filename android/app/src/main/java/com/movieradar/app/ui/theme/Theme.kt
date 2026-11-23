package com.movieradar.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Pure-black OLED palette with a single red accent (Netflix-like) and X-like greys. */
object RadarColors {
    val Black = Color(0xFF000000)
    val Surface = Color(0xFF0E0E12)
    val SurfaceHigh = Color(0xFF1A1A20)
    val Outline = Color(0xFF2F3336)
    val Accent = Color(0xFFE50914)
    val TextPrimary = Color(0xFFE7E9EA)
    val TextSecondary = Color(0xFF8B98A5)
    val Free = Color(0xFF2ECC71)
    val Gold = Color(0xFFF5C518)
}

private val Scheme = darkColorScheme(
    primary = RadarColors.Accent,
    onPrimary = Color.White,
    secondary = RadarColors.TextSecondary,
    background = RadarColors.Black,
    onBackground = RadarColors.TextPrimary,
    surface = RadarColors.Black,
    onSurface = RadarColors.TextPrimary,
    surfaceVariant = RadarColors.SurfaceHigh,
    onSurfaceVariant = RadarColors.TextSecondary,
    surfaceContainer = RadarColors.Surface,
    surfaceContainerHigh = RadarColors.SurfaceHigh,
    outline = RadarColors.Outline,
    outlineVariant = RadarColors.Outline,
    error = Color(0xFFFF6B6B),
)

private val RadarType = Typography(
    displaySmall = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
)

@Composable
fun MovieRadarTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = RadarType, content = content)
}
