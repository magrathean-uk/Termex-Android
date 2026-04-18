package com.termex.app

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.termex.app.BuildConfig
import com.termex.app.core.RootRouteDestination
import com.termex.app.core.billing.SubscriptionState
import com.termex.app.core.ssh.SSHConfigParser
import com.termex.app.ui.navigation.Route
import com.termex.app.ui.screens.CertificatesScreen
import com.termex.app.ui.screens.DiagnosticsScreen
import com.termex.app.ui.screens.ExtraKeysSettingsScreen
import com.termex.app.ui.screens.KeyListScreen
import com.termex.app.ui.screens.KnownHostsScreen
import com.termex.app.ui.screens.MainTabs
import com.termex.app.ui.screens.MultiTerminalScreen
import com.termex.app.ui.screens.OnboardingFlow
import com.termex.app.ui.screens.PaywallScreen
import com.termex.app.ui.screens.PortForwardingScreen
import com.termex.app.ui.screens.SharedServersScreen
import com.termex.app.ui.screens.SSHConfigBrowserScreen
import com.termex.app.ui.screens.ServerSettingsScreen
import com.termex.app.ui.screens.ServerTransferScreen
import com.termex.app.ui.screens.SyncSettingsScreen
import com.termex.app.ui.screens.TerminalScreen
import com.termex.app.ui.screens.WorkplacesScreen
import com.termex.app.ui.viewmodel.AppViewModel

private const val AD_HOC_WORKSPACE_ROUTE = "workspace_terminal?serverIds={serverIds}"

internal enum class StartupGate {
    LOADING,
    ONBOARDING,
    PAYWALL,
    MAIN
}

internal fun resolveStartupGate(
    hasCompletedOnboarding: Boolean,
    subscriptionState: SubscriptionState,
    demoModeActive: Boolean,
    paywallBypassed: Boolean
): StartupGate {
    if (hasCompletedOnboarding && subscriptionState is SubscriptionState.LOADING && !paywallBypassed && !demoModeActive) {
        return StartupGate.LOADING
    }
    return when {
        !hasCompletedOnboarding -> StartupGate.ONBOARDING
        paywallBypassed || demoModeActive || subscriptionState is SubscriptionState.SUBSCRIBED -> StartupGate.MAIN
        else -> StartupGate.PAYWALL
    }
}

private fun createAdHocWorkspaceRoute(serverIds: List<String>): String {
    return "workspace_terminal?serverIds=" + Uri.encode(serverIds.joinToString(","))
}

