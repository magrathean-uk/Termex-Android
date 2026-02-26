package com.termex.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.termex.app.R
import com.termex.app.core.ssh.HostKeyVerificationResult

@Composable
fun PasswordDialog(
    hostname: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ssh_dialog_password_required)) },
        text = {
            Column {
                Text(stringResource(R.string.ssh_dialog_enter_password, hostname))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.ssh_dialog_password_label)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(password) }) {
                Text(stringResource(R.string.ssh_dialog_connect))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun HostKeyVerificationDialog(
    verification: HostKeyVerificationResult,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val isChanged = verification is HostKeyVerificationResult.Changed

    AlertDialog(
        onDismissRequest = onReject,
        icon = if (isChanged) {
            { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
        } else null,
        title = {
            Text(
                text = if (isChanged) stringResource(R.string.ssh_dialog_host_key_changed) else stringResource(R.string.ssh_dialog_unknown_host),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                when (verification) {
                    is HostKeyVerificationResult.Unknown -> {
                        Text(stringResource(R.string.ssh_dialog_host_authenticity, verification.hostname, verification.port))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.ssh_dialog_key_fingerprint, verification.keyType))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = verification.fingerprint,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(stringResource(R.string.ssh_dialog_continue_connecting))
                    }
                    is HostKeyVerificationResult.Changed -> {
                        Text(
                            text = stringResource(R.string.ssh_dialog_host_key_warning),
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.ssh_dialog_possible_attack))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.ssh_dialog_host_info, verification.hostname, verification.port))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(stringResource(R.string.ssh_dialog_old_fingerprint))
                        Text(
                            text = verification.oldFingerprint,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(stringResource(R.string.ssh_dialog_new_fingerprint))
                        Text(
                            text = verification.newFingerprint,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(stringResource(R.string.ssh_dialog_accept_explanation))
                    }
                    is HostKeyVerificationResult.Trusted -> Unit
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onAccept) {
                Text(if (isChanged) stringResource(R.string.ssh_dialog_accept_new_key) else stringResource(R.string.ssh_dialog_accept))
            }
        },
        dismissButton = {
            TextButton(onClick = onReject) {
                Text(stringResource(R.string.ssh_dialog_reject))
            }
        }
    )
}
