package com.termex.app.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class KeepAliveInterval(val seconds: Int, val label: String) {
    SECONDS_15(15, "15 seconds"),
    SECONDS_30(30, "30 seconds"),
    SECONDS_60(60, "60 seconds"),
    SECONDS_120(120, "120 seconds"),
    DISABLED(0, "Disabled")
}

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
        private val KEY_KEEP_ALIVE_INTERVAL = intPreferencesKey("keep_alive_interval")
        private val KEY_DEMO_MODE_ENABLED = booleanPreferencesKey("demo_mode_enabled")
        private val KEY_BIOMETRIC_LOCK_ENABLED = booleanPreferencesKey("biometric_lock_enabled")
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

    val keepAliveIntervalFlow: Flow<KeepAliveInterval> = dataStore.data.map { prefs ->
        val seconds = prefs[KEY_KEEP_ALIVE_INTERVAL] ?: 30
        KeepAliveInterval.entries.firstOrNull { it.seconds == seconds } ?: KeepAliveInterval.SECONDS_30
    }

    val demoModeEnabledFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_DEMO_MODE_ENABLED] ?: false
    }

    val biometricLockEnabledFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_BIOMETRIC_LOCK_ENABLED] ?: false
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

    suspend fun setKeepAliveInterval(interval: KeepAliveInterval) {
        dataStore.edit { prefs ->
            prefs[KEY_KEEP_ALIVE_INTERVAL] = interval.seconds
        }
    }

    suspend fun setDemoModeEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_DEMO_MODE_ENABLED] = enabled
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

    suspend fun setBiometricLockEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_BIOMETRIC_LOCK_ENABLED] = enabled
        }
    }
}
