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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        title = { Text("Password Required") },
        text = {
            Column {
                Text("Enter password for $hostname")
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
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
                Text("Connect")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
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
                text = if (isChanged) "Host Key Changed" else "Unknown Host",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                when (verification) {
                    is HostKeyVerificationResult.Unknown -> {
                        Text("The authenticity of host '${verification.hostname}:${verification.port}' can't be established.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("${verification.keyType} key fingerprint is:")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = verification.fingerprint,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Are you sure you want to continue connecting?")
                    }
                    is HostKeyVerificationResult.Changed -> {
                        Text(
                            text = "WARNING: REMOTE HOST IDENTIFICATION HAS CHANGED!",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("It is possible that someone is doing something nasty!")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Host: ${verification.hostname}:${verification.port}")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Old fingerprint:")
                        Text(
                            text = verification.oldFingerprint,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("New fingerprint:")
                        Text(
                            text = verification.newFingerprint,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("If you expected this change (e.g., server reinstall), you can accept the new key.")
                    }
                    is HostKeyVerificationResult.Trusted -> Unit
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onAccept) {
                Text(if (isChanged) "Accept New Key" else "Accept")
            }
        },
        dismissButton = {
            TextButton(onClick = onReject) {
                Text("Reject")
            }
        }
    )
}
