package com.termex.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val PREFS_NAME = "termex_prefs"

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = PREFS_NAME)

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

class UserPreferences private constructor(
    private val dataStore: DataStore<Preferences>,
    private val context: Context
) {
    companion object {
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_FONT_SIZE = intPreferencesKey("font_size")
        private val KEY_FONT_FAMILY = stringPreferencesKey("font_family")
        private val KEY_COLOR_SCHEME = stringPreferencesKey("color_scheme")

        fun from(context: Context): UserPreferences {
            return UserPreferences(context.dataStore, context)
        }
    }

    val themeFlow: Flow<ThemeMode> = dataStore.data.map { prefs ->
        prefs[KEY_THEME_MODE]?.let { raw ->
            ThemeMode.values().firstOrNull { it.raw == raw }
        } ?: ThemeMode.AUTO
    }

    val terminalSettingsFlow: Flow<TerminalSettings> = dataStore.data.map { prefs ->
        TerminalSettings(
            fontSize = prefs[KEY_FONT_SIZE] ?: 14,
            fontFamily = prefs[KEY_FONT_FAMILY] ?: "Monospace",
            colorScheme = prefs[KEY_COLOR_SCHEME] ?: "Default"
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.raw
        }
    }

    suspend fun setTerminalSettings(settings: TerminalSettings) {
        dataStore.edit { prefs ->
            prefs[KEY_FONT_SIZE] = settings.fontSize
            prefs[KEY_FONT_FAMILY] = settings.fontFamily
            prefs[KEY_COLOR_SCHEME] = settings.colorScheme
        }
    }
}
