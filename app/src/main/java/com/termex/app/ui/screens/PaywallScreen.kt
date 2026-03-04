package com.termex.app.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.android.billingclient.api.ProductDetails
import com.termex.app.R
import com.termex.app.core.billing.SubscriptionState
import com.termex.app.ui.viewmodel.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Premium gradient colors
private val GradientStart = Color(0xFF1A1A2E)
private val GradientMid = Color(0xFF16213E)
private val GradientEnd = Color(0xFF0F3460)
private val AccentGold = Color(0xFFFFD700)
private val AccentTeal = Color(0xFF00D4AA)

@Composable
fun PaywallScreen(
    onSubscribed: () -> Unit,
    onRestore: () -> Unit,
    onDemoMode: (() -> Unit)? = null
) {
    val appViewModel: AppViewModel = hiltViewModel()
    val context = LocalContext.current
    val activity = context as? Activity
    val subscriptionState by appViewModel.subscriptionState.collectAsState()
    val scope = rememberCoroutineScope()
    
    var productDetails by remember { mutableStateOf<ProductDetails?>(null) }
    var isLoadingPrice by remember { mutableStateOf(true) }
    var showContent by remember { mutableStateOf(false) }

    val loadProductDetails: () -> Unit = {
        scope.launch {
            isLoadingPrice = true
            productDetails = appViewModel.getProductDetails()
            isLoadingPrice = false
        }
    }

    LaunchedEffect(Unit) {
        loadProductDetails()
        delay(100)
        showContent = true
    }
    
    // Block back button - paywall CANNOT be skipped
    BackHandler(enabled = true) {
        // Intentionally blocking back navigation - paywall is mandatory
    }
    
    LaunchedEffect(subscriptionState) {
        if (subscriptionState is SubscriptionState.SUBSCRIBED) {
            onSubscribed()
        }
    }
    
    val startFreeTrialText = stringResource(R.string.paywall_start_free_trial)
    val subscribeNowText = stringResource(R.string.paywall_subscribe_now)
    val freeTrialFormat = stringResource(R.string.paywall_free_trial_format)
    val afterTrialFormat = stringResource(R.string.paywall_price_after_trial_format)

    val pricingInfo = remember(productDetails) {
        buildPricingInfo(productDetails, startFreeTrialText, subscribeNowText, freeTrialFormat, afterTrialFormat)
    }
    
    // Pulsing animation for the subscribe button
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(GradientStart, GradientMid, GradientEnd)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            // Animated Logo
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(600)) + slideInVertically(
                    initialOffsetY = { -50 },
                    animationSpec = tween(600)
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .shadow(16.dp, CircleShape)
                            .clip(RoundedCornerShape(24.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.mipmap.ic_launcher),
                            contentDescription = "Termex app icon",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp
                            ),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AccentGold
                        ) {
                            Text(
                                text = stringResource(R.string.paywall_pro_badge),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = Color.Black
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Tagline
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(600, delayMillis = 200))
            ) {
                Text(
                    text = stringResource(R.string.paywall_tagline),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Features Card
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(600, delayMillis = 400)) + slideInVertically(
                    initialOffsetY = { 50 },
                    animationSpec = tween(600, delayMillis = 400)
                )
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.08f)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.paywall_everything_included),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = AccentTeal
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        PremiumFeatureItem(stringResource(R.string.paywall_feature_unlimited_servers))
                        PremiumFeatureItem(stringResource(R.string.paywall_feature_ssh_keys))
                        PremiumFeatureItem(stringResource(R.string.paywall_feature_snippets))
                        PremiumFeatureItem(stringResource(R.string.paywall_feature_port_forwarding))
                        PremiumFeatureItem(stringResource(R.string.paywall_feature_multi_terminal))
                        PremiumFeatureItem(stringResource(R.string.paywall_feature_host_key))
                        PremiumFeatureItem(stringResource(R.string.paywall_feature_jump_host))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Pricing Section
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(600, delayMillis = 600))
            ) {
                when {
                    isLoadingPrice -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = AccentTeal
                        )
                    }
                    productDetails == null -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.paywall_unable_to_load),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = loadProductDetails) {
                                Text(stringResource(R.string.paywall_tap_to_retry), color = AccentTeal)
                            }
                        }
                    }
                    else -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Trial badge
                            pricingInfo.trialText?.let { trialText ->
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = AccentGold,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = Color.Black
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = trialText.uppercase(),
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = Color.Black
                                        )
                                    }
                                }
                            }
                            
                            pricingInfo.priceText?.let { priceText ->
                                Text(
                                    text = priceText,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                            
                            Text(
                                text = stringResource(R.string.paywall_cancel_anytime),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
            // Subscribe Button
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(600, delayMillis = 800)) + slideInVertically(
                    initialOffsetY = { 30 },
                    animationSpec = tween(600, delayMillis = 800)
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                val details = productDetails ?: appViewModel.getProductDetails()
                                if (details != null && activity != null) {
                                    appViewModel.launchSubscriptionFlow(activity, details)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .scale(if (productDetails != null) pulseScale else 1f),
                        enabled = productDetails != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentTeal,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 8.dp,
                            pressedElevation = 4.dp
                        )
                    ) {
                        Text(
                            text = pricingInfo.buttonTitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedButton(
                        onClick = onRestore,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White.copy(alpha = 0.8f)
                        )
                    ) {
                        Text(stringResource(R.string.action_restore_purchases))
                    }

                    // Demo mode option — matches iOS PaywallView behavior
                    if (onDemoMode != null) {
                        TextButton(
                            onClick = onDemoMode,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.paywall_try_demo_mode),
                                color = Color.White.copy(alpha = 0.45f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Footer
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(600, delayMillis = 1000))
            ) {
                Text(
                    text = stringResource(R.string.paywall_subscription_terms),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PremiumFeatureItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = AccentTeal.copy(alpha = 0.2f),
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier
                    .padding(4.dp)
                    .size(16.dp),
                tint = AccentTeal
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

private data class PricingInfo(
    val trialText: String?,
    val priceText: String?,
    val buttonTitle: String,
    val buttonSubtext: String?
)

private fun buildPricingInfo(
    productDetails: ProductDetails?,
    startFreeTrialText: String,
    subscribeNowText: String,
    freeTrialFormat: String,
    afterTrialFormat: String
): PricingInfo {
    if (productDetails == null) {
        return PricingInfo(
            trialText = String.format(freeTrialFormat, "7-day"),
            priceText = String.format(afterTrialFormat, "$9.99/month"),
            buttonTitle = startFreeTrialText,
            buttonSubtext = null
        )
    }

    val offer = selectPreferredOffer(productDetails)
    val phases = offer?.pricingPhases?.pricingPhaseList.orEmpty()
    val trialPhase = phases.firstOrNull { it.priceAmountMicros == 0L }
    val paidPhase = phases.lastOrNull { it.priceAmountMicros > 0L } ?: phases.lastOrNull()

    val trialText = trialPhase?.billingPeriod?.let { period ->
        formatTrialPeriod(period)?.let { String.format(freeTrialFormat, it) }
    }
    val priceText = paidPhase?.let { phase ->
        val period = formatBillingPeriod(phase.billingPeriod)
        val basePrice = "${phase.formattedPrice}/$period"
        if (trialPhase != null) {
            String.format(afterTrialFormat, basePrice)
        } else {
            basePrice
        }
    }

    val buttonTitle = if (trialPhase != null) startFreeTrialText else subscribeNowText

    return PricingInfo(
        trialText = trialText,
        priceText = priceText,
        buttonTitle = buttonTitle,
        buttonSubtext = null
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
    val parts = parsePeriod(period) ?: return "month"
    return when {
        parts.years > 0 -> if (parts.years == 1) "year" else "${parts.years} years"
        parts.months > 0 -> if (parts.months == 1) "month" else "${parts.months} months"
        parts.weeks > 0 -> if (parts.weeks == 1) "week" else "${parts.weeks} weeks"
        parts.days > 0 -> if (parts.days == 1) "day" else "${parts.days} days"
        else -> "month"
    }
}
