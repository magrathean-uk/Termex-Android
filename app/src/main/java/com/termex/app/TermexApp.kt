package com.termex.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.termex.app.core.billing.SubscriptionState
import com.termex.app.ui.navigation.Route
import com.termex.app.ui.screens.CertificatesScreen
import com.termex.app.ui.screens.DiagnosticsScreen
import com.termex.app.ui.screens.KnownHostsScreen
import com.termex.app.ui.screens.MainTabs
import com.termex.app.ui.screens.MultiTerminalScreen
import com.termex.app.ui.screens.OnboardingFlow
import com.termex.app.ui.screens.PaywallScreen
import com.termex.app.ui.screens.PortForwardingScreen
import com.termex.app.ui.screens.SSHConfigBrowserScreen
import com.termex.app.ui.screens.ServerSettingsScreen
import com.termex.app.ui.screens.TerminalScreen
import com.termex.app.ui.screens.WorkplacesScreen
import com.termex.app.ui.viewmodel.AppViewModel

@Composable
fun TermexApp(
    viewModel: AppViewModel = hiltViewModel()
) {
    val hasCompletedOnboarding by viewModel.hasCompletedOnboarding.collectAsState()
    val subscriptionState by viewModel.subscriptionState.collectAsState()
    val demoModeEnabled by viewModel.demoModeEnabled.collectAsState()
    var demoModeActivated by rememberSaveable { mutableStateOf(false) }

    val navController = rememberNavController()
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

    // Show a loading screen while billing state is being determined (prevents paywall flash)
    if (hasCompletedOnboarding && subscriptionState is SubscriptionState.LOADING && !paywallBypassed && !demoModeActive) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Determine start destination
    // Demo mode users bypass the paywall
    val startDestination = when {
        !hasCompletedOnboarding -> Route.Onboarding.route
        !paywallRequired -> Route.Main.route
        else -> Route.Paywall.route
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
                navArgument("prefillJumpHost") { type = NavType.StringType; defaultValue = "" },
                navArgument("prefillForwardAgent") { type = NavType.BoolType; defaultValue = false },
                navArgument("prefillIdentitiesOnly") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val serverId = backStackEntry.arguments?.getString("serverId")
            val prefillHost = backStackEntry.arguments?.getString("prefillHost") ?: ""
            val prefillPort = backStackEntry.arguments?.getInt("prefillPort") ?: 0
            val prefillUser = backStackEntry.arguments?.getString("prefillUser") ?: ""
            val prefillKeyPath = backStackEntry.arguments?.getString("prefillKeyPath") ?: ""
            val prefillJumpHost = backStackEntry.arguments?.getString("prefillJumpHost") ?: ""
            val prefillForwardAgent = backStackEntry.arguments?.getBoolean("prefillForwardAgent") ?: false
            val prefillIdentitiesOnly = backStackEntry.arguments?.getBoolean("prefillIdentitiesOnly") ?: false
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
                    prefillJumpHost = prefillJumpHost,
                    prefillForwardAgent = prefillForwardAgent,
                    prefillIdentitiesOnly = prefillIdentitiesOnly,
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
                                prefillJumpHost = host.proxyJump ?: "",
                                prefillForwardAgent = host.forwardAgent ?: false,
                                prefillIdentitiesOnly = host.identitiesOnly ?: false
                            )
                        )
                    }
                )
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
