package com.termex.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.termex.app.R
import com.termex.app.core.transfer.TermexArchiveTransferException
import com.termex.app.ui.viewmodel.ServerTransferViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerTransferScreen(
    onNavigateBack: () -> Unit,
    viewModel: ServerTransferViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var summaryText by remember { mutableStateOf("Ready.") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var pendingExportBytes by remember { mutableStateOf<ByteArray?>(null) }
    var pendingExportName by remember { mutableStateOf("termex-export.termexarchive") }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("Could not read archive.")
                viewModel.importArchive(bytes, password)
            }.onSuccess { result ->
                summaryText = result.summary.summaryText
                errorText = null
            }.onFailure { failure ->
                errorText = failure.message ?: "Import failed."
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        val bytes = pendingExportBytes ?: return@rememberLauncherForActivityResult
        if (uri == null) {
            pendingExportBytes = null
            return@rememberLauncherForActivityResult
        }
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(bytes)
            } ?: error("Could not write archive.")
        }.onSuccess {
            summaryText = "Archive exported."
            errorText = null
        }.onFailure { failure ->
            errorText = failure.message ?: "Export failed."
        }
        pendingExportBytes = null
    }

    fun showError(message: String) {
        errorText = message
        summaryText = "Ready."
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Server Transfer") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Archive")
                    Text("Import or export a password-protected Termex archive.")
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                if (password.isBlank()) {
                                    showError(TermexArchiveTransferException.EmptyPassword.message ?: "Enter a password.")
                                    return@Button
                                }
                                if (password != confirmPassword) {
                                    showError(TermexArchiveTransferException.PasswordMismatch.message ?: "Passwords do not match.")
                                    return@Button
                                }
                                scope.launch {
                                    runCatching { viewModel.exportArchive(password) }
                                        .onSuccess { result ->
                                            pendingExportBytes = result.bytes
                                            pendingExportName = "termex-archive-v${result.summary.archiveVersion}.termexarchive"
                                            exportLauncher.launch(pendingExportName)
                                            summaryText = "Export ready."
                                            errorText = null
                                        }
                                        .onFailure { failure ->
                                            showError(failure.message ?: "Export failed.")
                                        }
                                }
                            }
                        ) {
                            Text("Export")
                        }
                        Button(
                            onClick = { importLauncher.launch(arrayOf("*/*")) }
                        ) {
                            Text("Import")
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Status")
                    Text(
                        text = if (errorText == null) summaryText else errorText!!,
                        color = if (errorText == null) androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                        else androidx.compose.material3.MaterialTheme.colorScheme.error
                    )
                    if (pendingExportBytes != null) {
                        Text("Waiting to save export file.")
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
