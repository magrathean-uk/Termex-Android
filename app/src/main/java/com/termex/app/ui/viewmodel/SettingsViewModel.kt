package com.termex.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termex.app.core.billing.SubscriptionManager
import com.termex.app.core.billing.SubscriptionState
import com.termex.app.data.prefs.TerminalSettings
import com.termex.app.data.prefs.ThemeMode
import com.termex.app.data.prefs.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    
    val subscriptionState: StateFlow<SubscriptionState> = subscriptionManager.subscriptionState
    
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            userPreferencesRepository.setThemeMode(mode)
        }
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
}
