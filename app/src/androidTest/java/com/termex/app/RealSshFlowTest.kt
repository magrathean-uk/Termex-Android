package com.termex.app

import android.util.Base64
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RealSshFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun connects_to_real_server_via_app_ui() {
        val args = InstrumentationRegistry.getArguments()
        val host = requireArg(args, "termexHost")
        val port = requireArg(args, "termexPort")
        val user = requireArg(args, "termexUser")
        val keyName = args.getString("termexKeyName") ?: "id_ed25519"
        val privateKey = decodeBase64(requireArg(args, "termexPrivateKeyBase64"))

        dismissOnboarding()
        importKey(keyName, privateKey)
        addServer(host, port, user, keyName)
        openServer(host)
        acceptHostKeyIfNeeded()
        waitForConnected("$user@$host:$port")
    }

    private fun dismissOnboarding() {
        waitForText("Welcome to Termex")
        clickText("Skip")
        clickText("Get Started")
        waitForText("Servers")
    }

    private fun importKey(keyName: String, privateKey: String) {
        clickText("Keys")
        clickContentDescription("Add")
        clickText("Import Key")
        replaceField("Key Name", keyName)
        replaceField("Private Key Content", privateKey)
        clickText("Import")
        waitForText(keyName)
    }

    private fun addServer(host: String, port: String, user: String, keyName: String) {
        clickText("Servers")
        waitForText("No servers. Tap + to add one.")
        clickContentDescription("Add Server")
        waitForText("Host")
        replaceField("Host", host)
        replaceField("Port", port)
        replaceField("Username", user)
        scrollToText("Method")
        clickClickableText("Method")
        clickText("SSH Key")
        clickClickableText("SSH Key", last = true)
        clickText(keyName)
        clickContentDescription("Save")
        waitForText(host)
    }

    private fun openServer(host: String) {
        clickText(host)
        waitUntilAnyOf("Accept", "Accept New Key", "Connection Failed", host)
    }

    private fun acceptHostKeyIfNeeded() {
        if (hasTextNode("Accept New Key")) {
            clickText("Accept New Key")
        } else if (hasTextNode("Accept")) {
            clickText("Accept")
        }
    }

    private fun waitForConnected(targetLabel: String) {
        composeRule.waitUntil(45_000) {
            hasContentDescriptionNode("Disconnect") || hasTextNode("Connection Failed") || hasTextNode(targetLabel)
        }

        if (hasTextNode("Connection Failed")) {
            throw AssertionError("Connection failed in app UI")
        }

        composeRule.onNodeWithContentDescription("Disconnect").fetchSemanticsNode()
        composeRule.onNodeWithText(targetLabel).fetchSemanticsNode()
    }

    private fun replaceField(label: String, value: String) {
        val matcher = hasSetTextAction() and (
            hasText(label) or hasAnyDescendant(hasText(label))
        )

        composeRule.waitUntil(10_000) {
            composeRule.onAllNodes(matcher, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onAllNodes(matcher, useUnmergedTree = true)[0].performTextReplacement(value)
    }

    private fun scrollToText(text: String) {
        composeRule.onNode(hasScrollAction(), useUnmergedTree = true)
            .performScrollToNode(hasText(text))
    }

    private fun clickText(text: String) {
        waitForText(text)
        val clickableMatcher = hasText(text) and hasClickAction()
        val clickableAncestorMatcher = hasClickAction() and hasAnyDescendant(hasText(text))
        if (composeRule.onAllNodes(clickableMatcher).fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onAllNodes(clickableMatcher)[0].performClick()
        } else if (composeRule.onAllNodes(clickableAncestorMatcher, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onAllNodes(clickableAncestorMatcher, useUnmergedTree = true)[0].performClick()
        } else {
            composeRule.onNodeWithText(text).performClick()
        }
    }

    private fun clickClickableText(text: String, last: Boolean = false) {
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodes(hasText(text) and hasClickAction())
                .fetchSemanticsNodes().isNotEmpty()
        }
        val nodes = composeRule.onAllNodes(hasText(text) and hasClickAction())
        val index = if (last) nodes.fetchSemanticsNodes().lastIndex else 0
        nodes[index].performClick()
    }

    private fun clickContentDescription(description: String) {
        val clickableMatcher = hasContentDescription(description) and hasClickAction()
        val clickableAncestorMatcher = hasClickAction() and hasAnyDescendant(hasContentDescription(description))
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodes(clickableMatcher).fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodes(clickableAncestorMatcher, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodes(hasContentDescription(description)).fetchSemanticsNodes().isNotEmpty()
        }
        if (composeRule.onAllNodes(clickableMatcher).fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onAllNodes(clickableMatcher)[0].performClick()
        } else if (composeRule.onAllNodes(clickableAncestorMatcher, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onAllNodes(clickableAncestorMatcher, useUnmergedTree = true)[0].performClick()
        } else {
            composeRule.onNodeWithContentDescription(description).performClick()
        }
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(10_000) {
            hasTextNode(text)
        }
    }

    private fun waitUntilAnyOf(vararg texts: String) {
        composeRule.waitUntil(20_000) {
            texts.any(::hasTextNode)
        }
    }

    private fun hasTextNode(text: String): Boolean {
        return composeRule.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() ||
            composeRule.onAllNodes(hasText(text), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }

    private fun hasContentDescriptionNode(description: String): Boolean {
        return composeRule.onAllNodes(hasContentDescription(description)).fetchSemanticsNodes().isNotEmpty() ||
            composeRule.onAllNodes(hasContentDescription(description), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }

    private fun requireArg(
        args: android.os.Bundle,
        name: String
    ): String = args.getString(name) ?: error("Missing instrumentation arg: $name")

    private fun decodeBase64(value: String): String {
        val decoded = Base64.decode(value, Base64.DEFAULT)
        return String(decoded, Charsets.UTF_8)
    }
}
