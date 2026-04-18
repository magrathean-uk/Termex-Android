package com.termex.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.termex.app.R
import com.termex.app.ui.AutomationTags
import com.termex.app.ui.viewmodel.KeysViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyListScreen(
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToCertificates: () -> Unit = {},
    repairTargetServerId: String? = null,
    autoOpenImportDialog: Boolean = false,
    viewModel: KeysViewModel = hiltViewModel()
) {
    val keys by viewModel.keys.collectAsState()
    val showGenerate by viewModel.showGenerateDialog.collectAsState()
    val showImport by viewModel.showImportDialog.collectAsState()

    var showMenu by remember { mutableStateOf(false) }
    var importAutoOpened by remember { mutableStateOf(false) }

    LaunchedEffect(autoOpenImportDialog) {
        if (autoOpenImportDialog && !importAutoOpened) {
            importAutoOpened = true
            viewModel.showImportDialog()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_ssh_keys)) },
                navigationIcon = {
                    onNavigateBack?.let {
                        IconButton(onClick = it) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    Box {
                        IconButton(
                            modifier = Modifier.testTag(AutomationTags.KEYS_ADD),
                            onClick = { showMenu = true }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_generate_new_key)) },
                                onClick = {
                                    showMenu = false
                                    viewModel.showGenerateDialog()
                                }
                            )
                            DropdownMenuItem(
                                modifier = Modifier.testTag(AutomationTags.KEYS_IMPORT),
                                text = { Text(stringResource(R.string.action_import_key)) },
                                onClick = {
                                    showMenu = false
                                    viewModel.showImportDialog()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.keys_certificates)) },
                                onClick = {
                                    showMenu = false
                                    onNavigateToCertificates()
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (keys.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.empty_ssh_keys))
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(keys, key = { it.name }) { key ->
                    val isEncrypted = remember(key.path) {
                        try {
                            File(key.path).readText().contains("ENCRYPTED")
                        } catch (_: Exception) {
                            false
                        }
                    }
                    ListItem(
                        headlineContent = { Text(key.name) },
                        supportingContent = {
                            Column {
                                Text(key.type)
                                if (key.fingerprint.isNotEmpty()) {
                                    Text(
                                        text = key.fingerprint,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                if (isEncrypted) {
                                    Row(modifier = Modifier.padding(top = 4.dp)) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        ) {
                                            Text(
                                                stringResource(R.string.keys_encrypted_badge),
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        trailingContent = {
                            IconButton(onClick = { viewModel.deleteKey(key) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.action_delete)
                                )
                            }
                        },
                        modifier = if (repairTargetServerId != null) {
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.repairServerWithSelectedKey(repairTargetServerId, key) {
                                        onNavigateBack?.invoke()
                                    }
                                }
                                .testTag("repair_key_${key.name}")
                        } else {
                            Modifier
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (showGenerate) {
        var name by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { viewModel.hideGenerateDialog() },
            title = { Text(stringResource(R.string.title_generate_key)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.label_key_name_example)) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.generateKey(name) },
                    enabled = name.isNotBlank()
                ) {
                    Text(stringResource(R.string.action_generate))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideGenerateDialog() }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showImport) {
        var name by remember { mutableStateOf("") }
        var privateKey by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { viewModel.hideImportDialog() },
            title = { Text(stringResource(R.string.title_import_private_key)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.label_key_name)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(AutomationTags.KEY_IMPORT_NAME)
                    )
                    OutlinedTextField(
                        value = privateKey,
                        onValueChange = { privateKey = it },
                        label = { Text(stringResource(R.string.label_private_key_content)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .testTag(AutomationTags.KEY_IMPORT_PRIVATE_KEY),
                        minLines = 4
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (repairTargetServerId != null) {
                            viewModel.importKeyForServer(
                                serverId = repairTargetServerId,
                                name = name,
                                privateContent = privateKey,
                                publicContent = null
                            ) {
                                onNavigateBack?.invoke()
                            }
                        } else {
                            viewModel.importKey(name, privateKey, null)
                        }
                    },
                    enabled = name.isNotBlank() && privateKey.isNotBlank()
                ) {
                    Text(stringResource(R.string.action_import))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideImportDialog() }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
