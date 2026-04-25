package com.termex.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.termex.app.core.security.AppLockCoordinator
import com.termex.app.core.security.AppLockUiState
import com.termex.app.data.prefs.ThemeMode
import com.termex.app.data.prefs.UserPreferencesRepository
import com.termex.app.ui.screens.BiometricLockScreen
import com.termex.app.ui.theme.TermexTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    @Inject
    lateinit var appLockCoordinator: AppLockCoordinator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by userPreferencesRepository.themeFlow.collectAsState(initial = ThemeMode.AUTO)
            val appLockState by appLockCoordinator.uiState.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = when (themeMode) {
                ThemeMode.AUTO -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            var authTrigger by remember { mutableStateOf(0) }
            var autoAttemptedForCurrentLock by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                appLockCoordinator.refreshAvailability()
            }

            LaunchedEffect(appLockState.isEnabled, appLockState.isLocked) {
                if (appLockState.isEnabled && appLockState.isLocked) {
                    if (!autoAttemptedForCurrentLock) {
                        autoAttemptedForCurrentLock = true
                        authTrigger++
                    }
                } else {
                    autoAttemptedForCurrentLock = false
                }
            }

            LaunchedEffect(authTrigger) {
                if (authTrigger > 0 && appLockState.isEnabled && appLockState.isLocked) {
                    appLockCoordinator.unlock(this@MainActivity)
                }
            }

            TermexTheme(useDarkTheme = useDarkTheme) {
                when {
                    !appLockState.isReady -> {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {}
                    }
                    shouldShowLockScreen(appLockState) -> {
                    BiometricLockScreen(
                        onAuthenticate = { authTrigger++ },
                        onCancel = { finishAffinity() },
                        errorMessage = appLockState.errorMessage,
                        isAuthenticating = appLockState.isAuthenticating
                    )
                    }
                    else -> {
                        TermexApp()
                    }
                }
            }
        }
    }
}

internal fun shouldShowLockScreen(state: AppLockUiState): Boolean {
    return state.isEnabled && state.isLocked
}
