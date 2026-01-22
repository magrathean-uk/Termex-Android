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

    // Nested destinations
    data object Terminal : Route("terminal/{serverId}") {
        fun createRoute(serverId: String) = "terminal/$serverId"
    }
    data object ServerSettings : Route("server_settings?serverId={serverId}") {
        fun createRoute(serverId: String? = null) =
            if (serverId != null) "server_settings?serverId=$serverId"
            else "server_settings"
    }
    data object PortForwarding : Route("port_forwarding/{serverId}") {
        fun createRoute(serverId: String) = "port_forwarding/$serverId"
    }
    data object SSHConfigBrowser : Route("ssh_config_browser")
    data object MultiTerminal : Route("multi_terminal/{workplaceId}") {
        fun createRoute(workplaceId: String) = "multi_terminal/$workplaceId"
    }
}
