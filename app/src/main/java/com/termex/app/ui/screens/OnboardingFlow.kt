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
import androidx.compose.ui.res.stringResource
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingFlow(
    onComplete: (demoModeActivated: Boolean) -> Unit,
    onEnableDemoMode: () -> Unit = {}
) {
    val onboardingPages = listOf(
        OnboardingPage(
            iconResId = R.mipmap.ic_launcher,
            title = stringResource(R.string.onboarding_welcome_title),
            description = stringResource(R.string.onboarding_welcome_description),
            isFirstPage = true
        ),
        OnboardingPage(
            icon = Icons.Default.Dns,
            title = stringResource(R.string.onboarding_servers_title),
            description = stringResource(R.string.onboarding_servers_description)
        ),
        OnboardingPage(
            icon = Icons.Default.Key,
            title = stringResource(R.string.onboarding_keys_title),
            description = stringResource(R.string.onboarding_keys_description)
        ),
        OnboardingPage(
            icon = Icons.Default.Code,
            title = stringResource(R.string.onboarding_snippets_title),
            description = stringResource(R.string.onboarding_snippets_description)
        )
    )
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
                                    Toast.makeText(context, context.getString(R.string.onboarding_demo_mode_enabled), Toast.LENGTH_SHORT).show()
                                }
                                logoTapCount >= 3 -> {
                                    val remaining = 5 - logoTapCount
                                    Toast.makeText(context, context.getString(R.string.onboarding_demo_mode_taps_remaining, remaining), Toast.LENGTH_SHORT).show()
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (pagerState.currentPage < onboardingPages.lastIndex) {
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(onboardingPages.lastIndex)
                                }
                            }
                        ) {
                            Text(stringResource(R.string.onboarding_skip))
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
                            if (pagerState.currentPage == onboardingPages.lastIndex) stringResource(R.string.onboarding_get_started)
                            else stringResource(R.string.onboarding_next)
                        )
                    }
                }

                // Explicit "Try Demo Mode" button on the last page — matches iOS behavior
                if (pagerState.currentPage == onboardingPages.lastIndex && !demoModeActivated) {
                    TextButton(
                        onClick = {
                            demoModeActivated = true
                            onEnableDemoMode()
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_try_demo_mode),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
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
    val windowWidth = LocalContext.current.resources.configuration.screenWidthDp.dp
    val isTablet = windowWidth >= 600.dp
    val iconSize = if (isTablet) 180.dp else 120.dp
    val titleStyle = if (isTablet) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineMedium
    val bodyStyle = if (isTablet) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge
    val topSpacing = if (isTablet) 48.dp else 32.dp
    val betweenSpacing = if (isTablet) 24.dp else 16.dp

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = if (isTablet) 640.dp else Int.MAX_VALUE.dp)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
        val iconModifier = Modifier
            .size(iconSize)
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

        Spacer(modifier = Modifier.height(topSpacing))

        Text(
            text = page.title,
            style = titleStyle,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(betweenSpacing))

        Text(
            text = page.description,
            style = bodyStyle,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        }
    }
}
