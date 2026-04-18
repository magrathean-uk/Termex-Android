package com.termex.app.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.termex.app.R
import com.termex.app.discovery.SharedServerDiscoveryItem
import com.termex.app.discovery.SharedServersDiscoveryState
import com.termex.app.ui.AutomationTags
import com.termex.app.ui.viewmodel.SharedServersEvent
import com.termex.app.ui.viewmodel.SharedServersViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedServersScreen(
    onNavigateBack: () -> Unit,
    onImportServer: (host: String, port: Int) -> Unit,
    viewModel: SharedServersViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SharedServersEvent.ImportServer -> {
                    onImportServer(event.target.host, event.target.port)
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.testTag(AutomationTags.SHARED_SERVERS_SCREEN),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.shared_servers_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        modifier = Modifier.testTag(AutomationTags.SHARED_SERVERS_REFRESH),
                        onClick = { viewModel.refresh() }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.shared_servers_refresh))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SharedServersSummary(state = state)
            }

            if (state.services.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            modifier = Modifier.testTag(AutomationTags.SHARED_SERVERS_EMPTY),
                            text = stringResource(R.string.shared_servers_empty)
                        )
                    }
                }
            } else {
                items(state.services, key = { it.id }) { service ->
                    SharedServerRow(
                        service = service,
                        onClick = { viewModel.onServiceSelected(service) }
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
private fun SharedServersSummary(
    state: SharedServersDiscoveryState
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state.isDiscovering) {
                CircularProgressIndicator(modifier = Modifier.padding(end = 16.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.shared_servers_header))
                Text(
                    text = state.errorMessage
                        ?: stringResource(R.string.shared_servers_supporting),
                    color = if (state.errorMessage == null) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
        }
    }
}

@Composable
private fun SharedServerRow(
    service: SharedServerDiscoveryItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        ListItem(
            headlineContent = { Text(service.serviceName) },
            supportingContent = {
                Text(
                    if (service.isResolved) {
                        stringResource(
                            R.string.shared_servers_resolved_supporting,
                            service.host.orEmpty(),
                            service.port ?: 0
                        )
                    } else {
                        stringResource(R.string.shared_servers_tap_to_import)
                    }
                )
            },
            leadingContent = {
                Icon(Icons.Default.Dns, contentDescription = null)
            },
            modifier = Modifier
                .testTag(AutomationTags.sharedServerItemTag(service.id))
                .clickable { onClick() }
        )
    }
}
