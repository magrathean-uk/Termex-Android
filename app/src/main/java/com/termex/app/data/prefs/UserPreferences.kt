package com.termex.app.data.prefs

enum class ThemeMode(val raw: String, val label: String) {
    AUTO("auto", "Auto"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark")
}

data class TerminalSettings(
    val fontSize: Int = 14,
    val fontFamily: String = "Monospace",
    val colorScheme: String = "Default"
)
