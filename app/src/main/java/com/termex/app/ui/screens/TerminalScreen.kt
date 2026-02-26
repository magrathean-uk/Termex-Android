package com.termex.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.termex.app.R
import com.termex.app.core.ssh.SSHConnectionState
import com.termex.app.ui.components.HostKeyVerificationDialog
import com.termex.app.ui.components.PasswordDialog
import com.termex.app.ui.components.TerminalKeyboard
import com.termex.app.ui.components.TerminalView
import com.termex.app.ui.theme.TerminalColorScheme
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
    val hostKeyVerification by viewModel.hostKeyVerification.collectAsState()
    val lines by viewModel.terminalLines.collectAsState()
    val cursorPosition by viewModel.cursorPosition.collectAsState()
    val showSnippetPicker by viewModel.showSnippetPicker.collectAsState()
    val snippets by viewModel.snippets.collectAsState()
    val terminalSettings by viewModel.terminalSettings.collectAsState()
    val colorScheme = TerminalColorScheme.fromName(terminalSettings.colorScheme)

    var ctrlActive by remember { mutableStateOf(false) }
    var altActive by remember { mutableStateOf(false) }
    var showDisconnectConfirm by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val snippetSheetState = rememberModalBottomSheetState()

    LaunchedEffect(serverId) {
        viewModel.connect(serverId)
    }

    BackHandler {
        if (connectionState is SSHConnectionState.Connected) {
            showDisconnectConfirm = true
        } else {
            onNavigateBack()
        }
    }

    // Disconnect confirmation dialog
    if (showDisconnectConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDisconnectConfirm = false },
            title = { Text(stringResource(R.string.terminal_disconnect_title)) },
            text = { Text(stringResource(R.string.terminal_disconnect_message, currentServer?.hostname ?: "")) },
            confirmButton = {
                TextButton(onClick = {
                    showDisconnectConfirm = false
                    viewModel.disconnect()
                    onNavigateBack()
                }) { Text(stringResource(R.string.terminal_disconnect)) }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectConfirm = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    if (needsPassword) {
        PasswordDialog(
            hostname = currentServer?.hostname ?: "",
            onConfirm = { password -> viewModel.providePassword(password) },
            onDismiss = {
                viewModel.disconnect()
                onNavigateBack()
            }
        )
    }

    hostKeyVerification?.let { verification ->
        HostKeyVerificationDialog(
            verification = verification,
            onAccept = { viewModel.acceptHostKey() },
            onReject = {
                viewModel.rejectHostKey()
                viewModel.disconnect()
                onNavigateBack()
            }
        )
    }

    // Snippet picker bottom sheet
    if (showSnippetPicker) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.hideSnippetPicker() },
            sheetState = snippetSheetState,
            containerColor = Color(0xFF1C1C1E),
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF636366)) }
        ) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text(
                    text = stringResource(R.string.title_snippets),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
                HorizontalDivider(color = Color(0xFF3A3A3C))
                if (snippets.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.terminal_no_snippets),
                            color = Color(0xFF636366),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn {
                        items(snippets) { snippet ->
                            ListItem(
                                headlineContent = {
                                    Text(snippet.name, color = Color.White)
                                },
                                supportingContent = {
                                    Text(
                                        text = snippet.command,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF98989D),
                                        maxLines = 2
                                    )
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clickable {
                                    viewModel.insertSnippet(snippet)
                                }
                            )
                            HorizontalDivider(color = Color(0xFF3A3A3C))
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = when (connectionState) {
                                is SSHConnectionState.Connected -> currentServer?.displayName ?: stringResource(R.string.terminal_title)
                                is SSHConnectionState.Connecting -> stringResource(R.string.terminal_connecting)
                                is SSHConnectionState.VerifyingHostKey -> stringResource(R.string.terminal_verifying_host)
                                is SSHConnectionState.Error -> stringResource(R.string.terminal_error)
                                is SSHConnectionState.Disconnected -> stringResource(R.string.terminal_disconnected)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        if (connectionState is SSHConnectionState.Connected) {
                            currentServer?.let { server ->
                                Text(
                                    text = "${server.username}@${server.hostname}:${server.port}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF98989D)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.disconnect()
                        onNavigateBack()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (connectionState is SSHConnectionState.Connected) {
                        // Snippets button
                        IconButton(onClick = { viewModel.showSnippetPicker() }) {
                            Icon(
                                Icons.Default.Code,
                                contentDescription = "Snippets",
                                tint = Color(0xFF98989D)
                            )
                        }
                        // Port Forwarding button (navigate back then to port forwarding)
                        currentServer?.let { server ->
                            if (server.portForwards.isNotEmpty()) {
                                IconButton(onClick = {
                                    // Navigate to port forwarding for this server
                                    // We pass a callback via the parent nav but can't easily do here
                                    // Show a simple indicator instead
                                }) {
                                    Icon(
                                        Icons.Default.Dns,
                                        contentDescription = "Port Forwards",
                                        tint = Color(0xFF30D158)
                                    )
                                }
                            }
                        }
                        IconButton(onClick = {
                            viewModel.disconnect()
                            onNavigateBack()
                        }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Disconnect",
                                tint = Color(0xFFFF453A)
                            )
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
        // Hidden text field for soft keyboard input — placed outside `when` to avoid state loss
        var textInput by remember { mutableStateOf("") }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            when (connectionState) {
                is SSHConnectionState.Connecting,
                is SSHConnectionState.VerifyingHostKey -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(color = Color(0xFF30D158))
                            Text(
                                text = if (connectionState is SSHConnectionState.Connecting)
                                    stringResource(R.string.terminal_connecting_to, currentServer?.displayName ?: "")
                                else stringResource(R.string.terminal_verifying_host_key),
                                color = Color(0xFF98989D)
                            )
                        }
                    }
                }

                is SSHConnectionState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.terminal_connection_failed),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFFFF453A)
                            )
                            Text(
                                text = (connectionState as SSHConnectionState.Error).message,
                                color = Color(0xFF98989D),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            TextButton(onClick = {
                                viewModel.disconnect()
                                onNavigateBack()
                            }) {
                                Text(stringResource(R.string.action_close), color = Color(0xFF0A84FF))
                            }
                        }
                    }
                }

                is SSHConnectionState.Connected -> {
                    // Terminal view
                    TerminalView(
                        lines = lines,
                        cursorPosition = cursorPosition,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        fontSize = terminalSettings.fontSize.toFloat(),
                        backgroundColor = colorScheme.background,
                        foregroundColor = colorScheme.foreground,
                        cursorColor = colorScheme.cursor,
                        onTap = { keyboardController?.show() },
                        onScroll = { delta -> viewModel.scrollTerminal(delta) },
                        onSizeChanged = { cols, rows, widthPx, heightPx ->
                            viewModel.resizeTerminal(cols, rows, widthPx, heightPx)
                        }
                    )

                    // Extended keyboard bar
                    TerminalKeyboard(
                        ctrlActive = ctrlActive,
                        altActive = altActive,
                        onCtrlToggle = { ctrlActive = !ctrlActive },
                        onAltToggle = { altActive = !altActive },
                        onKeyPress = { sequence ->
                            viewModel.sendInput(sequence)
                            if (!sequence.startsWith("\u001B")) {
                                ctrlActive = false
                                altActive = false
                            }
                        }
                    )

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
                            .focusRequester(focusRequester),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.None
                        )
                    )
                }

                is SSHConnectionState.Disconnected -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = stringResource(R.string.terminal_disconnected), color = Color(0xFF636366))
                            TextButton(onClick = { viewModel.connect(serverId) }) {
                                Text(stringResource(R.string.terminal_reconnect), color = Color(0xFF0A84FF))
                            }
                        }
                    }
                }
            }
        }
    }
}
