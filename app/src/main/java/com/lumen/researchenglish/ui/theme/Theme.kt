package com.lumen.researchenglish.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView

val Ivory = Color(0xFFF7F7F4)
val Paper = Color(0xFFFFFFFF)
val Graphite = Color(0xFF1D1D1F)
val Indigo = Color(0xFF4F5FBF)
val SoftIndigo = Color(0xFFE9EBFA)
val Sage = Color(0xFF3F8C68)
val WarmGray = Color(0xFF73736F)
val Hairline = Color(0xFFE5E5E1)

private val LightColors = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = SoftIndigo,
    onPrimaryContainer = Graphite,
    background = Ivory,
    onBackground = Graphite,
    surface = Paper,
    onSurface = Graphite,
    surfaceVariant = Color(0xFFF0F0EC),
    onSurfaceVariant = WarmGray,
    outline = Hairline,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFADB5FF),
    background = Color(0xFF111113),
    surface = Color(0xFF1C1C1E),
    onBackground = Color(0xFFF3F3F0),
    onSurface = Color(0xFFF3F3F0),
)

@Composable
fun LumenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.navigationBarColor = colors.background.toArgb()
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = androidx.compose.material3.Typography(),
        content = content,
    )
}
