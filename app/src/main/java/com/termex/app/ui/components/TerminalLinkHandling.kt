package com.termex.app.ui.components

import com.termex.app.data.prefs.LinkHandlingMode

enum class TerminalLinkAction {
    IGNORE,
    OPEN,
    CONFIRM
}

private val terminalLinkPattern = Regex("""(?i)\b(?:https?|ftp|ftps|ssh)://[^\s<>()"]+""")

fun extractTerminalLinks(text: String): List<String> {
    return terminalLinkPattern.findAll(text)
        .map { it.value.trimEnd('.', ',', ';', ':', ')', ']', '}') }
        .filter { it.isNotBlank() }
        .distinct()
        .toList()
}

fun linkActionFor(link: String, mode: LinkHandlingMode): TerminalLinkAction {
    val normalized = link.trim()
    if (normalized.isBlank() || !terminalLinkPattern.containsMatchIn(normalized)) {
        return TerminalLinkAction.IGNORE
    }
    return when (mode) {
        LinkHandlingMode.AUTOMATIC -> TerminalLinkAction.OPEN
        LinkHandlingMode.ASK_FIRST -> TerminalLinkAction.CONFIRM
    }
}
