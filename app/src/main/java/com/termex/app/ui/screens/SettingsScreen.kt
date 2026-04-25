package com.termex.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.termex.app.BuildConfig
import com.termex.app.R
import com.termex.app.data.prefs.KeepAliveInterval
import com.termex.app.data.prefs.LinkHandlingMode
import com.termex.app.data.prefs.ThemeMode
import com.termex.app.ui.AutomationTags
import com.termex.app.ui.theme.TerminalColorScheme
import com.termex.app.ui.theme.TerminalFont
import com.termex.app.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToKnownHosts: () -> Unit = {},
    onNavigateToSSHConfigBrowser: () -> Unit = {},
    onNavigateToDiagnostics: () -> Unit = {},
    onNavigateToServerTransfer: () -> Unit = {},
    onNavigateToBackupTransfer: () -> Unit = {},
    onNavigateToExtraKeys: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val terminalSettings by viewModel.terminalSettings.collectAsState()
    val terminalColorScheme by viewModel.terminalColorScheme.collectAsState()
    val keepAliveInterval by viewModel.keepAliveInterval.collectAsState()
    val demoModeEnabled by viewModel.demoModeEnabled.collectAsState()
    val biometricLockEnabled by viewModel.biometricLockEnabled.collectAsState()
    val linkHandlingMode by viewModel.linkHandlingMode.collectAsState()
    val terminalExtraKeys by viewModel.terminalExtraKeys.collectAsState()
    val diagnosticsSummary by viewModel.diagnosticsSummary.collectAsState()
    val appLockState by viewModel.appLockState.collectAsState()
    val biometricAvailability = appLockState.biometricAvailability

    var showKeepAliveMenu by remember { mutableStateOf(false) }
    var showColorSchemeMenu by remember { mutableStateOf(false) }
    var showLinkMenu by remember { mutableStateOf(false) }
    var showFontMenu by remember { mutableStateOf(false) }
    var showFontSizeMenu by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.title_settings)) })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(AutomationTags.SETTINGS_SCREEN)
        ) {
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.title_theme)) },
                    supportingContent = { Text(themeMode.label) },
                    modifier = Modifier.clickable {
                        val next = when (themeMode) {
                            ThemeMode.AUTO -> ThemeMode.LIGHT
                            ThemeMode.LIGHT -> ThemeMode.DARK
                            ThemeMode.DARK -> ThemeMode.AUTO
                        }
                        viewModel.setThemeMode(next)
                    }
                )
                HorizontalDivider()
            }

            item {
                val biometricToggleEnabled =
                    biometricAvailability == com.termex.app.core.security.BiometricAvailability.Available ||
                        biometricLockEnabled
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_biometric_lock)) },
                    supportingContent = {
                        Text(viewModel.appLockCoordinator.settingsDescription(appLockState))
                    },
                    trailingContent = {
                        Switch(
                            checked = biometricLockEnabled,
                            onCheckedChange = { viewModel.setBiometricLockEnabled(it) },
                            enabled = biometricToggleEnabled
                        )
                    },
                    modifier = Modifier.clickable(enabled = biometricToggleEnabled) {
                        viewModel.setBiometricLockEnabled(!biometricLockEnabled)
                    }
                )
                HorizontalDivider()
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_known_hosts)) },
                    supportingContent = { Text(stringResource(R.string.settings_known_hosts_supporting)) },
                    modifier = Modifier.clickable { onNavigateToKnownHosts() }
                )
                HorizontalDivider()
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_ssh_config_browser)) },
                    supportingContent = { Text(stringResource(R.string.settings_ssh_config_browser_supporting)) },
                    modifier = Modifier.clickable { onNavigateToSSHConfigBrowser() }
                )
                HorizontalDivider()
            }

            item {
                Box {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.title_color_scheme)) },
                        supportingContent = { Text(terminalColorScheme.displayName) },
                        modifier = Modifier.clickable { showColorSchemeMenu = true }
                    )
                    DropdownMenu(
                        expanded = showColorSchemeMenu,
                        onDismissRequest = { showColorSchemeMenu = false }
                    ) {
                        TerminalColorScheme.entries.forEach { scheme ->
                            DropdownMenuItem(
                                text = { Text(scheme.displayName) },
                                onClick = {
                                    viewModel.setColorScheme(scheme)
                                    showColorSchemeMenu = false
                                }
                            )
                        }
                    }
                }
                HorizontalDivider()
            }

            item {
                Box {
                    ListItem(
                        headlineContent = { Text("Link handling") },
                        supportingContent = { Text(linkHandlingMode.label) },
                        modifier = Modifier.clickable { showLinkMenu = true }
                    )
                    DropdownMenu(
                        expanded = showLinkMenu,
                        onDismissRequest = { showLinkMenu = false }
                    ) {
                        LinkHandlingMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode.label) },
                                onClick = {
                                    viewModel.setLinkHandlingMode(mode)
                                    showLinkMenu = false
                                }
                            )
                        }
                    }
                }
                HorizontalDivider()
            }

            item {
                ListItem(
                    headlineContent = { Text("Extra keys") },
                    supportingContent = { Text("${terminalExtraKeys.size} enabled") },
                    modifier = Modifier
                        .clickable { onNavigateToExtraKeys() }
                        .testTag(AutomationTags.SETTINGS_EXTRA_KEYS_ENTRY)
                )
                HorizontalDivider()
            }

            item {
                Box {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.title_font)) },
                        supportingContent = { Text(terminalSettings.fontFamily) },
                        modifier = Modifier.clickable { showFontMenu = true }
                    )
                    DropdownMenu(
                        expanded = showFontMenu,
                        onDismissRequest = { showFontMenu = false }
                    ) {
                        TerminalFont.entries.forEach { font ->
                            DropdownMenuItem(
                                text = { Text(font.displayName) },
                                onClick = {
                                    viewModel.setFontFamily(font.displayName)
                                    showFontMenu = false
                                }
                            )
                        }
                    }
                }
                HorizontalDivider()
            }

            item {
                Box {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.title_font_size)) },
                        supportingContent = { Text("${terminalSettings.fontSize}pt") },
                        modifier = Modifier.clickable { showFontSizeMenu = true }
                    )
                    DropdownMenu(
                        expanded = showFontSizeMenu,
                        onDismissRequest = { showFontSizeMenu = false }
                    ) {
                        listOf(10, 12, 14, 16, 18, 20, 24).forEach { size ->
                            DropdownMenuItem(
                                text = { Text("${size}pt") },
                                onClick = {
                                    viewModel.setFontSize(size)
                                    showFontSizeMenu = false
                                }
                            )
                        }
                    }
                }
                HorizontalDivider()
            }

            item {
                Box {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.title_keep_alive_interval)) },
                        supportingContent = { Text(keepAliveInterval.label) },
                        modifier = Modifier.clickable { showKeepAliveMenu = true }
                    )
                    DropdownMenu(
                        expanded = showKeepAliveMenu,
                        onDismissRequest = { showKeepAliveMenu = false }
                    ) {
                        KeepAliveInterval.entries.forEach { interval ->
                            DropdownMenuItem(
                                text = { Text(interval.label) },
                                onClick = {
                                    viewModel.setKeepAliveInterval(interval)
                                    showKeepAliveMenu = false
                                }
                            )
                        }
                    }
                }
                HorizontalDivider()
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.diagnostics_title)) },
                    supportingContent = { Text(diagnosticsSummary) },
                    modifier = Modifier.clickable { onNavigateToDiagnostics() }
                )
                HorizontalDivider()
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_backup_transfer)) },
                    supportingContent = { Text(stringResource(R.string.settings_backup_transfer_supporting)) },
                    modifier = Modifier
                        .clickable { onNavigateToBackupTransfer() }
                        .testTag(AutomationTags.SETTINGS_SYNC_ENTRY)
                )
                HorizontalDivider()
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.backup_transfer_archive_title)) },
                    supportingContent = { Text(stringResource(R.string.backup_transfer_archive_supporting)) },
                    modifier = Modifier.clickable { onNavigateToServerTransfer() }
                )
                HorizontalDivider()
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_clear_saved_sessions)) },
                    supportingContent = { Text(stringResource(R.string.settings_clear_saved_sessions_supporting)) },
                    modifier = Modifier.clickable { viewModel.clearSavedSessions() }
                )
                HorizontalDivider()
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.action_reset_app)) },
                    supportingContent = { Text(stringResource(R.string.reset_app_supporting)) },
                    modifier = Modifier.clickable { showResetDialog = true }
                )
                HorizontalDivider()
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.title_version)) },
                    supportingContent = {
                        val versionName = BuildConfig.VERSION_NAME
                        val label = if (demoModeEnabled) {
                            stringResource(R.string.version_demo_format, versionName)
                        } else {
                            versionName
                        }
                        Text(label)
                    },
                    modifier = Modifier.clickable { viewModel.onVersionTap() }
                )
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.title_reset_app)) },
            text = { Text(stringResource(R.string.reset_app_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetApp()
                        showResetDialog = false
                    }
                ) {
                    Text(stringResource(R.string.action_reset))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
