package com.termex.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termex.app.core.security.AppLockCoordinator
import com.termex.app.core.security.AppLockUiState
import com.termex.app.core.security.BiometricAvailability
import com.termex.app.data.diagnostics.DiagnosticsRepository
import com.termex.app.data.prefs.KeepAliveInterval
import com.termex.app.data.prefs.LinkHandlingMode
import com.termex.app.data.prefs.TerminalExtraKey
import com.termex.app.data.prefs.TerminalExtraKeyPreset
import com.termex.app.data.prefs.TerminalSettings
import com.termex.app.data.prefs.ThemeMode
import com.termex.app.data.prefs.UserPreferencesRepository
import com.termex.app.ui.theme.TerminalColorScheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    val appLockCoordinator: AppLockCoordinator,
    private val sessionRepository: com.termex.app.data.repository.SessionRepository,
    private val diagnosticsRepository: DiagnosticsRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = userPreferencesRepository.themeFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, ThemeMode.AUTO)

    val terminalSettings: StateFlow<TerminalSettings> = userPreferencesRepository.terminalSettingsFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, TerminalSettings())

    val terminalColorScheme: StateFlow<TerminalColorScheme> = userPreferencesRepository.terminalSettingsFlow
        .map { TerminalColorScheme.fromStoredValue(it.colorScheme) }
        .stateIn(viewModelScope, SharingStarted.Lazily, TerminalColorScheme.WHITE_ON_BLACK)

    val keepAliveInterval: StateFlow<KeepAliveInterval> = userPreferencesRepository.keepAliveIntervalFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, KeepAliveInterval.SECONDS_30)

    val demoModeEnabled: StateFlow<Boolean> = userPreferencesRepository.demoModeEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val biometricLockEnabled: StateFlow<Boolean> = userPreferencesRepository.biometricLockEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val linkHandlingMode: StateFlow<LinkHandlingMode> = userPreferencesRepository.linkHandlingModeFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, LinkHandlingMode.AUTOMATIC)

    val terminalExtraKeyPreset: StateFlow<TerminalExtraKeyPreset> = userPreferencesRepository.terminalExtraKeyPresetFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, TerminalExtraKeyPreset.STANDARD)

    val terminalExtraKeys: StateFlow<List<TerminalExtraKey>> = userPreferencesRepository.terminalExtraKeysFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, TerminalExtraKey.defaultKeys)

    val appLockState: StateFlow<AppLockUiState> = appLockCoordinator.uiState

    val diagnosticsSummary: StateFlow<String> = combine(
        diagnosticsRepository.events,
        sessionRepository.getAllSessions()
    ) { events, sessions ->
        "${events.size} events · ${sessions.size} saved sessions"
    }.stateIn(viewModelScope, SharingStarted.Lazily, "0 events · 0 saved sessions")

    // For demo mode activation (5 taps on version)
    private val _versionTapCount = MutableStateFlow(0)
    val versionTapCount: StateFlow<Int> = _versionTapCount.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            userPreferencesRepository.setThemeMode(mode)
        }
    }

    fun setKeepAliveInterval(interval: KeepAliveInterval) {
        viewModelScope.launch {
            userPreferencesRepository.setKeepAliveInterval(interval)
        }
    }

    // SECURITY: Demo mode can only be set during onboarding flow
    // No public setter exposed to prevent programmatic bypass

    fun onVersionTap() {
        // Demo mode can ONLY be activated during onboarding (for App Store reviewers)
        // Tapping version does nothing to prevent normal users from entering demo mode
        _versionTapCount.value++
        if (_versionTapCount.value >= 5) {
            _versionTapCount.value = 0
            // No demo mode toggle here - intentionally disabled
        }
    }

    fun resetVersionTapCount() {
        _versionTapCount.value = 0
    }
    
    fun setFontSize(size: Int) {
        viewModelScope.launch {
            val current = terminalSettings.value
            userPreferencesRepository.setTerminalSettings(current.copy(fontSize = size))
        }
    }
    
    fun setFontFamily(family: String) {
        viewModelScope.launch {
            val current = terminalSettings.value
            userPreferencesRepository.setTerminalSettings(current.copy(fontFamily = family))
        }
    }
    
    fun setColorScheme(scheme: TerminalColorScheme) {
        viewModelScope.launch {
            val current = terminalSettings.value
            userPreferencesRepository.setTerminalSettings(current.copy(colorScheme = scheme.raw))
        }
    }

    fun setLinkHandlingMode(mode: LinkHandlingMode) {
        viewModelScope.launch {
            userPreferencesRepository.setLinkHandlingMode(mode)
        }
    }

    fun setTerminalExtraKeyPreset(preset: TerminalExtraKeyPreset) {
        viewModelScope.launch {
            userPreferencesRepository.setTerminalExtraKeyPreset(preset)
        }
    }

    fun setTerminalExtraKeyIds(keyIds: List<String>) {
        viewModelScope.launch {
            userPreferencesRepository.setTerminalExtraKeyIds(keyIds)
        }
    }
    
    fun resetApp() {
        viewModelScope.launch {
            userPreferencesRepository.resetOnboarding()
            userPreferencesRepository.setDemoModeEnabled(false)
            userPreferencesRepository.setBiometricLockEnabled(false)
            sessionRepository.deleteAllSessions()
        }
    }

    fun setBiometricLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                appLockCoordinator.refreshAvailability()
                if (appLockCoordinator.uiState.value.biometricAvailability != BiometricAvailability.Available) {
                    return@launch
                }
            }
            userPreferencesRepository.setBiometricLockEnabled(enabled)
        }
    }
    
    fun clearSavedSessions() {
        viewModelScope.launch {
            sessionRepository.deleteAllSessions()
        }
    }
    
    init {
        // Cleanup old sessions (older than 7 days)
        viewModelScope.launch {
            val sevenDays = 7 * 24 * 60 * 60 * 1000L
            sessionRepository.cleanupOldSessions(sevenDays)
        }
    }
}
