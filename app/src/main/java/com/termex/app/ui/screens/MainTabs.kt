package com.termex.app.ui.screens

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.termex.app.R
import com.termex.app.ui.AutomationTags
import com.termex.app.ui.navigation.Route

@Composable
fun MainTabs(
    rootNavController: NavHostController
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    data class TabItem(val label: String, val icon: ImageVector, val route: String)

    val tabs = listOf(
        TabItem(stringResource(R.string.nav_servers), Icons.Default.Dns, Route.Servers.route),
        TabItem(stringResource(R.string.nav_workplaces), Icons.Default.Workspaces, Route.Workplaces.route),
        TabItem(stringResource(R.string.nav_keys), Icons.Default.Key, Route.Keys.route),
        TabItem(stringResource(R.string.nav_snippets), Icons.Default.Code, Route.Snippets.route),
        TabItem(stringResource(R.string.nav_settings), Icons.Default.Settings, Route.Settings.route),
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        modifier = Modifier.testTag(
                            when (tab.route) {
                                Route.Servers.route -> AutomationTags.MAIN_TAB_SERVERS
                                Route.Keys.route -> AutomationTags.MAIN_TAB_KEYS
                                Route.Settings.route -> AutomationTags.MAIN_TAB_SETTINGS
                                else -> "main_tab_${tab.route}"
                            }
                        ),
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Route.Servers.route,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(200)) },
            popEnterTransition = { fadeIn(animationSpec = tween(300)) },
            popExitTransition = { fadeOut(animationSpec = tween(200)) }
        ) {
            composable(Route.Servers.route) {
                ServerListScreen(
                    onServerClick = { server ->
                        rootNavController.navigate(Route.Terminal.createRoute(server.id))
                    },
                    onAddServer = {
                        rootNavController.navigate(Route.ServerSettings.createRoute(null))
                    },
                    onOpenSharedServers = {
                        rootNavController.navigate(Route.SharedServers.route)
                    },
                    onEditServer = { server ->
                        rootNavController.navigate(Route.ServerSettings.createRoute(server.id))
                    },
                    onPortForwarding = { server ->
                        rootNavController.navigate(Route.PortForwarding.createRoute(server.id))
                    },
                    onOpenMultiTerminal = { workplaceId ->
                        rootNavController.navigate(Route.MultiTerminal.createRoute(workplaceId))
                    },
                    onOpenKnownHosts = {
                        rootNavController.navigate(Route.KnownHosts.route)
                    },
                    onOpenCertificates = {
                        rootNavController.navigate(Route.Certificates.route)
                    },
                    onOpenSSHConfigBrowser = {
                        rootNavController.navigate(Route.SSHConfigBrowser.route)
                    }
                )
            }
            composable(Route.Workplaces.route) {
                WorkplacesScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onOpenMultiTerminal = { workplaceId ->
                        rootNavController.navigate(Route.MultiTerminal.createRoute(workplaceId))
                    }
                )
            }
            composable(Route.Keys.route) {
                KeyListScreen(
                    onNavigateToCertificates = {
                        rootNavController.navigate(Route.Certificates.route)
                    }
                )
            }
            composable(Route.Snippets.route) { SnippetListScreen() }
            composable(Route.Settings.route) {
                SettingsScreen(
                    onNavigateToKnownHosts = {
                        rootNavController.navigate(Route.KnownHosts.route)
                    },
                    onNavigateToSSHConfigBrowser = {
                        rootNavController.navigate(Route.SSHConfigBrowser.route)
                    },
                    onNavigateToDiagnostics = {
                        rootNavController.navigate(Route.Diagnostics.route)
                    },
                    onNavigateToServerTransfer = {
                        rootNavController.navigate(Route.ServerTransfer.route)
                    },
                    onNavigateToBackupTransfer = {
                        rootNavController.navigate(Route.SyncSettings.route)
                    },
                    onNavigateToExtraKeys = {
                        rootNavController.navigate(Route.ExtraKeysSettings.route)
                    }
                )
            }
        }
    }
}
