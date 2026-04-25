package com.termex.app

import android.content.Context
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.termex.app.core.PersistedRootRoute
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import kotlinx.coroutines.runBlocking

@HiltAndroidTest
@UninstallModules(SecurityModule::class)
@RunWith(AndroidJUnit4::class)
class ReleaseSurfaceProofTest {

    private val hiltRule = HiltAndroidRule(this)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @BindValue
    @JvmField
    val biometricAuthGateway: BiometricAuthGateway = ReleaseSurfaceFakeBiometricAuthGateway()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(hiltRule).around(composeRule)

    @Before
    fun setUp() {
        hiltRule.inject()
        resetAppState()
    }

    @Test
    fun newInstallUsersReachMainAfterOnboarding() {
        resetAppState()
        relaunchApp()

        composeRule.waitUntil(20_000) { hasNodeWithTag(AutomationTags.ONBOARDING_SKIP) }
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_SKIP).performClick()
        composeRule.waitUntil(20_000) { hasNodeWithTag(AutomationTags.ONBOARDING_PRIMARY) }
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_PRIMARY).performClick()
        composeRule.waitUntil(20_000) { hasNodeWithTag(AutomationTags.MAIN_TAB_SERVERS) }
        check(hasNodeWithTag(AutomationTags.MAIN_TAB_SERVERS))
    }

    @Test
    fun savedRouteDoesNotBlockOpenSourceUsers() {
        runBlocking {
            val prefs = entryPoint().userPreferencesRepository()
            prefs.completeOnboarding()
            prefs.setDemoModeEnabled(false)
            prefs.setPersistentSessionResumeServerId(null)
            prefs.setPersistedRootRoute(PersistedRootRoute.server("server-123"))
        }
        relaunchApp()

        composeRule.waitUntil(20_000) {
            hasNodeWithTag(AutomationTags.MAIN_TAB_SERVERS) || hasNodeWithTag(AutomationTags.TERMINAL_VIEW)
        }
        check(hasNodeWithTag(AutomationTags.MAIN_TAB_SERVERS) || hasNodeWithTag(AutomationTags.TERMINAL_VIEW))
    }

    @Test
    fun demoModeStillOpensMainAfterOnboarding() {
        resetAppState()
        relaunchApp()

        composeRule.waitUntil(20_000) { hasNodeWithTag(AutomationTags.ONBOARDING_SKIP) }
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_SKIP).performClick()
        composeRule.waitUntil(20_000) { hasNodeWithTag(AutomationTags.ONBOARDING_PRIMARY) }
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_PRIMARY).performClick()
        composeRule.waitUntil(20_000) { hasNodeWithTag(AutomationTags.MAIN_TAB_SERVERS) }
        check(hasNodeWithTag(AutomationTags.MAIN_TAB_SERVERS))
    }

    @Test
    fun appLockBlocksSubscribedUsersOnResumeWhenEnabled() {
        runBlocking {
            val prefs = entryPoint().userPreferencesRepository()
            prefs.completeOnboarding()
            prefs.setDemoModeEnabled(false)
            prefs.setBiometricLockEnabled(true)
            prefs.setPersistentSessionResumeServerId(null)
            prefs.setPersistedRootRoute(PersistedRootRoute.none())
        }
        val gateway = biometricAuthGateway as ReleaseSurfaceFakeBiometricAuthGateway
        gateway.availability = BiometricAvailability.Available
        gateway.result = BiometricResult.Canceled
        relaunchApp()

        composeRule.waitUntil(20_000) { hasNodeWithTag(AutomationTags.LOCK_SCREEN) }
        check(hasNodeWithTag(AutomationTags.LOCK_SCREEN))
        check(!hasNodeWithTag(AutomationTags.MAIN_TAB_SERVERS))
    }

    private fun resetAppState() {
        runBlocking {
            val prefs = entryPoint().userPreferencesRepository()
            prefs.resetOnboarding()
            prefs.setDemoModeEnabled(false)
            prefs.setBiometricLockEnabled(false)
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

private class ReleaseSurfaceFakeBiometricAuthGateway : BiometricAuthGateway {
    var availability: BiometricAvailability = BiometricAvailability.Available
    var result: BiometricResult = BiometricResult.Success

    override fun isBiometricAvailable(): BiometricAvailability = availability

    override suspend fun authenticate(activity: androidx.fragment.app.FragmentActivity): BiometricResult = result
}
