package com.termex.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.termex.app.R
import com.termex.app.domain.AuthMode
import com.termex.app.domain.PortForward
import com.termex.app.domain.PortForwardType
import com.termex.app.domain.Server
import com.termex.app.ui.AutomationTags
import com.termex.app.ui.viewmodel.IdentityImportTarget
import com.termex.app.ui.viewmodel.JumpHostMode
import com.termex.app.ui.viewmodel.OnboardingDraftState
import com.termex.app.ui.viewmodel.OnboardingStep
import com.termex.app.ui.viewmodel.OnboardingViewModel
import com.termex.app.ui.viewmodel.hasServerDraft

private enum class InlineImportDialog {
    KEY,
    CERTIFICATE
}

data class ForwardDraft(
    val type: PortForwardType = PortForwardType.LOCAL,
    val localPort: String = "",
    val remoteHost: String = "127.0.0.1",
    val remotePort: String = "",
    val bindAddress: String = "127.0.0.1"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingFlow(
    onComplete: (demoModeActivated: Boolean) -> Unit,
    onEnableDemoMode: () -> Unit = {},
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val draft by viewModel.draft.collectAsState()
    val keys by viewModel.keys.collectAsState()
    val certificates by viewModel.certificates.collectAsState()
    val servers by viewModel.servers.collectAsState()

    var demoModeActivated by remember { mutableStateOf(false) }
    var importDialog by remember { mutableStateOf<InlineImportDialog?>(null) }
    var importTarget by remember { mutableStateOf<IdentityImportTarget?>(null) }
    var showForwardDialog by remember { mutableStateOf(false) }

    LaunchedEffect(draft.completionReady) {
        if (draft.completionReady) {
            onComplete(demoModeActivated)
            viewModel.consumeCompletion()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.onboarding_title)) },
                navigationIcon = {
                    if (draft.step != OnboardingStep.WELCOME) {
                        IconButton(
                            modifier = Modifier.testTag(AutomationTags.ONBOARDING_BACK),
                            onClick = viewModel::back
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (draft.step != OnboardingStep.REVIEW) {
                    TextButton(
                        modifier = Modifier.testTag(AutomationTags.ONBOARDING_SKIP),
                        onClick = viewModel::skipToReview
                    ) {
                        Text(stringResource(R.string.onboarding_skip))
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Button(
                    modifier = Modifier.testTag(AutomationTags.ONBOARDING_PRIMARY),
                    onClick = {
                        if (draft.step == OnboardingStep.REVIEW) {
                            viewModel.finish(demoModeActivated)
                        } else {
                            viewModel.advance()
                        }
                    },
                    enabled = if (draft.step == OnboardingStep.REVIEW) !draft.isSaving else viewModel.canAdvance()
                ) {
                    Text(
                        text = when (draft.step) {
                            OnboardingStep.WELCOME -> stringResource(R.string.onboarding_start_setup)
                            OnboardingStep.REVIEW -> stringResource(R.string.onboarding_finish_setup)
                            else -> stringResource(R.string.onboarding_continue)
                        }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .testTag(stepTag(draft.step))
        ) {
            if (draft.errorMessage != null) {
                Text(
                    text = draft.errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            when (draft.step) {
                OnboardingStep.WELCOME -> WelcomeStep(
                    demoModeActivated = demoModeActivated,
                    onDemoMode = {
                        demoModeActivated = true
                        onEnableDemoMode()
                    }
                )
                OnboardingStep.DESTINATION -> DestinationStep(
                    draft = draft,
                    onNameChange = viewModel::updateServerName,
                    onHostChange = viewModel::updateHost,
                    onPortChange = viewModel::updatePort,
                    onUsernameChange = viewModel::updateUsername
                )
                OnboardingStep.AUTHENTICATION -> AuthenticationStep(
                    draft = draft,
                    keys = keys.map { it.path to it.name },
                    certificates = certificates.map { it.path to it.name },
                    onAuthModeChange = viewModel::updateAuthMode,
                    onPasswordChange = viewModel::updatePassword,
                    onIdentitiesOnlyChange = viewModel::updateIdentitiesOnly,
                    onSelectKey = { viewModel.selectKey(IdentityImportTarget.DESTINATION_KEY, it) },
                    onSelectCertificate = { viewModel.selectCertificate(IdentityImportTarget.DESTINATION_CERTIFICATE, it) },
                    onImportKey = {
                        importTarget = IdentityImportTarget.DESTINATION_KEY
                        importDialog = InlineImportDialog.KEY
                    },
                    onImportCertificate = {
                        importTarget = IdentityImportTarget.DESTINATION_CERTIFICATE
                        importDialog = InlineImportDialog.CERTIFICATE
                    }
                )
                OnboardingStep.JUMP_HOST -> JumpHostStep(
                    draft = draft,
                    servers = servers.filterNot(Server::isDemo),
                    keys = keys.map { it.path to it.name },
                    certificates = certificates.map { it.path to it.name },
                    onModeChange = viewModel::updateJumpHostMode,
                    onExistingJumpChange = viewModel::updateExistingJumpHost,
                    onReusePrimaryAuthChange = viewModel::updateJumpReusePrimaryAuth,
                    onJumpNameChange = viewModel::updateJumpName,
                    onJumpHostChange = viewModel::updateJumpHost,
                    onJumpPortChange = viewModel::updateJumpPort,
                    onJumpUsernameChange = viewModel::updateJumpUsername,
                    onJumpAuthModeChange = viewModel::updateJumpAuthMode,
                    onJumpPasswordChange = viewModel::updateJumpPassword,
                    onJumpIdentitiesOnlyChange = viewModel::updateJumpIdentitiesOnly,
                    onSelectKey = { viewModel.selectKey(IdentityImportTarget.JUMP_KEY, it) },
                    onSelectCertificate = { viewModel.selectCertificate(IdentityImportTarget.JUMP_CERTIFICATE, it) },
                    onImportKey = {
                        importTarget = IdentityImportTarget.JUMP_KEY
                        importDialog = InlineImportDialog.KEY
                    },
                    onImportCertificate = {
                        importTarget = IdentityImportTarget.JUMP_CERTIFICATE
                        importDialog = InlineImportDialog.CERTIFICATE
                    }
                )
                OnboardingStep.FORWARDING -> ForwardingStep(
                    draft = draft,
                    onAddForward = { showForwardDialog = true },
                    onDeleteForward = viewModel::removePortForward,
                    onPersistentSessionChange = viewModel::updatePersistentSessionEnabled,
                    onStartupCommandChange = viewModel::updateStartupCommand
                )
                OnboardingStep.REVIEW -> ReviewStep(
                    draft = draft,
                    jumpHostName = servers.firstOrNull { it.id == draft.existingJumpHostId }?.displayName
                )
            }
        }
    }

    if (showForwardDialog) {
        AddForwardDialog(
            onDismiss = { showForwardDialog = false },
            onSave = {
                viewModel.addPortForward(it)
                showForwardDialog = false
            }
        )
    }

    if (importDialog == InlineImportDialog.KEY && importTarget != null) {
        KeyImportDialog(
            onDismiss = { importDialog = null },
            onSave = { name, privateKey ->
                viewModel.importKey(importTarget!!, name, privateKey)
                importDialog = null
            }
        )
    }

    if (importDialog == InlineImportDialog.CERTIFICATE && importTarget != null) {
        CertificateImportDialog(
            onDismiss = { importDialog = null },
            onSave = { name, content ->
                viewModel.importCertificate(importTarget!!, name, content)
                importDialog = null
            }
        )
    }
}

@Composable
private fun WelcomeStep(
    demoModeActivated: Boolean,
    onDemoMode: () -> Unit
) {
    StepIntro(
        icon = Icons.Default.Route,
        title = stringResource(R.string.onboarding_welcome_title),
        body = stringResource(R.string.onboarding_guided_welcome_description)
    )
    Spacer(modifier = Modifier.height(20.dp))
    SectionCard(title = stringResource(R.string.onboarding_welcome_features_title)) {
        FeatureRow(icon = Icons.Default.Dns, title = stringResource(R.string.onboarding_feature_connection_title), body = stringResource(R.string.onboarding_feature_connection_body))
        FeatureRow(icon = Icons.Default.Key, title = stringResource(R.string.onboarding_feature_identity_title), body = stringResource(R.string.onboarding_feature_identity_body))
        FeatureRow(icon = Icons.Default.Tune, title = stringResource(R.string.onboarding_feature_jump_title), body = stringResource(R.string.onboarding_feature_jump_body))
    }
    Spacer(modifier = Modifier.height(12.dp))
    TextButton(
        modifier = Modifier.testTag(AutomationTags.ONBOARDING_DEMO_MODE),
        onClick = onDemoMode,
        enabled = !demoModeActivated
    ) {
        Text(stringResource(if (demoModeActivated) R.string.onboarding_demo_mode_enabled_state else R.string.onboarding_try_demo_mode))
    }
}

@Composable
private fun DestinationStep(
    draft: OnboardingDraftState,
    onNameChange: (String) -> Unit,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit
) {
    StepIntro(
        icon = Icons.Default.Dns,
        title = stringResource(R.string.onboarding_destination_title),
        body = stringResource(R.string.onboarding_destination_description)
    )
    Spacer(modifier = Modifier.height(20.dp))
    SectionCard(title = stringResource(R.string.onboarding_destination_section_title)) {
        OutlinedTextField(
            value = draft.serverName,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.server_settings_label_display_name)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(AutomationTags.ONBOARDING_SERVER_NAME),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = draft.host,
            onValueChange = onHostChange,
            label = { Text(stringResource(R.string.server_settings_label_host)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(AutomationTags.ONBOARDING_SERVER_HOST),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = draft.port,
                onValueChange = onPortChange,
                label = { Text(stringResource(R.string.server_settings_label_port)) },
                modifier = Modifier
                    .weight(1f)
                    .testTag(AutomationTags.ONBOARDING_SERVER_PORT),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = draft.username,
                onValueChange = onUsernameChange,
                label = { Text(stringResource(R.string.server_settings_label_username)) },
                modifier = Modifier
                    .weight(1.4f)
                    .testTag(AutomationTags.ONBOARDING_SERVER_USERNAME),
                singleLine = true
            )
        }
    }
}

@Composable
private fun AuthenticationStep(
    draft: OnboardingDraftState,
    keys: List<Pair<String, String>>,
    certificates: List<Pair<String, String>>,
    onAuthModeChange: (AuthMode) -> Unit,
    onPasswordChange: (String) -> Unit,
    onIdentitiesOnlyChange: (Boolean) -> Unit,
    onSelectKey: (String?) -> Unit,
    onSelectCertificate: (String?) -> Unit,
    onImportKey: () -> Unit,
    onImportCertificate: () -> Unit
) {
    StepIntro(
        icon = Icons.Default.Key,
        title = stringResource(R.string.onboarding_auth_title),
        body = stringResource(R.string.onboarding_auth_description)
    )
    Spacer(modifier = Modifier.height(20.dp))
    AuthEditor(
        title = stringResource(R.string.onboarding_auth_section_title),
        authMode = draft.auth.authMode,
        password = draft.auth.password,
        keyName = draft.auth.keyName,
        certificateName = draft.auth.certificateName,
        identitiesOnly = draft.auth.identitiesOnly,
        authModeTag = AutomationTags.ONBOARDING_AUTH_MODE,
        passwordTag = AutomationTags.ONBOARDING_AUTH_PASSWORD,
        keyTag = AutomationTags.ONBOARDING_KEY_PICKER,
        certificateTag = AutomationTags.ONBOARDING_CERTIFICATE_PICKER,
        identitiesOnlyTag = AutomationTags.ONBOARDING_IDENTITIES_ONLY,
        keys = keys,
        certificates = certificates,
        onAuthModeChange = onAuthModeChange,
        onPasswordChange = onPasswordChange,
        onIdentitiesOnlyChange = onIdentitiesOnlyChange,
        onSelectKey = onSelectKey,
        onSelectCertificate = onSelectCertificate,
        onImportKey = onImportKey,
        onImportCertificate = onImportCertificate
    )
}

@Composable
private fun JumpHostStep(
    draft: OnboardingDraftState,
    servers: List<Server>,
    keys: List<Pair<String, String>>,
    certificates: List<Pair<String, String>>,
    onModeChange: (JumpHostMode) -> Unit,
    onExistingJumpChange: (String?) -> Unit,
    onReusePrimaryAuthChange: (Boolean) -> Unit,
    onJumpNameChange: (String) -> Unit,
    onJumpHostChange: (String) -> Unit,
    onJumpPortChange: (String) -> Unit,
    onJumpUsernameChange: (String) -> Unit,
    onJumpAuthModeChange: (AuthMode) -> Unit,
    onJumpPasswordChange: (String) -> Unit,
    onJumpIdentitiesOnlyChange: (Boolean) -> Unit,
    onSelectKey: (String?) -> Unit,
    onSelectCertificate: (String?) -> Unit,
    onImportKey: () -> Unit,
    onImportCertificate: () -> Unit
) {
    StepIntro(
        icon = Icons.Default.Route,
        title = stringResource(R.string.onboarding_jump_title),
        body = stringResource(R.string.onboarding_jump_description)
    )
    Spacer(modifier = Modifier.height(20.dp))
    SectionCard(title = stringResource(R.string.onboarding_jump_mode_title)) {
        JumpModeChips(
            selected = draft.jumpHostMode,
            onSelected = onModeChange
        )
        if (draft.jumpHostMode == JumpHostMode.EXISTING) {
            Spacer(modifier = Modifier.height(12.dp))
            SelectionField(
                label = stringResource(R.string.server_settings_label_jump_host),
                value = servers.firstOrNull { it.id == draft.existingJumpHostId }?.displayName
                    ?: stringResource(R.string.server_settings_value_none),
                options = servers.map { it.id to it.displayName },
                onSelected = onExistingJumpChange,
                tag = AutomationTags.ONBOARDING_JUMP_EXISTING
            )
        }
    }

    if (draft.jumpHostMode == JumpHostMode.CUSTOM) {
        Spacer(modifier = Modifier.height(16.dp))
        SectionCard(title = stringResource(R.string.onboarding_jump_custom_title)) {
            OutlinedTextField(
                value = draft.jump.name,
                onValueChange = onJumpNameChange,
                label = { Text(stringResource(R.string.server_settings_label_display_name)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(AutomationTags.ONBOARDING_JUMP_NAME),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = draft.jump.host,
                onValueChange = onJumpHostChange,
                label = { Text(stringResource(R.string.server_settings_label_host)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(AutomationTags.ONBOARDING_JUMP_HOST),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = draft.jump.port,
                    onValueChange = onJumpPortChange,
                    label = { Text(stringResource(R.string.server_settings_label_port)) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag(AutomationTags.ONBOARDING_JUMP_PORT),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = draft.jump.username,
                    onValueChange = onJumpUsernameChange,
                    label = { Text(stringResource(R.string.server_settings_label_username)) },
                    modifier = Modifier
                        .weight(1.4f)
                        .testTag(AutomationTags.ONBOARDING_JUMP_USERNAME),
                    singleLine = true
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            ListItem(
                headlineContent = { Text(stringResource(R.string.onboarding_jump_reuse_auth_title)) },
                supportingContent = { Text(stringResource(R.string.onboarding_jump_reuse_auth_body)) },
                trailingContent = {
                    Switch(
                        checked = draft.jump.reusePrimaryAuth,
                        onCheckedChange = onReusePrimaryAuthChange,
                        modifier = Modifier.testTag(AutomationTags.ONBOARDING_JUMP_REUSE_AUTH)
                    )
                }
            )
        }

        if (!draft.jump.reusePrimaryAuth) {
            Spacer(modifier = Modifier.height(16.dp))
            AuthEditor(
                title = stringResource(R.string.onboarding_jump_auth_title),
                authMode = draft.jump.auth.authMode,
                password = draft.jump.auth.password,
                keyName = draft.jump.auth.keyName,
                certificateName = draft.jump.auth.certificateName,
                identitiesOnly = draft.jump.auth.identitiesOnly,
                authModeTag = AutomationTags.ONBOARDING_JUMP_AUTH_MODE,
                passwordTag = AutomationTags.ONBOARDING_JUMP_PASSWORD,
                keyTag = AutomationTags.ONBOARDING_JUMP_KEY_PICKER,
                certificateTag = AutomationTags.ONBOARDING_JUMP_CERTIFICATE_PICKER,
                identitiesOnlyTag = AutomationTags.ONBOARDING_JUMP_IDENTITIES_ONLY,
                keys = keys,
                certificates = certificates,
                onAuthModeChange = onJumpAuthModeChange,
                onPasswordChange = onJumpPasswordChange,
                onIdentitiesOnlyChange = onJumpIdentitiesOnlyChange,
                onSelectKey = onSelectKey,
                onSelectCertificate = onSelectCertificate,
                onImportKey = onImportKey,
                onImportCertificate = onImportCertificate
            )
        }
    }
}

@Composable
private fun ForwardingStep(
    draft: OnboardingDraftState,
    onAddForward: () -> Unit,
    onDeleteForward: (String) -> Unit,
    onPersistentSessionChange: (Boolean) -> Unit,
    onStartupCommandChange: (String) -> Unit
) {
    StepIntro(
        icon = Icons.Default.Tune,
        title = stringResource(R.string.onboarding_forwarding_title),
        body = stringResource(R.string.onboarding_forwarding_description)
    )
    Spacer(modifier = Modifier.height(20.dp))
    SectionCard(title = stringResource(R.string.onboarding_forwarding_section_title)) {
        TextButton(
            modifier = Modifier.testTag(AutomationTags.ONBOARDING_FORWARD_ADD),
            onClick = onAddForward
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.port_forwarding_add_title))
        }

        if (draft.portForwards.isEmpty()) {
            Text(
                text = stringResource(R.string.onboarding_forwarding_empty),
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            draft.portForwards.forEach { forward ->
                ListItem(
                    headlineContent = { Text(forward.displayString) },
                    trailingContent = {
                        IconButton(
                            modifier = Modifier.testTag(AutomationTags.ONBOARDING_FORWARD_DELETE_PREFIX + forward.id),
                            onClick = { onDeleteForward(forward.id) }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        }
                    }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    SectionCard(title = stringResource(R.string.onboarding_session_title)) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.server_settings_label_persistent_session)) },
            supportingContent = { Text(stringResource(R.string.onboarding_persistent_tmux_description)) },
            trailingContent = {
                Switch(
                    checked = draft.persistentSessionEnabled,
                    onCheckedChange = onPersistentSessionChange,
                    modifier = Modifier.testTag(AutomationTags.ONBOARDING_PERSISTENT_TMUX)
                )
            }
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = draft.startupCommand,
            onValueChange = onStartupCommandChange,
            label = { Text(stringResource(R.string.server_settings_label_startup_command)) },
            placeholder = { Text(stringResource(R.string.server_settings_placeholder_startup_command)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(AutomationTags.ONBOARDING_STARTUP_COMMAND),
            minLines = 2
        )
    }
}

@Composable
private fun ReviewStep(
    draft: OnboardingDraftState,
    jumpHostName: String?
) {
    StepIntro(
        icon = Icons.Default.Tune,
        title = stringResource(R.string.onboarding_review_title),
        body = stringResource(R.string.onboarding_review_description)
    )
    Spacer(modifier = Modifier.height(20.dp))
    SectionCard(title = stringResource(R.string.onboarding_review_summary_title)) {
        if (!draft.hasServerDraft()) {
            Text(
                text = stringResource(R.string.onboarding_review_skip_body),
                style = MaterialTheme.typography.bodyMedium
            )
            return@SectionCard
        }

        ReviewRow(label = stringResource(R.string.server_settings_label_display_name), value = draft.serverName.ifBlank { draft.host })
        ReviewRow(label = stringResource(R.string.server_settings_label_host), value = "${draft.username}@${draft.host}:${draft.port}")
        ReviewRow(label = stringResource(R.string.server_settings_label_method), value = draft.auth.authMode.name)
        ReviewRow(label = stringResource(R.string.onboarding_review_key), value = draft.auth.keyName ?: "None")
        ReviewRow(label = stringResource(R.string.onboarding_review_certificate), value = draft.auth.certificateName ?: "None")
        ReviewRow(label = stringResource(R.string.onboarding_review_jump), value = when (draft.jumpHostMode) {
            JumpHostMode.DIRECT -> stringResource(R.string.onboarding_jump_mode_direct)
            JumpHostMode.EXISTING -> jumpHostName ?: stringResource(R.string.server_settings_value_none)
            JumpHostMode.CUSTOM -> draft.jump.name.ifBlank { draft.jump.host }
        })
        ReviewRow(label = stringResource(R.string.onboarding_review_forwards), value = draft.portForwards.size.toString())
        ReviewRow(label = stringResource(R.string.server_settings_label_persistent_session), value = if (draft.persistentSessionEnabled) "On" else "Off")
        ReviewRow(label = stringResource(R.string.server_settings_label_startup_command), value = draft.startupCommand.ifBlank { "None" })
    }
}

@Composable
private fun AuthEditor(
    title: String,
    authMode: AuthMode,
    password: String,
    keyName: String?,
    certificateName: String?,
    identitiesOnly: Boolean,
    authModeTag: String,
    passwordTag: String,
    keyTag: String,
    certificateTag: String,
    identitiesOnlyTag: String,
    keys: List<Pair<String, String>>,
    certificates: List<Pair<String, String>>,
    onAuthModeChange: (AuthMode) -> Unit,
    onPasswordChange: (String) -> Unit,
    onIdentitiesOnlyChange: (Boolean) -> Unit,
    onSelectKey: (String?) -> Unit,
    onSelectCertificate: (String?) -> Unit,
    onImportKey: () -> Unit,
    onImportCertificate: () -> Unit
) {
    SectionCard(title = title) {
        Text(text = stringResource(R.string.server_settings_label_method), style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(authModeTag),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = authMode == AuthMode.PASSWORD,
                onClick = { onAuthModeChange(AuthMode.PASSWORD) },
                label = { Text(stringResource(R.string.server_settings_auth_password)) }
            )
            FilterChip(
                selected = authMode == AuthMode.KEY,
                onClick = { onAuthModeChange(AuthMode.KEY) },
                label = { Text(stringResource(R.string.server_settings_auth_ssh_key)) }
            )
        }

        if (authMode == AuthMode.PASSWORD) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text(stringResource(R.string.server_settings_auth_password)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(passwordTag)
            )
        } else {
            Spacer(modifier = Modifier.height(12.dp))
            SelectionField(
                label = stringResource(R.string.server_settings_auth_ssh_key),
                value = keyName ?: stringResource(R.string.server_settings_value_none),
                options = listOf(null to stringResource(R.string.server_settings_value_none)) + keys,
                onSelected = onSelectKey,
                tag = keyTag
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                    modifier = Modifier.testTag(AutomationTags.ONBOARDING_IMPORT_KEY),
                    onClick = onImportKey
                ) {
                    Text(stringResource(R.string.action_import_key))
                }
            }

            SelectionField(
                label = stringResource(R.string.onboarding_certificate_title),
                value = certificateName ?: stringResource(R.string.server_settings_value_none),
                options = listOf(null to stringResource(R.string.server_settings_value_none)) + certificates,
                onSelected = onSelectCertificate,
                tag = certificateTag
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                    modifier = Modifier.testTag(AutomationTags.ONBOARDING_IMPORT_CERTIFICATE),
                    onClick = onImportCertificate
                ) {
                    Text(stringResource(R.string.certificates_import_title))
                }
            }

            ListItem(
                headlineContent = { Text(stringResource(R.string.server_settings_label_identities_only)) },
                supportingContent = { Text(stringResource(R.string.server_settings_identities_only_description)) },
                trailingContent = {
                    Switch(
                        checked = identitiesOnly,
                        onCheckedChange = onIdentitiesOnlyChange,
                        modifier = Modifier.testTag(identitiesOnlyTag)
                    )
                }
            )
        }
    }
}

@Composable
private fun KeyImportDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var privateKey by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_import_private_key)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.label_key_name)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(AutomationTags.ONBOARDING_IMPORT_NAME)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = privateKey,
                    onValueChange = { privateKey = it },
                    label = { Text(stringResource(R.string.label_private_key_content)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(AutomationTags.ONBOARDING_IMPORT_PRIVATE_KEY),
                    minLines = 4
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, privateKey) },
                enabled = name.isNotBlank() && privateKey.isNotBlank()
            ) {
                Text(stringResource(R.string.action_import))
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
private fun CertificateImportDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.certificates_import_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.certificates_label_name)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(AutomationTags.ONBOARDING_IMPORT_NAME)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text(stringResource(R.string.certificates_label_content)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(AutomationTags.ONBOARDING_IMPORT_CERTIFICATE_CONTENT),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, content) },
                enabled = name.isNotBlank() && content.isNotBlank()
            ) {
                Text(stringResource(R.string.action_import))
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
private fun AddForwardDialog(
    onDismiss: () -> Unit,
    onSave: (PortForward) -> Unit
) {
    var form by remember { mutableStateOf(ForwardDraft()) }
    var showTypeMenu by remember { mutableStateOf(false) }
    val supportedTypes = listOf(PortForwardType.LOCAL, PortForwardType.REMOTE, PortForwardType.DYNAMIC)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.port_forwarding_add_title)) },
        text = {
            Column {
                SelectionField(
                    label = stringResource(R.string.port_forwarding_type),
                    value = form.type.name,
                    options = supportedTypes.map { it.name to it.name },
                    onSelected = { selected ->
                        if (selected != null) {
                            form = form.copy(type = PortForwardType.valueOf(selected))
                        }
                    },
                    tag = AutomationTags.PORT_FORWARD_TYPE,
                    expandedState = showTypeMenu,
                    onExpandedChange = { showTypeMenu = it }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = form.localPort,
                    onValueChange = { form = form.copy(localPort = it) },
                    label = { Text(stringResource(R.string.port_forwarding_local_port)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(AutomationTags.PORT_FORWARD_LOCAL_PORT),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                if (form.type != PortForwardType.DYNAMIC) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = form.remoteHost,
                        onValueChange = { form = form.copy(remoteHost = it) },
                        label = { Text(stringResource(R.string.port_forwarding_remote_host)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(AutomationTags.PORT_FORWARD_REMOTE_HOST)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = form.remotePort,
                        onValueChange = { form = form.copy(remotePort = it) },
                        label = { Text(stringResource(R.string.port_forwarding_remote_port)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(AutomationTags.PORT_FORWARD_REMOTE_PORT),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                if (form.type == PortForwardType.REMOTE) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = form.bindAddress,
                        onValueChange = { form = form.copy(bindAddress = it) },
                        label = { Text(stringResource(R.string.port_forwarding_bind_address)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(AutomationTags.PORT_FORWARD_BIND_ADDRESS)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                modifier = Modifier.testTag(AutomationTags.PORT_FORWARD_SAVE),
                onClick = {
                    onSave(
                        PortForward(
                            type = form.type,
                            localPort = form.localPort.toInt(),
                            remoteHost = if (form.type == PortForwardType.DYNAMIC) "127.0.0.1" else form.remoteHost,
                            remotePort = if (form.type == PortForwardType.DYNAMIC) form.localPort.toInt() else form.remotePort.toInt(),
                            bindAddress = form.bindAddress
                        )
                    )
                },
                enabled = form.localPort.toIntOrNull() != null &&
                    (form.type == PortForwardType.DYNAMIC || form.remotePort.toIntOrNull() != null)
            ) {
                Text(stringResource(R.string.action_save))
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
private fun StepIntro(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String
) {
    Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    Spacer(modifier = Modifier.height(12.dp))
    Text(text = title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(8.dp))
    Text(text = body, style = MaterialTheme.typography.bodyLarge)
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun FeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column {
            Text(text = title, fontWeight = FontWeight.SemiBold)
            Text(text = body, style = MaterialTheme.typography.bodySmall)
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun JumpModeChips(
    selected: JumpHostMode,
    onSelected: (JumpHostMode) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        JumpHostMode.entries.forEach { mode ->
            FilterChip(
                selected = selected == mode,
                onClick = { onSelected(mode) },
                label = {
                    Text(
                        text = when (mode) {
                            JumpHostMode.DIRECT -> stringResource(R.string.onboarding_jump_mode_direct)
                            JumpHostMode.EXISTING -> stringResource(R.string.onboarding_jump_mode_existing)
                            JumpHostMode.CUSTOM -> stringResource(R.string.onboarding_jump_mode_custom)
                        }
                    )
                },
                modifier = Modifier.testTag(AutomationTags.ONBOARDING_JUMP_MODE + "_" + mode.name.lowercase())
            )
        }
    }
}

@Composable
private fun SelectionField(
    label: String,
    value: String,
    options: List<Pair<String?, String>>,
    onSelected: (String?) -> Unit,
    tag: String,
    expandedState: Boolean? = null,
    onExpandedChange: ((Boolean) -> Unit)? = null
) {
    var expandedInternal by remember { mutableStateOf(false) }
    val expanded = expandedState ?: expandedInternal
    val setExpanded = onExpandedChange ?: { expandedInternal = it }

    Box {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(tag)
                .clickable { setExpanded(true) }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { setExpanded(false) }
        ) {
            options.forEach { (id, title) ->
                DropdownMenuItem(
                    text = { Text(title) },
                    onClick = {
                        onSelected(id)
                        setExpanded(false)
                    }
                )
            }
        }
    }
}

private fun stepTag(step: OnboardingStep): String {
    return when (step) {
        OnboardingStep.WELCOME -> AutomationTags.ONBOARDING_STEP_WELCOME
        OnboardingStep.DESTINATION -> AutomationTags.ONBOARDING_STEP_DESTINATION
        OnboardingStep.AUTHENTICATION -> AutomationTags.ONBOARDING_STEP_AUTH
        OnboardingStep.JUMP_HOST -> AutomationTags.ONBOARDING_STEP_JUMP
        OnboardingStep.FORWARDING -> AutomationTags.ONBOARDING_STEP_FORWARDING
        OnboardingStep.REVIEW -> AutomationTags.ONBOARDING_STEP_REVIEW
    }
}
