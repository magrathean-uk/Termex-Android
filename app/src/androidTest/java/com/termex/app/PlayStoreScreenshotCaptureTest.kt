package com.termex.app

import android.content.Context
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.billingclient.api.Purchase
import com.termex.app.core.billing.SubscriptionManager
import com.termex.app.core.billing.SubscriptionState
import com.termex.app.domain.AuthMode
import com.termex.app.domain.Server
import com.termex.app.domain.Workplace
import com.termex.app.testing.AutomationSeedBridge
import com.termex.app.ui.AutomationTags
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class PlayStoreScreenshotCaptureTest {

    private val subscriptionState = MutableStateFlow<SubscriptionState>(
        SubscriptionState.SUBSCRIBED(mockk<Purchase>(relaxed = true))
    )

    @BindValue
    @JvmField
    val subscriptionManager: SubscriptionManager = mockk<SubscriptionManager>(relaxed = true).also {
        every { it.subscriptionState } returns subscriptionState
        every { it.querySubscriptionStatus() } returns Unit
    }

    private val hiltRule = HiltAndroidRule(this)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(hiltRule).around(composeRule)

    @Before
    fun setUp() {
        hiltRule.inject()
        clearOutput()
    }

    @Test
    fun capturePhoneStoreScreens() {
        resetAppState()

        waitForTag(AutomationTags.ONBOARDING_STEP_WELCOME)
        capture("01-onboarding-welcome")

        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_PRIMARY).performClick()
        waitForTag(AutomationTags.ONBOARDING_STEP_DESTINATION)
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_SERVER_NAME).performTextInput("Prod Cluster")
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_SERVER_HOST).performTextInput("prod.example.com")
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_SERVER_PORT).performTextReplacement("22")
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_SERVER_USERNAME).performTextInput("deploy")
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_PRIMARY).performClick()

        waitForTag(AutomationTags.ONBOARDING_STEP_AUTH)
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_AUTH_MODE).performClick()
        clickText("Password")
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_AUTH_PASSWORD).performTextInput("hunter2")
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_PRIMARY).performClick()

        waitForTag(AutomationTags.ONBOARDING_STEP_JUMP)
        composeRule.onNodeWithTag("onboarding_jump_mode_custom").performClick()
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_JUMP_NAME).performTextInput("Bastion")
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_JUMP_HOST).performTextInput("jump.example.com")
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_JUMP_PORT).performTextReplacement("22")
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_JUMP_USERNAME).performTextInput("ops")
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_PRIMARY).performClick()

        waitForTag(AutomationTags.ONBOARDING_STEP_FORWARDING)
        addForward(type = "LOCAL", localPort = "8080", remoteHost = "127.0.0.1", remotePort = "80")
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_PERSISTENT_TMUX).performClick()
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_STARTUP_COMMAND).performTextInput("cd /srv/app && tmux attach")
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_PRIMARY).performClick()

        waitForTag(AutomationTags.ONBOARDING_STEP_REVIEW)
        capture("02-onboarding-review")

        seedOverviewState()

        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_PRIMARY).performClick()
        waitForTag(AutomationTags.MAIN_TAB_SERVERS)
        composeRule.waitUntil(10_000) { hasTextNode("Production Workspace") }
        capture("03-servers-overview")

        composeRule.onNodeWithTag(AutomationTags.MAIN_TAB_SETTINGS).performClick()
        waitForTag(AutomationTags.SETTINGS_SCREEN)
        composeRule.onNodeWithTag(AutomationTags.SETTINGS_SCREEN)
            .performScrollToNode(hasTestTag(AutomationTags.SETTINGS_EXTRA_KEYS_ENTRY))
        capture("04-settings-overview")

        composeRule.onNodeWithTag(AutomationTags.SETTINGS_EXTRA_KEYS_ENTRY).performClick()
        waitForTag(AutomationTags.EXTRA_KEYS_SCREEN)
        composeRule.onNodeWithTag(AutomationTags.EXTRA_KEYS_SCREEN)
            .performScrollToNode(hasTestTag(AutomationTags.extraKeyRowTag("esc")))
        capture("05-extra-keys")
    }

    private fun addForward(
        type: String,
        localPort: String,
        remoteHost: String,
        remotePort: String
    ) {
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_FORWARD_ADD).performClick()
        if (type != "LOCAL") {
            composeRule.onNodeWithTag(AutomationTags.PORT_FORWARD_TYPE).performClick()
            clickText(type)
        }
        composeRule.onNodeWithTag(AutomationTags.PORT_FORWARD_LOCAL_PORT).performTextInput(localPort)
        composeRule.onNodeWithTag(AutomationTags.PORT_FORWARD_REMOTE_HOST)
            .performTextReplacement(remoteHost)
        composeRule.onNodeWithTag(AutomationTags.PORT_FORWARD_REMOTE_PORT).performTextInput(remotePort)
        composeRule.onNodeWithTag(AutomationTags.PORT_FORWARD_SAVE).performClick()
    }

    private fun seedOverviewState() {
        runBlocking {
            val bridge = entryPoint()
            val workplace = Workplace(name = "Production Workspace")
            bridge.workplaceRepository().addWorkplace(workplace)
            bridge.serverRepository().addServer(
                Server(
                    id = UUID.randomUUID().toString(),
                    name = "Analytics API",
                    hostname = "api.example.com",
                    port = 22,
                    username = "deploy",
                    authMode = AuthMode.KEY,
                    workplaceId = workplace.id,
                    persistentSessionEnabled = true,
                    startupCommand = "cd /srv/app"
                )
            )
            bridge.serverRepository().addServer(
                Server(
                    id = UUID.randomUUID().toString(),
                    name = "Logs Node",
                    hostname = "logs.example.com",
                    port = 22,
                    username = "root",
                    authMode = AuthMode.KEY,
                    workplaceId = workplace.id
                )
            )
            bridge.serverRepository().addServer(
                Server(
                    id = UUID.randomUUID().toString(),
                    name = "Staging API",
                    hostname = "staging.example.com",
                    port = 2222,
                    username = "deploy",
                    authMode = AuthMode.PASSWORD
                )
            )
        }
    }

    private fun resetAppState() {
        runBlocking {
            val prefs = entryPoint().userPreferencesRepository()
            prefs.resetOnboarding()
            prefs.setDemoModeEnabled(false)
            prefs.setPersistentSessionResumeServerId(null)
        }
    }

    private fun clearOutput() {
        executeShell("rm -rf /sdcard/Download/termex-playstore")
        executeShell("mkdir -p /sdcard/Download/termex-playstore")
    }

    private fun capture(name: String) {
        composeRule.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(300)
        executeShell("screencap -p /sdcard/Download/termex-playstore/$name.png")
    }

    private fun waitForTag(tag: String) {
        composeRule.waitUntil(20_000) { hasNodeWithTag(tag) }
    }

    private fun clickText(text: String) {
        composeRule.waitUntil(10_000) { hasTextNode(text) }
        composeRule.onNodeWithText(text, substring = false).performClick()
    }

    private fun hasNodeWithTag(tag: String): Boolean {
        return composeRule.onAllNodes(hasTestTag(tag), useUnmergedTree = true)
            .fetchSemanticsNodes().isNotEmpty()
    }

    private fun hasTextNode(text: String): Boolean {
        return composeRule.onAllNodes(hasText(text), useUnmergedTree = true)
            .fetchSemanticsNodes().isNotEmpty()
    }

    private fun entryPoint() = AutomationSeedBridge.from(appContext())

    private fun appContext(): Context = composeRule.activity.applicationContext

    private fun executeShell(command: String) {
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command).close()
    }
}
