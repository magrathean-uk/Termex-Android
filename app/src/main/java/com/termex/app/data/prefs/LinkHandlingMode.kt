package com.termex.app.data.prefs

enum class LinkHandlingMode(val raw: String, val label: String) {
    AUTOMATIC("automatic", "Automatic"),
    ASK_FIRST("ask_first", "Ask First");

    companion object {
        fun fromRaw(raw: String?): LinkHandlingMode {
            return entries.firstOrNull { it.raw == raw } ?: AUTOMATIC
        }
    }
}
