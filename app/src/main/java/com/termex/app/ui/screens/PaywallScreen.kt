package com.termex.app.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.android.billingclient.api.ProductDetails
import com.termex.app.R
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
    
    // Product details for dynamic pricing
    var productDetails by remember { mutableStateOf<ProductDetails?>(null) }
    var isLoadingPrice by remember { mutableStateOf(true) }

    val loadProductDetails: () -> Unit = {
        scope.launch {
            isLoadingPrice = true
            productDetails = subscriptionManager.getProductDetails()
            isLoadingPrice = false
        }
    }

    // Fetch product details on launch
    LaunchedEffect(Unit) {
        loadProductDetails()
    }
    
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
    
    val pricingInfo = remember(productDetails) {
        buildPricingInfo(productDetails)
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
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher),
                contentDescription = "Termex app icon",
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(16.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))

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
            
            // Dynamic pricing from Google Play
            when {
                isLoadingPrice -> {
                    CircularProgressIndicator(modifier = Modifier.height(24.dp))
                }
                productDetails == null -> {
                    Text(
                        text = "Unable to load subscription details.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    TextButton(onClick = loadProductDetails) {
                        Text("Tap to retry")
                    }
                }
                else -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        pricingInfo.trialText?.let { trialText ->
                            Text(
                                text = trialText,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        pricingInfo.priceText?.let { priceText ->
                            Text(
                                text = priceText,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Text(
                            text = "Cancel anytime in Settings",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    scope.launch {
                        val details = productDetails ?: subscriptionManager.getProductDetails()
                        if (details != null && activity != null) {
                            subscriptionManager.launchSubscriptionFlow(activity, details)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = productDetails != null
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(pricingInfo.buttonTitle)
                    pricingInfo.buttonSubtext?.let { subtext ->
                        Text(
                            text = subtext,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                }
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

private data class PricingInfo(
    val trialText: String?,
    val priceText: String?,
    val buttonTitle: String,
    val buttonSubtext: String?
)

private fun buildPricingInfo(productDetails: ProductDetails?): PricingInfo {
    if (productDetails == null) {
        return PricingInfo(
            trialText = null,
            priceText = null,
            buttonTitle = "Start Free Trial",
            buttonSubtext = null
        )
    }

    val offer = selectPreferredOffer(productDetails)
    val phases = offer?.pricingPhases?.pricingPhaseList.orEmpty()
    val trialPhase = phases.firstOrNull { it.priceAmountMicros == 0L }
    val paidPhase = phases.lastOrNull { it.priceAmountMicros > 0L } ?: phases.lastOrNull()

    val trialText = trialPhase?.billingPeriod?.let { period ->
        formatTrialPeriod(period)?.let { "$it free trial" }
    }
    val priceText = paidPhase?.let { phase ->
        val period = formatBillingPeriod(phase.billingPeriod)
        val basePrice = "${phase.formattedPrice}/$period"
        if (trialPhase != null) {
            "$basePrice after trial"
        } else {
            basePrice
        }
    }

    val buttonTitle = if (trialPhase != null) "Start Free Trial" else "Subscribe"
    val buttonSubtext = null

    return PricingInfo(
        trialText = trialText,
        priceText = priceText,
        buttonTitle = buttonTitle,
        buttonSubtext = buttonSubtext
    )
}

private fun selectPreferredOffer(
    productDetails: ProductDetails
): ProductDetails.SubscriptionOfferDetails? {
    val offers = productDetails.subscriptionOfferDetails ?: return null
    return offers.firstOrNull { offer ->
        offer.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L }
    } ?: offers.firstOrNull()
}

private data class PeriodParts(
    val years: Int,
    val months: Int,
    val weeks: Int,
    val days: Int
)

private fun parsePeriod(period: String): PeriodParts? {
    val regex = Regex("^P(?:(\\d+)Y)?(?:(\\d+)M)?(?:(\\d+)W)?(?:(\\d+)D)?$")
    val match = regex.matchEntire(period) ?: return null
    val years = match.groupValues[1].toIntOrNull() ?: 0
    val months = match.groupValues[2].toIntOrNull() ?: 0
    val weeks = match.groupValues[3].toIntOrNull() ?: 0
    val days = match.groupValues[4].toIntOrNull() ?: 0
    return PeriodParts(years, months, weeks, days)
}

private fun formatTrialPeriod(period: String): String? {
    val parts = parsePeriod(period) ?: return null
    val totalDays = parts.days + (parts.weeks * 7)
    return when {
        totalDays > 0 -> "${totalDays}-day"
        parts.months > 0 -> if (parts.months == 1) "1-month" else "${parts.months}-month"
        parts.years > 0 -> if (parts.years == 1) "1-year" else "${parts.years}-year"
        else -> null
    }
}

private fun formatBillingPeriod(period: String): String {
    val parts = parsePeriod(period) ?: return "year"
    return when {
        parts.years > 0 -> if (parts.years == 1) "year" else "${parts.years} years"
        parts.months > 0 -> if (parts.months == 1) "month" else "${parts.months} months"
        parts.weeks > 0 -> if (parts.weeks == 1) "week" else "${parts.weeks} weeks"
        parts.days > 0 -> if (parts.days == 1) "day" else "${parts.days} days"
        else -> "year"
    }
}
