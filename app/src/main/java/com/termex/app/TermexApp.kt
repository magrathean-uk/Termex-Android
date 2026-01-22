package com.termex.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.termex.app.core.billing.SubscriptionState
import com.termex.app.ui.navigation.Route
import com.termex.app.ui.screens.MainTabs
import com.termex.app.ui.screens.MultiTerminalScreen
import com.termex.app.ui.screens.OnboardingFlow
import com.termex.app.ui.screens.PaywallScreen
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

    val navController = rememberNavController()

    // Determine start destination
    // Demo mode users bypass the paywall
    // Show paywall unless actively subscribed (LOADING, ERROR, NOT_SUBSCRIBED all show paywall)
    val startDestination = when {
        !hasCompletedOnboarding -> Route.Onboarding.route
        demoModeEnabled -> Route.Main.route
        subscriptionState is SubscriptionState.SUBSCRIBED -> Route.Main.route
        else -> Route.Paywall.route  // NOT_SUBSCRIBED, LOADING, ERROR all show paywall
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Route.Onboarding.route) {
            OnboardingFlow(
                onComplete = {
                    viewModel.completeOnboarding()
                    // Navigate to main only if demo mode or subscribed, otherwise to paywall
                    val destination = when {
                        demoModeEnabled -> Route.Main.route
                        subscriptionState is SubscriptionState.SUBSCRIBED -> Route.Main.route
                        else -> Route.Paywall.route
                    }
                    navController.navigate(destination) {
                        popUpTo(Route.Onboarding.route) { inclusive = true }
                    }
                },
                onEnableDemoMode = {
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
                }
            )
        }
        
        composable(Route.Main.route) {
            MainTabs(rootNavController = navController)
        }
        
        composable(
            route = Route.Terminal.route,
            arguments = listOf(navArgument("serverId") { type = NavType.StringType })
        ) { backStackEntry ->
            val serverId = backStackEntry.arguments?.getString("serverId") ?: return@composable
            TerminalScreen(
                serverId = serverId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Route.ServerSettings.route,
            arguments = listOf(navArgument("serverId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val serverId = backStackEntry.arguments?.getString("serverId")
            ServerSettingsScreen(
                serverId = serverId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Route.MultiTerminal.route,
            arguments = listOf(navArgument("workplaceId") { type = NavType.StringType })
        ) { backStackEntry ->
            val workplaceId = backStackEntry.arguments?.getString("workplaceId") ?: return@composable
            MultiTerminalScreen(
                workplaceId = workplaceId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Route.Workplaces.route) {
            WorkplacesScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenMultiTerminal = { workplaceId ->
                    navController.navigate(Route.MultiTerminal.createRoute(workplaceId))
                }
            )
        }
    }
}
