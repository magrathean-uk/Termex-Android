package com.termex.app.ui.screens

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.termex.app.ui.viewmodel.MultiTerminalViewModel
import com.termex.app.ui.viewmodel.TerminalPaneState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiTerminalScreen(
    workplaceId: String,
    onNavigateBack: () -> Unit,
    viewModel: MultiTerminalViewModel = hiltViewModel()
) {
    val servers by viewModel.servers.collectAsState()
    val paneStates by viewModel.paneStates.collectAsState()
    val selectedPane by viewModel.selectedPane.collectAsState()
    val hostKeyPrompt by viewModel.hostKeyPrompt.collectAsState()

    val keyboardController = LocalSoftwareKeyboardController.current
    var ctrlActive by remember { mutableStateOf(false) }
    var altActive by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(servers) {
        if (servers.isNotEmpty() && paneStates.isEmpty()) {
            viewModel.initializePanes(servers)
            servers.forEach { server ->
                viewModel.connectServer(server)
            }
        }
    }

    // Auto-select first pane
    LaunchedEffect(paneStates) {
        if (selectedPane == null && paneStates.isNotEmpty()) {
            viewModel.selectPane(paneStates.keys.first())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.multi_terminal_title)) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.disconnectAll()
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
        hostKeyPrompt?.let { prompt ->
            HostKeyVerificationDialog(
                verification = prompt.result,
                onAccept = { viewModel.acceptHostKey() },
                onReject = { viewModel.rejectHostKey() }
            )
        }
        if (servers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(text = stringResource(R.string.multi_terminal_no_servers), color = Color.Gray)
            }
        } else {
            val configuration = LocalConfiguration.current
            val isTablet = configuration.screenWidthDp >= 600
            val maxPanes = if (isTablet) 4 else 2
            val paneCount = minOf(servers.size, maxPanes)

            val rows = if (isTablet) {
                if (paneCount <= 2) 1 else 2
            } else {
                paneCount
            }
            val cols = if (isTablet) {
                if (paneCount == 1) 1 else 2
            } else {
                1
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Terminal panes
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    repeat(rows) { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            repeat(cols) { col ->
                                val index = row * cols + col
                                if (index < paneCount) {
                                    val server = servers[index]
                                    val paneState = paneStates[server.id]
                                    TerminalPane(
                                        paneState = paneState,
                                        isSelected = selectedPane == server.id,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clickable {
                                                viewModel.selectPane(server.id)
                                                keyboardController?.show()
                                            },
                                        onConnect = { viewModel.connectServer(server) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Shared keyboard bar for the selected pane
                if (selectedPane != null) {
                    TerminalKeyboard(
                        ctrlActive = ctrlActive,
                        altActive = altActive,
                        onCtrlToggle = { ctrlActive = !ctrlActive },
                        onAltToggle = { altActive = !altActive },
                        onKeyPress = { sequence ->
                            selectedPane?.let { paneId ->
                                viewModel.sendInput(paneId, sequence)
                            }
                            if (!sequence.startsWith("\u001B")) {
                                ctrlActive = false
                                altActive = false
                            }
                        }
                    )

                    // Hidden text field for soft keyboard input
                    var textInput by remember { mutableStateOf("") }
                    androidx.compose.foundation.text.BasicTextField(
                        value = textInput,
                        onValueChange = { newValue ->
                            if (newValue.length > textInput.length) {
                                val newChars = newValue.substring(textInput.length)
                                selectedPane?.let { paneId ->
                                    viewModel.sendInput(paneId, newChars)
                                }
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
            }
        }
    }
}

@Composable
private fun TerminalPane(
    paneState: TerminalPaneState?,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onConnect: () -> Unit
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
                            modifier = Modifier.fillMaxSize()
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
