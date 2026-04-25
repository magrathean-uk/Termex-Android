package com.termex.app.ui.screens

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.termex.app.R
import com.termex.app.domain.Server
import com.termex.app.ui.AutomationTags
import com.termex.app.ui.viewmodel.RecentSessionSummary
import com.termex.app.ui.viewmodel.ServersViewModel
import com.termex.app.ui.viewmodel.WorkplaceSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerListScreen(
    onServerClick: (Server) -> Unit,
    onAddServer: () -> Unit,
    onOpenSharedServers: () -> Unit,
    onEditServer: (Server) -> Unit,
    onPortForwarding: (Server) -> Unit,
    onOpenMultiTerminal: (String) -> Unit = {},
    onOpenKnownHosts: () -> Unit = {},
    onOpenCertificates: () -> Unit = {},
    onOpenSSHConfigBrowser: () -> Unit = {},
    viewModel: ServersViewModel = hiltViewModel()
) {
    val servers by viewModel.servers.collectAsState()
    val activeServers by viewModel.activeServers.collectAsState()
    val recentSessions by viewModel.recentSessions.collectAsState()
    val workplaceSummaries by viewModel.workplaceSummaries.collectAsState()
    val serverIssues by viewModel.serverIssues.collectAsState()
    val demoModeEnabled by viewModel.demoModeEnabled.collectAsState()

    var serverToDelete by remember { mutableStateOf<Server?>(null) }

    serverToDelete?.let { server ->
        AlertDialog(
            onDismissRequest = { serverToDelete = null },
            title = { Text(stringResource(R.string.server_delete_title)) },
            text = { Text(stringResource(R.string.server_delete_confirmation, server.displayName)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteServer(server)
                        serverToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { serverToDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) }
            )
        },
        floatingActionButton = {
            androidx.compose.material3.FloatingActionButton(
                modifier = Modifier.testTag(AutomationTags.SERVERS_ADD),
                onClick = onAddServer
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_add_server))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag(AutomationTags.SERVER_LIST)
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (demoModeEnabled) {
                item {
                    DemoModeBanner(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }

            if (serverIssues.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    pluralStringResource(
                                        R.plurals.server_issues_summary,
                                        serverIssues.size,
                                        serverIssues.size
                                    )
                                )
                            },
                            supportingContent = {
                                Text(stringResource(R.string.server_issues_summary_supporting))
                            },
                            leadingContent = {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }

            if (activeServers.isNotEmpty()) {
                item {
                    SectionHeader(stringResource(R.string.home_section_active_sessions))
                }
                items(activeServers, key = { "active_${it.id}" }) { server ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        ListItem(
                            headlineContent = { Text(server.displayName) },
                            supportingContent = {
                                Text(stringResource(R.string.home_active_session_supporting, server.username, server.hostname, server.port))
                            },
                            trailingContent = {
                                Text(
                                    text = stringResource(R.string.home_open_session),
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            modifier = Modifier.clickable { onServerClick(server) }
                        )
                    }
                }
            }

            if (recentSessions.isNotEmpty()) {
                item {
                    SectionHeader(stringResource(R.string.home_section_recent_sessions))
                }
                items(recentSessions, key = { it.sessionId }) { session ->
                    RecentSessionCard(
                        session = session,
                        onClick = { onServerClick(session.server) }
                    )
                }
            }

            if (workplaceSummaries.isNotEmpty()) {
                item {
                    SectionHeader(stringResource(R.string.title_workplaces))
                }
                items(workplaceSummaries, key = { it.id }) { workplace ->
                    WorkplaceSummaryCard(
                        workplace = workplace,
                        onClick = { onOpenMultiTerminal(workplace.id) }
                    )
                }
            }

            item {
                SectionHeader(stringResource(R.string.home_section_tools))
            }
            item {
                ToolsCard(
                    onOpenKnownHosts = onOpenKnownHosts,
                    onOpenCertificates = onOpenCertificates,
                    onOpenSSHConfigBrowser = onOpenSSHConfigBrowser,
                    onOpenSharedServers = onOpenSharedServers,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item {
                SectionHeader(stringResource(R.string.title_servers))
            }

            if (servers.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.empty_servers))
                    }
                }
            } else {
                items(servers, key = { it.id }) { server ->
                    ServerRow(
                        server = server,
                        issueMessage = serverIssues[server.id],
                        onServerClick = onServerClick,
                        onEditServer = onEditServer,
                        onPortForwarding = onPortForwarding,
                        onDeleteRequest = { serverToDelete = it },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(96.dp))
            }
        }
    }
}

