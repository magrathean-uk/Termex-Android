package com.termex.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.termex.app.core.ssh.SSHConnectionState
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

    LaunchedEffect(servers) {
        if (servers.isNotEmpty() && paneStates.isEmpty()) {
            viewModel.initializePanes(servers)
            // Auto-connect all servers
            servers.forEach { server ->
                viewModel.connectServer(server)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Multi-Terminal") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.disconnectAll()
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1C1C1E)
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        if (servers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No servers in this workplace",
                    color = Color.Gray
                )
            }
        } else {
            // Determine if tablet based on screen width (600dp is typical tablet breakpoint)
            val configuration = LocalConfiguration.current
            val isTablet = configuration.screenWidthDp >= 600

            // Phone: max 2 panes, Tablet: max 4 panes
            val maxPanes = if (isTablet) 4 else 2
            val paneCount = minOf(servers.size, maxPanes)

            // Layout: phones get vertical stack (2 rows, 1 col), tablets get 2x2 grid
            val rows = if (isTablet) {
                if (paneCount <= 2) 1 else 2
            } else {
                paneCount // On phone, stack vertically
            }
            val cols = if (isTablet) {
                if (paneCount == 1) 1 else 2
            } else {
                1 // On phone, single column
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
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
                                        .clickable { viewModel.selectPane(server.id) },
                                    onConnect = { viewModel.connectServer(server) }
                                )
                            }
                        }
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
    onConnect: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.DarkGray

    Box(
        modifier = modifier
            .border(2.dp, borderColor)
            .background(Color.Black)
    ) {
        if (paneState == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading...", color = Color.Gray)
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2C2C2E))
                        .padding(4.dp)
                ) {
                    Text(
                        text = paneState.server.displayName,
                        color = Color.White,
                        fontSize = 10.sp
                    )
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
                            Text(
                                text = "Tap to connect",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                    is SSHConnectionState.Connecting -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Connecting...",
                                color = Color.Yellow,
                                fontSize = 12.sp
                            )
                        }
                    }
                    is SSHConnectionState.Connected -> {
                        TerminalView(
                            lines = paneState.lines,
                            cursorPosition = paneState.cursorPosition,
                            fontSize = 10f,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    is SSHConnectionState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Error: ${(paneState.connectionState as SSHConnectionState.Error).message}",
                                color = Color.Red,
                                fontSize = 10.sp
                            )
                        }
                    }
                    is SSHConnectionState.VerifyingHostKey -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Verifying host key...",
                                color = Color.Yellow,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
