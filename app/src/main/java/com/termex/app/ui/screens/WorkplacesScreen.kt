package com.termex.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.termex.app.R
import com.termex.app.domain.Server
import com.termex.app.domain.Workplace
import com.termex.app.ui.viewmodel.WorkplacesViewModel

internal object WorkplacesTags {
    const val AddWorkplace = "workplaces:add_workplace"
    const val EmptyAddWorkplace = "workplaces:empty_add_workplace"
    const val DeleteConfirm = "workplaces:delete_confirm"
    const val DeleteDismiss = "workplaces:delete_dismiss"
    const val AddServerDone = "workplaces:add_server_done"

    fun card(workplaceId: String) = "workplaces:card:$workplaceId"
    fun expand(workplaceId: String) = "workplaces:expand:$workplaceId"
    fun open(workplaceId: String) = "workplaces:open:$workplaceId"
    fun edit(workplaceId: String) = "workplaces:edit:$workplaceId"
    fun delete(workplaceId: String) = "workplaces:delete:$workplaceId"
    fun addServer(workplaceId: String) = "workplaces:add_server:$workplaceId"
    fun removeServer(workplaceId: String, serverId: String) = "workplaces:remove_server:$workplaceId:$serverId"
    fun addServerOption(serverId: String) = "workplaces:add_server_option:$serverId"
}

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
    val workplaceToDelete by viewModel.workplaceToDelete.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_workplaces)) },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_back)
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            androidx.compose.material3.FloatingActionButton(
                modifier = Modifier.testTag(WorkplacesTags.AddWorkplace),
                onClick = { viewModel.showAddDialog() }
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.action_add_workplace)
                )
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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.workplaces_empty),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.workplaces_empty_supporting),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { viewModel.showAddDialog() },
                        modifier = Modifier.testTag(WorkplacesTags.EmptyAddWorkplace)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.workplaces_empty_action))
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(workplaces, key = { it.id }) { workplace ->
                    val workplaceServers = allServers.filter { it.workplaceId == workplace.id }
                    WorkplaceCard(
                        workplace = workplace,
                        servers = workplaceServers,
                        isExpanded = expandedWorkplace == workplace.id,
                        onToggleExpand = { viewModel.toggleExpanded(workplace.id) },
                        onOpenAll = {
                            viewModel.rememberWorkplaceRoute(workplace.id)
                            onOpenMultiTerminal(workplace.id)
                        },
                        onEdit = { viewModel.showEditDialog(workplace) },
                        onDelete = { viewModel.requestDeleteWorkplace(workplace) },
                        onAddServer = { viewModel.showAddServerToWorkplace(workplace.id) },
                        onRemoveServer = { viewModel.removeServerFromWorkplace(it) }
                    )
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDialog() },
            title = {
                Text(
                    stringResource(
                        if (formState.isEditing) R.string.workplaces_edit_title else R.string.workplaces_new_title
                    )
                )
            },
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

    workplaceToDelete?.let { workplace ->
        val workplaceServers = allServers.filter { it.workplaceId == workplace.id }

        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteWorkplaceDialog() },
            title = { Text(stringResource(R.string.workplaces_delete_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.workplaces_delete_message, workplace.name))
                    Text(
                        text = pluralStringResource(
                            R.plurals.workplaces_server_count,
                            workplaceServers.size,
                            workplaceServers.size
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    modifier = Modifier.testTag(WorkplacesTags.DeleteConfirm),
                    onClick = { viewModel.confirmDeleteWorkplace() }
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    modifier = Modifier.testTag(WorkplacesTags.DeleteDismiss),
                    onClick = { viewModel.dismissDeleteWorkplaceDialog() }
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    showAddServerDialog?.let { workplaceId ->
        val selectedWorkplace = workplaces.firstOrNull { it.id == workplaceId }
        val availableServers = allServers.filter { server ->
            !server.isDemo && server.workplaceId != workplaceId
        }

        AlertDialog(
            onDismissRequest = { viewModel.dismissAddServerDialog() },
            title = {
                Text(
                    stringResource(
                        R.string.workplaces_add_server_title,
                        selectedWorkplace?.name ?: stringResource(R.string.title_workplaces)
                    )
                )
            },
            text = {
                if (availableServers.isEmpty()) {
                    Text(stringResource(R.string.workplaces_add_server_empty))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(availableServers, key = { it.id }) { server ->
                            ListItem(
                                headlineContent = { Text(server.displayName) },
                                supportingContent = { Text("${server.username}@${server.hostname}") },
                                leadingContent = {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(WorkplacesTags.addServerOption(server.id))
                                    .clickable {
                                        viewModel.addServerToWorkplace(server, workplaceId)
                                        viewModel.dismissAddServerDialog()
                                    }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    modifier = Modifier.testTag(WorkplacesTags.AddServerDone),
                    onClick = { viewModel.dismissAddServerDialog() }
                ) {
                    Text(stringResource(R.string.action_done))
                }
            }
        )
    }
}

@Composable
private fun WorkplaceCard(
    workplace: Workplace,
    servers: List<Server>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onOpenAll: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddServer: () -> Unit,
    onRemoveServer: (Server) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag(WorkplacesTags.card(workplace.id))
    ) {
        Column {
            ListItem(
                headlineContent = { Text(workplace.name) },
                supportingContent = {
                    Text(
                        pluralStringResource(
                            R.plurals.workplaces_server_count,
                            servers.size,
                            servers.size
                        )
                    )
                },
                leadingContent = {
                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.testTag(WorkplacesTags.expand(workplace.id))
                    ) {
                        Icon(
                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = stringResource(
                                if (isExpanded) R.string.action_collapse_workplace else R.string.action_expand_workplace
                            )
                        )
                    }
                },
                trailingContent = {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (servers.isNotEmpty()) {
                            TextButton(
                                onClick = onOpenAll,
                                modifier = Modifier.testTag(WorkplacesTags.open(workplace.id))
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.action_open_workplace))
                            }
                        }
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.testTag(WorkplacesTags.edit(workplace.id))
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = stringResource(R.string.action_edit_workplace)
                            )
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.testTag(WorkplacesTags.delete(workplace.id))
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.action_delete_workplace)
                            )
                        }
                    }
                },
                modifier = Modifier.clickable { onToggleExpand() }
            )

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    HorizontalDivider()

                    if (servers.isEmpty()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.workplaces_no_servers),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = onAddServer,
                                modifier = Modifier.testTag(WorkplacesTags.addServer(workplace.id))
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.action_add_server_to_workplace))
                            }
                        }
                    } else {
                        Column {
                            servers.forEachIndexed { index, server ->
                                ListItem(
                                    headlineContent = { Text(server.displayName) },
                                    supportingContent = {
                                        Text("${server.username}@${server.hostname}")
                                    },
                                    trailingContent = {
                                        IconButton(
                                            onClick = { onRemoveServer(server) },
                                            modifier = Modifier.testTag(
                                                WorkplacesTags.removeServer(workplace.id, server.id)
                                            )
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = stringResource(
                                                    R.string.action_remove_server_from_workplace
                                                )
                                            )
                                        }
                                    },
                                    modifier = Modifier.padding(start = 32.dp)
                                )
                                if (index < servers.lastIndex) {
                                    HorizontalDivider(modifier = Modifier.padding(start = 32.dp))
                                }
                            }

                            TextButton(
                                onClick = onAddServer,
                                modifier = Modifier
                                    .padding(start = 32.dp, bottom = 8.dp)
                                    .testTag(WorkplacesTags.addServer(workplace.id))
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.action_add_server_to_workplace))
                            }
                        }
                    }
                }
            }
        }
    }
}
