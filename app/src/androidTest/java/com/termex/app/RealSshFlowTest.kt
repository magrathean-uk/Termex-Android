package com.termex.app

import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.printToString
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.termex.app.domain.AuthMode
import com.termex.app.domain.PortForward
import com.termex.app.domain.PortForwardType
import com.termex.app.domain.Server
import com.termex.app.core.ssh.AuthPreference
import com.termex.app.core.ssh.SSHConnectionConfig
import com.termex.app.testing.AutomationSeedBridge
import com.termex.app.testing.AutomationSeedBridgeEntryPoint
import com.termex.app.ui.AutomationTags
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.Before
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.net.InetSocketAddress
import java.net.Socket
import java.io.File
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class RealSshFlowTest {

    private val hiltRule = HiltAndroidRule(this)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(hiltRule).around(composeRule)

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun keyAuthConnectsToLiveServer() {
        val fixture = requireFixture()

        dismissOnboarding()
        seedKeyServer(
            displayName = fixture.liveKey.serverName,
            host = fixture.liveKey.host,
            port = fixture.liveKey.port,
            username = fixture.liveKey.username,
            keyName = fixture.liveKey.keyName,
            keyText = fixture.liveKey.keyText
        )
        openServer(fixture.liveKey.serverName)
        acceptAllHostKeysIfNeeded()
        waitForConnected(fixture.liveKey.serverName, fixture.liveKey.connectionLabel)
    }

    @Test
    fun trustsLiveHostKeyBeforeRotation() {
        val fixture = requireFixture()
        val serverName = "${fixture.liveKey.serverName} changed host"

        dismissOnboarding()
        val server = seedKeyServer(
            displayName = serverName,
            host = fixture.liveKey.host,
            port = fixture.liveKey.port,
            username = fixture.liveKey.username,
            keyName = fixture.liveKey.keyName,
            keyText = fixture.liveKey.keyText
        )
        openServer(serverName)
        acceptAllHostKeysIfNeeded()
        waitForConnected(serverName, fixture.liveKey.connectionLabel)
        navigateBackToMainTabs()
    }

    @Test
    fun changedHostKeyPromptsOnReconnect() {
        val fixture = requireFixture()
        val serverName = "${fixture.liveKey.serverName} changed host"

        dismissOnboarding()
        ensureKeyServerExists(
            displayName = serverName,
            host = fixture.liveKey.host,
            port = fixture.liveKey.port,
            username = fixture.liveKey.username,
            keyName = fixture.liveKey.keyName,
            keyText = fixture.liveKey.keyText
        )
        openServer(serverName)
        waitForHostKeyPrompt()
        check(hasNodeWithTag(AutomationTags.HOST_KEY_ACCEPT))
        check(hasNodeWithTag(AutomationTags.HOST_KEY_REJECT))
        composeRule.onNodeWithTag(AutomationTags.HOST_KEY_REJECT).performClick()
    }

    @Test
    fun slowHostTrustThenConnectsToLiveServer() {
        val fixture = requireFixture()

        dismissOnboarding()
        seedKeyServer(
            displayName = "${fixture.liveKey.serverName} trust",
            host = fixture.liveKey.host,
            port = fixture.liveKey.port,
            username = fixture.liveKey.username,
            keyName = fixture.liveKey.keyName,
            keyText = fixture.liveKey.keyText
        )
        openServer("${fixture.liveKey.serverName} trust")
        waitForHostKeyPrompt()
        acceptAllHostKeysIfNeeded()
        waitForConnected("${fixture.liveKey.serverName} trust", fixture.liveKey.connectionLabel)
    }

    @Test
    fun rejectingUnknownHostKeyStopsConnection() {
        val fixture = requireFixture()

        dismissOnboarding()
        seedKeyServer(
            displayName = "${fixture.liveKey.serverName} reject",
            host = fixture.liveKey.host,
            port = fixture.liveKey.port,
            username = fixture.liveKey.username,
            keyName = fixture.liveKey.keyName,
            keyText = fixture.liveKey.keyText
        )
        openServer("${fixture.liveKey.serverName} reject")
        waitForHostKeyPrompt()
        rejectHostKey()
        composeRule.waitUntil(20_000) {
            !hasNodeWithTag(AutomationTags.HOST_KEY_ACCEPT) &&
                !hasNodeWithTag(AutomationTags.TERMINAL_DISCONNECT)
        }
        navigateBackToMainTabs()
        composeRule.onNodeWithTag(AutomationTags.MAIN_TAB_SERVERS).assertExists()
    }

    @Test
    fun passwordAuthConnectsToLiveServer() {
        val fixture = requireFixture()

        dismissOnboarding()
        seedPasswordServer(
            displayName = fixture.password.serverName,
            host = fixture.password.host,
            port = fixture.password.port,
            username = fixture.password.username
        )
        openServer(fixture.password.serverName)
        replaceTagText(AutomationTags.PASSWORD_FIELD, fixture.password.password)
        composeRule.onNodeWithTag(AutomationTags.PASSWORD_CONNECT).performClick()
        acceptAllHostKeysIfNeeded()
        waitForConnected(
            fixture.password.serverName,
            "${fixture.password.username}@${fixture.password.host}:${fixture.password.port}"
        )
    }

    @Test
    fun wrongPasswordDoesNotCrash() {
        val fixture = requireFixture()

        dismissOnboarding()
        seedPasswordServer(
            displayName = "${fixture.password.serverName} wrong",
            host = fixture.password.host,
            port = fixture.password.port,
            username = fixture.password.username
        )
        openServer("${fixture.password.serverName} wrong")
        replaceTagText(AutomationTags.PASSWORD_FIELD, "wrong-password")
        composeRule.onNodeWithTag(AutomationTags.PASSWORD_CONNECT).performClick()
        acceptAllHostKeysIfNeeded()
        waitForConnectionFailure()
        closeConnectionFailure()
        composeRule.onNodeWithTag(AutomationTags.MAIN_TAB_SERVERS).assertExists()
    }

    @Test
    fun certificateAuthConnectsToLiveServer() {
        val fixture = requireFixture()
        val certificate = fixture.certificate ?: error("certificate fixture missing")

        dismissOnboarding()
        seedCertificateServer(
            displayName = certificate.serverName,
            host = certificate.host,
            port = certificate.port,
            username = certificate.username,
            keyName = certificate.keyName,
            keyText = certificate.keyText,
            certificateName = certificate.certificateName,
            certificateText = certificate.certificateText
        )
        openServer(certificate.serverName)
        acceptAllHostKeysIfNeeded()
        waitForConnected(certificate.serverName, certificate.connectionLabel)
    }

    @Test
    fun jumpHostConnectsToLiveServer() {
        val fixture = requireFixture()
        val jump = fixture.jump ?: error("jump fixture missing")

        dismissOnboarding()

        val jumpServer = seedKeyServer(
            displayName = jump.serverName,
            host = jump.host,
            port = jump.port,
            username = jump.username,
            keyName = jump.keyName,
            keyText = fixture.liveKey.keyText
        )
        seedKeyServer(
            displayName = "${fixture.liveKey.serverName} jump target",
            host = jump.targetHost,
            port = jump.targetPort,
            username = fixture.liveKey.username,
            keyName = fixture.liveKey.keyName,
            keyText = fixture.liveKey.keyText,
            jumpHostId = jumpServer.id
        )

        openServer("${fixture.liveKey.serverName} jump target")
        acceptAllHostKeysIfNeeded()
        waitForConnected(
            "${fixture.liveKey.serverName} jump target",
            "${fixture.liveKey.username}@${jump.targetHost}:${jump.targetPort}"
        )
    }

    @Test
    fun localForwardOpensSshBanner() {
        val fixture = requireFixture()
        val serverName = "${fixture.liveKey.serverName} forward"

        dismissOnboarding()
        val server = seedKeyServer(
            displayName = serverName,
            host = fixture.liveKey.host,
            port = fixture.liveKey.port,
            username = fixture.liveKey.username,
            keyName = fixture.liveKey.keyName,
            keyText = fixture.liveKey.keyText,
            portForwards = listOf(
                PortForward(
                    type = PortForwardType.LOCAL,
                    localPort = 0,
                    remoteHost = "127.0.0.1",
                    remotePort = 4223
                )
            )
        )

        openServer(serverName)
        acceptAllHostKeysIfNeeded()
        waitForConnected(serverName, fixture.liveKey.connectionLabel)
        navigateBackToMainTabs()

        openPortForwardingFromServerList(serverName)
        startForward()
        val activeForward = waitForForwardActive(server.id, server.portForwards.first().id)
        val localPort = activeForward.boundAddress?.port ?: error("Missing local forward bound port")
        acceptAllHostKeysIfNeeded()
        waitForText("Active")

        val banner = readSocketBanner(localPort)
        check(banner.contains("SSH-")) { "Missing SSH banner on local forward" }
    }

    @Test
    fun remoteForwardExposesServiceOnHost() {
        val fixture = requireFixture()
        val serverName = "${fixture.liveKey.serverName} remote forward"
        val remoteTarget = remoteForwardTarget()
        val localTarget = remoteTarget
            ?: entryPoint().socketProbeHelper()
                .startReachableBannerServer("REMOTE-FORWARD-OK\n")
                .let { it.host to it.port }
        val localTargetHost = localTarget.first
        val localTargetPort = localTarget.second
        val localProbe = Socket().use { socket ->
            socket.connect(InetSocketAddress(localTargetHost, localTargetPort), 5_000)
            socket.soTimeout = 5_000
            String(socket.getInputStream().readUpTo(64), StandardCharsets.UTF_8)
        }
        check(localProbe.contains("REMOTE-FORWARD-OK")) {
            "Local target probe failed: target=$localTargetHost:$localTargetPort data=$localProbe"
        }

        try {
            dismissOnboarding()
            val server = seedKeyServer(
                displayName = serverName,
                host = fixture.liveKey.host,
                port = fixture.liveKey.port,
                username = fixture.liveKey.username,
                keyName = fixture.liveKey.keyName,
                keyText = fixture.liveKey.keyText,
                portForwards = listOf(
                    PortForward(
                        type = PortForwardType.REMOTE,
                        localPort = localTargetPort,
                        remoteHost = localTargetHost,
                        remotePort = 0,
                        bindAddress = "127.0.0.1"
                    )
                )
            )

            openServer(serverName)
            acceptAllHostKeysIfNeeded()
            waitForConnected(serverName, fixture.liveKey.connectionLabel)
            navigateBackToMainTabs()

            openPortForwardingFromServerList(serverName)
            startForward()
            val activeForward = waitForForwardActive(server.id, server.portForwards.first().id)
            val remoteListenPort = activeForward.boundAddress?.port ?: error("Missing remote forward bound port")
            val probeKey = "${server.id}:remote-probe"
            val probeConfig = SSHConnectionConfig(
                hostname = fixture.liveKey.host,
                port = fixture.liveKey.port,
                username = fixture.liveKey.username,
                identityPath = server.keyId ?: error("Missing key path"),
                authPreference = AuthPreference.KEY
            )
            val probeConnect = runBlocking {
                entryPoint().connectionManager().connect(probeKey, probeConfig)
            }
            check(probeConnect.isSuccess) {
                "Probe connection failed: ${probeConnect.exceptionOrNull()?.message}"
            }
            val output = runBlocking {
                try {
                    entryPoint().connectionManager().runShellCommand(
                        probeKey,
                        """
                        python3 -c "import socket,time; port=$remoteListenPort; last=None
candidates=['127.0.0.1','localhost']
try:
    host_ip=socket.gethostbyname(socket.gethostname())
    if host_ip not in candidates:
        candidates.append(host_ip)
except OSError:
    pass
for _ in range(20):
    for host in candidates:
        try:
            s=socket.create_connection((host,port),5)
            data=s.recv(64)
            print(f'host={host} data={data!r}', flush=True)
            raise SystemExit(0)
        except OSError as e:
            last=e
            print(f'host={host} error={type(e).__name__}:{e}', flush=True)
            time.sleep(0.1)
else:
    raise last"
                        """.trimIndent(),
                        null,
                        10_000
                    )
                } finally {
                    entryPoint().connectionManager().disconnect(probeKey)
                }
            }
            check(output.contains("REMOTE-FORWARD-OK")) { "Missing remote forward banner: $output" }
        } finally {
            if (remoteTarget == null) {
                entryPoint().socketProbeHelper().stopBannerServer(localTargetPort)
            }
        }
    }

    @Test
    fun dynamicForwardRoutesSocksTraffic() {
        val fixture = requireFixture()
        val serverName = "${fixture.liveKey.serverName} dynamic forward"

        dismissOnboarding()
        val server = seedKeyServer(
            displayName = serverName,
            host = fixture.liveKey.host,
            port = fixture.liveKey.port,
            username = fixture.liveKey.username,
            keyName = fixture.liveKey.keyName,
            keyText = fixture.liveKey.keyText,
            portForwards = listOf(
                PortForward(
                    type = PortForwardType.DYNAMIC,
                    localPort = 0,
                    remoteHost = "127.0.0.1",
                    remotePort = fixture.liveKey.port
                )
            )
        )

        openServer(serverName)
        acceptAllHostKeysIfNeeded()
        waitForConnected(serverName, fixture.liveKey.connectionLabel)
        navigateBackToMainTabs()

        openPortForwardingFromServerList(serverName)
        startForward()
        val activeForward = waitForForwardActive(server.id, server.portForwards.first().id)
        val socksPort = activeForward.boundAddress?.port ?: error("Missing SOCKS bound port")

        val banner = readBannerThroughSocks(
            socksPort = socksPort,
            targetHost = "localhost",
            targetPort = fixture.jump?.targetPort ?: error("Missing jump fixture target port")
        )
        check(banner.contains("SSH-")) { "Missing SOCKS banner" }
    }

    @Test
    fun forwardAgentMakesSshAuthSockVisible() {
        val fixture = requireFixture()
        val serverName = "${fixture.liveKey.serverName} agent"

        dismissOnboarding()
        val server = seedKeyServer(
            displayName = serverName,
            host = fixture.liveKey.host,
            port = fixture.liveKey.port,
            username = fixture.liveKey.username,
            keyName = fixture.liveKey.keyName,
            keyText = fixture.liveKey.keyText,
            forwardAgent = true
        )

        openServer(serverName)
        acceptAllHostKeysIfNeeded()
        waitForConnected(serverName, fixture.liveKey.connectionLabel)
        val output = runBlocking {
            entryPoint().connectionManager().runShellCommand(
                server.id,
                "test -n \"\$SSH_AUTH_SOCK\" && echo present || echo missing\r",
                "present",
                10_000
            )
        }
        check(output.contains("present")) { "SSH_AUTH_SOCK missing: $output" }
    }

    @Test
    fun persistentTmuxSessionRestoresAfterRelaunch() {
        val fixture = requireFixture()

        dismissOnboarding()
        seedKeyServer(
            displayName = "${fixture.liveKey.serverName} persistent",
            host = fixture.liveKey.host,
            port = fixture.liveKey.port,
            username = fixture.liveKey.username,
            keyName = fixture.liveKey.keyName,
            keyText = fixture.liveKey.keyText,
            persistentSession = true
        )

        openServer("${fixture.liveKey.serverName} persistent")
        acceptAllHostKeysIfNeeded()
        waitForConnected("${fixture.liveKey.serverName} persistent", fixture.liveKey.connectionLabel)

        sendTerminalCommand("tmux display-message -p '#S'\r")
        waitForTranscript("termex-")

        navigateBackToMainTabs()

        composeRule.activityRule.scenario.recreate()

        waitForTerminalTitle("${fixture.liveKey.serverName} persistent")
        waitForConnected("${fixture.liveKey.serverName} persistent", fixture.liveKey.connectionLabel)
    }

    private fun dismissOnboarding() {
        if (hasNodeWithTag(AutomationTags.MAIN_TAB_SERVERS) || hasNodeWithTag(AutomationTags.TERMINAL_DISCONNECT)) {
            return
        }
        if (hasNodeWithTag(AutomationTags.ONBOARDING_SKIP)) {
            composeRule.onNodeWithTag(AutomationTags.ONBOARDING_SKIP).performClick()
        }
        if (hasNodeWithTag(AutomationTags.ONBOARDING_PRIMARY)) {
            composeRule.onNodeWithTag(AutomationTags.ONBOARDING_PRIMARY).performClick()
        } else {
            composeRule.waitUntil(10_000) {
                hasNodeWithTag(AutomationTags.ONBOARDING_PRIMARY) ||
                    hasNodeWithTag(AutomationTags.MAIN_TAB_SERVERS) ||
                    hasNodeWithTag(AutomationTags.TERMINAL_DISCONNECT) ||
                    hasNodeWithTag(AutomationTags.HOST_KEY_ACCEPT) ||
                    hasTextNode("Connection Failed")
            }
            if (hasNodeWithTag(AutomationTags.ONBOARDING_PRIMARY)) {
                composeRule.onNodeWithTag(AutomationTags.ONBOARDING_PRIMARY).performClick()
            }
        }
        composeRule.waitUntil(20_000) {
            hasNodeWithTag(AutomationTags.MAIN_TAB_SERVERS) ||
                hasNodeWithTag(AutomationTags.TERMINAL_DISCONNECT) ||
                hasNodeWithTag(AutomationTags.HOST_KEY_ACCEPT) ||
                hasTextNode("Connection Failed")
        }
    }

    private fun importKey(keyName: String, privateKey: String) {
        composeRule.onNodeWithTag(AutomationTags.MAIN_TAB_KEYS).performClick()
        composeRule.onNodeWithTag(AutomationTags.KEYS_ADD).performClick()
        composeRule.onNodeWithTag(AutomationTags.KEYS_IMPORT).performClick()
        replaceTagText(AutomationTags.KEY_IMPORT_NAME, keyName)
        replaceTagText(AutomationTags.KEY_IMPORT_PRIVATE_KEY, privateKey)
        clickText("Import")
        waitForText(keyName)
    }

    private fun importCertificate(name: String, content: String) {
        clickText("Certificates")
        composeRule.onNodeWithTag(AutomationTags.CERTIFICATES_ADD).performClick()
        replaceTagText(AutomationTags.CERTIFICATE_IMPORT_NAME, name)
        replaceTagText(AutomationTags.CERTIFICATE_IMPORT_CONTENT, content)
        clickText("Import")
        waitForText(name)
    }

    private fun seedKeyServer(
        displayName: String,
        host: String,
        port: Int,
        username: String,
        keyName: String,
        keyText: String,
        jumpHostId: String? = null,
        persistentSession: Boolean = false,
        forwardAgent: Boolean = false,
        portForwards: List<PortForward> = emptyList()
    ): Server {
        val keyPath = writeKeyFile(keyName, keyText)
        val server = Server(
            id = UUID.randomUUID().toString(),
            name = displayName,
            hostname = host,
            port = port,
            username = username,
            authMode = AuthMode.KEY,
            keyId = keyPath,
            jumpHostId = jumpHostId,
            persistentSessionEnabled = persistentSession,
            forwardAgent = forwardAgent,
            portForwards = portForwards
        )
        seedServer(server)
        return server
    }

    private fun seedCertificateServer(
        displayName: String,
        host: String,
        port: Int,
        username: String,
        keyName: String,
        keyText: String,
        certificateName: String,
        certificateText: String
    ): Server {
        val keyPath = writeKeyFile(keyName, keyText)
        val certificatePath = writeCertificateFile(certificateName, certificateText)
        val server = Server(
            id = UUID.randomUUID().toString(),
            name = displayName,
            hostname = host,
            port = port,
            username = username,
            authMode = AuthMode.KEY,
            keyId = keyPath,
            certificatePath = certificatePath
        )
        seedServer(server)
        return server
    }

    private fun seedPasswordServer(
        displayName: String,
        host: String,
        port: Int,
        username: String
    ): Server {
        val server = Server(
            id = UUID.randomUUID().toString(),
            name = displayName,
            hostname = host,
            port = port,
            username = username,
            authMode = AuthMode.PASSWORD
        )
        seedServer(server)
        return server
    }

    private fun openServersAdd() {
        composeRule.onNodeWithTag(AutomationTags.MAIN_TAB_SERVERS).performClick()
        composeRule.onNodeWithTag(AutomationTags.SERVERS_ADD).performClick()
        composeRule.waitUntil(10_000) { hasNodeWithTag(AutomationTags.SERVER_FIELD_HOST) }
    }

    private fun fillBaseServerFields(displayName: String, host: String, port: Int, username: String) {
        replaceTagText(AutomationTags.SERVER_FIELD_DISPLAY_NAME, displayName)
        replaceTagText(AutomationTags.SERVER_FIELD_HOST, host)
        replaceTagText(AutomationTags.SERVER_FIELD_PORT, port.toString())
        replaceTagText(AutomationTags.SERVER_FIELD_USERNAME, username)
    }

    private fun chooseAuthMode(label: String) {
        composeRule.onNodeWithTag(AutomationTags.SERVER_AUTH_MODE).performClick()
        clickText(label)
    }

    private fun choosePicker(tag: String, optionText: String) {
        composeRule.onNodeWithTag(tag).performClick()
        clickText(optionText)
    }

    private fun saveServer() {
        composeRule.onNodeWithTag(AutomationTags.SERVER_SAVE).performClick()
        composeRule.waitUntil(10_000) { hasNodeWithTag(AutomationTags.MAIN_TAB_SERVERS) }
    }

    private fun openServer(displayName: String) {
        if (hasNodeWithTag(AutomationTags.MAIN_TAB_SERVERS)) {
            composeRule.onNodeWithTag(AutomationTags.MAIN_TAB_SERVERS).performClick()
        }
        val serverId = entryPoint().serverRepository()
            .let { repository ->
                runBlocking { repository.getAllServers().first().firstOrNull { it.displayName == displayName }?.id }
            } ?: error("missing server: $displayName")
        scrollToServerRow(serverId)
        composeRule.onNodeWithTag(AutomationTags.serverRowTag(serverId), useUnmergedTree = true).performClick()
        composeRule.waitUntil(20_000) {
            hasNodeWithTag(AutomationTags.HOST_KEY_ACCEPT) ||
                hasNodeWithTag(AutomationTags.PASSWORD_FIELD) ||
                hasNodeWithTag(AutomationTags.TERMINAL_DISCONNECT) ||
                terminalTitleContains(displayName) ||
                hasTextNode("Connection Failed")
        }
    }

    private fun openPortForwardingFromServerList(displayName: String) {
        composeRule.onNodeWithTag(AutomationTags.MAIN_TAB_SERVERS).performClick()
        val serverId = entryPoint().serverRepository()
            .let { repository ->
                runBlocking { repository.getAllServers().first().firstOrNull { it.displayName == displayName }?.id }
            } ?: error("missing server for port forwarding: $displayName")
        scrollToServerRow(serverId)
        composeRule.onNodeWithTag(AutomationTags.serverMenuTag(serverId), useUnmergedTree = true).performClick()
        clickText("Port Forwarding")
    }

    private fun startForwardingDialog(
        type: PortForwardType,
        localPort: Int,
        remoteHost: String = "",
        remotePort: Int = 0,
        bindAddress: String = "127.0.0.1"
    ) {
        composeRule.onNodeWithTag(AutomationTags.PORT_FORWARD_ADD).performClick()
        composeRule.onNodeWithTag(AutomationTags.PORT_FORWARD_TYPE).performClick()
        clickText(type.name)
        replaceTagText(AutomationTags.PORT_FORWARD_LOCAL_PORT, localPort.toString())
        if (type != PortForwardType.DYNAMIC) {
            replaceTagText(AutomationTags.PORT_FORWARD_REMOTE_HOST, remoteHost)
            replaceTagText(AutomationTags.PORT_FORWARD_REMOTE_PORT, remotePort.toString())
        }
        if (type == PortForwardType.REMOTE) {
            replaceTagText(AutomationTags.PORT_FORWARD_BIND_ADDRESS, bindAddress)
        }
        composeRule.onNodeWithTag(AutomationTags.PORT_FORWARD_SAVE).performClick()
    }

    private fun addLocalForward(localPort: Int, remoteHost: String, remotePort: Int) {
        composeRule.onNodeWithTag(AutomationTags.PORT_FORWARD_ADD).performClick()
        replaceTagText(AutomationTags.PORT_FORWARD_LOCAL_PORT, localPort.toString())
        replaceTagText(AutomationTags.PORT_FORWARD_REMOTE_HOST, remoteHost)
        replaceTagText(AutomationTags.PORT_FORWARD_REMOTE_PORT, remotePort.toString())
        composeRule.onNodeWithTag(AutomationTags.PORT_FORWARD_SAVE).performClick()
    }

    private fun startForward() {
        clickContentDescription("Start")
    }

    private fun waitForHostKeyPrompt() {
        composeRule.waitUntil(20_000) {
            hasNodeWithTag(AutomationTags.HOST_KEY_ACCEPT) || hasTextNode("Connection Failed")
        }
    }

    private fun acceptAllHostKeysIfNeeded() {
        repeat(30) {
            if (hasNodeWithTag(AutomationTags.HOST_KEY_ACCEPT)) {
                composeRule.onNodeWithTag(AutomationTags.HOST_KEY_ACCEPT).performClick()
                composeRule.waitForIdle()
            } else if (hasNodeWithTag(AutomationTags.TERMINAL_DISCONNECT) || hasTextNode("Connection Failed")) {
                return
            }
            Thread.sleep(500)
        }
    }

    private fun rejectHostKey() {
        composeRule.waitUntil(20_000) { hasNodeWithTag(AutomationTags.HOST_KEY_REJECT) }
        composeRule.onNodeWithTag(AutomationTags.HOST_KEY_REJECT).performClick()
    }

    private fun waitForConnected(displayName: String, connectionLabel: String) {
        waitForTerminalTitle(displayName)
        composeRule.waitUntil(45_000) {
            hasNodeWithTag(AutomationTags.TERMINAL_DISCONNECT) && terminalConnectionLabelContains(connectionLabel)
        }
    }

    private fun waitForTerminalTitle(displayName: String) {
        composeRule.waitUntil(45_000) { terminalTitleContains(displayName) || hasTextNode("Connection Failed") }
        if (hasTextNode("Connection Failed")) {
            error("Connection failed in app UI:\n${composeRule.onRoot(useUnmergedTree = true).printToString()}")
        }
    }

    private fun waitForConnectionFailure() {
        composeRule.waitUntil(45_000) { hasTextNode("Connection Failed") || terminalTitleContains("Error") }
    }

    private fun closeConnectionFailure() {
        clickText("Close")
    }

    private fun sendTerminalCommand(command: String) {
        composeRule.onNodeWithTag(AutomationTags.TERMINAL_INPUT, useUnmergedTree = true)
            .performTextInput(command)
    }

    private fun waitForTranscript(fragment: String) {
        composeRule.waitUntil(20_000) { terminalTranscriptContains(fragment) }
    }

    private fun waitForForwardActive(serverId: String, forwardId: String): com.termex.app.core.ssh.ActivePortForward {
        composeRule.waitUntil(20_000) {
            val state = entryPoint().portForwardManager().activeForwards.value.firstOrNull {
                it.sessionKey == serverId && it.config.id == forwardId
            }
            if (state?.error != null) {
                error("Forward error: ${state.error}")
            }
            state?.isActive == true
        }
        return entryPoint().portForwardManager().activeForwards.value.first {
            it.sessionKey == serverId && it.config.id == forwardId && it.isActive
        }
    }

    private fun readSocketBanner(port: Int): String {
        return readSocketBanner("127.0.0.1", port)
    }

    private fun readSocketBanner(host: String, port: Int): String {
        Socket(host, port).use { socket ->
            socket.soTimeout = 5_000
            val bytes = ByteArray(128)
            val count = socket.getInputStream().read(bytes)
            return String(bytes, 0, count, Charsets.UTF_8)
        }
    }

    private fun readBannerThroughSocks(socksPort: Int, targetHost: String, targetPort: Int): String {
        var lastFailure: Throwable? = null
        repeat(10) {
            try {
                return readBannerThroughSocksOnce(socksPort, targetHost, targetPort)
            } catch (t: Throwable) {
                lastFailure = t
                Thread.sleep(250)
            }
        }
        throw IllegalStateException(lastFailure?.message ?: "SOCKS probe failed", lastFailure)
    }

    private fun readBannerThroughSocksOnce(socksPort: Int, targetHost: String, targetPort: Int): String {
        Socket("127.0.0.1", socksPort).use { socket ->
            socket.soTimeout = 5_000
            val input = socket.getInputStream()
            val output = socket.getOutputStream()

            output.write(byteArrayOf(0x05, 0x01, 0x00))
            output.flush()
            input.readExactly(2)

            val ipv4 = Regex("""^(\d{1,3}\.){3}\d{1,3}$""").matches(targetHost)
            val request = if (ipv4) {
                val octets = targetHost.split('.').map { it.toInt().toByte() }
                byteArrayOf(
                    0x05,
                    0x01,
                    0x00,
                    0x01,
                    octets[0],
                    octets[1],
                    octets[2],
                    octets[3],
                    ((targetPort shr 8) and 0xFF).toByte(),
                    (targetPort and 0xFF).toByte()
                )
            } else {
                val hostBytes = targetHost.toByteArray(StandardCharsets.UTF_8)
                ByteArray(7 + hostBytes.size).also { bytes ->
                    bytes[0] = 0x05
                    bytes[1] = 0x01
                    bytes[2] = 0x00
                    bytes[3] = 0x03
                    bytes[4] = hostBytes.size.toByte()
                    System.arraycopy(hostBytes, 0, bytes, 5, hostBytes.size)
                    bytes[5 + hostBytes.size] = ((targetPort shr 8) and 0xFF).toByte()
                    bytes[6 + hostBytes.size] = (targetPort and 0xFF).toByte()
                }
            }
            output.write(request)
            output.flush()

            val response = input.readExactly(4)
            if (response[1].toInt() != 0x00) {
                error("SOCKS connect failed with code ${response[1].toInt() and 0xFF}")
            }
            val addressLength = when (response[3].toInt() and 0xFF) {
                0x01 -> 4
                0x04 -> 16
                0x03 -> input.read().takeIf { it > 0 } ?: error("Missing SOCKS domain response length")
                else -> error("Unsupported SOCKS response address type ${response[3].toInt() and 0xFF}")
            }
            if (addressLength > 0) {
                input.readExactly(addressLength)
            }
            input.readExactly(2)

            val bytes = ByteArray(128)
            val count = input.read(bytes)
            return String(bytes, 0, count, Charsets.UTF_8)
        }
    }

    private fun InputStream.readUpTo(maxBytes: Int): ByteArray {
        val bytes = ByteArray(maxBytes)
        val count = read(bytes)
        return if (count <= 0) ByteArray(0) else bytes.copyOf(count)
    }

    private fun InputStream.readExactly(byteCount: Int): ByteArray {
        val bytes = ByteArray(byteCount)
        var offset = 0
        while (offset < byteCount) {
            val read = read(bytes, offset, byteCount - offset)
            if (read < 0) error("Expected $byteCount bytes, got $offset")
            offset += read
        }
        return bytes
    }

    private fun clickText(text: String) {
        composeRule.waitUntil(10_000) { hasTextNode(text) }
        composeRule.onNodeWithText(text, useUnmergedTree = true).performClick()
    }

    private fun replaceTagText(tag: String, value: String) {
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).performTextReplacement(value)
    }

    private fun replaceFieldByLabel(label: String, value: String) {
        val matcher = hasSetTextAction() and (hasText(label) or hasAnyDescendant(hasText(label)))
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodes(matcher, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodes(matcher, useUnmergedTree = true)[0].performTextReplacement(value)
    }

    private fun clickContentDescription(description: String) {
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodes(hasContentDescription(description), useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodes(hasContentDescription(description), useUnmergedTree = true)[0].performClick()
    }

    private fun navigateBackToMainTabs() {
        repeat(3) {
            if (hasNodeWithTag(AutomationTags.MAIN_TAB_SERVERS)) {
                return
            }
            clickContentDescription("Back")
            composeRule.waitForIdle()
            Thread.sleep(500)
        }
        composeRule.waitUntil(10_000) { hasNodeWithTag(AutomationTags.MAIN_TAB_SERVERS) }
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(10_000) { hasTextNode(text) }
    }

    private fun hasTextNode(text: String): Boolean {
        return composeRule.onAllNodes(hasText(text), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }

    private fun hasNodeWithTag(tag: String): Boolean {
        return composeRule.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }

    private fun scrollToServerRow(serverId: String) {
        composeRule.waitUntil(10_000) { hasNodeWithTag(AutomationTags.SERVER_LIST) }
        val rowTag = AutomationTags.serverRowTag(serverId)
        composeRule.onNodeWithTag(AutomationTags.SERVER_LIST, useUnmergedTree = true)
            .performScrollToNode(hasTestTag(rowTag))
        composeRule.waitUntil(10_000) { hasNodeWithTag(rowTag) }
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

    private fun terminalTranscriptContains(fragment: String): Boolean {
        val nodes = composeRule.onAllNodes(hasTestTag(AutomationTags.TERMINAL_VIEW), useUnmergedTree = true)
            .fetchSemanticsNodes()
        return nodes.any { node ->
            if (SemanticsProperties.ContentDescription !in node.config) {
                false
            } else {
                node.config[SemanticsProperties.ContentDescription]
                    .joinToString("\n")
                    .contains(fragment)
            }
        }
    }

    private fun requireFixture(): LiveSshTestFixture {
        return loadLiveSshFixtureOrNull()
            ?: error("Set termexFixtureBase64.")
    }

    private fun remoteForwardTarget(): Pair<String, Int>? {
        val arguments = InstrumentationRegistry.getArguments()
        val host = arguments.getString("termexRemoteForwardTargetHost") ?: return null
        val port = arguments.getString("termexRemoteForwardTargetPort")?.toIntOrNull() ?: return null
        return host to port
    }

    private fun writeKeyFile(name: String, content: String): String {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        return runBlocking {
            val keyRepository = entryPoint().keyRepository()
            keyRepository.importKey(name = name, privateKeyContent = content)
            keyRepository.getKeyPath(name)
        }
    }

    private fun writeCertificateFile(name: String, content: String): String {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        runBlocking {
            entryPoint().certificateRepository().importCertificate(name, content)
        }
        return File(targetContext.filesDir, "ssh_certs/${File(name).name.replace("..", "")}").absolutePath
    }

    private fun seedServer(server: Server) {
        runBlocking {
            entryPoint().serverRepository().addServer(server)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(AutomationTags.MAIN_TAB_SERVERS).performClick()
        scrollToServerRow(server.id)
    }

    private fun ensureKeyServerExists(
        displayName: String,
        host: String,
        port: Int,
        username: String,
        keyName: String,
        keyText: String
    ) {
        val serverExists = runBlocking {
            entryPoint().serverRepository()
                .getAllServers()
                .first()
                .any { it.displayName == displayName }
        }
        if (!serverExists) {
            seedKeyServer(
                displayName = displayName,
                host = host,
                port = port,
                username = username,
                keyName = keyName,
                keyText = keyText
            )
        }
    }

    private fun entryPoint(): AutomationSeedBridgeEntryPoint {
        val applicationContext = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        return AutomationSeedBridge.from(applicationContext)
    }
}
