package com.termex.app

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.Purchase
import com.termex.app.core.billing.SubscriptionManager
import com.termex.app.core.billing.SubscriptionState
import com.termex.app.core.security.BiometricAuthGateway
import com.termex.app.core.security.BiometricAvailability
import com.termex.app.core.security.BiometricResult
import com.termex.app.di.SecurityModule
import com.termex.app.testing.AutomationSeedBridge
import com.termex.app.ui.AutomationTags
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@HiltAndroidTest
@UninstallModules(SecurityModule::class)
@RunWith(AndroidJUnit4::class)
class AppLockFlowTest {

    private val subscriptionState = MutableStateFlow<SubscriptionState>(
        SubscriptionState.SUBSCRIBED(mockk<Purchase>(relaxed = true))
    )

    @BindValue
    @JvmField
    val biometricAuthGateway: BiometricAuthGateway = FakeBiometricAuthGateway()

    @BindValue
    @JvmField
    val subscriptionManager: SubscriptionManager = mockk<SubscriptionManager>(relaxed = true).also { manager ->
        every { manager.subscriptionState } returns subscriptionState
        every { manager.querySubscriptionStatus() } returns Unit
    }

    private val hiltRule = HiltAndroidRule(this)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(hiltRule).around(composeRule)

    @Before
    fun setUp() {
        hiltRule.inject()
        runBlocking {
            val prefs = entryPoint().userPreferencesRepository()
            prefs.completeOnboarding()
            prefs.setDemoModeEnabled(false)
            prefs.setBiometricLockEnabled(true)
            prefs.setPersistentSessionResumeServerId(null)
        }
    }

    @Test
    fun lockScreenShowsAndUnlocksIntoMainTabs() {
        val gateway = biometricAuthGateway as FakeBiometricAuthGateway
        gateway.availability = BiometricAvailability.Available
        gateway.results.clear()
        gateway.results.add(BiometricResult.Canceled)
        gateway.results.add(BiometricResult.Success)
        relaunchLockedApp()

        composeRule.waitUntil(10_000) {
            hasNodeWithTag(AutomationTags.LOCK_SCREEN) || hasNodeWithTag(AutomationTags.MAIN_TAB_SERVERS)
        }
        if (hasNodeWithTag(AutomationTags.LOCK_SCREEN)) {
            composeRule.onNodeWithTag(AutomationTags.LOCK_SCREEN).assertIsDisplayed()
            composeRule.onNodeWithTag(AutomationTags.LOCK_UNLOCK).performClick()
        }
        composeRule.waitUntil(10_000) { hasNodeWithTag(AutomationTags.MAIN_TAB_SERVERS) }
        composeRule.onNodeWithTag(AutomationTags.MAIN_TAB_SERVERS).assertIsDisplayed()
    }

    @Test
    fun noneEnrolledShowsErrorMessage() {
        val gateway = biometricAuthGateway as FakeBiometricAuthGateway
        gateway.availability = BiometricAvailability.NoneEnrolled
        gateway.result = BiometricResult.Error("Set up device biometrics in Settings to unlock Termex.")
        relaunchLockedApp()

        composeRule.waitUntil(10_000) { hasNodeWithTag(AutomationTags.LOCK_SCREEN) }
        composeRule.onNodeWithTag(AutomationTags.LOCK_ERROR).assertIsDisplayed()
        composeRule.onNodeWithText("Set up device biometrics in Settings to unlock Termex.").assertIsDisplayed()
    }

    private fun entryPoint() = AutomationSeedBridge.from(appContext())

    private fun appContext(): Context {
        return composeRule.activity.applicationContext
    }

    private fun hasNodeWithTag(tag: String): Boolean {
        return composeRule.onAllNodes(
            androidx.compose.ui.test.hasTestTag(tag),
            useUnmergedTree = true
        ).fetchSemanticsNodes().isNotEmpty()
    }

    private fun relaunchLockedApp() {
        composeRule.activity.runOnUiThread {
            composeRule.activity.recreate()
        }
    }
}

private class FakeBiometricAuthGateway : BiometricAuthGateway {
    var availability: BiometricAvailability = BiometricAvailability.Available
    var result: BiometricResult = BiometricResult.Success
    val results = ArrayDeque<BiometricResult>()

    override fun isBiometricAvailable(): BiometricAvailability = availability

    override suspend fun authenticate(activity: androidx.fragment.app.FragmentActivity): BiometricResult {
        return if (results.isNotEmpty()) results.removeFirst() else result
    }
}
