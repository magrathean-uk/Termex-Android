package com.termex.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import com.termex.app.core.security.BiometricAuthManager
import com.termex.app.core.security.BiometricResult
import com.termex.app.data.prefs.ThemeMode
import com.termex.app.data.prefs.UserPreferencesRepository
import com.termex.app.ui.screens.BiometricLockScreen
import com.termex.app.ui.theme.TermexTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    
    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository
    
    @Inject
    lateinit var biometricAuthManager: BiometricAuthManager
    
    private var needsBiometricAuth = mutableStateOf(false)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by userPreferencesRepository.themeFlow.collectAsState(initial = ThemeMode.AUTO)
            val biometricLockEnabled by userPreferencesRepository.biometricLockEnabledFlow.collectAsState(initial = false)
            val systemDark = isSystemInDarkTheme()
            
            var isLocked by remember { mutableStateOf(biometricLockEnabled) }
            
            val useDarkTheme = when (themeMode) {
                ThemeMode.AUTO -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            
            TermexTheme(useDarkTheme = useDarkTheme) {
                if (biometricLockEnabled && isLocked) {
                    BiometricLockScreen(
                        onAuthenticate = {},
                        onCancel = { finish() }
                    )
                    
                    LaunchedEffect(Unit) {
                        val result = biometricAuthManager.authenticate(this@MainActivity)
                        when (result) {
                            BiometricResult.Success -> isLocked = false
                            BiometricResult.Canceled -> finish()
                            is BiometricResult.Error -> finish()
                            is BiometricResult.Lockout -> finish()
                        }
                    }
                } else {
                    TermexApp()
                }
            }
            
            LaunchedEffect(biometricLockEnabled) {
                if (!biometricLockEnabled) {
                    isLocked = false
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Re-lock when returning to app from background
        lifecycleScope.launch {
            val biometricEnabled = try {
                userPreferencesRepository.biometricLockEnabledFlow.first()
            } catch (e: Exception) {
                false
            }
            if (biometricEnabled) {
                needsBiometricAuth.value = true
            }
        }
    }
}
