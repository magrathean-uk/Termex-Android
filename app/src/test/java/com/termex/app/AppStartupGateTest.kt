package com.termex.app

import com.termex.app.core.security.AppLockUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppStartupGateTest {

    @Test
    fun `startup gate routes onboarding first then main`() {
        assertEquals(
            StartupGate.ONBOARDING,
            resolveStartupGate(hasCompletedOnboarding = false)
        )
        assertEquals(
            StartupGate.MAIN,
            resolveStartupGate(hasCompletedOnboarding = true)
        )
    }

    @Test
    fun `lock screen only shows when app lock is both enabled and locked`() {
        assertTrue(
            shouldShowLockScreen(
                AppLockUiState(
                    isReady = true,
                    isEnabled = true,
                    isLocked = true,
                    isAuthenticating = true
                )
            )
        )
        assertFalse(
            shouldShowLockScreen(
                AppLockUiState(isReady = true, isEnabled = true, isLocked = false)
            )
        )
        assertFalse(
            shouldShowLockScreen(
                AppLockUiState(isReady = true, isEnabled = false, isLocked = true)
            )
        )
    }
}
