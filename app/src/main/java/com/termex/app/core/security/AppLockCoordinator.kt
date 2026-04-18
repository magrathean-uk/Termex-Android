package com.termex.app.core.security

import android.os.SystemClock
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.termex.app.data.prefs.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class AppLockUiState(
    val isReady: Boolean = false,
    val isEnabled: Boolean = false,
    val isLocked: Boolean = false,
    val isAuthenticating: Boolean = false,
    val biometricAvailability: BiometricAvailability = BiometricAvailability.Unknown,
    val errorMessage: String? = null
)

@Singleton
class AppLockCoordinator @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val biometricAuthManager: BiometricAuthGateway
) : DefaultLifecycleObserver {

    companion object {
        internal const val LOCK_GRACE_PERIOD_MS = 30_000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(AppLockUiState())
    val uiState: StateFlow<AppLockUiState> = _uiState.asStateFlow()

    @Volatile
    private var lastBackgroundElapsedRealtime: Long? = null

    @Volatile
    private var lifecycleAttached = false

    @Volatile
    private var lastKnownEnabled: Boolean? = null

    init {
        scope.launch {
            userPreferencesRepository.biometricLockEnabledFlow.collect { enabled ->
                handleEnabledChanged(enabled)
            }
        }
    }

    fun attachProcessLifecycle() {
        if (lifecycleAttached) return
        lifecycleAttached = true
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        onAppForegrounded()
    }

    override fun onStop(owner: LifecycleOwner) {
        onAppBackgrounded()
    }

    fun refreshAvailability() {
        updateAvailabilityState()
    }

    suspend fun unlock(activity: FragmentActivity): BiometricResult {
        val stateBefore = uiState.value
        if (!stateBefore.isEnabled) {
            _uiState.update { it.copy(isLocked = false, isAuthenticating = false, errorMessage = null) }
            return BiometricResult.Success
        }
        if (stateBefore.isAuthenticating) {
            return BiometricResult.Canceled
        }

        val availability = biometricAuthManager.isBiometricAvailable()
        _uiState.update {
            it.copy(
                biometricAvailability = availability,
                errorMessage = availability.userFacingMessageOrNull(),
                isAuthenticating = false
            )
        }
        if (availability != BiometricAvailability.Available) {
            lockNow(errorMessage = availability.userFacingMessageOrNull())
            return BiometricResult.Error(availability.userFacingMessageOrNull() ?: "Unlock unavailable")
        }

        _uiState.update {
            it.copy(
                isAuthenticating = true,
                errorMessage = null,
                biometricAvailability = availability
            )
        }

        return when (val result = biometricAuthManager.authenticate(activity)) {
            BiometricResult.Success -> {
                lastBackgroundElapsedRealtime = null
                _uiState.update {
                    it.copy(
                        isLocked = false,
                        isAuthenticating = false,
                        errorMessage = null,
                        biometricAvailability = availability
                    )
                }
                result
            }
            BiometricResult.Canceled -> {
                _uiState.update {
                    it.copy(
                        isLocked = true,
                        isAuthenticating = false,
                        errorMessage = "Unlock was cancelled.",
                        biometricAvailability = availability
                    )
                }
                result
            }
            is BiometricResult.Error -> {
                _uiState.update {
                    it.copy(
                        isLocked = true,
                        isAuthenticating = false,
                        errorMessage = result.message.ifBlank { "Unlock failed." },
                        biometricAvailability = availability
                    )
                }
                result
            }
            is BiometricResult.Lockout -> {
                _uiState.update {
                    it.copy(
                        isLocked = true,
                        isAuthenticating = false,
                        errorMessage = result.message.ifBlank { "Biometric unlock is temporarily locked." },
                        biometricAvailability = availability
                    )
                }
                result
            }
        }
    }

    fun lockNow(errorMessage: String? = null) {
        if (!uiState.value.isEnabled) return
        _uiState.update {
            it.copy(
                isLocked = true,
                isAuthenticating = false,
                errorMessage = errorMessage ?: it.errorMessage
            )
        }
    }

    fun onAppBackgrounded(atElapsedRealtime: Long = SystemClock.elapsedRealtime()) {
        if (!uiState.value.isEnabled) return
        lastBackgroundElapsedRealtime = atElapsedRealtime
    }

    fun onAppForegrounded(atElapsedRealtime: Long = SystemClock.elapsedRealtime()) {
        updateAvailabilityState()
        val state = uiState.value
        if (!state.isEnabled) return

        val lastBackground = lastBackgroundElapsedRealtime
        if (lastBackground == null) {
            if (lastKnownEnabled == true && state.isLocked && state.errorMessage == null) {
                _uiState.update {
                    it.copy(errorMessage = state.biometricAvailability.userFacingMessageOrNull())
                }
            }
            return
        }

        val elapsed = atElapsedRealtime - lastBackground
        lastBackgroundElapsedRealtime = null
        if (elapsed >= LOCK_GRACE_PERIOD_MS) {
            lockNow(errorMessage = uiState.value.biometricAvailability.userFacingMessageOrNull())
        }
    }

    fun settingsDescription(state: AppLockUiState = uiState.value): String {
        return when {
            state.biometricAvailability == BiometricAvailability.Available && state.isEnabled -> {
                "Require device biometrics when returning to Termex after 30 seconds in the background."
            }
            state.biometricAvailability == BiometricAvailability.Available -> {
                "Unlock Termex with device biometrics after the app has been away for 30 seconds."
            }
            state.isEnabled -> {
                state.errorMessage ?: "Biometric unlock is currently unavailable. Termex will remain locked until authentication succeeds or you disable app lock."
            }
            else -> {
                state.biometricAvailability.userFacingMessageOrNull()
                    ?: "Biometric unlock is not available on this device right now."
            }
        }
    }

    private fun handleEnabledChanged(enabled: Boolean) {
        val availability = biometricAuthManager.isBiometricAvailable()
        val previousEnabled = lastKnownEnabled
        lastKnownEnabled = enabled

        _uiState.update { current ->
            when {
                !enabled -> current.copy(
                    isReady = true,
                    isEnabled = false,
                    isLocked = false,
                    isAuthenticating = false,
                    biometricAvailability = availability,
                    errorMessage = null
                )
                previousEnabled == null || previousEnabled == false -> current.copy(
                    isReady = true,
                    isEnabled = true,
                    isLocked = true,
                    isAuthenticating = false,
                    biometricAvailability = availability,
                    errorMessage = availability.userFacingMessageOrNull()
                )
                else -> current.copy(
                    isReady = true,
                    isEnabled = true,
                    biometricAvailability = availability,
                    errorMessage = if (current.isLocked) availability.userFacingMessageOrNull() else null
                )
            }
        }

        if (!enabled) {
            lastBackgroundElapsedRealtime = null
        }
    }

    private fun updateAvailabilityState() {
        val availability = biometricAuthManager.isBiometricAvailable()
        _uiState.update { current ->
            current.copy(
                isReady = true,
                biometricAvailability = availability,
                errorMessage = when {
                    !current.isLocked -> null
                    current.isAuthenticating -> null
                    availability == BiometricAvailability.Available -> current.errorMessage
                    else -> availability.userFacingMessageOrNull()
                }
            )
        }
    }
}

private fun BiometricAvailability.userFacingMessageOrNull(): String? = when (this) {
    BiometricAvailability.Available -> null
    BiometricAvailability.NoHardware -> "This device does not have biometric hardware."
    BiometricAvailability.HardwareUnavailable -> "Biometric hardware is unavailable right now. Try again in a moment."
    BiometricAvailability.NoneEnrolled -> "Set up device biometrics in Settings to unlock Termex."
    BiometricAvailability.SecurityUpdateRequired -> "A security update is required before biometrics can be used."
    BiometricAvailability.Unsupported -> "Biometric unlock is not supported on this device configuration."
    BiometricAvailability.Unknown -> "Biometric unlock is unavailable right now."
}
