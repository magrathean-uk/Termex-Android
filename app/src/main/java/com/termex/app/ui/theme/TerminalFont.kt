package com.termex.app.ui.theme

enum class TerminalFont(
    val displayName: String,
    val fontFamily: String
) {
    MONOSPACE("System Monospace", "monospace"),
    JETBRAINS_MONO("JetBrains Mono", "jetbrains_mono"),
    FIRA_CODE("Fira Code", "fira_code"),
    SOURCE_CODE_PRO("Source Code Pro", "source_code_pro"),
    UBUNTU_MONO("Ubuntu Mono", "ubuntu_mono");

    companion object {
        fun fromName(name: String): TerminalFont {
            return entries.find { it.displayName == name || it.fontFamily == name } ?: MONOSPACE
        }
    }
}
