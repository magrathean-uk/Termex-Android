package com.termex.app

import com.termex.app.core.billing.SubscriptionState
import com.termex.app.core.security.AppLockUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppStartupGateTest {

    @Test
    fun `startup gate prefers onboarding then paywall then main`() {
        assertEquals(
            StartupGate.ONBOARDING,
            resolveStartupGate(
                hasCompletedOnboarding = false,
                subscriptionState = SubscriptionState.NOT_SUBSCRIBED,
                demoModeActive = false,
                paywallBypassed = false
            )
        )
        assertEquals(
            StartupGate.PAYWALL,
            resolveStartupGate(
                hasCompletedOnboarding = true,
                subscriptionState = SubscriptionState.NOT_SUBSCRIBED,
                demoModeActive = false,
                paywallBypassed = false
            )
        )
        assertEquals(
            StartupGate.MAIN,
            resolveStartupGate(
                hasCompletedOnboarding = true,
                subscriptionState = SubscriptionState.SUBSCRIBED(mockkPurchase()),
                demoModeActive = false,
                paywallBypassed = false
            )
        )
    }

    @Test
    fun `startup gate keeps loading state from flashing paywall`() {
        assertEquals(
            StartupGate.LOADING,
            resolveStartupGate(
                hasCompletedOnboarding = true,
                subscriptionState = SubscriptionState.LOADING,
                demoModeActive = false,
                paywallBypassed = false
            )
        )
    }

    @Test
    fun `loading gate falls through to main when demo mode or paywall bypass is active`() {
        assertEquals(
            StartupGate.MAIN,
            resolveStartupGate(
                hasCompletedOnboarding = true,
                subscriptionState = SubscriptionState.LOADING,
                demoModeActive = true,
                paywallBypassed = false
            )
        )
        assertEquals(
            StartupGate.MAIN,
            resolveStartupGate(
                hasCompletedOnboarding = true,
                subscriptionState = SubscriptionState.LOADING,
                demoModeActive = false,
                paywallBypassed = true
            )
        )
    }

    @Test
    fun `onboarding still wins over loading`() {
        assertEquals(
            StartupGate.ONBOARDING,
            resolveStartupGate(
                hasCompletedOnboarding = false,
                subscriptionState = SubscriptionState.LOADING,
                demoModeActive = false,
                paywallBypassed = false
            )
        )
    }

    @Test
    fun `demo mode and paywall bypass both route to main`() {
        assertEquals(
            StartupGate.MAIN,
            resolveStartupGate(
                hasCompletedOnboarding = true,
                subscriptionState = SubscriptionState.NOT_SUBSCRIBED,
                demoModeActive = true,
                paywallBypassed = false
            )
        )
        assertEquals(
            StartupGate.MAIN,
            resolveStartupGate(
                hasCompletedOnboarding = true,
                subscriptionState = SubscriptionState.NOT_SUBSCRIBED,
                demoModeActive = false,
                paywallBypassed = true
            )
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

    private fun mockkPurchase() = io.mockk.mockk<com.android.billingclient.api.Purchase>()
}
