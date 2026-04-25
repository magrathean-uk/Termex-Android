package com.termex.app.ui.theme

import androidx.compose.ui.graphics.Color

enum class TerminalColorScheme(
    val raw: String,
    val displayName: String,
    val detail: String,
    val foreground: Color,
    val background: Color,
    val cursor: Color
) {
    WHITE_ON_BLACK(
        raw = "whiteOnBlack",
        displayName = "White on Black",
        detail = "Classic focused terminal look.",
        foreground = Color(0xFFFFFFFF),
        background = Color(0xFF000000),
        cursor = Color(0xFFFFFFFF)
    ),
    BLACK_ON_WHITE(
        raw = "blackOnWhite",
        displayName = "Black on White",
        detail = "Bright, paper-like shell view.",
        foreground = Color(0xFF000000),
        background = Color(0xFFFFFFFF),
        cursor = Color(0xFF000000)
    ),
    PHOSPHOR_GREEN(
        raw = "phosphorGreen",
        displayName = "Phosphor Green",
        detail = "Retro green glow without the gimmick.",
        foreground = Color(0xFF99FF9E),
        background = Color(0xFF05140A),
        cursor = Color(0xFFB8FFC0)
    ),
    AMBER_CRT(
        raw = "amberCRT",
        displayName = "Amber CRT",
        detail = "Warm amber tube vibe for long sessions.",
        foreground = Color(0xFFFFC463),
        background = Color(0xFF1A0F05),
        cursor = Color(0xFFFFD68A)
    ),
    OCEAN(
        raw = "ocean",
        displayName = "Ocean",
        detail = "Cool cyan shell with darker deep-sea chrome.",
        foreground = Color(0xFFA8EEF7),
        background = Color(0xFF08131F),
        cursor = Color(0xFFC7F8FF)
    ),
    NORD_NIGHT(
        raw = "nordNight",
        displayName = "Nord Night",
        detail = "Muted arctic dark theme with softer contrast.",
        foreground = Color(0xFFD8DEE9),
        background = Color(0xFF2E3440),
        cursor = Color(0xFFE5E9F0)
    ),
    SOLARIZED_DARK(
        raw = "solarizedDark",
        displayName = "Solarized Dark",
        detail = "Low-glare dark palette for heavy command work.",
        foreground = Color(0xFF93A1A1),
        background = Color(0xFF002B36),
        cursor = Color(0xFFEEE8D5)
    ),
    PAPER(
        raw = "paper",
        displayName = "Paper",
        detail = "Soft bright canvas that stays readable in daylight.",
        foreground = Color(0xFF1F2328),
        background = Color(0xFFF7F3EA),
        cursor = Color(0xFF111111)
    ),
    SEPIA(
        raw = "sepia",
        displayName = "Sepia",
        detail = "Warm cream background with ink-like contrast.",
        foreground = Color(0xFF45301A),
        background = Color(0xFFF4E4C8),
        cursor = Color(0xFF372616)
    ),
    FOREST_MIST(
        raw = "forestMist",
        displayName = "Forest Mist",
        detail = "Gentle green-tinted light theme with calm chrome.",
        foreground = Color(0xFF243B2D),
        background = Color(0xFFEFF7EF),
        cursor = Color(0xFF193123)
    );

    companion object {
        fun fromStoredValue(value: String?): TerminalColorScheme {
            return entries.firstOrNull { scheme ->
                scheme.raw == value || scheme.displayName == value
            } ?: WHITE_ON_BLACK
        }
    }
}
