package com.termex.app.ui.theme

import androidx.compose.ui.graphics.Color

enum class TerminalColorScheme(
    val displayName: String,
    val foreground: Color,
    val background: Color,
    val cursor: Color
) {
    DEFAULT(
        displayName = "Default",
        foreground = Color(0xFFE5E5E5),
        background = Color(0xFF000000),
        cursor = Color(0xFFE5E5E5)
    ),
    GREEN_ON_BLACK(
        displayName = "Green on Black",
        foreground = Color(0xFF00FF00),
        background = Color(0xFF000000),
        cursor = Color(0xFF00FF00)
    ),
    WHITE_ON_BLACK(
        displayName = "White on Black",
        foreground = Color(0xFFFFFFFF),
        background = Color(0xFF000000),
        cursor = Color(0xFFFFFFFF)
    ),
    AMBER_ON_BLACK(
        displayName = "Amber on Black",
        foreground = Color(0xFFFFB000),
        background = Color(0xFF000000),
        cursor = Color(0xFFFFB000)
    ),
    SOLARIZED_DARK(
        displayName = "Solarized Dark",
        foreground = Color(0xFF839496),
        background = Color(0xFF002B36),
        cursor = Color(0xFF93A1A1)
    ),
    DRACULA(
        displayName = "Dracula",
        foreground = Color(0xFFF8F8F2),
        background = Color(0xFF282A36),
        cursor = Color(0xFFF8F8F2)
    );

    companion object {
        fun fromName(name: String): TerminalColorScheme {
            return entries.find { it.displayName == name } ?: DEFAULT
        }
    }
}
