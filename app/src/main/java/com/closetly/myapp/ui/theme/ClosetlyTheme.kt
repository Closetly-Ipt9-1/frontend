package com.closetly.myapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

private val ClosetlyColorScheme = darkColorScheme(
    primary = Color(0xFFF1AA22),
    onPrimary = Color(0xFF172F38),
    primaryContainer = Color(0xFF3E5360),
    onPrimaryContainer = Color(0xFFFFD77D),
    secondary = Color(0xFF88B8B1),
    onSecondary = Color(0xFF102D32),
    secondaryContainer = Color(0xFF254F5D),
    onSecondaryContainer = Color(0xFFD4ECE8),
    tertiary = Color(0xFFB8C7D2),
    onTertiary = Color(0xFF17323D),
    tertiaryContainer = Color(0xFF1F4553),
    onTertiaryContainer = Color(0xFFE8F2F6),
    background = Color(0xFF183742),
    onBackground = Color(0xFFEFF5F3),
    surface = Color(0xFF244D5A),
    onSurface = Color(0xFFF3F7F4),
    surfaceVariant = Color(0xFF315B68),
    onSurfaceVariant = Color(0xFFC6D5D8),
    outline = Color(0xFF54717A),
    outlineVariant = Color(0xFF3C6570),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFF6D2B2B),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val ClosetlyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private val ClosetlyTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    )
)

@Composable
fun ClosetlyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ClosetlyColorScheme,
        typography = ClosetlyTypography,
        shapes = ClosetlyShapes,
        content = content
    )
}
