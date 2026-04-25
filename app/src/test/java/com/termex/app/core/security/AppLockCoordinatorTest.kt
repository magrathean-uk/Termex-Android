package com.termex.app.core.security

import androidx.fragment.app.FragmentActivity
import com.termex.app.data.prefs.UserPreferencesRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppLockCoordinatorTest {

    @Test
    fun `enabling lock immediately enters locked state`() = runTest {
        val enabledFlow = MutableStateFlow(false)
        val userPreferencesRepository = mockk<UserPreferencesRepository>()
        val biometricAuthManager = mockk<BiometricAuthGateway>()

        every { userPreferencesRepository.biometricLockEnabledFlow } returns enabledFlow
        every { biometricAuthManager.isBiometricAvailable() } returns BiometricAvailability.Available

        val coordinator = AppLockCoordinator(userPreferencesRepository, biometricAuthManager)
        enabledFlow.value = true

        waitUntil { coordinator.uiState.value.isEnabled && coordinator.uiState.value.isLocked }

        assertTrue(coordinator.uiState.value.isEnabled)
        assertTrue(coordinator.uiState.value.isLocked)
    }

    @Test
    fun `successful unlock relocks after grace period in background`() = runTest {
        val enabledFlow = MutableStateFlow(true)
        val userPreferencesRepository = mockk<UserPreferencesRepository>()
        val biometricAuthManager = mockk<BiometricAuthGateway>()
        val activity = mockk<FragmentActivity>(relaxed = true)

        every { userPreferencesRepository.biometricLockEnabledFlow } returns enabledFlow
        every { biometricAuthManager.isBiometricAvailable() } returns BiometricAvailability.Available
        coEvery { biometricAuthManager.authenticate(any()) } returns BiometricResult.Success

        val coordinator = AppLockCoordinator(userPreferencesRepository, biometricAuthManager)
        waitUntil { coordinator.uiState.value.isEnabled && coordinator.uiState.value.isLocked }

        coordinator.unlock(activity)
        waitUntil { !coordinator.uiState.value.isLocked }

        coordinator.onAppBackgrounded(atElapsedRealtime = 0)
        coordinator.onAppForegrounded(atElapsedRealtime = 30_001)

        assertTrue(coordinator.uiState.value.isLocked)
    }

    @Test
    fun `successful unlock stays unlocked within grace period`() = runTest {
        val enabledFlow = MutableStateFlow(true)
        val userPreferencesRepository = mockk<UserPreferencesRepository>()
        val biometricAuthManager = mockk<BiometricAuthGateway>()
        val activity = mockk<FragmentActivity>(relaxed = true)

        every { userPreferencesRepository.biometricLockEnabledFlow } returns enabledFlow
        every { biometricAuthManager.isBiometricAvailable() } returns BiometricAvailability.Available
        coEvery { biometricAuthManager.authenticate(any()) } returns BiometricResult.Success

        val coordinator = AppLockCoordinator(userPreferencesRepository, biometricAuthManager)
        waitUntil { coordinator.uiState.value.isEnabled && coordinator.uiState.value.isLocked }

        coordinator.unlock(activity)
        waitUntil { !coordinator.uiState.value.isLocked }

        coordinator.onAppBackgrounded(atElapsedRealtime = 0)
        coordinator.onAppForegrounded(atElapsedRealtime = 29_999)

        assertFalse(coordinator.uiState.value.isLocked)
    }

    @Test
    fun `none enrolled availability keeps lock screen up`() = runTest {
        val enabledFlow = MutableStateFlow(true)
        val userPreferencesRepository = mockk<UserPreferencesRepository>()
        val biometricAuthManager = mockk<BiometricAuthGateway>()
        val activity = mockk<FragmentActivity>(relaxed = true)

        every { userPreferencesRepository.biometricLockEnabledFlow } returns enabledFlow
        every { biometricAuthManager.isBiometricAvailable() } returns BiometricAvailability.NoneEnrolled

        val coordinator = AppLockCoordinator(userPreferencesRepository, biometricAuthManager)
        waitUntil { coordinator.uiState.value.isEnabled && coordinator.uiState.value.isLocked }

        val result = coordinator.unlock(activity)

        assertTrue(result is BiometricResult.Error)
        assertTrue(coordinator.uiState.value.isLocked)
        assertTrue(coordinator.uiState.value.errorMessage?.contains("Settings") == true)
    }

    @Test
    fun `canceled unlock keeps lock screen up`() = runTest {
        val enabledFlow = MutableStateFlow(true)
        val userPreferencesRepository = mockk<UserPreferencesRepository>()
        val biometricAuthManager = mockk<BiometricAuthGateway>()
        val activity = mockk<FragmentActivity>(relaxed = true)

        every { userPreferencesRepository.biometricLockEnabledFlow } returns enabledFlow
        every { biometricAuthManager.isBiometricAvailable() } returns BiometricAvailability.Available
        coEvery { biometricAuthManager.authenticate(any()) } returns BiometricResult.Canceled

        val coordinator = AppLockCoordinator(userPreferencesRepository, biometricAuthManager)
        waitUntil { coordinator.uiState.value.isEnabled && coordinator.uiState.value.isLocked }

        val result = coordinator.unlock(activity)

        assertTrue(result is BiometricResult.Canceled)
        assertTrue(coordinator.uiState.value.isLocked)
        assertTrue(coordinator.uiState.value.errorMessage?.contains("cancelled") == true)
    }

    @Test
    fun `error unlock keeps lock screen up`() = runTest {
        val enabledFlow = MutableStateFlow(true)
        val userPreferencesRepository = mockk<UserPreferencesRepository>()
        val biometricAuthManager = mockk<BiometricAuthGateway>()
        val activity = mockk<FragmentActivity>(relaxed = true)

        every { userPreferencesRepository.biometricLockEnabledFlow } returns enabledFlow
        every { biometricAuthManager.isBiometricAvailable() } returns BiometricAvailability.Available
        coEvery { biometricAuthManager.authenticate(any()) } returns BiometricResult.Error("boom")

        val coordinator = AppLockCoordinator(userPreferencesRepository, biometricAuthManager)
        waitUntil { coordinator.uiState.value.isEnabled && coordinator.uiState.value.isLocked }

        val result = coordinator.unlock(activity)

        assertTrue(result is BiometricResult.Error)
        assertTrue(coordinator.uiState.value.isLocked)
        assertTrue(coordinator.uiState.value.errorMessage == "boom")
    }

    @Test
    fun `lockout unlock keeps lock screen up`() = runTest {
        val enabledFlow = MutableStateFlow(true)
        val userPreferencesRepository = mockk<UserPreferencesRepository>()
        val biometricAuthManager = mockk<BiometricAuthGateway>()
        val activity = mockk<FragmentActivity>(relaxed = true)

        every { userPreferencesRepository.biometricLockEnabledFlow } returns enabledFlow
        every { biometricAuthManager.isBiometricAvailable() } returns BiometricAvailability.Available
        coEvery { biometricAuthManager.authenticate(any()) } returns BiometricResult.Lockout("locked out")

        val coordinator = AppLockCoordinator(userPreferencesRepository, biometricAuthManager)
        waitUntil { coordinator.uiState.value.isEnabled && coordinator.uiState.value.isLocked }

        val result = coordinator.unlock(activity)

        assertTrue(result is BiometricResult.Lockout)
        assertTrue(coordinator.uiState.value.isLocked)
        assertTrue(coordinator.uiState.value.errorMessage == "locked out")
    }

    private suspend fun waitUntil(predicate: () -> Boolean) {
        withTimeout(1_000) {
            while (!predicate()) {
                delay(10)
            }
        }
    }
}
