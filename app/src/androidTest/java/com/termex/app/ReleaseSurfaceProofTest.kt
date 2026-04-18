package com.termex.app

import android.content.Context
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.Purchase
import com.termex.app.core.PersistedRootRoute
import com.termex.app.core.billing.SubscriptionManager
import com.termex.app.core.billing.SubscriptionState
import com.termex.app.testing.AutomationSeedBridge
import com.termex.app.ui.AutomationTags
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ReleaseSurfaceProofTest {

    private val hiltRule = HiltAndroidRule(this)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val subscriptionState = MutableStateFlow<SubscriptionState>(
        SubscriptionState.NOT_SUBSCRIBED
    )

    private val subscriptionManagerMock = mockk<SubscriptionManager>(relaxed = true).also {
        every { it.subscriptionState } returns subscriptionState
        every { it.querySubscriptionStatus() } returns Unit
    }

    @BindValue
    @JvmField
    val subscriptionManager: SubscriptionManager = subscriptionManagerMock

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(hiltRule).around(composeRule)

    @Before
    fun setUp() {
        hiltRule.inject()
        resetAppState()
    }

    @Test
    fun newInstallSubscribedUsersReachMainAfterOnboarding() {
        resetAppState()
        subscriptionState.value = SubscriptionState.SUBSCRIBED(mockk<Purchase>(relaxed = true))
        relaunchApp()

        composeRule.waitUntil(20_000) { hasNodeWithTag(AutomationTags.ONBOARDING_SKIP) }
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_SKIP).performClick()
        composeRule.waitUntil(20_000) { hasNodeWithTag(AutomationTags.ONBOARDING_PRIMARY) }
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_PRIMARY).performClick()
        composeRule.waitUntil(20_000) { hasNodeWithTag(AutomationTags.MAIN_TAB_SERVERS) }
        check(hasNodeWithTag(AutomationTags.MAIN_TAB_SERVERS))
        check(!hasNodeWithTag(AutomationTags.PAYWALL_SCREEN))
    }

    @Test
    fun newInstallUnsubscribedUsersReachPaywallAfterOnboarding() {
        resetAppState()
        subscriptionState.value = SubscriptionState.NOT_SUBSCRIBED
        relaunchApp()

        composeRule.waitUntil(20_000) { hasNodeWithTag(AutomationTags.ONBOARDING_SKIP) }
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_SKIP).performClick()
        composeRule.waitUntil(20_000) { hasNodeWithTag(AutomationTags.ONBOARDING_PRIMARY) }
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_PRIMARY).performClick()
        composeRule.waitUntil(20_000) { hasNodeWithTag(AutomationTags.PAYWALL_SCREEN) }
        check(hasNodeWithTag(AutomationTags.PAYWALL_SCREEN))
        check(!hasNodeWithTag(AutomationTags.MAIN_TAB_SERVERS))
    }

    @Test
    fun savedRouteStillStopsAtPaywallWhenUnsubscribed() {
        runBlocking {
            val prefs = entryPoint().userPreferencesRepository()
            prefs.completeOnboarding()
            prefs.setDemoModeEnabled(false)
            prefs.setPersistentSessionResumeServerId(null)
            prefs.setPersistedRootRoute(PersistedRootRoute.server("server-123"))
        }
        subscriptionState.value = SubscriptionState.NOT_SUBSCRIBED
        relaunchApp()

        composeRule.waitUntil(20_000) { hasNodeWithTag(AutomationTags.PAYWALL_SCREEN) }
        check(hasNodeWithTag(AutomationTags.PAYWALL_SCREEN))
        check(!hasNodeWithTag(AutomationTags.MAIN_TAB_SERVERS))
    }

    private fun resetAppState() {
        runBlocking {
            val prefs = entryPoint().userPreferencesRepository()
            prefs.resetOnboarding()
            prefs.setDemoModeEnabled(false)
            prefs.setPersistentSessionResumeServerId(null)
            prefs.setPersistedRootRoute(PersistedRootRoute.none())
        }
    }

    private fun relaunchApp() {
        composeRule.activity.runOnUiThread {
            composeRule.activity.recreate()
        }
    }

    private fun entryPoint() = AutomationSeedBridge.from(appContext())

    private fun appContext(): Context = composeRule.activity.applicationContext

    private fun hasNodeWithTag(tag: String): Boolean {
        return composeRule.onAllNodes(hasTestTag(tag), useUnmergedTree = true)
            .fetchSemanticsNodes().isNotEmpty()
    }
}
