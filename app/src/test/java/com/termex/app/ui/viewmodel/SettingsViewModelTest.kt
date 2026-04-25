package com.termex.app.ui.viewmodel

import com.termex.app.core.security.AppLockCoordinator
import com.termex.app.core.security.AppLockUiState
import com.termex.app.core.security.BiometricAvailability
import com.termex.app.data.diagnostics.DiagnosticEvent
import com.termex.app.data.diagnostics.DiagnosticsRepository
import com.termex.app.data.prefs.KeepAliveInterval
import com.termex.app.data.prefs.LinkHandlingMode
import com.termex.app.data.prefs.TerminalExtraKey
import com.termex.app.data.prefs.TerminalExtraKeyPreset
import com.termex.app.data.prefs.TerminalSettings
import com.termex.app.data.prefs.ThemeMode
import com.termex.app.data.prefs.UserPreferencesRepository
import com.termex.app.ui.theme.TerminalColorScheme
import com.termex.app.data.repository.SessionRepository
import com.termex.app.domain.SessionState
import com.termex.app.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `enabling biometric lock refreshes availability when biometrics are unavailable`() = runTest {
        val userPreferencesRepository = mockk<UserPreferencesRepository>()
        val appLockCoordinator = mockk<AppLockCoordinator>(relaxed = true)
        val sessionRepository = mockk<SessionRepository>()
        val diagnosticsRepository = mockk<DiagnosticsRepository>()

        every { userPreferencesRepository.themeFlow } returns MutableStateFlow(ThemeMode.AUTO)
        every { userPreferencesRepository.terminalSettingsFlow } returns MutableStateFlow(TerminalSettings())
        every { userPreferencesRepository.linkHandlingModeFlow } returns MutableStateFlow(LinkHandlingMode.AUTOMATIC)
        every { userPreferencesRepository.terminalExtraKeyPresetFlow } returns MutableStateFlow(TerminalExtraKeyPreset.STANDARD)
        every { userPreferencesRepository.terminalExtraKeysFlow } returns MutableStateFlow(TerminalExtraKey.defaultKeys)
        every { userPreferencesRepository.keepAliveIntervalFlow } returns MutableStateFlow(KeepAliveInterval.SECONDS_30)
        every { userPreferencesRepository.demoModeEnabledFlow } returns MutableStateFlow(false)
        every { userPreferencesRepository.biometricLockEnabledFlow } returns MutableStateFlow(false)
        every { appLockCoordinator.uiState } returns MutableStateFlow(
            AppLockUiState(
                isEnabled = false,
                isLocked = false,
                biometricAvailability = BiometricAvailability.NoneEnrolled
            )
        )
        every { diagnosticsRepository.events } returns MutableStateFlow(emptyList<DiagnosticEvent>())
        every { sessionRepository.getAllSessions() } returns flowOf(emptyList<SessionState>())
        coEvery { sessionRepository.cleanupOldSessions(any()) } returns Unit
        coEvery { userPreferencesRepository.setBiometricLockEnabled(any()) } returns Unit

        val viewModel = SettingsViewModel(
            userPreferencesRepository = userPreferencesRepository,
            appLockCoordinator = appLockCoordinator,
            sessionRepository = sessionRepository,
            diagnosticsRepository = diagnosticsRepository
        )

        viewModel.setBiometricLockEnabled(true)
        advanceUntilIdle()

        verify(exactly = 1) { appLockCoordinator.refreshAvailability() }
        coVerify(exactly = 0) { userPreferencesRepository.setBiometricLockEnabled(true) }
    }

    @Test
    fun `enabling biometric lock persists when biometrics are available`() = runTest {
        val userPreferencesRepository = mockk<UserPreferencesRepository>()
        val appLockCoordinator = mockk<AppLockCoordinator>(relaxed = true)
        val sessionRepository = mockk<SessionRepository>()
        val diagnosticsRepository = mockk<DiagnosticsRepository>()

        every { userPreferencesRepository.themeFlow } returns MutableStateFlow(ThemeMode.AUTO)
        every { userPreferencesRepository.terminalSettingsFlow } returns MutableStateFlow(TerminalSettings())
        every { userPreferencesRepository.linkHandlingModeFlow } returns MutableStateFlow(LinkHandlingMode.AUTOMATIC)
        every { userPreferencesRepository.terminalExtraKeyPresetFlow } returns MutableStateFlow(TerminalExtraKeyPreset.STANDARD)
        every { userPreferencesRepository.terminalExtraKeysFlow } returns MutableStateFlow(TerminalExtraKey.defaultKeys)
        every { userPreferencesRepository.keepAliveIntervalFlow } returns MutableStateFlow(KeepAliveInterval.SECONDS_30)
        every { userPreferencesRepository.demoModeEnabledFlow } returns MutableStateFlow(false)
        every { userPreferencesRepository.biometricLockEnabledFlow } returns MutableStateFlow(false)
        every { appLockCoordinator.uiState } returns MutableStateFlow(
            AppLockUiState(
                isEnabled = false,
                isLocked = false,
                biometricAvailability = BiometricAvailability.Available
            )
        )
        every { diagnosticsRepository.events } returns MutableStateFlow(emptyList<DiagnosticEvent>())
        every { sessionRepository.getAllSessions() } returns flowOf(emptyList<SessionState>())
        coEvery { sessionRepository.cleanupOldSessions(any()) } returns Unit
        coEvery { userPreferencesRepository.setBiometricLockEnabled(any()) } returns Unit

        val viewModel = SettingsViewModel(
            userPreferencesRepository = userPreferencesRepository,
            appLockCoordinator = appLockCoordinator,
            sessionRepository = sessionRepository,
            diagnosticsRepository = diagnosticsRepository
        )

        viewModel.setBiometricLockEnabled(true)
        advanceUntilIdle()

        verify(exactly = 1) { appLockCoordinator.refreshAvailability() }
        coVerify(exactly = 1) { userPreferencesRepository.setBiometricLockEnabled(true) }
    }

    @Test
    fun `terminal polish setters persist through repository`() = runTest {
        val userPreferencesRepository = mockk<UserPreferencesRepository>()
        val appLockCoordinator = mockk<AppLockCoordinator>(relaxed = true)
        val sessionRepository = mockk<SessionRepository>()
        val diagnosticsRepository = mockk<DiagnosticsRepository>()

        every { userPreferencesRepository.themeFlow } returns MutableStateFlow(ThemeMode.AUTO)
        every { userPreferencesRepository.terminalSettingsFlow } returns MutableStateFlow(TerminalSettings())
        every { userPreferencesRepository.linkHandlingModeFlow } returns MutableStateFlow(LinkHandlingMode.AUTOMATIC)
        every { userPreferencesRepository.terminalExtraKeyPresetFlow } returns MutableStateFlow(TerminalExtraKeyPreset.STANDARD)
        every { userPreferencesRepository.terminalExtraKeysFlow } returns MutableStateFlow(TerminalExtraKey.defaultKeys)
        every { userPreferencesRepository.keepAliveIntervalFlow } returns MutableStateFlow(KeepAliveInterval.SECONDS_30)
        every { userPreferencesRepository.demoModeEnabledFlow } returns MutableStateFlow(false)
        every { userPreferencesRepository.biometricLockEnabledFlow } returns MutableStateFlow(false)
        every { appLockCoordinator.uiState } returns MutableStateFlow(
            AppLockUiState(
                isEnabled = false,
                isLocked = false,
                biometricAvailability = BiometricAvailability.Available
            )
        )
        every { diagnosticsRepository.events } returns MutableStateFlow(emptyList<DiagnosticEvent>())
        every { sessionRepository.getAllSessions() } returns flowOf(emptyList<SessionState>())
        coEvery { sessionRepository.cleanupOldSessions(any()) } returns Unit
        coEvery { userPreferencesRepository.setTerminalSettings(any()) } returns Unit
        coEvery { userPreferencesRepository.setLinkHandlingMode(any()) } returns Unit
        coEvery { userPreferencesRepository.setTerminalExtraKeyPreset(any()) } returns Unit
        coEvery { userPreferencesRepository.setTerminalExtraKeyIds(any()) } returns Unit

        val viewModel = SettingsViewModel(
            userPreferencesRepository = userPreferencesRepository,
            appLockCoordinator = appLockCoordinator,
            sessionRepository = sessionRepository,
            diagnosticsRepository = diagnosticsRepository
        )

        viewModel.setColorScheme(TerminalColorScheme.OCEAN)
        viewModel.setLinkHandlingMode(LinkHandlingMode.ASK_FIRST)
        viewModel.setTerminalExtraKeyPreset(TerminalExtraKeyPreset.CODING)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            userPreferencesRepository.setTerminalSettings(TerminalSettings(colorScheme = TerminalColorScheme.OCEAN.raw))
        }
        coVerify(exactly = 1) { userPreferencesRepository.setLinkHandlingMode(LinkHandlingMode.ASK_FIRST) }
        coVerify(exactly = 1) { userPreferencesRepository.setTerminalExtraKeyPreset(TerminalExtraKeyPreset.CODING) }

        viewModel.setTerminalExtraKeyIds(listOf("esc", "tab"))
        advanceUntilIdle()

        coVerify(exactly = 1) { userPreferencesRepository.setTerminalExtraKeyIds(listOf("esc", "tab")) }
    }
}
