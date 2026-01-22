package com.termex.app.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.termex.app.core.billing.SubscriptionManager
import com.termex.app.core.billing.SubscriptionState
import kotlinx.coroutines.launch

@Composable
fun PaywallScreen(
    onSubscribed: () -> Unit,
    onRestore: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val subscriptionManager = SubscriptionManager.getInstance(context)
    val subscriptionState by subscriptionManager.subscriptionState.collectAsState()
    val scope = rememberCoroutineScope()
    
    // CRITICAL: Block back button - paywall CANNOT be skipped
    // Users MUST subscribe or restore purchases to proceed
    BackHandler(enabled = true) {
        // Do nothing - intentionally blocking back navigation
        // The paywall is mandatory and cannot be bypassed
    }
    
    // Navigate when subscribed
    LaunchedEffect(subscriptionState) {
        if (subscriptionState is SubscriptionState.SUBSCRIBED) {
            onSubscribed()
        }
    }
    
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Termex Pro",
                style = MaterialTheme.typography.headlineLarge
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Unlock unlimited SSH connections, snippets, and more.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Features list
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                FeatureItem("✓ Unlimited server connections")
                FeatureItem("✓ SSH key management")
                FeatureItem("✓ Command snippets")
                FeatureItem("✓ Custom themes")
                FeatureItem("✓ Priority support")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "7-day free trial, then £9.99/year",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    scope.launch {
                        val productDetails = subscriptionManager.getProductDetails()
                        if (productDetails != null && activity != null) {
                            subscriptionManager.launchSubscriptionFlow(activity, productDetails)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Free Trial")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onRestore,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Restore Purchases")
            }
        }
    }
}

@Composable
private fun FeatureItem(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}
