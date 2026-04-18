package com.termex.app.data.prefs

enum class TerminalExtraKeyPreset(val raw: String, val label: String, val detail: String) {
    STANDARD("standard", "Standard", "Core shell controls with arrows and a pipe."),
    NAVIGATION("navigation", "Navigation", "Faster cursor movement with Home, End, and paging."),
    CODING("coding", "Coding", "Common symbols for shell work and quick edits."),
    FUNCTION_ROW("function_row", "Function Row", "Keep F1 through F12 close for remote tools.");

    val keyIds: List<String>
        get() = keys.map(TerminalExtraKey::raw)

    val keys: List<TerminalExtraKey>
        get() = when (this) {
            STANDARD -> TerminalExtraKey.defaultKeys
            NAVIGATION -> listOf(
                TerminalExtraKey.ESC,
                TerminalExtraKey.TAB,
                TerminalExtraKey.CTRL,
                TerminalExtraKey.ALT,
                TerminalExtraKey.HOME,
                TerminalExtraKey.END,
                TerminalExtraKey.PAGE_UP,
                TerminalExtraKey.PAGE_DOWN,
                TerminalExtraKey.UP,
                TerminalExtraKey.DOWN,
                TerminalExtraKey.LEFT,
                TerminalExtraKey.RIGHT
            )
            CODING -> listOf(
                TerminalExtraKey.ESC,
                TerminalExtraKey.TAB,
                TerminalExtraKey.CTRL,
                TerminalExtraKey.ALT,
                TerminalExtraKey.PIPE,
                TerminalExtraKey.SLASH,
                TerminalExtraKey.DASH,
                TerminalExtraKey.TILDE,
                TerminalExtraKey.HASH,
                TerminalExtraKey.HOME,
                TerminalExtraKey.END
            )
            FUNCTION_ROW -> listOf(
                TerminalExtraKey.ESC,
                TerminalExtraKey.TAB,
                TerminalExtraKey.CTRL,
                TerminalExtraKey.ALT,
                TerminalExtraKey.F1,
                TerminalExtraKey.F2,
                TerminalExtraKey.F3,
                TerminalExtraKey.F4,
                TerminalExtraKey.F5,
                TerminalExtraKey.F6,
                TerminalExtraKey.F7,
                TerminalExtraKey.F8,
                TerminalExtraKey.F9,
                TerminalExtraKey.F10,
                TerminalExtraKey.F11,
                TerminalExtraKey.F12
            )
        }

    companion object {
        fun fromRaw(raw: String?): TerminalExtraKeyPreset {
            return entries.firstOrNull { it.raw == raw } ?: STANDARD
        }
    }
}
