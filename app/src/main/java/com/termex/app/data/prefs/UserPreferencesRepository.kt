package com.termex.app.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

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

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_FONT_SIZE = intPreferencesKey("font_size")
        private val KEY_FONT_FAMILY = stringPreferencesKey("font_family")
        private val KEY_COLOR_SCHEME = stringPreferencesKey("color_scheme")
        private val KEY_ONBOARDING_COMPLETE = stringPreferencesKey("onboarding_complete")
    }

    val themeFlow: Flow<ThemeMode> = dataStore.data.map { prefs ->
        prefs[KEY_THEME_MODE]?.let { raw ->
            ThemeMode.entries.firstOrNull { it.raw == raw }
        } ?: ThemeMode.AUTO
    }

    val terminalSettingsFlow: Flow<TerminalSettings> = dataStore.data.map { prefs ->
        TerminalSettings(
            fontSize = prefs[KEY_FONT_SIZE] ?: 14,
            fontFamily = prefs[KEY_FONT_FAMILY] ?: "Monospace",
            colorScheme = prefs[KEY_COLOR_SCHEME] ?: "Default"
        )
    }
    
    val hasCompletedOnboarding: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETE] == "true"
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
    
    suspend fun completeOnboarding() {
        dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETE] = "true"
        }
    }
    
    suspend fun resetOnboarding() {
        dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETE] = "false"
        }
    }
}
