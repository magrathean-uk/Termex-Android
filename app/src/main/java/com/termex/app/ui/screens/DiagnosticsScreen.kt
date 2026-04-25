package com.termex.app.ui.screens

import android.content.Intent
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.termex.app.R
import com.termex.app.data.diagnostics.DiagnosticEvent
import com.termex.app.data.diagnostics.DiagnosticSeverity
import com.termex.app.ui.viewmodel.DiagnosticsSummary
import com.termex.app.ui.viewmodel.DiagnosticsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: DiagnosticsViewModel = hiltViewModel()
) {
    val summary by viewModel.summary.collectAsState()
    val events by viewModel.events.collectAsState()
    val context = LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }
    var showClearSessionsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.diagnostics_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_close))
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.diagnostics_share_subject))
                                putExtra(Intent.EXTRA_TEXT, viewModel.buildExportText())
                            }
                            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.diagnostics_share)))
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(R.string.diagnostics_share))
                    }
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.diagnostics_clear))
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
                SummaryCard(
                    summary = summary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.diagnostics_clear_saved_sessions)) },
                        supportingContent = { Text(stringResource(R.string.diagnostics_clear_saved_sessions_supporting)) }
                    )
                    HorizontalDivider()
                    TextButton(
                        onClick = { showClearSessionsDialog = true },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(stringResource(R.string.settings_clear_saved_sessions))
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.diagnostics_recent_events),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            if (events.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.diagnostics_no_events),
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(events, key = { it.id }) { event ->
                    DiagnosticEventRow(
                        event = event,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.diagnostics_clear_title)) },
            text = { Text(stringResource(R.string.diagnostics_clear_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearDiagnostics()
                        showClearDialog = false
                    }
                ) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showClearSessionsDialog) {
        AlertDialog(
            onDismissRequest = { showClearSessionsDialog = false },
            title = { Text(stringResource(R.string.settings_clear_saved_sessions)) },
            text = { Text(stringResource(R.string.diagnostics_clear_saved_sessions_dialog)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearSavedSessions()
                        showClearSessionsDialog = false
                    }
                ) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearSessionsDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun SummaryCard(
    summary: DiagnosticsSummary,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.diagnostics_summary_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.diagnostics_summary_connections, summary.activeConnectionCount, summary.savedSessionCount))
            Text(stringResource(R.string.diagnostics_summary_servers, summary.serverCount, summary.workplaceCount))
            Text(stringResource(R.string.diagnostics_summary_credentials, summary.keyCount, summary.certificateCount, summary.knownHostCount, summary.credentialIssueCount))
            Text(stringResource(R.string.diagnostics_summary_events, summary.eventCount))
        }
    }
}

@Composable
private fun DiagnosticEventRow(
    event: DiagnosticEvent,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        ListItem(
            headlineContent = { Text(event.title) },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = buildString {
                            append(event.severity.name)
                            append(" · ")
                            append(event.category)
                            event.serverId?.let {
                                append(" · ")
                                append(it)
                            }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when (event.severity) {
                            DiagnosticSeverity.INFO -> MaterialTheme.colorScheme.primary
                            DiagnosticSeverity.WARNING -> MaterialTheme.colorScheme.tertiary
                            DiagnosticSeverity.ERROR -> MaterialTheme.colorScheme.error
                        }
                    )
                    event.detail?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            trailingContent = {
                Text(
                    text = DateUtils.getRelativeTimeSpanString(
                        event.timestamp,
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS
                    ).toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
    }
}
