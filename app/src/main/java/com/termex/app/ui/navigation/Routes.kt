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
    
    // Nested destinations
    data object Terminal : Route("terminal/{serverId}") {
        fun createRoute(serverId: String) = "terminal/$serverId"
    }
    data object AddEditServer : Route("add_edit_server?serverId={serverId}") {
        fun createRoute(serverId: String? = null) = 
            if (serverId != null) "add_edit_server?serverId=$serverId" 
            else "add_edit_server"
    }
}
