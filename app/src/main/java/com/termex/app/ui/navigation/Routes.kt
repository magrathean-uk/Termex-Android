package com.termex.app.ui.navigation

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Route(val route: String) {

    // Top-level destinations
    data object Onboarding : Route("onboarding")
    data object Main : Route("main")

    // Main tab destinations
    data object Servers : Route("servers")
    data object SharedServers : Route("shared_servers")
    data object Keys : Route("keys")
    data object Snippets : Route("snippets")
    data object Settings : Route("settings")
    data object Workplaces : Route("workplaces")
    data object Certificates : Route("certificates")
    data object KnownHosts : Route("known_hosts")
    data object Diagnostics : Route("diagnostics")
    data object ServerTransfer : Route("server_transfer")
    data object SyncSettings : Route("sync_settings")
    data object ExtraKeysSettings : Route("extra_keys_settings")
    data object KeyRepair : Route("key_repair/{serverId}") {
        fun createRoute(serverId: String) = "key_repair/$serverId"
    }
    data object CertificateRepair : Route("certificate_repair/{serverId}") {
        fun createRoute(serverId: String) = "certificate_repair/$serverId"
    }

    // Nested destinations
    data object Terminal : Route("terminal/{serverId}") {
        fun createRoute(serverId: String) = "terminal/$serverId"
    }
    data object ServerSettings : Route("server_settings?serverId={serverId}&prefillHost={prefillHost}&prefillPort={prefillPort}&prefillUser={prefillUser}&prefillKeyPath={prefillKeyPath}&prefillCertificatePath={prefillCertificatePath}&prefillJumpHost={prefillJumpHost}&prefillForwardAgent={prefillForwardAgent}&prefillIdentitiesOnly={prefillIdentitiesOnly}&prefillForwards={prefillForwards}") {
        fun createRoute(
            serverId: String? = null,
            prefillHost: String = "",
            prefillPort: Int = 0,
            prefillUser: String = "",
            prefillKeyPath: String = "",
            prefillCertificatePath: String = "",
            prefillJumpHost: String = "",
            prefillForwardAgent: Boolean = false,
            prefillIdentitiesOnly: Boolean = false,
            prefillForwards: String = ""
        ) = buildString {
            append("server_settings")
            if (serverId != null) append("?serverId=$serverId")
            else append("?serverId=")
            append("&prefillHost=${encodeRouteArg(prefillHost)}")
            append("&prefillPort=$prefillPort")
            append("&prefillUser=${encodeRouteArg(prefillUser)}")
            append("&prefillKeyPath=${encodeRouteArg(prefillKeyPath)}")
            append("&prefillCertificatePath=${encodeRouteArg(prefillCertificatePath)}")
            append("&prefillJumpHost=${encodeRouteArg(prefillJumpHost)}")
            append("&prefillForwardAgent=$prefillForwardAgent")
            append("&prefillIdentitiesOnly=$prefillIdentitiesOnly")
            append("&prefillForwards=${encodeRouteArg(prefillForwards)}")
        }
    }
    data object PortForwarding : Route("port_forwarding/{serverId}") {
        fun createRoute(serverId: String) = "port_forwarding/$serverId"
    }
    data object SSHConfigBrowser : Route("ssh_config_browser")
    data object MultiTerminal : Route("multi_terminal/{workplaceId}") {
        fun createRoute(workplaceId: String) = "multi_terminal/$workplaceId"
    }
}

private fun encodeRouteArg(value: String): String {
    return URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")
}
