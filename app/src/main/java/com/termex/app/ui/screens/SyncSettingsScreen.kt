package com.termex.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.termex.app.R
import com.termex.app.core.sync.MissingSecretIssue
import com.termex.app.core.sync.MissingSecretKind
import com.termex.app.ui.AutomationTags
import com.termex.app.ui.viewmodel.SyncSettingsViewModel
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToServerTransfer: () -> Unit = {},
    onNavigateToServerRepair: (String) -> Unit = {},
    onNavigateToKeyRepair: (String) -> Unit = {},
    onNavigateToCertificateRepair: (String) -> Unit = {},
    viewModel: SyncSettingsViewModel = hiltViewModel()
) {
    val syncMode by viewModel.syncMode.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val missingSecretIssues by viewModel.missingSecretIssues.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshRepairState()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.backup_transfer_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag(AutomationTags.SYNC_SCREEN),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.backup_transfer_local_title))
                    Text(syncMode.detail)
                    Text("Android backup and device transfer can carry metadata between phones.")
                    Text("Passwords, private keys, and secret stores stay local.")
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.backup_transfer_archive_title))
                    Text(stringResource(R.string.backup_transfer_archive_supporting))
                    Button(onClick = onNavigateToServerTransfer) {
                        Text(stringResource(R.string.backup_transfer_open_archive))
                    }
                }
            }

            if (missingSecretIssues.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Repair Missing Secrets")
                        missingSecretIssues.forEach { issue ->
                            MissingSecretIssueRow(
                                issue = issue,
                                onRepair = {
                                    when (issue.kind) {
                                        MissingSecretKind.PASSWORD -> issue.serverId?.let(onNavigateToServerRepair)
                                        MissingSecretKind.PRIVATE_KEY -> issue.serverId?.let(onNavigateToKeyRepair)
                                        MissingSecretKind.CERTIFICATE -> issue.serverId?.let(onNavigateToCertificateRepair)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Summary")
                    Text("Backup status: ${syncStatus.message}")
                    syncStatus.syncedAtMillis?.let { time ->
                        Text("Last backup: ${Date(time)}")
                    }
                    if (syncStatus.missingSecretCount > 0) {
                        Text("Missing secrets: ${syncStatus.missingSecretCount}")
                    }
                    errorMessage?.let { message ->
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(64.dp))
            TextButton(onClick = onNavigateBack) {
                Text(stringResource(R.string.action_back))
            }
        }
    }
}

@Composable
private fun MissingSecretIssueRow(
    issue: MissingSecretIssue,
    onRepair: () -> Unit
) {
    ListItem(
        headlineContent = { Text(issue.label) },
        supportingContent = { Text(issue.detail) },
        trailingContent = {
            TextButton(
                onClick = onRepair,
                modifier = issue.serverId?.let { Modifier.testTag(AutomationTags.syncRepairTag(it)) } ?: Modifier
            ) {
                Text(
                    when (issue.kind) {
                        MissingSecretKind.PASSWORD -> "Fix Password"
                        MissingSecretKind.PRIVATE_KEY -> "Fix Key"
                        MissingSecretKind.CERTIFICATE -> "Fix Certificate"
                    }
                )
            }
        }
    )
}