@Composable
fun TermexApp(
    viewModel: AppViewModel = hiltViewModel()
) {
    val hasCompletedOnboarding by viewModel.hasCompletedOnboarding.collectAsState()
    val subscriptionState by viewModel.subscriptionState.collectAsState()
    val demoModeEnabled by viewModel.demoModeEnabled.collectAsState()
    val startupRouteState by viewModel.startupRouteState.collectAsState()
    var demoModeActivated by rememberSaveable { mutableStateOf(false) }

    val paywallBypassed = BuildConfig.BYPASS_PAYWALL
    val demoModeActive = demoModeEnabled || demoModeActivated
    val paywallRequired = !paywallBypassed &&
        !demoModeActive &&
        subscriptionState !is SubscriptionState.SUBSCRIBED &&
        subscriptionState !is SubscriptionState.LOADING

    LaunchedEffect(demoModeEnabled) {
        if (demoModeEnabled) {
            demoModeActivated = false
        }
    }

    val startupGate = resolveStartupGate(
        hasCompletedOnboarding = hasCompletedOnboarding,
        subscriptionState = subscriptionState,
        demoModeActive = demoModeActive,
        paywallBypassed = paywallBypassed
    )

    if (startupGate == StartupGate.LOADING) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val startDestination = when (startupGate) {
        StartupGate.ONBOARDING -> Route.Onboarding.route
        StartupGate.PAYWALL -> Route.Paywall.route
        StartupGate.MAIN -> Route.Main.route
        StartupGate.LOADING -> Route.Main.route
    }
    
    key(startDestination) {
        val navController = rememberNavController()
        var hasNavigatedToStartupRoute by remember(startupGate) { mutableStateOf(false) }

        LaunchedEffect(startupGate, startupRouteState) {
            if (startupGate != StartupGate.MAIN || hasNavigatedToStartupRoute) return@LaunchedEffect
            hasNavigatedToStartupRoute = true
            viewModel.saveRootRoute(startupRouteState.persistedRoute)
            when (val destination = startupRouteState.destination) {
                RootRouteDestination.None -> Unit
                is RootRouteDestination.Server -> {
                    navController.navigate(Route.Terminal.createRoute(destination.serverId))
                }
                is RootRouteDestination.Workplace -> {
                    navController.navigate(Route.MultiTerminal.createRoute(destination.workplaceId))
                }
                is RootRouteDestination.Workspace -> {
                    navController.navigate(createAdHocWorkspaceRoute(destination.serverIds))
                }
            }
        }

        NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = {
                slideInHorizontally(initialOffsetX = { 300 }, animationSpec = tween(350)) +
                    fadeIn(animationSpec = tween(350))
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { -300 }, animationSpec = tween(350)) +
                    fadeOut(animationSpec = tween(200))
            },
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { -300 }, animationSpec = tween(350)) +
                    fadeIn(animationSpec = tween(350))
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { 300 }, animationSpec = tween(350)) +
                    fadeOut(animationSpec = tween(200))
            }
        ) {
            composable(Route.Onboarding.route) {
                OnboardingFlow(
                    onComplete = { demoModeActivated ->
                        viewModel.completeOnboarding()
                        // Navigate to main only if demo mode or subscribed, otherwise to paywall
                        val destination = when {
                            paywallBypassed -> Route.Main.route
                            demoModeActivated || demoModeActive -> Route.Main.route
                            subscriptionState is SubscriptionState.SUBSCRIBED -> Route.Main.route
                            else -> Route.Paywall.route
                        }
                        navController.navigate(destination) {
                            popUpTo(Route.Onboarding.route) { inclusive = true }
                        }
                    },
                    onEnableDemoMode = {
                        demoModeActivated = true
                        viewModel.enableDemoMode()
                    }
                )
            }

            composable(Route.Paywall.route) {
                PaywallScreen(
                    onSubscribed = {
                        viewModel.refreshSubscription()
                        navController.navigate(Route.Main.route) {
                            popUpTo(Route.Paywall.route) { inclusive = true }
                        }
                    },
                    onRestore = {
                        viewModel.refreshSubscription()
                    },
                    onDemoMode = {
                        demoModeActivated = true
                        viewModel.enableDemoMode()
                        navController.navigate(Route.Main.route) {
                            popUpTo(Route.Paywall.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Route.Main.route) {
                PaywallGate(
                    paywallRequired = paywallRequired,
                    onSubscribed = { viewModel.refreshSubscription() },
                    onRestore = { viewModel.refreshSubscription() },
                    onDemoMode = {
                        demoModeActivated = true
                        viewModel.enableDemoMode()
                    }
                ) {
                    MainTabs(rootNavController = navController)
                }
            }

            composable(Route.SharedServers.route) {
                PaywallGate(
                    paywallRequired = paywallRequired,
                    onSubscribed = { viewModel.refreshSubscription() },
                    onRestore = { viewModel.refreshSubscription() }
                ) {
                    SharedServersScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onImportServer = { host, port ->
                            navController.navigate(
                                Route.ServerSettings.createRoute(
                                    serverId = null,
                                    prefillHost = host,
                                    prefillPort = port
                                )
                            ) {
                                popUpTo(Route.SharedServers.route) { inclusive = true }
                            }
                        }
                    )
                }
            }

            composable(
                route = Route.Terminal.route,
                arguments = listOf(navArgument("serverId") { type = NavType.StringType })
            ) { backStackEntry ->
                val serverId = backStackEntry.arguments?.getString("serverId") ?: return@composable
                PaywallGate(
                    paywallRequired = paywallRequired,
                    onSubscribed = { viewModel.refreshSubscription() },
                    onRestore = { viewModel.refreshSubscription() }
                ) {
                    TerminalScreen(
                        serverId = serverId,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToPortForwarding = { id ->
                            navController.navigate(Route.PortForwarding.createRoute(id))
                        }
                    )
                }
            }

        composable(
            route = Route.ServerSettings.route,
            arguments = listOf(
                navArgument("serverId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("prefillHost") { type = NavType.StringType; defaultValue = "" },
                navArgument("prefillPort") { type = NavType.IntType; defaultValue = 0 },
                navArgument("prefillUser") { type = NavType.StringType; defaultValue = "" },
                navArgument("prefillKeyPath") { type = NavType.StringType; defaultValue = "" },
                navArgument("prefillCertificatePath") { type = NavType.StringType; defaultValue = "" },
                navArgument("prefillJumpHost") { type = NavType.StringType; defaultValue = "" },
                navArgument("prefillForwardAgent") { type = NavType.BoolType; defaultValue = false },
                navArgument("prefillIdentitiesOnly") { type = NavType.BoolType; defaultValue = false },
                navArgument("prefillForwards") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val serverId = backStackEntry.arguments?.getString("serverId")
            val prefillHost = backStackEntry.arguments?.getString("prefillHost") ?: ""
            val prefillPort = backStackEntry.arguments?.getInt("prefillPort") ?: 0
            val prefillUser = backStackEntry.arguments?.getString("prefillUser") ?: ""
            val prefillKeyPath = backStackEntry.arguments?.getString("prefillKeyPath") ?: ""
            val prefillCertificatePath = backStackEntry.arguments?.getString("prefillCertificatePath") ?: ""
            val prefillJumpHost = backStackEntry.arguments?.getString("prefillJumpHost") ?: ""
            val prefillForwardAgent = backStackEntry.arguments?.getBoolean("prefillForwardAgent") ?: false
            val prefillIdentitiesOnly = backStackEntry.arguments?.getBoolean("prefillIdentitiesOnly") ?: false
            val prefillForwards = backStackEntry.arguments?.getString("prefillForwards") ?: ""
            PaywallGate(
                paywallRequired = paywallRequired,
                onSubscribed = { viewModel.refreshSubscription() },
                onRestore = { viewModel.refreshSubscription() }
            ) {
                ServerSettingsScreen(
                    serverId = serverId,
                    prefillHost = prefillHost,
                    prefillPort = prefillPort,
                    prefillUser = prefillUser,
                    prefillKeyPath = prefillKeyPath,
                    prefillCertificatePath = prefillCertificatePath,
                    prefillJumpHost = prefillJumpHost,
                    prefillForwardAgent = prefillForwardAgent,
                    prefillIdentitiesOnly = prefillIdentitiesOnly,
                    prefillForwards = prefillForwards,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        composable(
            route = Route.PortForwarding.route,
            arguments = listOf(navArgument("serverId") { type = NavType.StringType })
        ) { backStackEntry ->
            val serverId = backStackEntry.arguments?.getString("serverId") ?: return@composable
            PaywallGate(
                paywallRequired = paywallRequired,
                onSubscribed = { viewModel.refreshSubscription() },
                onRestore = { viewModel.refreshSubscription() }
            ) {
                PortForwardingScreen(
                    serverId = serverId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

            composable(
                route = Route.MultiTerminal.route,
                arguments = listOf(navArgument("workplaceId") { type = NavType.StringType })
            ) { backStackEntry ->
                val workplaceId = backStackEntry.arguments?.getString("workplaceId") ?: return@composable
            PaywallGate(
                paywallRequired = paywallRequired,
                onSubscribed = { viewModel.refreshSubscription() },
                onRestore = { viewModel.refreshSubscription() }
            ) {
                MultiTerminalScreen(
                    workplaceId = workplaceId,
                    onNavigateBack = { navController.popBackStack() }
                )
                }
            }

            composable(
                route = AD_HOC_WORKSPACE_ROUTE,
                arguments = listOf(
                    navArgument("serverIds") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) {
                PaywallGate(
                    paywallRequired = paywallRequired,
                    onSubscribed = { viewModel.refreshSubscription() },
                    onRestore = { viewModel.refreshSubscription() }
                ) {
                    MultiTerminalScreen(
                        workplaceId = "",
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }

        composable(Route.Workplaces.route) {
            PaywallGate(
                paywallRequired = paywallRequired,
                onSubscribed = { viewModel.refreshSubscription() },
                onRestore = { viewModel.refreshSubscription() }
            ) {
                WorkplacesScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onOpenMultiTerminal = { workplaceId ->
                        navController.navigate(Route.MultiTerminal.createRoute(workplaceId))
                    }
                )
            }
        }

        composable(Route.KnownHosts.route) {
            PaywallGate(
                paywallRequired = paywallRequired,
                onSubscribed = { viewModel.refreshSubscription() },
                onRestore = { viewModel.refreshSubscription() }
            ) {
                KnownHostsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

            composable(Route.Certificates.route) {
                PaywallGate(
                    paywallRequired = paywallRequired,
                    onSubscribed = { viewModel.refreshSubscription() },
                    onRestore = { viewModel.refreshSubscription() }
                ) {
                    CertificatesScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }

            composable(
                route = Route.KeyRepair.route,
                arguments = listOf(navArgument("serverId") { type = NavType.StringType })
            ) { backStackEntry ->
                val serverId = backStackEntry.arguments?.getString("serverId") ?: return@composable
                PaywallGate(
                    paywallRequired = paywallRequired,
                    onSubscribed = { viewModel.refreshSubscription() },
                    onRestore = { viewModel.refreshSubscription() }
                ) {
                    KeyListScreen(
                        onNavigateBack = { navController.popBackStack() },
                        repairTargetServerId = serverId,
                        autoOpenImportDialog = true
                    )
                }
            }

            composable(
                route = Route.CertificateRepair.route,
                arguments = listOf(navArgument("serverId") { type = NavType.StringType })
            ) { backStackEntry ->
                val serverId = backStackEntry.arguments?.getString("serverId") ?: return@composable
                PaywallGate(
                    paywallRequired = paywallRequired,
                    onSubscribed = { viewModel.refreshSubscription() },
                    onRestore = { viewModel.refreshSubscription() }
                ) {
                    CertificatesScreen(
                        onNavigateBack = { navController.popBackStack() },
                        repairTargetServerId = serverId,
                        autoOpenImportDialog = true
                    )
                }
            }


        composable(Route.Diagnostics.route) {
            PaywallGate(
                paywallRequired = paywallRequired,
                onSubscribed = { viewModel.refreshSubscription() },
                onRestore = { viewModel.refreshSubscription() }
            ) {
                DiagnosticsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        composable(Route.ServerTransfer.route) {
            PaywallGate(
                paywallRequired = paywallRequired,
                onSubscribed = { viewModel.refreshSubscription() },
                onRestore = { viewModel.refreshSubscription() }
            ) {
                ServerTransferScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

            composable(Route.SyncSettings.route) {
                PaywallGate(
                    paywallRequired = paywallRequired,
                    onSubscribed = { viewModel.refreshSubscription() },
                    onRestore = { viewModel.refreshSubscription() }
                ) {
                    SyncSettingsScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToServerTransfer = {
                            navController.navigate(Route.ServerTransfer.route)
                        },
                        onNavigateToServerRepair = { serverId ->
                            navController.navigate(Route.ServerSettings.createRoute(serverId = serverId))
                        },
                        onNavigateToKeyRepair = { serverId ->
                            navController.navigate(Route.KeyRepair.createRoute(serverId))
                        },
                        onNavigateToCertificateRepair = { serverId ->
                            navController.navigate(Route.CertificateRepair.createRoute(serverId))
                        }
                    )
                }
            }

            composable(Route.ExtraKeysSettings.route) {
                ExtraKeysSettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

        composable(Route.SSHConfigBrowser.route) {
            PaywallGate(
                paywallRequired = paywallRequired,
                onSubscribed = { viewModel.refreshSubscription() },
                onRestore = { viewModel.refreshSubscription() }
            ) {
                SSHConfigBrowserScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onApplyHost = { host ->
                        navController.navigate(
                            Route.ServerSettings.createRoute(
                                serverId = null,
                                prefillHost = host.hostname ?: host.host,
                                prefillPort = host.port ?: 22,
                                prefillUser = host.user ?: "",
                                prefillKeyPath = host.identityFile ?: "",
                                prefillCertificatePath = host.certificateFile ?: "",
                                prefillJumpHost = host.proxyJump ?: "",
                                prefillForwardAgent = host.forwardAgent ?: false,
                                prefillIdentitiesOnly = host.identitiesOnly ?: false,
                                prefillForwards = SSHConfigParser.encodePortForwards(host.portForwards)
                            )
                        )
                    }
                )
            }
        }
        }
    }
}

@Composable
private fun PaywallGate(
    paywallRequired: Boolean,
    onSubscribed: () -> Unit,
    onRestore: () -> Unit,
    onDemoMode: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    if (paywallRequired) {
        PaywallScreen(
            onSubscribed = onSubscribed,
            onRestore = onRestore,
            onDemoMode = onDemoMode
        )
    } else {
        content()
    }
}
