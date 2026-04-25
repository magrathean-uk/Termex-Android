package com.termex.app

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.termex.app.domain.AuthMode
import com.termex.app.domain.Server
import com.termex.app.testing.AutomationSeedBridge
import com.termex.app.testing.AutomationSeedBridgeEntryPoint
import com.termex.app.ui.AutomationTags
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ForegroundServiceDemoTest {

    private val hiltRule = HiltAndroidRule(this)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(hiltRule).around(composeRule)

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun recordsSpecialUseForegroundServiceFlow() {
        val fixture = loadLiveSshFixtureOrNull() ?: error("Set termexFixtureBase64.")
        val displayName = "${fixture.liveKey.serverName} demo"

        prepareAppState()
        seedKeyServer(
            displayName = displayName,
            host = fixture.liveKey.host,
            port = fixture.liveKey.port,
            username = fixture.liveKey.username,
            keyName = fixture.liveKey.keyName,
            keyText = fixture.liveKey.keyText
        )
        openServer(displayName)
        acceptAllHostKeysIfNeeded()
        waitForConnected(displayName, fixture.liveKey.connectionLabel)
        Thread.sleep(2_000)
        captureComposeScreen("01-connected")

        executeShell("cmd statusbar expand-notifications")
        Thread.sleep(1_500)
        captureScreen("02-notification")

        executeShell("input keyevent KEYCODE_HOME")
        Thread.sleep(1_000)
        executeShell("cmd statusbar expand-notifications")
        Thread.sleep(1_500)
        captureScreen("03-home-notification")
    }

    private fun prepareAppState() {
        runBlocking {
            val prefs = entryPoint().userPreferencesRepository()
            prefs.completeOnboarding()
            prefs.setDemoModeEnabled(false)
            prefs.setPersistentSessionResumeServerId(null)
        }
        File(appContext().getExternalFilesDir(null), "fgs-demo-screens").deleteRecursively()
        composeRule.waitUntil(20_000) { hasNodeWithTag(AutomationTags.MAIN_TAB_SERVERS) }
    }

    private fun seedKeyServer(
        displayName: String,
        host: String,
        port: Int,
        username: String,
        keyName: String,
        keyText: String
    ) {
        val keyPath = writeKeyFile(keyName, keyText)
        val server = Server(
            id = UUID.randomUUID().toString(),
            name = displayName,
            hostname = host,
            port = port,
            username = username,
            authMode = AuthMode.KEY,
            keyId = keyPath
        )
        seedServer(server)
    }

    private fun openServer(displayName: String) {
        if (hasNodeWithTag(AutomationTags.MAIN_TAB_SERVERS)) {
            composeRule.onNodeWithTag(AutomationTags.MAIN_TAB_SERVERS).performClick()
        }
        val serverId = runBlocking {
            entryPoint().serverRepository()
                .getAllServers()
                .first()
                .firstOrNull { it.displayName == displayName }
                ?.id
                ?: error("missing server: $displayName")
        }
        composeRule.onNodeWithTag(AutomationTags.serverRowTag(serverId), useUnmergedTree = true).performClick()
        composeRule.waitUntil(20_000) {
            hasNodeWithTag(AutomationTags.HOST_KEY_ACCEPT) ||
                hasNodeWithTag(AutomationTags.TERMINAL_DISCONNECT)
        }
    }

    private fun acceptAllHostKeysIfNeeded() {
        repeat(30) {
            if (hasNodeWithTag(AutomationTags.HOST_KEY_ACCEPT)) {
                composeRule.onNodeWithTag(AutomationTags.HOST_KEY_ACCEPT).performClick()
                composeRule.waitForIdle()
            } else if (hasNodeWithTag(AutomationTags.TERMINAL_DISCONNECT)) {
                return
            }
            Thread.sleep(500)
        }
    }

    private fun waitForConnected(displayName: String, connectionLabel: String) {
        composeRule.waitUntil(45_000) {
            terminalTitleContains(displayName) &&
                hasNodeWithTag(AutomationTags.TERMINAL_DISCONNECT) &&
                terminalConnectionLabelContains(connectionLabel)
        }
    }

    private fun writeKeyFile(name: String, content: String): String {
        return runBlocking {
            val keyRepository = entryPoint().keyRepository()
            keyRepository.importKey(name = name, privateKeyContent = content)
            keyRepository.getKeyPath(name)
        }
    }

    private fun seedServer(server: Server) {
        runBlocking {
            entryPoint().serverRepository().addServer(server)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(AutomationTags.MAIN_TAB_SERVERS).performClick()
        composeRule.waitUntil(20_000) { hasTextNode(server.name) }
    }

    private fun terminalTitleContains(fragment: String): Boolean {
        val nodes = composeRule.onAllNodes(hasTestTag(AutomationTags.TERMINAL_TITLE), useUnmergedTree = true)
            .fetchSemanticsNodes()
        return nodes.any { node ->
            if (SemanticsProperties.Text !in node.config) {
                false
            } else {
                node.config[SemanticsProperties.Text]
                    .joinToString("") { annotated -> annotated.text }
                    .contains(fragment)
            }
        }
    }

    private fun terminalConnectionLabelContains(fragment: String): Boolean {
        val nodes = composeRule.onAllNodes(hasTestTag(AutomationTags.TERMINAL_CONNECTION_LABEL), useUnmergedTree = true)
            .fetchSemanticsNodes()
        return nodes.any { node ->
            if (SemanticsProperties.Text !in node.config) {
                false
            } else {
                node.config[SemanticsProperties.Text]
                    .joinToString("") { annotated -> annotated.text }
                    .contains(fragment)
            }
        }
    }

    private fun hasTextNode(text: String): Boolean {
        return composeRule.onAllNodes(hasText(text), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }

    private fun hasNodeWithTag(tag: String): Boolean {
        return composeRule.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }

    private fun executeShell(command: String) {
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command).close()
    }

    private fun captureScreen(name: String) {
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            ?: error("Failed to capture screenshot: $name")
        val dir = File(appContext().getExternalFilesDir(null), "fgs-demo-screens").apply {
            mkdirs()
        }
        val target = File(dir, "$name.png")
        FileOutputStream(target).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    private fun captureComposeScreen(name: String) {
        val bitmap = composeRule.onRoot(useUnmergedTree = true).captureToImage().asAndroidBitmap()
        val dir = File(appContext().getExternalFilesDir(null), "fgs-demo-screens").apply {
            mkdirs()
        }
        val target = File(dir, "$name.png")
        FileOutputStream(target).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    private fun entryPoint(): AutomationSeedBridgeEntryPoint {
        return AutomationSeedBridge.from(appContext())
    }

    private fun appContext(): Context = composeRule.activity.applicationContext
}
