package com.termex.app.ui.components

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.termex.app.R
import com.termex.app.core.ssh.HostKeyVerificationResult
import com.termex.app.core.ssh.SSHConnectionState
import com.termex.app.data.diagnostics.DiagnosticEvent
import com.termex.app.domain.AuthMode
import com.termex.app.domain.Server

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionReportSheet(
    server: Server?,
    connectionState: SSHConnectionState,
    events: List<DiagnosticEvent>,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.connection_report_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.connection_report_state)) },
                    supportingContent = { Text(connectionStateLabel(connectionState)) }
                )
            }

            server?.let { currentServer ->
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.connection_report_target)) },
                        supportingContent = { Text("${currentServer.username}@${currentServer.hostname}:${currentServer.port}") }
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.connection_report_auth)) },
                        supportingContent = { Text(authModeLabel(currentServer.authMode)) }
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.connection_report_jump_host)) },
                        supportingContent = {
                            Text(currentServer.jumpHostId ?: stringResource(R.string.server_settings_value_none))
                        }
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.connection_report_port_forwards)) },
                        supportingContent = {
                            Text(
                                pluralStringResource(
                                    R.plurals.connection_report_port_forward_count,
                                    currentServer.portForwards.size,
                                    currentServer.portForwards.size
                                )
                            )
                        }
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.connection_report_security_flags)) },
                        supportingContent = {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    stringResource(
                                        R.string.connection_report_forward_agent,
                                        if (currentServer.forwardAgent) stringResource(R.string.settings_enabled) else stringResource(R.string.settings_disabled)
                                    )
                                )
                                Text(
                                    stringResource(
                                        R.string.connection_report_identities_only,
                                        if (currentServer.identitiesOnly) stringResource(R.string.settings_enabled) else stringResource(R.string.settings_disabled)
                                    )
                                )
                            }
                        }
                    )
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            item {
                Text(
                    text = stringResource(R.string.connection_report_recent_events),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            if (events.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.connection_report_no_events),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(events, key = { it.id }) { event ->
                    ListItem(
                        headlineContent = { Text(event.title) },
                        supportingContent = {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = DateUtils.getRelativeTimeSpanString(
                                        event.timestamp,
                                        System.currentTimeMillis(),
                                        DateUtils.MINUTE_IN_MILLIS
                                    ).toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                event.detail?.let {
                                    Text(
                                        text = it,
                                        fontFamily = FontFamily.Monospace,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun authModeLabel(authMode: AuthMode): String = when (authMode) {
    AuthMode.PASSWORD -> "Password"
    AuthMode.KEY -> "SSH key"
    AuthMode.AUTO -> "Auto"
}

private fun connectionStateLabel(connectionState: SSHConnectionState): String = when (connectionState) {
    SSHConnectionState.Connected -> "Connected"
    SSHConnectionState.Connecting -> "Connecting"
    SSHConnectionState.Disconnected -> "Disconnected"
    is SSHConnectionState.Error -> connectionState.message
    is SSHConnectionState.VerifyingHostKey -> when (connectionState.result) {
        is HostKeyVerificationResult.Changed -> "Verifying changed host key"
        is HostKeyVerificationResult.Unknown -> "Verifying new host key"
        HostKeyVerificationResult.Trusted -> "Trusted host key"
    }
}