@Composable
private fun DemoModeBanner(
    modifier: Modifier = Modifier
) {
    val bannerColor = MaterialTheme.colorScheme.tertiaryContainer
    val bannerTextColor = MaterialTheme.colorScheme.onTertiaryContainer
    Row(
        modifier = modifier
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

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 20.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServerRow(
    server: Server,
    issueMessage: String?,
    onServerClick: (Server) -> Unit,
    onEditServer: (Server) -> Unit,
    onPortForwarding: (Server) -> Unit,
    onDeleteRequest: (Server) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart && !server.isDemo) {
                onDeleteRequest(server)
                false
            } else {
                false
            }
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
        },
        modifier = modifier
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            ListItem(
                headlineContent = {
                    if (server.isDemo) {
                        Text(stringResource(R.string.server_name_demo_format, server.displayName))
                    } else {
                        Text(server.displayName)
                    }
                },
                supportingContent = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            if (server.isDemo) {
                                stringResource(R.string.try_demo_server)
                            } else {
                                "${server.username}@${server.hostname}:${server.port}"
                            }
                        )
                        issueMessage?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                leadingContent = {
                    if (issueMessage != null) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Icon(
                            Icons.Default.Dns,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                trailingContent = {
                    if (!server.isDemo) {
                        Box {
                            IconButton(
                                modifier = Modifier
                                    .testTag(AutomationTags.serverMenuTag(server.id)),
                                onClick = { showMenu = true }
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.action_more)
                                )
                            }
                            androidx.compose.material3.DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_edit)) },
                                    onClick = {
                                        showMenu = false
                                        onEditServer(server)
                                    },
                                    leadingIcon = { Icon(Icons.Default.Edit, null) }
                                )
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_port_forwarding)) },
                                    onClick = {
                                        showMenu = false
                                        onPortForwarding(server)
                                    },
                                    leadingIcon = { Icon(Icons.Default.Dns, null) }
                                )
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_delete)) },
                                    onClick = {
                                        showMenu = false
                                        onDeleteRequest(server)
                                    },
                                    leadingIcon = { Icon(Icons.Default.Delete, null) }
                                )
                            }
                        }
                    }
                },
                modifier = Modifier
                    .testTag(AutomationTags.serverRowTag(server.id))
                    .clickable(enabled = issueMessage == null) { onServerClick(server) }
            )
        }
    }
}

@Composable
private fun RecentSessionCard(
    session: RecentSessionSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        ListItem(
            headlineContent = { Text(session.server.displayName) },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(session.preview)
                    Text(
                        text = stringResource(
                            R.string.home_recent_session_time,
                            DateUtils.getRelativeTimeSpanString(
                                session.lastActiveAt,
                                System.currentTimeMillis(),
                                DateUtils.MINUTE_IN_MILLIS
                            ).toString()
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            modifier = Modifier.clickable { onClick() }
        )
    }
}

@Composable
private fun WorkplaceSummaryCard(
    workplace: WorkplaceSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        ListItem(
            headlineContent = { Text(workplace.name) },
            supportingContent = {
                Text(
                    pluralStringResource(
                        R.plurals.workplaces_server_count,
                        workplace.serverCount,
                        workplace.serverCount
                    )
                )
            },
            trailingContent = {
                Text(
                    text = stringResource(R.string.home_open_workspace),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge
                )
            },
            modifier = Modifier.clickable { onClick() }
        )
    }
}

@Composable
private fun ToolsCard(
    onOpenKnownHosts: () -> Unit,
    onOpenCertificates: () -> Unit,
    onOpenSSHConfigBrowser: () -> Unit,
    onOpenSharedServers: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_ssh_config_browser)) },
            supportingContent = { Text(stringResource(R.string.settings_ssh_config_browser_supporting)) },
            leadingContent = { Icon(Icons.Default.Dns, contentDescription = null) },
            modifier = Modifier.clickable { onOpenSSHConfigBrowser() }
        )
        HorizontalDivider()
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_known_hosts)) },
            supportingContent = { Text(stringResource(R.string.settings_known_hosts_supporting)) },
            leadingContent = { Icon(Icons.Default.Security, contentDescription = null) },
            modifier = Modifier.clickable { onOpenKnownHosts() }
        )
        HorizontalDivider()
        ListItem(
            headlineContent = { Text(stringResource(R.string.certificates_title)) },
            supportingContent = { Text(stringResource(R.string.certificates_empty_description)) },
            leadingContent = { Icon(Icons.Default.Key, contentDescription = null) },
            modifier = Modifier.clickable { onOpenCertificates() }
        )
        HorizontalDivider()
        ListItem(
            headlineContent = { Text(stringResource(R.string.shared_servers_title)) },
            supportingContent = { Text(stringResource(R.string.shared_servers_supporting)) },
            leadingContent = { Icon(Icons.Default.Dns, contentDescription = null) },
            modifier = Modifier
                .testTag(AutomationTags.SHARED_SERVERS_ENTRY)
                .clickable { onOpenSharedServers() }
        )
    }
}
