package com.termex.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.termex.app.R
import com.termex.app.domain.Server
import com.termex.app.ui.viewmodel.ServersViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerListScreen(
    onServerClick: (Server) -> Unit,
    onAddServer: () -> Unit,
    onEditServer: (Server) -> Unit,
    onPortForwarding: (Server) -> Unit,
    viewModel: ServersViewModel = hiltViewModel()
) {
    val servers by viewModel.servers.collectAsState()
    val demoModeEnabled by viewModel.demoModeEnabled.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_servers)) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddServer) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_add_server))
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Demo Mode Warning Banner
            if (demoModeEnabled) {
                val bannerColor = MaterialTheme.colorScheme.tertiaryContainer
                val bannerTextColor = MaterialTheme.colorScheme.onTertiaryContainer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bannerColor)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = bannerTextColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.demo_mode_active),
                            color = bannerTextColor,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.demo_mode_message),
                            color = bannerTextColor.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            if (servers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.empty_servers))
                }
            } else {
                var serverToDelete by remember { mutableStateOf<Server?>(null) }
                
                // Delete confirmation dialog
                serverToDelete?.let { server ->
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { serverToDelete = null },
                        title = { Text("Delete Server") },
                        text = { Text("Are you sure you want to delete \"${server.displayName}\"?") },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.deleteServer(server)
                                serverToDelete = null
                            }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                        },
                        dismissButton = {
                            TextButton(onClick = { serverToDelete = null }) { Text("Cancel") }
                        }
                    )
                }
                
                LazyColumn(modifier = Modifier.weight(1f)) {
                items(servers, key = { it.id }) { server ->
                    var showMenu by remember { mutableStateOf(false) }

                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart && !server.isDemo) {
                                serverToDelete = server
                                false // Don't dismiss, let dialog confirm
                            } else false
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        enableDismissFromEndToStart = !server.isDemo,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.errorContainer)
                                    .padding(end = 24.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    ) {
                    ListItem(
                        headlineContent = {
                            if (server.isDemo) {
                                Text("${server.displayName} (Demo)")
                            } else {
                                Text(server.displayName)
                            }
                        },
                        supportingContent = {
                            if (server.isDemo) {
                                Text(stringResource(R.string.try_demo_server))
                            } else {
                                Text("${server.username}@${server.hostname}:${server.port}")
                            }
                        },
                        modifier = Modifier.clickable { onServerClick(server) },
                        trailingContent = {
                            // Don't show edit/delete for demo server
                            if (!server.isDemo) {
                                Box {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = stringResource(R.string.action_more),
                                        modifier = Modifier.clickable { showMenu = true }
                                    )
                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.action_edit)) },
                                            onClick = {
                                                showMenu = false
                                                onEditServer(server)
                                            },
                                            leadingIcon = { Icon(Icons.Default.Edit, null) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.action_port_forwarding)) },
                                            onClick = {
                                                showMenu = false
                                                onPortForwarding(server)
                                            },
                                            leadingIcon = { Icon(Icons.Default.Dns, null) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.action_delete)) },
                                            onClick = {
                                                showMenu = false
                                                serverToDelete = server
                                            },
                                            leadingIcon = { Icon(Icons.Default.Delete, null) }
                                        )
                                    }
                                }
                            }
                        }
                    )
                    } // SwipeToDismissBox
                    HorizontalDivider()
                }
            }
            }
        }
    }
}
