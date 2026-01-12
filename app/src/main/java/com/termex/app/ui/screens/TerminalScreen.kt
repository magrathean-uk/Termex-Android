package com.termex.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.termex.app.core.ssh.SSHConnectionState
import com.termex.app.ui.components.TerminalKeyboard
import com.termex.app.ui.components.TerminalView
import com.termex.app.ui.viewmodel.TerminalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    serverId: String,
    onNavigateBack: () -> Unit,
    viewModel: TerminalViewModel = hiltViewModel()
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val currentServer by viewModel.currentServer.collectAsState()
    val needsPassword by viewModel.needsPassword.collectAsState()
    val lines by viewModel.terminalLines.collectAsState()
    val cursorPosition by viewModel.cursorPosition.collectAsState()
    
    var ctrlActive by remember { mutableStateOf(false) }
    var altActive by remember { mutableStateOf(false) }
    
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    
    // Connect on launch
    LaunchedEffect(serverId) {
        viewModel.connect(serverId)
    }
    
    // Handle back press
    BackHandler {
        viewModel.disconnect()
        onNavigateBack()
    }
    
    // Password dialog
    if (needsPassword) {
        PasswordDialog(
            hostname = currentServer?.hostname ?: "",
            onConfirm = { password ->
                viewModel.providePassword(password)
            },
            onDismiss = {
                viewModel.disconnect()
                onNavigateBack()
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = when (connectionState) {
                            is SSHConnectionState.Connected -> currentServer?.displayName ?: "Terminal"
                            is SSHConnectionState.Connecting -> "Connecting..."
                            is SSHConnectionState.Error -> "Error"
                            is SSHConnectionState.Disconnected -> "Disconnected"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.disconnect()
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (connectionState is SSHConnectionState.Connected) {
                        IconButton(onClick = {
                            viewModel.disconnect()
                            onNavigateBack()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Disconnect")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1C1C1E)
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            when (connectionState) {
                is SSHConnectionState.Connecting -> {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(16.dp)
                    )
                }
                is SSHConnectionState.Error -> {
                    Text(
                        text = "Error: ${(connectionState as SSHConnectionState.Error).message}",
                        color = Color.Red,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                is SSHConnectionState.Connected -> {
                    // Terminal view
                    TerminalView(
                        lines = lines,
                        cursorPosition = cursorPosition,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        onTap = {
                            keyboardController?.show()
                        }
                    )
                    
                    // Extended keyboard
                    TerminalKeyboard(
                        ctrlActive = ctrlActive,
                        altActive = altActive,
                        onCtrlToggle = { ctrlActive = !ctrlActive },
                        onAltToggle = { altActive = !altActive },
                        onKeyPress = { sequence ->
                            viewModel.sendInput(sequence)
                            if (!sequence.startsWith("\u001B")) {
                                // Reset modifiers after non-modifier key
                                ctrlActive = false
                                altActive = false
                            }
                        }
                    )
                    
                    // Hidden text field for keyboard input
                    var textInput by remember { mutableStateOf("") }
                    androidx.compose.foundation.text.BasicTextField(
                        value = textInput,
                        onValueChange = { newValue ->
                            if (newValue.length > textInput.length) {
                                val newChars = newValue.substring(textInput.length)
                                viewModel.sendInput(newChars)
                            }
                            textInput = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(0.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.None
                        ),
                        keyboardActions = KeyboardActions(
                            onAny = {
                                viewModel.sendInput("\n")
                            }
                        )
                    )
                }
                is SSHConnectionState.Disconnected -> {
                    Text(
                        text = "Disconnected",
                        color = Color.Gray,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PasswordDialog(
    hostname: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Password Required") },
        text = {
            Column {
                Text("Enter password for $hostname")
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(password) }) {
                Text("Connect")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
