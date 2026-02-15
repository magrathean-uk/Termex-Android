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
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.termex.app.R
import com.termex.app.ui.navigation.Route

@Composable
fun MainTabs(
    rootNavController: NavHostController
) {
    val navController = rememberNavController()
    var selectedItem by remember { mutableIntStateOf(0) }
    
    val items = listOf(
        stringResource(R.string.nav_servers),
        stringResource(R.string.nav_keys),
        stringResource(R.string.nav_snippets),
        stringResource(R.string.nav_settings)
    )
    val icons = listOf(
        Icons.Default.Dns,
        Icons.Default.Key,
        Icons.Default.Code,
        Icons.Default.Settings
    )
    val routes = listOf(
        Route.Servers.route,
        Route.Keys.route,
        Route.Snippets.route,
        Route.Settings.route
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(icons[index], contentDescription = item) },
                        label = { Text(item) },
                        selected = selectedItem == index,
                        onClick = {
                            selectedItem = index
                            navController.navigate(routes[index]) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
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
            modifier = Modifier.padding(padding),
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
                    onEditServer = { server ->
                        rootNavController.navigate(Route.ServerSettings.createRoute(server.id))
                    },
                    onPortForwarding = { server ->
                        rootNavController.navigate(Route.PortForwarding.createRoute(server.id))
                    }
                )
            }
            composable(Route.Keys.route) { KeyListScreen() }
            composable(Route.Snippets.route) { SnippetListScreen() }
            composable(Route.Settings.route) {
                SettingsScreen(
                    onNavigateToWorkplaces = {
                        rootNavController.navigate(Route.Workplaces.route)
                    }
                )
            }
        }
    }
}
