package com.termex.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = TermexGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB8F5D8),
    onPrimaryContainer = Color(0xFF002112),
    secondary = TermexGreen,
    onSecondary = Color.White,
    background = Color(0xFFFBFDF8),
    surface = Color.White,
    onBackground = Color(0xFF191C1A),
    onSurface = Color(0xFF191C1A),
)

private val DarkColors = darkColorScheme(
    primary = TermexGreen,
    onPrimary = Color(0xFF003823),
    primaryContainer = Color(0xFF005234),
    onPrimaryContainer = Color(0xFFB8F5D8),
    secondary = TermexGreen,
    onSecondary = Color(0xFF003823),
    background = Color(0xFF191C1A),
    surface = Color(0xFF1E1E1E),
    onBackground = Color(0xFFE1E3DF),
    onSurface = Color(0xFFE1E3DF),
)

@Composable
fun TermexTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Use dynamic color on Android 12+
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useDarkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        useDarkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}