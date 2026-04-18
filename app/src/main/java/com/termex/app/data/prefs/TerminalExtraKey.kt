package com.termex.app.data.prefs

enum class TerminalExtraKey(
    val raw: String,
    val label: String,
    val sequence: String
) {
    ESC("esc", "Esc", "\u001B"),
    TAB("tab", "Tab", "\t"),
    CTRL("ctrl", "Ctrl", ""),
    ALT("alt", "Alt", ""),
    HOME("home", "Home", "\u001B[H"),
    END("end", "End", "\u001B[F"),
    UP("up", "↑", "\u001B[A"),
    DOWN("down", "↓", "\u001B[B"),
    LEFT("left", "←", "\u001B[D"),
    RIGHT("right", "→", "\u001B[C"),
    PIPE("pipe", "|", "|"),
    TILDE("tilde", "~", "~"),
    SLASH("slash", "/", "/"),
    DASH("dash", "-", "-"),
    HASH("hash", "#", "#"),
    PAGE_UP("page_up", "PgUp", "\u001B[5~"),
    PAGE_DOWN("page_down", "PgDn", "\u001B[6~"),
    F1("f1", "F1", "\u001BOP"),
    F2("f2", "F2", "\u001BOQ"),
    F3("f3", "F3", "\u001BOR"),
    F4("f4", "F4", "\u001BOS"),
    F5("f5", "F5", "\u001B[15~"),
    F6("f6", "F6", "\u001B[17~"),
    F7("f7", "F7", "\u001B[18~"),
    F8("f8", "F8", "\u001B[19~"),
    F9("f9", "F9", "\u001B[20~"),
    F10("f10", "F10", "\u001B[21~"),
    F11("f11", "F11", "\u001B[23~"),
    F12("f12", "F12", "\u001B[24~");

    companion object {
        val defaultKeys: List<TerminalExtraKey> = listOf(
            ESC,
            TAB,
            CTRL,
            ALT,
            UP,
            DOWN,
            LEFT,
            RIGHT,
            PIPE
        )

        fun fromRaw(raw: String?): TerminalExtraKey? {
            return entries.firstOrNull { it.raw == raw }
        }

        fun fromRawList(raws: List<String>): List<TerminalExtraKey> {
            return raws.mapNotNull(::fromRaw).ifEmpty { defaultKeys }
        }
    }
}
