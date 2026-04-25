package com.termex.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import com.termex.app.domain.SSHCertificate
import com.termex.app.ui.AutomationTags
import com.termex.app.ui.viewmodel.CertificatesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CertificatesScreen(
    onNavigateBack: (() -> Unit)? = null,
    repairTargetServerId: String? = null,
    autoOpenImportDialog: Boolean = false,
    viewModel: CertificatesViewModel = hiltViewModel()
) {
    val certificates by viewModel.certificates.collectAsState()
    val showImport by viewModel.showImportDialog.collectAsState()
    var certToDelete by remember { mutableStateOf<SSHCertificate?>(null) }
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
                title = { Text(stringResource(R.string.certificates_title)) },
                navigationIcon = {
                    onNavigateBack?.let { back ->
                        IconButton(onClick = back) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                modifier = Modifier.testTag(AutomationTags.CERTIFICATES_ADD),
                onClick = { viewModel.showImportDialog() }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Import Certificate")
            }
        }
    ) { padding ->
        if (certificates.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.certificates_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.certificates_empty_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 32.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(certificates, key = { it.id }) { cert ->
                    ListItem(
                        headlineContent = { Text(cert.name) },
                        supportingContent = {
                            Column {
                                Text(
                                    text = cert.keyId.ifEmpty { cert.certificateType.name.lowercase() },
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (cert.caFingerprint.isNotEmpty()) {
                                    Text(
                                        text = stringResource(R.string.certificates_ca_fingerprint, cert.caFingerprint),
                                        fontFamily = FontFamily.Monospace,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        leadingContent = {
                            FilterChip(
                                selected = cert.isValid,
                                onClick = {},
                                label = {
                                    Text(
                                        text = cert.validityStatus,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            )
                        },
                        trailingContent = {
                            IconButton(onClick = { certToDelete = cert }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        modifier = if (repairTargetServerId != null) {
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.repairServerWithSelectedCertificate(repairTargetServerId, cert) {
                                        onNavigateBack?.invoke()
                                    }
                                }
                                .testTag("repair_certificate_${cert.id}")
                        } else {
                            Modifier
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (showImport) {
        var name by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { viewModel.hideImportDialog() },
            title = { Text(stringResource(R.string.certificates_import_title)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.certificates_label_name)) },
                        placeholder = { Text(stringResource(R.string.certificates_placeholder_name)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(AutomationTags.CERTIFICATE_IMPORT_NAME),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text(stringResource(R.string.certificates_label_content)) },
                        placeholder = { Text(stringResource(R.string.certificates_placeholder_content)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .testTag(AutomationTags.CERTIFICATE_IMPORT_CONTENT),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (repairTargetServerId != null) {
                            viewModel.importCertificateForServer(
                                serverId = repairTargetServerId,
                                name = name,
                                content = content
                            ) {
                                onNavigateBack?.invoke()
                            }
                        } else {
                            viewModel.importCertificate(name, content)
                        }
                    },
                    enabled = name.isNotBlank() && content.isNotBlank()
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

    certToDelete?.let { cert ->
        AlertDialog(
            onDismissRequest = { certToDelete = null },
            title = { Text(stringResource(R.string.certificates_delete_title)) },
            text = { Text(stringResource(R.string.certificates_delete_message, cert.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCertificate(cert)
                        certToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { certToDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
