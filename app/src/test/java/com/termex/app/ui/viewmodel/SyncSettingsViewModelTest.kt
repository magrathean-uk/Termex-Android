package com.termex.app.ui.viewmodel

import android.app.Activity
import android.content.Context
import com.termex.app.core.sync.GoogleDriveAuthorizationManager
import com.termex.app.core.sync.MetadataSyncService
import com.termex.app.core.sync.SyncAuthorizationState
import com.termex.app.data.prefs.SyncMode
import com.termex.app.data.prefs.SyncStatus
import com.termex.app.data.prefs.UserPreferencesRepository
import com.termex.app.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SyncSettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `stored account email stays disconnected when google sync hidden`() = runTest {
        val userPreferencesRepository = mockk<UserPreferencesRepository>()
        val metadataSyncService = mockk<MetadataSyncService>()
        val authorizationManager = mockk<GoogleDriveAuthorizationManager>()

        every { userPreferencesRepository.syncModeFlow } returns MutableStateFlow(SyncMode.GOOGLE_DRIVE)
        every { userPreferencesRepository.syncStatusFlow } returns MutableStateFlow(SyncStatus())
        every { userPreferencesRepository.syncGoogleAccountEmailFlow } returns MutableStateFlow("person@example.com")
        coEvery { metadataSyncService.findMissingSecretIssues() } returns emptyList()
        coEvery { userPreferencesRepository.setSyncMode(any()) } returns Unit
        coEvery { userPreferencesRepository.setSyncGoogleAccountEmail(any()) } returns Unit

        val viewModel = SyncSettingsViewModel(
            userPreferencesRepository = userPreferencesRepository,
            metadataSyncService = metadataSyncService,
            googleDriveAuthorizationManager = authorizationManager
        )
        advanceUntilIdle()

        assertEquals(SyncAuthorizationState.Disconnected, viewModel.authState.value)
    }

    @Test
    fun `connect account stays local only when google sync hidden`() = runTest {
        val userPreferencesRepository = mockk<UserPreferencesRepository>()
        val metadataSyncService = mockk<MetadataSyncService>()
        val authorizationManager = mockk<GoogleDriveAuthorizationManager>()
        val emailFlow = MutableStateFlow<String?>(null)

        every { userPreferencesRepository.syncModeFlow } returns MutableStateFlow(SyncMode.LOCAL_ONLY)
        every { userPreferencesRepository.syncStatusFlow } returns MutableStateFlow(SyncStatus())
        every { userPreferencesRepository.syncGoogleAccountEmailFlow } returns emailFlow
        coEvery { metadataSyncService.findMissingSecretIssues() } returns emptyList()
        coEvery { userPreferencesRepository.setSyncMode(any()) } returns Unit
        coEvery { userPreferencesRepository.setSyncGoogleAccountEmail(any()) } returns Unit

        val viewModel = SyncSettingsViewModel(
            userPreferencesRepository = userPreferencesRepository,
            metadataSyncService = metadataSyncService,
            googleDriveAuthorizationManager = authorizationManager
        )

        viewModel.connectAccount(mockk<Activity>())
        advanceUntilIdle()

        assertEquals(SyncAuthorizationState.Disconnected, viewModel.authState.value)
        assertEquals("Cloud sync is hidden right now.", viewModel.errorMessage.value)
        coVerify(atLeast = 1) { userPreferencesRepository.setSyncMode(SyncMode.LOCAL_ONLY) }
        coVerify(atLeast = 1) { userPreferencesRepository.setSyncGoogleAccountEmail(null) }
        coVerify(exactly = 0) { authorizationManager.signIn(any()) }
    }

    @Test
    fun `sync now stays blocked when google sync hidden`() = runTest {
        val userPreferencesRepository = mockk<UserPreferencesRepository>()
        val metadataSyncService = mockk<MetadataSyncService>()
        val authorizationManager = mockk<GoogleDriveAuthorizationManager>()
        every { userPreferencesRepository.syncModeFlow } returns MutableStateFlow(SyncMode.GOOGLE_DRIVE)
        every { userPreferencesRepository.syncStatusFlow } returns MutableStateFlow(SyncStatus())
        every { userPreferencesRepository.syncGoogleAccountEmailFlow } returns MutableStateFlow("sync@example.com")
        coEvery { metadataSyncService.findMissingSecretIssues() } returns emptyList()
        coEvery { userPreferencesRepository.setSyncMode(any()) } returns Unit
        coEvery { userPreferencesRepository.setSyncStatus(any()) } returns Unit
        coEvery { userPreferencesRepository.setSyncGoogleAccountEmail(any()) } returns Unit

        val viewModel = SyncSettingsViewModel(
            userPreferencesRepository = userPreferencesRepository,
            metadataSyncService = metadataSyncService,
            googleDriveAuthorizationManager = authorizationManager
        )

        viewModel.syncNow(mockk<Activity>())
        advanceUntilIdle()

        assertEquals(SyncAuthorizationState.Disconnected, viewModel.authState.value)
        assertEquals("Cloud sync is hidden right now.", viewModel.errorMessage.value)
        coVerify(exactly = 0) { authorizationManager.authorizeDriveAccess(any(), any()) }
        coVerify(exactly = 0) { metadataSyncService.syncWithGoogleDrive(any(), any()) }
    }

    @Test
    fun `disconnect clears email and switches to local only`() = runTest {
        val userPreferencesRepository = mockk<UserPreferencesRepository>()
        val metadataSyncService = mockk<MetadataSyncService>()
        val authorizationManager = mockk<GoogleDriveAuthorizationManager>()
        val emailFlow = MutableStateFlow<String?>("sync@example.com")

        every { userPreferencesRepository.syncModeFlow } returns MutableStateFlow(SyncMode.GOOGLE_DRIVE)
        every { userPreferencesRepository.syncStatusFlow } returns MutableStateFlow(SyncStatus())
        every { userPreferencesRepository.syncGoogleAccountEmailFlow } returns emailFlow
        coEvery { metadataSyncService.findMissingSecretIssues() } returns emptyList()
        coEvery { authorizationManager.disconnect(any()) } coAnswers { emailFlow.value = null }
        coEvery { userPreferencesRepository.setSyncGoogleAccountEmail(any()) } returns Unit
        coEvery { userPreferencesRepository.setSyncMode(any()) } returns Unit

        val viewModel = SyncSettingsViewModel(
            userPreferencesRepository = userPreferencesRepository,
            metadataSyncService = metadataSyncService,
            googleDriveAuthorizationManager = authorizationManager
        )

        viewModel.disconnect(mockk<Context>())
        advanceUntilIdle()

        assertEquals(SyncAuthorizationState.Disconnected, viewModel.authState.value)
        coVerify(atLeast = 1) { userPreferencesRepository.setSyncGoogleAccountEmail(null) }
        coVerify(atLeast = 1) { userPreferencesRepository.setSyncMode(SyncMode.LOCAL_ONLY) }
        coVerify(exactly = 0) { authorizationManager.disconnect(any()) }
    }
}
