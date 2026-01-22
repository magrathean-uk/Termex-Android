package com.termex.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termex.app.core.billing.SubscriptionManager
import com.termex.app.core.billing.SubscriptionState
import com.termex.app.data.prefs.KeepAliveInterval
import com.termex.app.data.prefs.TerminalSettings
import com.termex.app.data.prefs.ThemeMode
import com.termex.app.data.prefs.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val subscriptionManager: SubscriptionManager
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = userPreferencesRepository.themeFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, ThemeMode.AUTO)

    val terminalSettings: StateFlow<TerminalSettings> = userPreferencesRepository.terminalSettingsFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, TerminalSettings())

    val keepAliveInterval: StateFlow<KeepAliveInterval> = userPreferencesRepository.keepAliveIntervalFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, KeepAliveInterval.SECONDS_30)

    val demoModeEnabled: StateFlow<Boolean> = userPreferencesRepository.demoModeEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val subscriptionState: StateFlow<SubscriptionState> = subscriptionManager.subscriptionState

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

    fun setDemoMode(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setDemoModeEnabled(enabled)
        }
    }

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
    
    fun setColorScheme(scheme: String) {
        viewModelScope.launch {
            val current = terminalSettings.value
            userPreferencesRepository.setTerminalSettings(current.copy(colorScheme = scheme))
        }
    }
    
    fun restorePurchases() {
        subscriptionManager.querySubscriptionStatus()
    }

    fun resetApp() {
        viewModelScope.launch {
            userPreferencesRepository.resetOnboarding()
            userPreferencesRepository.setDemoModeEnabled(false)
        }
    }
}
