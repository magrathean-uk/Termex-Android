package com.termex.app.ui.navigation

sealed class Route(val route: String) {

    // Top-level destinations
    data object Onboarding : Route("onboarding")
    data object Paywall : Route("paywall")
    data object Main : Route("main")

    // Main tab destinations
    data object Servers : Route("servers")
    data object Keys : Route("keys")
    data object Snippets : Route("snippets")
    data object Settings : Route("settings")
    data object Workplaces : Route("workplaces")
    data object Certificates : Route("certificates")
    data object KnownHosts : Route("known_hosts")
    data object Diagnostics : Route("diagnostics")

    // Nested destinations
    data object Terminal : Route("terminal/{serverId}") {
        fun createRoute(serverId: String) = "terminal/$serverId"
    }
    data object ServerSettings : Route("server_settings?serverId={serverId}&prefillHost={prefillHost}&prefillPort={prefillPort}&prefillUser={prefillUser}&prefillKeyPath={prefillKeyPath}&prefillJumpHost={prefillJumpHost}&prefillForwardAgent={prefillForwardAgent}&prefillIdentitiesOnly={prefillIdentitiesOnly}") {
        fun createRoute(
            serverId: String? = null,
            prefillHost: String = "",
            prefillPort: Int = 0,
            prefillUser: String = "",
            prefillKeyPath: String = "",
            prefillJumpHost: String = "",
            prefillForwardAgent: Boolean = false,
            prefillIdentitiesOnly: Boolean = false
        ) = buildString {
            append("server_settings")
            if (serverId != null) append("?serverId=$serverId")
            else append("?serverId=")
            append("&prefillHost=${android.net.Uri.encode(prefillHost)}")
            append("&prefillPort=$prefillPort")
            append("&prefillUser=${android.net.Uri.encode(prefillUser)}")
            append("&prefillKeyPath=${android.net.Uri.encode(prefillKeyPath)}")
            append("&prefillJumpHost=${android.net.Uri.encode(prefillJumpHost)}")
            append("&prefillForwardAgent=$prefillForwardAgent")
            append("&prefillIdentitiesOnly=$prefillIdentitiesOnly")
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
