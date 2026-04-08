package com.termex.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.termex.app.R
import com.termex.app.domain.Server
import com.termex.app.domain.Workplace
import com.termex.app.ui.viewmodel.WorkplacesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkplacesScreen(
    onNavigateBack: (() -> Unit)? = null,
    onOpenMultiTerminal: (String) -> Unit,
    viewModel: WorkplacesViewModel = hiltViewModel()
) {
    val workplaces by viewModel.workplaces.collectAsState()
    val allServers by viewModel.allServers.collectAsState()
    val showDialog by viewModel.showDialog.collectAsState()
    val formState by viewModel.formState.collectAsState()
    val showAddServerDialog by viewModel.showAddServerDialog.collectAsState()
    val expandedWorkplace by viewModel.expandedWorkplace.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_workplaces)) },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showAddDialog() }) {
                Icon(Icons.Default.Add, contentDescription = "Add Workplace")
            }
        }
    ) { padding ->
        if (workplaces.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.workplaces_empty))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(workplaces, key = { it.id }) { workplace ->
                    WorkplaceCard(
                        workplace = workplace,
                        isExpanded = expandedWorkplace == workplace.id,
                        onToggleExpand = { viewModel.toggleExpanded(workplace.id) },
                        onOpenAll = { onOpenMultiTerminal(workplace.id) },
                        onEdit = { viewModel.showEditDialog(workplace) },
                        onDelete = { viewModel.deleteWorkplace(workplace) },
                        onAddServer = { viewModel.showAddServerToWorkplace(workplace.id) },
                        onRemoveServer = { viewModel.removeServerFromWorkplace(it) },
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    // Add Workplace Dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDialog() },
            title = { Text(stringResource(if (formState.isEditing) R.string.workplaces_edit_title else R.string.workplaces_new_title)) },
            text = {
                OutlinedTextField(
                    value = formState.name,
                    onValueChange = { viewModel.updateName(it) },
                    label = { Text(stringResource(R.string.label_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.saveWorkplace() },
                    enabled = formState.name.isNotBlank()
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

    // Add Server to Workplace Dialog
    showAddServerDialog?.let { workplaceId ->
        val serversInWorkplace by viewModel.getServersForWorkplace(workplaceId).collectAsState()
        val availableServers = allServers.filter { server ->
            !server.isDemo && server.workplaceId != workplaceId
        }

        AlertDialog(
            onDismissRequest = { viewModel.dismissAddServerDialog() },
            title = { Text(stringResource(R.string.action_add_server)) },
            text = {
                if (availableServers.isEmpty()) {
                    Text(stringResource(R.string.workplaces_no_servers_available))
                } else {
                    LazyColumn {
                        items(availableServers, key = { it.id }) { server ->
                            ListItem(
                                headlineContent = { Text(server.displayName) },
                                supportingContent = { Text("${server.username}@${server.hostname}") },
                                modifier = Modifier.clickable {
                                    viewModel.addServerToWorkplace(server, workplaceId)
                                    viewModel.dismissAddServerDialog()
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissAddServerDialog() }) {
                    Text(stringResource(R.string.action_done))
                }
            }
        )
    }
}

@Composable
private fun WorkplaceCard(
    workplace: Workplace,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onOpenAll: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddServer: () -> Unit,
    onRemoveServer: (Server) -> Unit,
    viewModel: WorkplacesViewModel
) {
    val servers by viewModel.getServersForWorkplace(workplace.id).collectAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column {
            // Header
            ListItem(
                headlineContent = { Text(workplace.name) },
                supportingContent = { Text(pluralStringResource(R.plurals.workplaces_server_count, servers.size, servers.size)) },
                leadingContent = {
                    IconButton(onClick = onToggleExpand) {
                        Icon(
                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand"
                        )
                    }
                },
                trailingContent = {
                    Row {
                        if (servers.isNotEmpty()) {
                            IconButton(onClick = onOpenAll) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Open All")
                            }
                        }
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                },
                modifier = Modifier.clickable { onToggleExpand() }
            )

            // Expanded content - servers list
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    HorizontalDivider()

                    if (servers.isEmpty()) {
                        Text(
                            text = stringResource(R.string.workplaces_no_servers),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        servers.forEach { server ->
                            ListItem(
                                headlineContent = { Text(server.displayName) },
                                supportingContent = { Text("${server.username}@${server.hostname}") },
                                trailingContent = {
                                    IconButton(onClick = { onRemoveServer(server) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove")
                                    }
                                },
                                modifier = Modifier.padding(start = 32.dp)
                            )
                        }
                    }

                    // Add Server button
                    TextButton(
                        onClick = onAddServer,
                        modifier = Modifier.padding(start = 32.dp, bottom = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_add_server))
                    }
                }
            }
        }
    }
}
