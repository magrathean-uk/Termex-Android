package com.termex.app.core

import android.content.Context
import androidx.room.Room
import com.termex.app.data.local.TermexDatabase
import com.termex.app.data.prefs.UserPreferences
import com.termex.app.data.repository.ServerRepositoryImpl
import com.termex.app.domain.ServerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppEnvironment private constructor(
    private val context: Context
) {
    // Database
    val database: TermexDatabase by lazy {
        Room.databaseBuilder(
            context,
            TermexDatabase::class.java, "termex-database"
        ).build()
    }

    // Repositories
    val serverRepository: ServerRepository by lazy {
        ServerRepositoryImpl(database.serverDao())
    }

    // Preferences
    val userPreferences: UserPreferences by lazy {
        UserPreferences.from(context)
    }

    // Onboarding State
    private val _hasCompletedOnboarding = MutableStateFlow(false)
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding.asStateFlow()

    init {
        loadOnboardingState()
    }

    private fun loadOnboardingState() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _hasCompletedOnboarding.value = prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false)
    }

    fun completeOnboarding() {
        _hasCompletedOnboarding.value = true
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ONBOARDING_COMPLETE, true)
            .apply()
    }

    fun resetOnboarding() {
        _hasCompletedOnboarding.value = false
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ONBOARDING_COMPLETE, false)
            .apply()
    }

    companion object {
        @Volatile
        private var INSTANCE: AppEnvironment? = null
        private const val PREFS_NAME = "termex_app_state"
        private const val KEY_ONBOARDING_COMPLETE = "hasCompletedOnboarding"

        fun getInstance(context: Context): AppEnvironment {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppEnvironment(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}