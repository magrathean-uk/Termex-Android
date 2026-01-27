package com.termex.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.termex.app.R
import kotlinx.coroutines.launch

data class OnboardingPage(
    val icon: ImageVector? = null,
    val iconResId: Int? = null,
    val title: String,
    val description: String,
    val isFirstPage: Boolean = false
)

private val onboardingPages = listOf(
    OnboardingPage(
        iconResId = R.mipmap.ic_launcher,
        title = "Welcome to Termex",
        description = "A powerful SSH terminal client for Android. Connect to your servers securely from anywhere.",
        isFirstPage = true
    ),
    OnboardingPage(
        icon = Icons.Default.Dns,
        title = "Manage Servers",
        description = "Save your server connections with support for password and key-based authentication."
    ),
    OnboardingPage(
        icon = Icons.Default.Key,
        title = "SSH Key Management",
        description = "Generate and import SSH keys directly on your device. Your private keys never leave your phone."
    ),
    OnboardingPage(
        icon = Icons.Default.Code,
        title = "Command Snippets",
        description = "Save frequently used commands as snippets for quick access during your terminal sessions."
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingFlow(
    onComplete: (demoModeActivated: Boolean) -> Unit,
    onEnableDemoMode: () -> Unit = {}
) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Track taps on the logo for hidden demo mode activation
    var logoTapCount by remember { mutableIntStateOf(0) }
    var demoModeActivated by remember { mutableStateOf(false) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                OnboardingPageContent(
                    page = onboardingPages[page],
                    onLogoTap = if (onboardingPages[page].isFirstPage && !demoModeActivated) {
                        {
                            logoTapCount++
                            when {
                                logoTapCount >= 5 -> {
                                    demoModeActivated = true
                                    onEnableDemoMode()
                                    Toast.makeText(context, "Demo mode enabled", Toast.LENGTH_SHORT).show()
                                }
                                logoTapCount >= 3 -> {
                                    val remaining = 5 - logoTapCount
                                    Toast.makeText(context, "$remaining taps to enable demo mode", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } else null,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Page indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(onboardingPages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(if (isSelected) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            // Bottom buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (pagerState.currentPage < onboardingPages.lastIndex) {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(onboardingPages.lastIndex)
                            }
                        }
                    ) {
                        Text("Skip")
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Button(
                    onClick = {
                        if (pagerState.currentPage == onboardingPages.lastIndex) {
                            onComplete(demoModeActivated)
                        } else {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    }
                ) {
                    Text(
                        if (pagerState.currentPage == onboardingPages.lastIndex) "Get Started"
                        else "Next"
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    onLogoTap: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val iconModifier = Modifier
            .size(120.dp)
            .then(
                if (onLogoTap != null) {
                    Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onLogoTap() }
                } else Modifier
            )

        when {
            page.iconResId != null -> {
                Image(
                    painter = painterResource(id = page.iconResId),
                    contentDescription = "Termex app icon",
                    modifier = iconModifier.clip(RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Fit
                )
            }
            page.icon != null -> {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    modifier = iconModifier,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}
