package com.termex.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.termex.app.core.security.BiometricAuthManager
import com.termex.app.core.security.BiometricResult
import com.termex.app.data.prefs.ThemeMode
import com.termex.app.data.prefs.UserPreferencesRepository
import com.termex.app.ui.screens.BiometricLockScreen
import com.termex.app.ui.theme.TermexTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    
    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository
    
    @Inject
    lateinit var biometricAuthManager: BiometricAuthManager
    
    // Compose-visible mutable state for lock
    private var isLocked = mutableStateOf(true)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by userPreferencesRepository.themeFlow.collectAsState(initial = ThemeMode.AUTO)
            val biometricLockEnabled by userPreferencesRepository.biometricLockEnabledFlow.collectAsState(initial = true)
            val systemDark = isSystemInDarkTheme()
            
            // Sync lock state: if biometric is disabled, always unlock
            LaunchedEffect(biometricLockEnabled) {
                if (!biometricLockEnabled) {
                    isLocked.value = false
                }
            }

            // Re-lock when app resumes from background
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner, biometricLockEnabled) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_START && biometricLockEnabled) {
                        isLocked.value = true
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }
            
            val useDarkTheme = when (themeMode) {
                ThemeMode.AUTO -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            
            TermexTheme(useDarkTheme = useDarkTheme) {
                if (biometricLockEnabled && isLocked.value) {
                    BiometricLockScreen(
                        onAuthenticate = {},
                        onCancel = { finish() }
                    )
                    
                    LaunchedEffect(isLocked.value) {
                        if (isLocked.value) {
                            val result = biometricAuthManager.authenticate(this@MainActivity)
                            when (result) {
                                BiometricResult.Success -> isLocked.value = false
                                BiometricResult.Canceled -> finish()
                                is BiometricResult.Error -> finish()
                                is BiometricResult.Lockout -> finish()
                            }
                        }
                    }
                } else {
                    TermexApp()
                }
            }
        }
    }
}
