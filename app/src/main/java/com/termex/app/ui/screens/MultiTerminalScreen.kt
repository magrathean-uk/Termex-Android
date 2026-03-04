package com.termex.app.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.termex.app.R
import com.termex.app.core.ssh.SSHConnectionState
import com.termex.app.ui.components.HostKeyVerificationDialog
import com.termex.app.ui.components.TerminalKeyboard
import com.termex.app.ui.components.TerminalView
import com.termex.app.ui.viewmodel.AppViewModel
import com.termex.app.ui.viewmodel.MultiTerminalViewModel
import com.termex.app.ui.viewmodel.TerminalPaneState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiTerminalScreen(
    workplaceId: String,
    onNavigateBack: () -> Unit,
    viewModel: MultiTerminalViewModel = hiltViewModel(),
    appViewModel: AppViewModel = hiltViewModel()
) {
    val servers by viewModel.servers.collectAsState()
    val paneStates by viewModel.paneStates.collectAsState()
    val selectedPane by viewModel.selectedPane.collectAsState()
    val hostKeyPrompt by viewModel.hostKeyPrompt.collectAsState()
    val demoModeActive by appViewModel.demoModeEnabled.collectAsState()

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    var ctrlActive by remember { mutableStateOf(false) }
    var altActive by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    // Track connected pane IDs — only changes when a pane connects/disconnects, not on every output line
    val connectedPaneIds = remember(paneStates) {
        paneStates.filter { it.value.connectionState is SSHConnectionState.Connected }.keys
    }

    LaunchedEffect(servers) {
        if (servers.isNotEmpty() && paneStates.isEmpty()) {
            viewModel.initializePanes(servers)
            servers.forEach { server ->
                viewModel.connectServer(server)
            }
        }
    }

    // Auto-select first pane
    LaunchedEffect(paneStates.keys) {
        if (selectedPane == null && paneStates.isNotEmpty()) {
            viewModel.selectPane(paneStates.keys.first())
        }
    }

    // Auto-focus the hidden text field when the selected pane first connects.
    // Does NOT call keyboardController.show() — keyboard only appears on explicit user tap.
    LaunchedEffect(selectedPane, connectedPaneIds) {
        if (selectedPane != null && connectedPaneIds.contains(selectedPane)) {
            try { focusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.multi_terminal_title)) },
                navigationIcon = {
                    IconButton(onClick = {
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1C1C1E),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Demo mode banner — matches iOS warning banner behavior
            if (demoModeActive) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFF9F0A))
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.demo_mode_banner),
                        color = Color.Black,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                HorizontalDivider(color = Color(0xFFCC7A00), thickness = 1.dp)
            }

            hostKeyPrompt?.let { prompt ->
                HostKeyVerificationDialog(
                    verification = prompt.result,
                    onAccept = { viewModel.acceptHostKey() },
                    onReject = { viewModel.rejectHostKey() }
                )
            }
            if (servers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = stringResource(R.string.multi_terminal_no_servers), color = Color.Gray)
                }
            } else {
                val configuration = LocalConfiguration.current
                val isTablet = configuration.screenWidthDp >= 600
                val maxPanes = if (isTablet) 4 else 2
                val paneCount = minOf(servers.size, maxPanes)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                ) {
                    // Terminal panes — animated card layout
                    Column(modifier = Modifier.weight(1f)) {
                        if (isTablet || paneCount == 1) {
                            // Tablet: side-by-side with animated horizontal weights
                            Row(modifier = Modifier.weight(1f)) {
                                repeat(paneCount) { index ->
                                    val server = servers[index]
                                    val isSelected = selectedPane == server.id
                                    val weight by animateFloatAsState(
                                        targetValue = if (paneCount == 1) 1f
                                                      else if (isSelected) 0.85f else 0.15f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        ),
                                        label = "pane_weight_$index"
                                    )
                                    TerminalPane(
                                        paneState = paneStates[server.id],
                                        isSelected = isSelected,
                                        modifier = Modifier
                                            .weight(weight.coerceAtLeast(0.01f))
                                            .fillMaxHeight(),
                                        onConnect = { viewModel.connectServer(server) },
                                        onTap = {
                                            viewModel.selectPane(server.id)
                                            try { focusRequester.requestFocus(); keyboardController?.show() }
                                            catch (_: Exception) {}
                                        },
                                        onSizeChanged = { c, r, w, h ->
                                            viewModel.resizeTerminal(server.id, c, r, w, h)
                                        }
                                    )
                                    if (index < paneCount - 1) {
                                        Box(modifier = Modifier.fillMaxHeight().width(2.dp).background(Color(0xFF2C2C2E)))
                                    }
                                }
                            }
                        } else {
                            // Phone: stacked vertically — selected pane expands, others collapse to header
                            repeat(paneCount) { index ->
                                val server = servers[index]
                                val isSelected = selectedPane == server.id
                                val weight by animateFloatAsState(
                                    targetValue = if (isSelected) 0.88f else 0.12f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    ),
                                    label = "pane_weight_$index"
                                )
                                TerminalPane(
                                    paneState = paneStates[server.id],
                                    isSelected = isSelected,
                                    modifier = Modifier
                                        .weight(weight.coerceAtLeast(0.01f))
                                        .fillMaxWidth(),
                                    onConnect = { viewModel.connectServer(server) },
                                    onTap = {
                                        viewModel.selectPane(server.id)
                                        try { focusRequester.requestFocus(); keyboardController?.show() }
                                        catch (_: Exception) {}
                                    },
                                    onSizeChanged = { c, r, w, h ->
                                        viewModel.resizeTerminal(server.id, c, r, w, h)
                                    }
                                )
                                if (index < paneCount - 1) {
                                    Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color(0xFF2C2C2E)))
                                }
                            }
                        }
                    }

                    // Shared keyboard bar + hidden input field
                    if (selectedPane != null) {
                        TerminalKeyboard(
                            ctrlActive = ctrlActive,
                            altActive = altActive,
                            onCtrlToggle = { ctrlActive = !ctrlActive },
                            onAltToggle = { altActive = !altActive },
                            onKeyPress = { sequence ->
                                selectedPane?.let { paneId -> viewModel.sendInput(paneId, sequence) }
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

                        var textInput by remember { mutableStateOf("") }
                        androidx.compose.foundation.text.BasicTextField(
                            value = textInput,
                            onValueChange = { newValue ->
                                if (newValue.length > textInput.length) {
                                    val newChars = newValue.substring(textInput.length)
                                    selectedPane?.let { paneId -> viewModel.sendInput(paneId, newChars) }
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
                                    val send: (String) -> Unit = { selectedPane?.let { p -> viewModel.sendInput(p, it) } }
                                    when (keyEvent.key) {
                                        Key.DirectionUp -> { send(if (isCtrl) "\u001b[1;5A" else "\u001b[A"); true }
                                        Key.DirectionDown -> { send(if (isCtrl) "\u001b[1;5B" else "\u001b[B"); true }
                                        Key.DirectionRight -> { send(if (isCtrl) "\u001b[1;5C" else "\u001b[C"); true }
                                        Key.DirectionLeft -> { send(if (isCtrl) "\u001b[1;5D" else "\u001b[D"); true }
                                        Key.Home -> { send(if (isCtrl) "\u001b[1;5H" else "\u001b[H"); true }
                                        Key.MoveEnd -> { send(if (isCtrl) "\u001b[1;5F" else "\u001b[F"); true }
                                        Key.PageUp -> { send("\u001b[5~"); true }
                                        Key.PageDown -> { send("\u001b[6~"); true }
                                        Key.Insert -> { send("\u001b[2~"); true }
                                        Key.Delete -> { send("\u001b[3~"); true }
                                        Key.Enter -> { send("\r"); true }
                                        Key.Tab -> { send("\t"); true }
                                        Key.Escape -> { send("\u001b"); true }
                                        Key.Backspace -> { send("\u007F"); true }
                                        Key.F1 -> { send("\u001bOP"); true }
                                        Key.F2 -> { send("\u001bOQ"); true }
                                        Key.F3 -> { send("\u001bOR"); true }
                                        Key.F4 -> { send("\u001bOS"); true }
                                        Key.F5 -> { send("\u001b[15~"); true }
                                        Key.F6 -> { send("\u001b[17~"); true }
                                        Key.F7 -> { send("\u001b[18~"); true }
                                        Key.F8 -> { send("\u001b[19~"); true }
                                        Key.F9 -> { send("\u001b[20~"); true }
                                        Key.F10 -> { send("\u001b[21~"); true }
                                        Key.F11 -> { send("\u001b[23~"); true }
                                        Key.F12 -> { send("\u001b[24~"); true }
                                        else -> {
                                            if (isCtrl) {
                                                val rawChar = keyEvent.nativeKeyEvent.getUnicodeChar(0).toChar()
                                                when {
                                                    rawChar.isLetter() -> { send((rawChar.lowercaseChar() - 'a' + 1).toChar().toString()); true }
                                                    rawChar == '[' -> { send("\u001b"); true }
                                                    else -> false
                                                }
                                            } else false
                                        }
                                    }
                                },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Ascii,
                                imeAction = ImeAction.None
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TerminalPane(
    paneState: TerminalPaneState?,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onConnect: () -> Unit,
    onTap: () -> Unit = {},
    onSizeChanged: (cols: Int, rows: Int, widthPx: Int, heightPx: Int) -> Unit = { _, _, _, _ -> }
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF3A3A3C)

    Box(
        modifier = modifier
            .border(if (isSelected) 2.dp else 1.dp, borderColor)
            .background(Color(0xFF0A0A0A))
    ) {
        if (paneState == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.multi_terminal_loading), color = Color.Gray)
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isSelected) Color(0xFF2C2C2E) else Color(0xFF1C1C1E))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Status dot
                        Box(
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .background(
                                    color = when (paneState.connectionState) {
                                        is SSHConnectionState.Connected -> Color(0xFF30D158)
                                        is SSHConnectionState.Connecting,
                                        is SSHConnectionState.VerifyingHostKey -> Color(0xFFFF9F0A)
                                        is SSHConnectionState.Error -> Color(0xFFFF453A)
                                        else -> Color(0xFF636366)
                                    },
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                                .padding(4.dp)
                        )
                        Text(
                            text = paneState.server.displayName,
                            color = if (isSelected) Color.White else Color(0xFFAEAEB2),
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }

                // Content
                when (paneState.connectionState) {
                    is SSHConnectionState.Disconnected -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { onConnect() },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = stringResource(R.string.multi_terminal_disconnected), color = Color(0xFF636366), fontSize = 12.sp)
                                Text(text = stringResource(R.string.multi_terminal_tap_to_connect), color = Color(0xFF48484A), fontSize = 11.sp)
                            }
                        }
                    }
                    is SSHConnectionState.Connecting -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = stringResource(R.string.multi_terminal_connecting), color = Color(0xFFFF9F0A), fontSize = 12.sp)
                        }
                    }
                    is SSHConnectionState.VerifyingHostKey -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = stringResource(R.string.multi_terminal_verifying_host), color = Color(0xFFFF9F0A), fontSize = 12.sp)
                        }
                    }
                    is SSHConnectionState.Connected -> {
                        TerminalView(
                            lines = paneState.lines,
                            cursorPosition = paneState.cursorPosition,
                            fontSize = 11f,
                            modifier = Modifier.fillMaxSize(),
                            onTap = onTap,
                            onSizeChanged = onSizeChanged
                        )
                    }
                    is SSHConnectionState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = (paneState.connectionState as SSHConnectionState.Error).message,
                                color = Color(0xFFFF453A),
                                fontSize = 10.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
