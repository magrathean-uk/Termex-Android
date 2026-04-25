package com.termex.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.termex.app.R
import com.termex.app.ui.components.HostKeyVerificationDialog
import com.termex.app.ui.components.PasswordDialog
import com.termex.app.domain.PortForwardType
import com.termex.app.ui.AutomationTags
import com.termex.app.ui.viewmodel.PortForwardingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortForwardingScreen(
    serverId: String,
    onNavigateBack: () -> Unit,
    viewModel: PortForwardingViewModel = hiltViewModel()
) {
    val supportedTypes = listOf(PortForwardType.LOCAL, PortForwardType.REMOTE, PortForwardType.DYNAMIC)
    val server by viewModel.server.collectAsState()
    val showDialog by viewModel.showDialog.collectAsState()
    val formState by viewModel.formState.collectAsState()
    val activeForwards by viewModel.activeForwards.collectAsState()
    val needsPassword by viewModel.needsPassword.collectAsState()
    val hostKeyVerification by viewModel.hostKeyVerification.collectAsState()

    if (needsPassword) {
        PasswordDialog(
            hostname = server?.hostname ?: "",
            onConfirm = { password -> viewModel.providePassword(password) },
            onDismiss = { viewModel.cancelPasswordPrompt() }
        )
    }

    hostKeyVerification?.let { verification ->
        HostKeyVerificationDialog(
            verification = verification,
            onAccept = { viewModel.acceptHostKey() },
            onReject = { viewModel.rejectHostKey() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.port_forwarding_title, server?.displayName ?: "")) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                modifier = Modifier.testTag(AutomationTags.PORT_FORWARD_ADD),
                onClick = { viewModel.showAddDialog() }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Port Forward")
            }
        }
    ) { padding ->
        val portForwards = server?.portForwards?.filter { it.type in supportedTypes } ?: emptyList()

        if (portForwards.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.port_forwarding_empty))
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(portForwards, key = { it.id }) { pf ->
                    val activeState = activeForwards.find { it.config.id == pf.id }
                    val isActive = activeState?.isActive == true
                    val error = activeState?.error

                    ListItem(
                        headlineContent = { Text(pf.displayString) },
                        supportingContent = {
                            if (error != null) {
                                Text(error, color = MaterialTheme.colorScheme.error)
                            } else {
                                Text(stringResource(if (isActive) R.string.port_forwarding_active else R.string.port_forwarding_inactive))
                            }
                        },
                        leadingContent = {
                            Icon(
                                if (isActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = if (isActive) "Stop" else "Start",
                                tint = if (isActive) Color.Green else Color.Gray,
                                modifier = Modifier.clickable { viewModel.togglePortForward(pf) }
                            )
                        },
                        trailingContent = {
                            IconButton(onClick = { viewModel.deletePortForward(pf) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        },
                        modifier = Modifier.clickable { viewModel.showEditDialog(pf) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (showDialog) {
        var showTypeMenu by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { viewModel.dismissDialog() },
            title = { Text(stringResource(if (formState.isEditing) R.string.port_forwarding_edit_title else R.string.port_forwarding_add_title)) },
            text = {
                Column {
                    Box {
                        OutlinedTextField(
                            value = formState.type.name,
                            onValueChange = {},
                            label = { Text(stringResource(R.string.port_forwarding_type)) },
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(AutomationTags.PORT_FORWARD_TYPE)
                                .clickable { showTypeMenu = true }
                        )
                        DropdownMenu(
                            expanded = showTypeMenu,
                            onDismissRequest = { showTypeMenu = false }
                        ) {
                            supportedTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.name) },
                                    onClick = {
                                        viewModel.updateType(type)
                                        showTypeMenu = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = formState.localPort,
                        onValueChange = { viewModel.updateLocalPort(it) },
                        label = { Text(stringResource(R.string.port_forwarding_local_port)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .testTag(AutomationTags.PORT_FORWARD_LOCAL_PORT)
                    )

                    if (formState.type != PortForwardType.DYNAMIC) {
                        OutlinedTextField(
                            value = formState.remoteHost,
                            onValueChange = { viewModel.updateRemoteHost(it) },
                            label = { Text(stringResource(R.string.port_forwarding_remote_host)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .testTag(AutomationTags.PORT_FORWARD_REMOTE_HOST)
                        )

                        OutlinedTextField(
                            value = formState.remotePort,
                            onValueChange = { viewModel.updateRemotePort(it) },
                            label = { Text(stringResource(R.string.port_forwarding_remote_port)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .testTag(AutomationTags.PORT_FORWARD_REMOTE_PORT)
                        )
                    }

                    // Bind address — shown for REMOTE forwards (iOS feature parity)
                    if (formState.type == PortForwardType.REMOTE) {
                        OutlinedTextField(
                            value = formState.bindAddress,
                            onValueChange = { viewModel.updateBindAddress(it) },
                            label = { Text(stringResource(R.string.port_forwarding_bind_address)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .testTag(AutomationTags.PORT_FORWARD_BIND_ADDRESS)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    modifier = Modifier.testTag(AutomationTags.PORT_FORWARD_SAVE),
                    onClick = { viewModel.savePortForward() },
                    enabled = formState.localPort.isNotBlank() &&
                            (formState.type == PortForwardType.DYNAMIC || formState.remotePort.isNotBlank())
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDialog() }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
