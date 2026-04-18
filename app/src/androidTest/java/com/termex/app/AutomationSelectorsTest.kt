package com.termex.app

import android.content.Context
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.Purchase
import com.termex.app.core.billing.SubscriptionManager
import com.termex.app.core.billing.SubscriptionState
import com.termex.app.domain.AuthMode
import com.termex.app.domain.Server
import com.termex.app.domain.Snippet
import com.termex.app.domain.Workplace
import com.termex.app.testing.AutomationSeedBridge
import com.termex.app.ui.AutomationTags
import com.termex.app.ui.screens.WorkplacesTags
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
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AutomationSelectorsTest {

    private val subscriptionState = MutableStateFlow<SubscriptionState>(
        SubscriptionState.SUBSCRIBED(mockk<Purchase>(relaxed = true))
    )

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
    }

    @Test
    fun serverSettingsExposeStableAutomationTags() {
        dismissOnboarding()

        composeRule.onNodeWithTag(AutomationTags.MAIN_TAB_SERVERS).performClick()
        composeRule.onNodeWithTag(AutomationTags.SERVERS_ADD).performClick()

        composeRule.waitUntil(10_000) { hasNodeWithTag(AutomationTags.SERVER_FIELD_HOST) }
        check(hasNodeWithTag(AutomationTags.SERVER_FIELD_DISPLAY_NAME))
        check(hasNodeWithTag(AutomationTags.SERVER_FIELD_HOST))
        check(hasNodeWithTag(AutomationTags.SERVER_FIELD_PORT))
        check(hasNodeWithTag(AutomationTags.SERVER_FIELD_USERNAME))
        check(hasNodeWithTag(AutomationTags.SERVER_AUTH_MODE))
        check(hasNodeWithTag(AutomationTags.SERVER_SAVE))

        composeRule.onNodeWithTag(AutomationTags.SERVER_AUTH_MODE).performClick()
        clickText("SSH Key")

        composeRule.waitUntil(10_000) { hasNodeWithTag(AutomationTags.SERVER_KEY_PICKER) }
        check(hasNodeWithTag(AutomationTags.SERVER_KEY_PICKER))
        check(hasNodeWithTag(AutomationTags.SERVER_CERTIFICATE_PICKER))
        check(hasNodeWithTag(AutomationTags.SERVER_JUMP_HOST_PICKER))
        check(hasNodeWithTag(AutomationTags.SERVER_FORWARD_AGENT))
        check(hasNodeWithTag(AutomationTags.SERVER_IDENTITIES_ONLY))
        check(hasNodeWithTag(AutomationTags.SERVER_PERSISTENT_SESSION))
    }

    @Test
    fun onboardingFlowExposesStableTagsAndCreatesServer() {
        restartIntoOnboarding()

        composeRule.waitUntil(10_000) { hasNodeWithTag(AutomationTags.ONBOARDING_STEP_WELCOME) }
        check(hasNodeWithTag(AutomationTags.ONBOARDING_DEMO_MODE))
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_PRIMARY).performClick()

        composeRule.waitUntil(10_000) { hasNodeWithTag(AutomationTags.ONBOARDING_STEP_DESTINATION) }
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_SERVER_NAME).performTextInput("Prod Guided")
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_SERVER_HOST).performTextInput("prod.example.com")
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_SERVER_PORT).performTextReplacement("22")
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_SERVER_USERNAME).performTextInput("root")
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_PRIMARY).performClick()

        composeRule.waitUntil(10_000) { hasNodeWithTag(AutomationTags.ONBOARDING_STEP_AUTH) }
        clickText("SSH Key")
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_IMPORT_KEY).performClick()
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_IMPORT_NAME).performTextInput("guided-key")
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_IMPORT_PRIVATE_KEY)
            .performTextInput("-----BEGIN PRIVATE KEY-----\nZmFrZQ==\n-----END PRIVATE KEY-----")
        clickText("Import")
        composeRule.waitUntil(10_000) { hasTextNode("guided-key") }

        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_IMPORT_CERTIFICATE).performClick()
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_IMPORT_NAME).performTextInput("guided-cert.pub")
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_IMPORT_CERTIFICATE_CONTENT)
            .performTextInput("ssh-ed25519-cert-v01@openssh.com AAAAC3NzaC1lZDI1NTE5AAAAIFRlc3Q= demo")
        clickText("Import")
        composeRule.waitUntil(10_000) { hasTextNode("guided-cert.pub") }
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_PRIMARY).performClick()

        composeRule.waitUntil(10_000) { hasNodeWithTag(AutomationTags.ONBOARDING_STEP_JUMP) }
        composeRule.onNodeWithTag("onboarding_jump_mode_custom").performClick()
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_JUMP_NAME).performTextInput("Bastion")
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_JUMP_HOST).performTextInput("jump.example.com")
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_JUMP_PORT).performTextReplacement("22")
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_JUMP_USERNAME).performTextInput("jump")
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_PRIMARY).performClick()

        composeRule.waitUntil(10_000) { hasNodeWithTag(AutomationTags.ONBOARDING_STEP_FORWARDING) }
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_FORWARD_ADD).performClick()
        composeRule.onNodeWithTag(AutomationTags.PORT_FORWARD_LOCAL_PORT).performTextInput("8080")
        composeRule.onNodeWithTag(AutomationTags.PORT_FORWARD_REMOTE_HOST).performTextReplacement("127.0.0.1")
        composeRule.onNodeWithTag(AutomationTags.PORT_FORWARD_REMOTE_PORT).performTextInput("80")
        composeRule.onNodeWithTag(AutomationTags.PORT_FORWARD_SAVE).performClick()
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_PERSISTENT_TMUX).performClick()
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_STARTUP_COMMAND).performTextInput("cd /srv/app")
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_PRIMARY).performClick()

        composeRule.waitUntil(10_000) { hasNodeWithTag(AutomationTags.ONBOARDING_STEP_REVIEW) }
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_PRIMARY).performClick()
        composeRule.waitUntil(20_000) { hasNodeWithTag(AutomationTags.MAIN_TAB_SERVERS) }
        composeRule.waitUntil(10_000) { hasTextNode("Prod Guided") }
    }

    @Test
    fun keyAndCertificateImportDialogsExposeStableAutomationTags() {
        dismissOnboarding()

        composeRule.onNodeWithTag(AutomationTags.MAIN_TAB_KEYS).performClick()
        composeRule.onNodeWithTag(AutomationTags.KEYS_ADD).performClick()
        composeRule.onNodeWithTag(AutomationTags.KEYS_IMPORT).performClick()

        composeRule.waitUntil(10_000) { hasNodeWithTag(AutomationTags.KEY_IMPORT_NAME) }
        check(hasNodeWithTag(AutomationTags.KEY_IMPORT_NAME))
        check(hasNodeWithTag(AutomationTags.KEY_IMPORT_PRIVATE_KEY))
        clickText("Cancel")

        composeRule.onNodeWithTag(AutomationTags.KEYS_ADD).performClick()
        clickText("Certificates")
        composeRule.waitUntil(10_000) { hasNodeWithTag(AutomationTags.CERTIFICATES_ADD) }
        composeRule.onNodeWithTag(AutomationTags.CERTIFICATES_ADD).performClick()

        composeRule.waitUntil(10_000) { hasNodeWithTag(AutomationTags.CERTIFICATE_IMPORT_NAME) }
        check(hasNodeWithTag(AutomationTags.CERTIFICATE_IMPORT_NAME))
        check(hasNodeWithTag(AutomationTags.CERTIFICATE_IMPORT_CONTENT))
    }

    @Test
    fun extraKeysScreenExposesStableAutomationTags() {
        dismissOnboarding()

        composeRule.onNodeWithTag(AutomationTags.MAIN_TAB_SETTINGS).performClick()
        composeRule.waitUntil(10_000) { hasNodeWithTag(AutomationTags.SETTINGS_SCREEN) }
        composeRule.onNodeWithTag(AutomationTags.SETTINGS_SCREEN)
            .performScrollToNode(hasTestTag(AutomationTags.SETTINGS_EXTRA_KEYS_ENTRY))
        composeRule.waitUntil(10_000) { hasNodeWithTag(AutomationTags.SETTINGS_EXTRA_KEYS_ENTRY) }
        composeRule.onNodeWithTag(AutomationTags.SETTINGS_EXTRA_KEYS_ENTRY).performClick()

        composeRule.waitUntil(10_000) { hasNodeWithTag(AutomationTags.EXTRA_KEYS_SCREEN) }
        check(hasNodeWithTag(AutomationTags.EXTRA_KEYS_PREVIEW))
        check(hasNodeWithTag(AutomationTags.extraKeyPresetTag("standard")))
        check(hasNodeWithTag(AutomationTags.extraKeyRowTag("esc")))
        check(hasNodeWithTag(AutomationTags.extraKeyToggleTag("esc")))
        composeRule.onNodeWithTag(AutomationTags.EXTRA_KEYS_SCREEN)
            .performScrollToNode(hasTestTag(AutomationTags.EXTRA_KEYS_RESET))
        check(hasNodeWithTag(AutomationTags.EXTRA_KEYS_RESET))
    }

    @Test
    fun syncScreenExposesStableAutomationTags() {
        dismissOnboarding()

        composeRule.onNodeWithText("Settings").performClick()
        composeRule.waitUntil(10_000) { hasNodeWithTag(AutomationTags.SETTINGS_SCREEN) }
        composeRule.onNodeWithTag(AutomationTags.SETTINGS_SCREEN)
            .performScrollToNode(hasTestTag(AutomationTags.SETTINGS_SYNC_ENTRY))
        composeRule.waitUntil(10_000) { hasNodeWithTag(AutomationTags.SETTINGS_SYNC_ENTRY) }
        composeRule.onNodeWithTag(AutomationTags.SETTINGS_SYNC_ENTRY).performClick()

        composeRule.waitUntil(10_000) { hasNodeWithTag(AutomationTags.SYNC_SCREEN) }
        check(hasTextNode("Android backup and device transfer can carry metadata between phones."))
    }

    @Test
    fun passwordPromptUsesStableAutomationTags() {
        dismissOnboarding()

        seedServer(
            Server(
                id = UUID.randomUUID().toString(),
                name = "Prompt Server",
                hostname = "127.0.0.1",
                port = 22,
                username = "demo",
                authMode = AuthMode.PASSWORD
            )
        )

        clickText("Prompt Server")
        composeRule.waitUntil(10_000) { hasNodeWithTag(AutomationTags.PASSWORD_FIELD) }
        check(hasNodeWithTag(AutomationTags.PASSWORD_FIELD))
        check(hasNodeWithTag(AutomationTags.PASSWORD_CONNECT))
        check(hasNodeWithTag(AutomationTags.PASSWORD_CANCEL))
    }

    @Test
    fun snippetScreenExposesStableAutomationTags() {
        dismissOnboarding()

        val snippet = Snippet(
            id = UUID.randomUUID().toString(),
            name = "Deploy",
            command = "deploy --dry-run"
        )
        seedSnippet(snippet)

        composeRule.onNodeWithTag("main_tab_snippets").performClick()
        composeRule.waitUntil(10_000) { hasTextNode("Deploy") }

        check(hasNodeWithTag("snippets.add"))
        check(hasNodeWithTag("snippet.edit.${snippet.id}"))
        check(hasNodeWithTag("snippet.duplicate.${snippet.id}"))
        check(hasNodeWithTag("snippet.delete.${snippet.id}"))

        composeRule.onNodeWithTag("snippet.edit.${snippet.id}").performClick()
        composeRule.waitUntil(10_000) { hasNodeWithTag("snippetEditor.name") }
        check(hasNodeWithTag("snippetEditor.name"))
        check(hasNodeWithTag("snippetEditor.command"))
        check(hasNodeWithTag("snippetEditor.save"))
        check(hasNodeWithTag("snippetEditor.cancel"))
    }

    @Test
    fun workplacesScreenExposesStableAutomationTags() {
        dismissOnboarding()

        val workplaceId = UUID.randomUUID().toString()
        val workplace = Workplace(id = workplaceId, name = "Ops")
        val assignedServer = Server(
            id = UUID.randomUUID().toString(),
            name = "Ops Server",
            hostname = "10.0.0.10",
            port = 22,
            username = "ops",
            authMode = AuthMode.PASSWORD,
            workplaceId = workplaceId
        )
        val spareServer = Server(
            id = UUID.randomUUID().toString(),
            name = "Spare Server",
            hostname = "10.0.0.11",
            port = 22,
            username = "ops",
            authMode = AuthMode.PASSWORD
        )

        seedWorkplace(workplace)
        seedServer(assignedServer)
        seedServer(spareServer)

        composeRule.onNodeWithTag("main_tab_workplaces").performClick()
        composeRule.waitUntil(10_000) { hasNodeWithTag(WorkplacesTags.card(workplaceId)) }

        check(hasNodeWithTag(WorkplacesTags.card(workplaceId)))
        check(hasNodeWithTag(WorkplacesTags.expand(workplaceId)))
        check(hasNodeWithTag(WorkplacesTags.open(workplaceId)))
        check(hasNodeWithTag(WorkplacesTags.edit(workplaceId)))
        check(hasNodeWithTag(WorkplacesTags.delete(workplaceId)))

        composeRule.onNodeWithTag(WorkplacesTags.expand(workplaceId)).performClick()
        composeRule.waitUntil(10_000) { hasNodeWithTag(WorkplacesTags.addServer(workplaceId)) }
        check(hasNodeWithTag(WorkplacesTags.removeServer(workplaceId, assignedServer.id)))

        composeRule.onNodeWithTag(WorkplacesTags.addServer(workplaceId)).performClick()
        composeRule.waitUntil(10_000) { hasNodeWithTag(WorkplacesTags.addServerOption(spareServer.id)) }
        check(hasNodeWithTag(WorkplacesTags.addServerOption(spareServer.id)))
        check(hasNodeWithTag(WorkplacesTags.AddServerDone))

        composeRule.onNodeWithTag(WorkplacesTags.delete(workplaceId)).performClick()
        composeRule.waitUntil(10_000) { hasNodeWithTag(WorkplacesTags.DeleteConfirm) }
        check(hasNodeWithTag(WorkplacesTags.DeleteConfirm))
        check(hasNodeWithTag(WorkplacesTags.DeleteDismiss))
    }

    private fun dismissOnboarding() {
        if (hasNodeWithTag(AutomationTags.ONBOARDING_SKIP)) {
            composeRule.onNodeWithTag(AutomationTags.ONBOARDING_SKIP).performClick()
        }
        composeRule.waitUntil(10_000) { hasNodeWithTag(AutomationTags.ONBOARDING_PRIMARY) }
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_PRIMARY).performClick()
        composeRule.waitUntil(20_000) { hasNodeWithTag(AutomationTags.MAIN_TAB_SERVERS) }
    }

    private fun seedServer(server: Server) {
        runBlocking {
            entryPoint().serverRepository().addServer(server)
        }
        composeRule.onNodeWithTag(AutomationTags.MAIN_TAB_SERVERS).performClick()
        composeRule.waitUntil(10_000) { hasTextNode(server.name) }
    }

    private fun insertServer(server: Server) {
        runBlocking {
            entryPoint().serverRepository().addServer(server)
        }
    }

    private fun seedSnippet(snippet: Snippet) {
        runBlocking {
            entryPoint().snippetRepository().addSnippet(snippet)
        }
    }

    private fun seedWorkplace(workplace: Workplace) {
        runBlocking {
            entryPoint().workplaceRepository().addWorkplace(workplace)
        }
    }

    private fun seedKey(name: String) {
        runBlocking {
            entryPoint().keyRepository().importKey(
                name,
                "-----BEGIN PRIVATE KEY-----\\nZmFrZQ==\\n-----END PRIVATE KEY-----",
                null
            )
        }
    }

    private fun seedCertificate(name: String) {
        runBlocking {
            entryPoint().certificateRepository().importCertificate(
                name,
                "ssh-ed25519-cert-v01@openssh.com AAAAC3NzaC1lZDI1NTE5AAAAIFRlc3Q= demo"
            )
        }
    }

    private fun restartIntoOnboarding() {
        runBlocking {
            entryPoint().userPreferencesRepository().setDemoModeEnabled(false)
            entryPoint().userPreferencesRepository().resetOnboarding()
        }
        composeRule.activity.runOnUiThread {
            composeRule.activity.recreate()
        }
    }

    private fun clickText(text: String) {
        composeRule.waitUntil(10_000) { hasTextNode(text) }
        composeRule.onNodeWithText(text, useUnmergedTree = true).performClick()
    }

    private fun hasNodeWithTag(tag: String): Boolean {
        return composeRule.onAllNodes(hasTestTag(tag), useUnmergedTree = true)
            .fetchSemanticsNodes().isNotEmpty()
    }

    private fun hasTextNode(text: String): Boolean {
        return composeRule.onAllNodes(
            androidx.compose.ui.test.hasText(text),
            useUnmergedTree = true
        ).fetchSemanticsNodes().isNotEmpty()
    }

    private fun entryPoint() = AutomationSeedBridge.from(appContext())

    private fun appContext(): Context {
        return composeRule.activity.applicationContext
    }
}
