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
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.termex.app.R
import com.termex.app.core.ssh.SSHConnectionState
import com.termex.app.ui.components.ConnectionReportSheet
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
    onNavigateToPortForwarding: ((String) -> Unit)? = null,
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
    val recentDiagnosticEvents by viewModel.recentDiagnosticEvents.collectAsState()
    val colorScheme = TerminalColorScheme.fromName(terminalSettings.colorScheme)

    var ctrlActive by remember { mutableStateOf(false) }
    var altActive by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val snippetSheetState = rememberModalBottomSheetState()
    var showConnectionReport by remember { mutableStateOf(false) }

    LaunchedEffect(serverId) {
        viewModel.connect(serverId)
    }

    // Silently focus the hidden text field when connected so input is routed correctly.
    // Does NOT call keyboardController.show() — keyboard only appears when user taps the terminal.
    LaunchedEffect(connectionState) {
        if (connectionState is SSHConnectionState.Connected) {
            try { focusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    // Back always navigates away without disconnecting — the session stays alive in the
    // background (ConnectionManager singleton + foreground service). Use the red X button
    // in the toolbar to explicitly disconnect.
    BackHandler { onNavigateBack() }

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

    if (showConnectionReport) {
        ConnectionReportSheet(
            server = currentServer,
            connectionState = connectionState,
            events = recentDiagnosticEvents,
            onDismiss = { showConnectionReport = false }
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
                    IconButton(onClick = { onNavigateBack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (connectionState is SSHConnectionState.Connected) {
                        IconButton(onClick = { showConnectionReport = true }) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = stringResource(R.string.connection_report_title),
                                tint = Color(0xFF98989D)
                            )
                        }
                        // Snippets button
                        IconButton(onClick = { viewModel.showSnippetPicker() }) {
                            Icon(
                                Icons.Default.Code,
                                contentDescription = "Snippets",
                                tint = Color(0xFF98989D)
                            )
                        }
                        // Port Forwarding button
                        currentServer?.let { server ->
                            if (server.portForwards.isNotEmpty()) {
                                IconButton(onClick = {
                                    onNavigateToPortForwarding?.invoke(server.id)
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
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { viewModel.connect(serverId) }) {
                                    Text(stringResource(R.string.terminal_reconnect), color = Color(0xFF30D158))
                                }
                                TextButton(onClick = {
                                    viewModel.disconnect()
                                    onNavigateBack()
                                }) {
                                    Text(stringResource(R.string.action_close), color = Color(0xFF0A84FF))
                                }
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
                        onTap = {
                            try { focusRequester.requestFocus() } catch (_: Exception) {}
                            keyboardController?.show()
                        },
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
                        },
                        onHideKeyboard = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
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
                            .height(1.dp)
                            .focusRequester(focusRequester)
                            .onPreviewKeyEvent { keyEvent ->
                                if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                val isCtrl = keyEvent.isCtrlPressed
                                when (keyEvent.key) {
                                    // Navigation keys → ANSI sequences
                                    Key.DirectionUp -> { viewModel.sendInput(if (isCtrl) "\u001b[1;5A" else "\u001b[A"); true }
                                    Key.DirectionDown -> { viewModel.sendInput(if (isCtrl) "\u001b[1;5B" else "\u001b[B"); true }
                                    Key.DirectionRight -> { viewModel.sendInput(if (isCtrl) "\u001b[1;5C" else "\u001b[C"); true }
                                    Key.DirectionLeft -> { viewModel.sendInput(if (isCtrl) "\u001b[1;5D" else "\u001b[D"); true }
                                    Key.Home -> { viewModel.sendInput(if (isCtrl) "\u001b[1;5H" else "\u001b[H"); true }
                                    Key.MoveEnd -> { viewModel.sendInput(if (isCtrl) "\u001b[1;5F" else "\u001b[F"); true }
                                    Key.PageUp -> { viewModel.sendInput("\u001b[5~"); true }
                                    Key.PageDown -> { viewModel.sendInput("\u001b[6~"); true }
                                    Key.Insert -> { viewModel.sendInput("\u001b[2~"); true }
                                    Key.Delete -> { viewModel.sendInput("\u001b[3~"); true }
                                    // Whitespace / control
                                    Key.Enter -> { viewModel.sendInput("\r"); true }
                                    Key.Tab -> { viewModel.sendInput("\t"); true }
                                    Key.Escape -> { viewModel.sendInput("\u001b"); true }
                                    Key.Backspace -> { viewModel.sendInput("\u007F"); true }
                                    // Function keys
                                    Key.F1 -> { viewModel.sendInput("\u001bOP"); true }
                                    Key.F2 -> { viewModel.sendInput("\u001bOQ"); true }
                                    Key.F3 -> { viewModel.sendInput("\u001bOR"); true }
                                    Key.F4 -> { viewModel.sendInput("\u001bOS"); true }
                                    Key.F5 -> { viewModel.sendInput("\u001b[15~"); true }
                                    Key.F6 -> { viewModel.sendInput("\u001b[17~"); true }
                                    Key.F7 -> { viewModel.sendInput("\u001b[18~"); true }
                                    Key.F8 -> { viewModel.sendInput("\u001b[19~"); true }
                                    Key.F9 -> { viewModel.sendInput("\u001b[20~"); true }
                                    Key.F10 -> { viewModel.sendInput("\u001b[21~"); true }
                                    Key.F11 -> { viewModel.sendInput("\u001b[23~"); true }
                                    Key.F12 -> { viewModel.sendInput("\u001b[24~"); true }
                                    else -> {
                                        if (isCtrl) {
                                            // Ctrl+A..Z → \x01..\x1a; Ctrl+[ → ESC
                                            val rawChar = keyEvent.nativeKeyEvent.getUnicodeChar(0).toChar()
                                            when {
                                                rawChar.isLetter() -> {
                                                    viewModel.sendInput((rawChar.lowercaseChar() - 'a' + 1).toChar().toString())
                                                    true
                                                }
                                                rawChar == '[' -> { viewModel.sendInput("\u001b"); true }
                                                else -> false
                                            }
                                        } else false
                                        // Printable chars fall through to onValueChange (IME / soft keyboard path)
                                    }
                                }
                            },
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
