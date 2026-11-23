package com.automatelinux.carShare.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF0F172A)
val Muted = Color(0xFF64748B)
val Page = Color(0xFFF1F5F9)
val Track = Color(0xFFE2E8F0)
val Teal = Color(0xFF0D9488)
val TealSoft = Color(0xFFCCFBF1)
val Orange = Color(0xFFEA580C)
val OrangeSoft = Color(0xFFFFEDD5)

private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = Color.White,
    secondaryContainer = Track,
    onSecondaryContainer = Ink,
    background = Page,
    surface = Color.White,
    onSurface = Ink,
)

// Multiplatform theme (no Android-only dynamic color, so it compiles for iOS too).
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, content = content)
}
