package com.termex.app.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.termex.app.core.PersistedRootRoute
import com.termex.app.core.RootRouteKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
        private val KEY_PERSISTED_ROOT_ROUTE = stringPreferencesKey("persisted_root_route")
        private val KEY_PERSISTENT_SESSION_RESUME_SERVER_ID = stringPreferencesKey("persistent_session_resume_server_id")
        private val KEY_LINK_HANDLING_MODE = stringPreferencesKey("link_handling_mode")
        private val KEY_EXTRA_KEY_PRESET = stringPreferencesKey("extra_key_preset")
        private val KEY_EXTRA_KEY_IDS = stringPreferencesKey("extra_key_ids")
        private val KEY_SYNC_MODE = stringPreferencesKey("sync_mode")
        private val KEY_SYNC_STATUS = stringPreferencesKey("sync_status")
        private val KEY_SYNC_GOOGLE_ACCOUNT_EMAIL = stringPreferencesKey("sync_google_account_email")
    }

    private val json = Json {
        ignoreUnknownKeys = true
    }

    val themeFlow: Flow<ThemeMode> = dataStore.data.map { prefs ->
        ThemeMode.fromRaw(prefs[KEY_THEME_MODE])
    }

    val terminalSettingsFlow: Flow<TerminalSettings> = dataStore.data.map { prefs ->
        TerminalSettings(
            fontSize = prefs[KEY_FONT_SIZE] ?: 14,
            fontFamily = prefs[KEY_FONT_FAMILY] ?: "Monospace",
            colorScheme = prefs[KEY_COLOR_SCHEME] ?: "whiteOnBlack"
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

    val persistedRootRouteFlow: Flow<PersistedRootRoute> = dataStore.data.map { prefs ->
        val raw = prefs[KEY_PERSISTED_ROOT_ROUTE] ?: return@map PersistedRootRoute.none()
        runCatching {
            json.decodeFromString<PersistedRootRoute>(raw)
        }.getOrDefault(PersistedRootRoute.none())
    }

    val persistentSessionResumeServerIdFlow: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_PERSISTENT_SESSION_RESUME_SERVER_ID]
    }

    val linkHandlingModeFlow: Flow<LinkHandlingMode> = dataStore.data.map { prefs ->
        LinkHandlingMode.fromRaw(prefs[KEY_LINK_HANDLING_MODE])
    }

    val terminalExtraKeyPresetFlow: Flow<TerminalExtraKeyPreset> = dataStore.data.map { prefs ->
        TerminalExtraKeyPreset.fromRaw(prefs[KEY_EXTRA_KEY_PRESET])
    }

    val terminalExtraKeyIdsFlow: Flow<List<String>> = dataStore.data.map { prefs ->
        prefs[KEY_EXTRA_KEY_IDS]?.let { raw ->
            runCatching { json.decodeFromString<List<String>>(raw) }.getOrNull()
        } ?: terminalExtraKeyPresetFlowValue(prefs)
    }

    val terminalExtraKeysFlow: Flow<List<TerminalExtraKey>> = terminalExtraKeyIdsFlow.map { rawIds ->
        TerminalExtraKey.fromRawList(rawIds)
    }

    val syncModeFlow: Flow<SyncMode> = dataStore.data.map { prefs ->
        SyncMode.LOCAL_ONLY
    }

    val syncStatusFlow: Flow<SyncStatus> = dataStore.data.map { prefs ->
        prefs[KEY_SYNC_STATUS]?.let { raw ->
            runCatching { json.decodeFromString<SyncStatus>(raw) }.getOrNull()
        } ?: SyncStatus()
    }

    val syncGoogleAccountEmailFlow: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_SYNC_GOOGLE_ACCOUNT_EMAIL]?.takeIf { it.isNotBlank() }
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

    suspend fun setLinkHandlingMode(mode: LinkHandlingMode) {
        dataStore.edit { prefs ->
            prefs[KEY_LINK_HANDLING_MODE] = mode.raw
        }
    }

    suspend fun setTerminalExtraKeyPreset(preset: TerminalExtraKeyPreset) {
        dataStore.edit { prefs ->
            prefs[KEY_EXTRA_KEY_PRESET] = preset.raw
            prefs[KEY_EXTRA_KEY_IDS] = json.encodeToString(preset.keyIds)
        }
    }

    suspend fun setTerminalExtraKeyIds(keyIds: List<String>) {
        val normalized = keyIds.mapNotNull(TerminalExtraKey::fromRaw)
            .distinct()
            .map(TerminalExtraKey::raw)
            .ifEmpty { TerminalExtraKey.defaultKeys.map(TerminalExtraKey::raw) }
        dataStore.edit { prefs ->
            prefs[KEY_EXTRA_KEY_IDS] = json.encodeToString(normalized)
        }
    }

    suspend fun setSyncMode(mode: SyncMode) {
        dataStore.edit { prefs ->
            prefs[KEY_SYNC_MODE] = SyncMode.LOCAL_ONLY.raw
        }
    }

    suspend fun setSyncStatus(status: SyncStatus) {
        dataStore.edit { prefs ->
            prefs[KEY_SYNC_STATUS] = json.encodeToString(status)
        }
    }

    suspend fun setSyncGoogleAccountEmail(email: String?) {
        dataStore.edit { prefs ->
            if (email.isNullOrBlank()) {
                prefs.remove(KEY_SYNC_GOOGLE_ACCOUNT_EMAIL)
            } else {
                prefs[KEY_SYNC_GOOGLE_ACCOUNT_EMAIL] = email.trim()
            }
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

    suspend fun setPersistedRootRoute(route: PersistedRootRoute) {
        dataStore.edit { prefs ->
            if (route.kind == RootRouteKind.NONE) {
                prefs.remove(KEY_PERSISTED_ROOT_ROUTE)
            } else {
                prefs[KEY_PERSISTED_ROOT_ROUTE] = json.encodeToString(route)
            }
        }
    }

    suspend fun setPersistentSessionResumeServerId(serverId: String?) {
        dataStore.edit { prefs ->
            if (serverId.isNullOrBlank()) {
                prefs.remove(KEY_PERSISTENT_SESSION_RESUME_SERVER_ID)
            } else {
                prefs[KEY_PERSISTENT_SESSION_RESUME_SERVER_ID] = serverId
            }
        }
    }

    private fun terminalExtraKeyPresetFlowValue(prefs: Preferences): List<String> {
        return TerminalExtraKeyPreset.fromRaw(prefs[KEY_EXTRA_KEY_PRESET]).keyIds
    }
}
