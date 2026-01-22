package com.termex.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.termex.app.domain.AuthMode
import com.termex.app.ui.viewmodel.ServerSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSettingsScreen(
    serverId: String?,
    onNavigateBack: () -> Unit,
    viewModel: ServerSettingsViewModel = hiltViewModel()
) {
    val formState by viewModel.formState.collectAsState()
    val keys by viewModel.keys.collectAsState()
    val servers by viewModel.servers.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    var showAuthModeMenu by remember { mutableStateOf(false) }
    var showKeyMenu by remember { mutableStateOf(false) }
    var showJumpHostMenu by remember { mutableStateOf(false) }
    var showPasswordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(serverId) {
        if (serverId != null) {
            viewModel.loadServer(serverId)
        }
    }

    LaunchedEffect(isSaving) {
        if (isSaving) {
            viewModel.saveServer()
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (serverId == null) "New Server" else "Edit Server") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.triggerSave() },
                        enabled = formState.hostname.isNotBlank() && formState.username.isNotBlank()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Connection Section
            SectionHeader("CONNECTION")
            SettingsCard {
                OutlinedTextField(
                    value = formState.name,
                    onValueChange = { viewModel.updateName(it) },
                    label = { Text("Display Name") },
                    placeholder = { Text("Optional") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = formState.hostname,
                    onValueChange = { viewModel.updateHostname(it) },
                    label = { Text("Host") },
                    placeholder = { Text("example.com or IP address") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = formState.port,
                        onValueChange = { viewModel.updatePort(it) },
                        label = { Text("Port") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = formState.username,
                    onValueChange = { viewModel.updateUsername(it) },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Authentication Section
            SectionHeader("AUTHENTICATION")
            SettingsCard {
                // Auth Mode
                SettingsRow(
                    title = "Method",
                    value = when (formState.authMode) {
                        AuthMode.PASSWORD -> "Password"
                        AuthMode.KEY -> "SSH Key"
                        AuthMode.AUTO -> "Auto"
                    },
                    onClick = { showAuthModeMenu = true }
                )
                DropdownMenu(
                    expanded = showAuthModeMenu,
                    onDismissRequest = { showAuthModeMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Password") },
                        onClick = {
                            viewModel.updateAuthMode(AuthMode.PASSWORD)
                            showAuthModeMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("SSH Key") },
                        onClick = {
                            viewModel.updateAuthMode(AuthMode.KEY)
                            showAuthModeMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Auto (try both)") },
                        onClick = {
                            viewModel.updateAuthMode(AuthMode.AUTO)
                            showAuthModeMenu = false
                        }
                    )
                }
            }

            // Password Card (show for PASSWORD or AUTO)
            if (formState.authMode == AuthMode.PASSWORD || formState.authMode == AuthMode.AUTO) {
                Spacer(modifier = Modifier.height(12.dp))
                SettingsCard {
                    OutlinedTextField(
                        value = formState.password,
                        onValueChange = { viewModel.updatePassword(it) },
                        label = { Text("Password") },
                        placeholder = { Text(if (formState.hasStoredPassword) "Password saved" else "Enter password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (showPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            Row {
                                if (formState.password.isNotEmpty() || formState.hasStoredPassword) {
                                    IconButton(onClick = { viewModel.clearPassword() }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear password")
                                    }
                                }
                                IconButton(onClick = { showPasswordVisible = !showPasswordVisible }) {
                                    Icon(
                                        if (showPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (showPasswordVisible) "Hide" else "Show"
                                    )
                                }
                            }
                        }
                    )
                    if (formState.hasStoredPassword && formState.password.isEmpty()) {
                        Text(
                            text = "Password is saved. Enter new password to change or tap X to remove.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // SSH Key Card (show for KEY or AUTO)
            if (formState.authMode == AuthMode.KEY || formState.authMode == AuthMode.AUTO) {
                Spacer(modifier = Modifier.height(12.dp))
                SettingsCard {
                    SettingsRow(
                        title = "SSH Key",
                        value = formState.selectedKeyName ?: "None",
                        onClick = { showKeyMenu = true }
                    )
                    DropdownMenu(
                        expanded = showKeyMenu,
                        onDismissRequest = { showKeyMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None") },
                            onClick = {
                                viewModel.updateSelectedKey(null, null)
                                showKeyMenu = false
                            }
                        )
                        keys.forEach { key ->
                            DropdownMenuItem(
                                text = { Text(key.name) },
                                onClick = {
                                    viewModel.updateSelectedKey(key.path, key.name)
                                    showKeyMenu = false
                                }
                            )
                        }
                    }
                    if (keys.isEmpty()) {
                        Text(
                            text = "No SSH keys. Generate or import keys in the Keys tab.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Advanced Section
            SectionHeader("ADVANCED")
            SettingsCard {
                // Jump Host
                SettingsRow(
                    title = "Jump Host",
                    value = formState.jumpHostName ?: "None",
                    onClick = { showJumpHostMenu = true }
                )
                DropdownMenu(
                    expanded = showJumpHostMenu,
                    onDismissRequest = { showJumpHostMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("None") },
                        onClick = {
                            viewModel.updateJumpHost(null, null)
                            showJumpHostMenu = false
                        }
                    )
                    servers.filter { it.id != serverId && !it.isDemo }.forEach { server ->
                        DropdownMenuItem(
                            text = { Text(server.displayName) },
                            onClick = {
                                viewModel.updateJumpHost(server.id, server.displayName)
                                showJumpHostMenu = false
                            }
                        )
                    }
                }

                SettingsDivider()

                // Forward Agent
                ListItem(
                    headlineContent = { Text("Forward Agent") },
                    supportingContent = { Text("Forward SSH agent to remote host") },
                    trailingContent = {
                        Switch(
                            checked = formState.forwardAgent,
                            onCheckedChange = { viewModel.updateForwardAgent(it) }
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        trailingContent = {
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier.clickable { onClick() },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun SettingsDivider() {
    androidx.compose.material3.HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}
