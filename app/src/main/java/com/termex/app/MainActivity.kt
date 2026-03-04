package com.termex.app

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
    // Tracks when the app was last in the foreground (for grace period)
    private var lastForegroundElapsed = 0L
    private val lockGracePeriodMs = 30_000L
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by userPreferencesRepository.themeFlow.collectAsState(initial = ThemeMode.AUTO)
            val biometricLockEnabled by userPreferencesRepository.biometricLockEnabledFlow
                // In debug/dev builds, default to OFF so testing isn't blocked by biometrics
                .collectAsState(initial = !BuildConfig.DEBUG)
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
                    when (event) {
                        Lifecycle.Event.ON_STOP -> {
                            // Record when we went to background
                            if (biometricLockEnabled) {
                                lastForegroundElapsed = SystemClock.elapsedRealtime()
                            }
                        }
                        Lifecycle.Event.ON_START -> {
                            // Only re-lock if we were in the background long enough
                            if (biometricLockEnabled && lastForegroundElapsed > 0L) {
                                val elapsed = SystemClock.elapsedRealtime() - lastForegroundElapsed
                                if (elapsed >= lockGracePeriodMs) {
                                    isLocked.value = true
                                }
                                lastForegroundElapsed = 0L
                            }
                        }
                        else -> {}
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
                    var authTrigger by remember { mutableStateOf(0) }

                    BiometricLockScreen(
                        onAuthenticate = { authTrigger++ },
                        onCancel = { finish() }
                    )
                    
                    LaunchedEffect(authTrigger) {
                        val result = biometricAuthManager.authenticate(this@MainActivity)
                        when (result) {
                            BiometricResult.Success -> isLocked.value = false
                            BiometricResult.Canceled -> { /* stay on lock screen, user can retry */ }
                            is BiometricResult.Error -> finish()
                            is BiometricResult.Lockout -> finish()
                        }
                    }
                } else {
                    TermexApp()
                }
            }
        }
    }
}
