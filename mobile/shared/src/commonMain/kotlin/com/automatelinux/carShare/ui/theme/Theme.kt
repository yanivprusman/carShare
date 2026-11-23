package com.automatelinux.carShare.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

// Night-road palette: navy surfaces, one colour per company, white for what you read.
val Page = Color(0xFF0B1222)
val Card = Color(0xFF131C2E)
val Raised = Color(0xFF1B2640)
val Line = Color(0xFF26324A)
val Ink = Color(0xFFF1F5F9)
val Muted = Color(0xFF94A3B8)
val Faint = Color(0xFF64748B)

val Teal = Color(0xFF2DD4BF)
val TealDeep = Color(0xFF0D9488)
val Orange = Color(0xFFFB923C)
val OrangeDeep = Color(0xFFEA580C)

private val Colors = darkColorScheme(
    primary = Ink,
    onPrimary = Page,
    background = Page,
    surface = Card,
    onSurface = Ink,
    onBackground = Ink,
)

@Composable
fun AppTheme(font: FontFamily, content: @Composable () -> Unit) {
    val base = Typography()
    val t = Typography(
        displayLarge = base.displayLarge.copy(fontFamily = font),
        displayMedium = base.displayMedium.copy(fontFamily = font),
        displaySmall = base.displaySmall.copy(fontFamily = font),
        headlineLarge = base.headlineLarge.copy(fontFamily = font),
        headlineMedium = base.headlineMedium.copy(fontFamily = font),
        headlineSmall = base.headlineSmall.copy(fontFamily = font),
        titleLarge = base.titleLarge.copy(fontFamily = font),
        titleMedium = base.titleMedium.copy(fontFamily = font),
        titleSmall = base.titleSmall.copy(fontFamily = font),
        bodyLarge = base.bodyLarge.copy(fontFamily = font),
        bodyMedium = base.bodyMedium.copy(fontFamily = font),
        bodySmall = base.bodySmall.copy(fontFamily = font),
        labelLarge = base.labelLarge.copy(fontFamily = font),
        labelMedium = base.labelMedium.copy(fontFamily = font),
        labelSmall = base.labelSmall.copy(fontFamily = font),
    )
    MaterialTheme(colorScheme = Colors, typography = t, content = content)
}
