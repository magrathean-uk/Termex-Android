package com.termex.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.termex.app.data.prefs.ThemeMode
import com.termex.app.data.prefs.UserPreferencesRepository
import com.termex.app.ui.theme.TermexTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by userPreferencesRepository.themeFlow.collectAsState(initial = ThemeMode.AUTO)
            val systemDark = isSystemInDarkTheme()
            
            val useDarkTheme = when (themeMode) {
                ThemeMode.AUTO -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            
            TermexTheme(useDarkTheme = useDarkTheme) {
                TermexApp()
            }
        }
    }
}
