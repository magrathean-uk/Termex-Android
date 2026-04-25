package com.termex.app.data.prefs

import kotlinx.serialization.Serializable

enum class ThemeMode(val raw: String, val label: String) {
    AUTO("auto", "Auto"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark");

    companion object {
        fun fromRaw(raw: String?): ThemeMode {
            return entries.firstOrNull { it.raw == raw } ?: AUTO
        }
    }
}

data class TerminalSettings(
    val fontSize: Int = 14,
    val fontFamily: String = "Monospace",
    val colorScheme: String = "whiteOnBlack"
)

enum class SyncMode(val raw: String, val label: String, val detail: String) {
    LOCAL_ONLY(
        raw = "local_only",
        label = "Local Only",
        detail = "Keep metadata on this device. Android backup and device transfer can move it to another phone."
    ),
    GOOGLE_DRIVE(
        raw = "google_drive",
        label = "Google Drive",
        detail = "Sync metadata through Google Drive app data."
    );

    companion object {
        fun fromRaw(raw: String?): SyncMode {
            return LOCAL_ONLY
        }
    }
}

@Serializable
data class SyncStatus(
    val message: String = "Not synced yet.",
    val syncedAtMillis: Long? = null,
    val missingSecretCount: Int = 0,
    val googleAccountEmail: String? = null
)
