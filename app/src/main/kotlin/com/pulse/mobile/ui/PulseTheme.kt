package com.pulse.mobile.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val PulseMint = Color(0xFF14F195)
val PulseViolet = Color(0xFF9945FF)
val PulseBackground = Color(0xFF080B16)
val PulseSurface = Color(0xFF14192B)
val PulseMuted = Color(0xFF9AA4BF)
val PulseOutline = Color(0xFF29314B)

@Composable
fun PulseTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(
        primary = PulseViolet, onPrimary = Color.White,
        primaryContainer = Color(0xFF2C1948), onPrimaryContainer = Color(0xFFE9D7FF),
        secondary = PulseMint, onSecondary = Color(0xFF03271A),
        secondaryContainer = Color(0xFF10342C), onSecondaryContainer = Color(0xFFC0FFE7),
        background = PulseBackground, onBackground = Color.White,
        surface = PulseSurface, onSurface = Color.White,
        surfaceVariant = Color(0xFF1C233A), onSurfaceVariant = PulseMuted,
        outline = PulseOutline, error = Color(0xFFFFA69F)
    ), typography = Typography(
        displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 64.sp, lineHeight = 70.sp, letterSpacing = (-2).sp),
        headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-1).sp),
        headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp),
        titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
        titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
        bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
        bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
        labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
        labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
        labelSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp)
    ), content = content)
}
