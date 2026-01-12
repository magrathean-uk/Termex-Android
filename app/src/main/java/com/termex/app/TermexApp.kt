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
import com.termex.app.ui.screens.OnboardingFlow
import com.termex.app.ui.screens.PaywallScreen
import com.termex.app.ui.screens.TerminalScreen
import com.termex.app.ui.viewmodel.AppViewModel

@Composable
fun TermexApp(
    viewModel: AppViewModel = hiltViewModel()
) {
    val hasCompletedOnboarding by viewModel.hasCompletedOnboarding.collectAsState()
    val subscriptionState by viewModel.subscriptionState.collectAsState()
    
    val navController = rememberNavController()
    
    // Determine start destination
    val startDestination = when {
        !hasCompletedOnboarding -> Route.Onboarding.route
        subscriptionState is SubscriptionState.NOT_SUBSCRIBED -> Route.Paywall.route
        else -> Route.Main.route
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Route.Onboarding.route) {
            OnboardingFlow(
                onComplete = {
                    viewModel.completeOnboarding()
                    navController.navigate(Route.Paywall.route) {
                        popUpTo(Route.Onboarding.route) { inclusive = true }
                    }
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
                onSkip = {
                    // For testing: skip paywall
                    navController.navigate(Route.Main.route) {
                        popUpTo(Route.Paywall.route) { inclusive = true }
                    }
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
    }
}
