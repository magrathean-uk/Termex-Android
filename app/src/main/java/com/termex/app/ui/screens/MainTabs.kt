package com.termex.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.termex.app.BuildConfig
import com.termex.app.R
import com.termex.app.domain.AuthMode
import com.termex.app.domain.Server
import com.termex.app.ui.navigation.Route
import com.termex.app.ui.viewmodel.KeysViewModel
import com.termex.app.ui.viewmodel.ServersViewModel
import com.termex.app.ui.viewmodel.SettingsViewModel
import com.termex.app.ui.viewmodel.SnippetsViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTabs(
    rootNavController: NavHostController
) {
    val navController = rememberNavController()
    var selectedItem by remember { mutableIntStateOf(0) }
    
    val items = listOf(
        stringResource(R.string.nav_servers),
        stringResource(R.string.nav_keys),
        stringResource(R.string.nav_snippets),
        stringResource(R.string.nav_settings)
    )
    val icons = listOf(
        Icons.Default.Dns,
        Icons.Default.Key,
        Icons.Default.Code,
        Icons.Default.Settings
    )
    val routes = listOf(
        Route.Servers.route,
        Route.Keys.route,
        Route.Snippets.route,
        Route.Settings.route
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(icons[index], contentDescription = item) },
                        label = { Text(item) },
                        selected = selectedItem == index,
                        onClick = {
                            selectedItem = index
                            navController.navigate(routes[index]) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Route.Servers.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Route.Servers.route) {
                ServerListScreen(
                    onServerClick = { server ->
                        rootNavController.navigate(Route.Terminal.createRoute(server.id))
                    },
                    onAddServer = {
                        rootNavController.navigate(Route.ServerSettings.createRoute(null))
                    },
                    onEditServer = { server ->
                        rootNavController.navigate(Route.ServerSettings.createRoute(server.id))
                    },
                    onPortForwarding = { server ->
                        rootNavController.navigate(Route.PortForwarding.createRoute(server.id))
                    }
                )
            }
            composable(Route.Keys.route) { KeyListScreen() }
            composable(Route.Snippets.route) { SnippetListScreen() }
            composable(Route.Settings.route) {
                SettingsScreen(
                    onNavigateToWorkplaces = {
                        rootNavController.navigate(Route.Workplaces.route)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerListScreen(
    onServerClick: (Server) -> Unit,
    onAddServer: () -> Unit,
    onEditServer: (Server) -> Unit,
    onPortForwarding: (Server) -> Unit,
    viewModel: ServersViewModel = hiltViewModel()
) {
    val servers by viewModel.servers.collectAsState()
    val demoModeEnabled by viewModel.demoModeEnabled.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_servers)) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddServer) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_add_server))
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Demo Mode Warning Banner
            if (demoModeEnabled) {
                val bannerColor = MaterialTheme.colorScheme.tertiaryContainer
                val bannerTextColor = MaterialTheme.colorScheme.onTertiaryContainer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bannerColor)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = bannerTextColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.demo_mode_active),
                            color = bannerTextColor,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.demo_mode_message),
                            color = bannerTextColor.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            if (servers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.empty_servers))
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                items(servers, key = { it.id }) { server ->
                    var showMenu by remember { mutableStateOf(false) }

                    ListItem(
                        headlineContent = {
                            if (server.isDemo) {
                                Text("${server.displayName} (Demo)")
                            } else {
                                Text(server.displayName)
                            }
                        },
                        supportingContent = {
                            if (server.isDemo) {
                                Text(stringResource(R.string.try_demo_server))
                            } else {
                                Text("${server.username}@${server.hostname}:${server.port}")
                            }
                        },
                        modifier = Modifier.clickable { onServerClick(server) },
                        trailingContent = {
                            // Don't show edit/delete for demo server
                            if (!server.isDemo) {
                                Box {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = stringResource(R.string.action_more),
                                        modifier = Modifier.clickable { showMenu = true }
                                    )
                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.action_edit)) },
                                            onClick = {
                                                showMenu = false
                                                onEditServer(server)
                                            },
                                            leadingIcon = { Icon(Icons.Default.Edit, null) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.action_port_forwarding)) },
                                            onClick = {
                                                showMenu = false
                                                onPortForwarding(server)
                                            },
                                            leadingIcon = { Icon(Icons.Default.Dns, null) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.action_delete)) },
                                            onClick = {
                                                showMenu = false
                                                viewModel.deleteServer(server)
                                            },
                                            leadingIcon = { Icon(Icons.Default.Delete, null) }
                                        )
                                    }
                                }
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyListScreen(
    viewModel: KeysViewModel = hiltViewModel()
) {
    val keys by viewModel.keys.collectAsState()
    val showGenerate by viewModel.showGenerateDialog.collectAsState()
    val showImport by viewModel.showImportDialog.collectAsState()
    
    var showMenu by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.title_ssh_keys)) })
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_add_key))
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
                        text = { Text(stringResource(R.string.action_import_key)) },
                        onClick = {
                            showMenu = false
                            viewModel.showImportDialog()
                        }
                    )
                }
            }
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
                    ListItem(
                        headlineContent = { Text(key.name) },
                        supportingContent = {
                            Column {
                                Text(key.type)
                                if (key.fingerprint.isNotEmpty()) {
                                    Text(
                                        text = key.fingerprint,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        maxLines = 1,
                                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        },
                        trailingContent = {
                            androidx.compose.material3.IconButton(
                                onClick = { viewModel.deleteKey(key) }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
                            }
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
            title = { Text(stringResource(R.string.title_generate_rsa_key)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.label_key_name_example)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = privateKey,
                        onValueChange = { privateKey = it },
                        label = { Text(stringResource(R.string.label_private_key_content)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        minLines = 4
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.importKey(name, privateKey, null) },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnippetListScreen(
    viewModel: SnippetsViewModel = hiltViewModel()
) {
    val snippets by viewModel.snippets.collectAsState()
    val showDialog by viewModel.showAddDialog.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.title_snippets)) })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showAddDialog() }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_add_snippet))
            }
        }
    ) { padding ->
        if (snippets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.empty_snippets))
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(snippets, key = { it.id }) { snippet ->
                     ListItem(
                        headlineContent = { Text(snippet.name) },
                        supportingContent = { 
                            Text(
                                text = snippet.command,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        },
                        trailingContent = {
                            androidx.compose.material3.IconButton(
                                onClick = { viewModel.deleteSnippet(snippet) }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
    
    if (showDialog) {
        var name by remember { mutableStateOf("") }
        var command by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { viewModel.dismissAddDialog() },
            title = { Text(stringResource(R.string.title_add_snippet)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.label_name)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = command,
                        onValueChange = { command = it },
                        label = { Text(stringResource(R.string.label_command)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.addSnippet(name, command) },
                    enabled = name.isNotBlank() && command.isNotBlank()
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissAddDialog() }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToWorkplaces: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val terminalSettings by viewModel.terminalSettings.collectAsState()
    val keepAliveInterval by viewModel.keepAliveInterval.collectAsState()
    val demoModeEnabled by viewModel.demoModeEnabled.collectAsState()

    var showKeepAliveMenu by remember { mutableStateOf(false) }
    var showColorSchemeMenu by remember { mutableStateOf(false) }
    var showFontMenu by remember { mutableStateOf(false) }
    var showFontSizeMenu by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.title_settings)) })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.title_theme)) },
                    supportingContent = { Text(themeMode.label) },
                    modifier = Modifier.clickable {
                        val next = when (themeMode) {
                            com.termex.app.data.prefs.ThemeMode.AUTO -> com.termex.app.data.prefs.ThemeMode.LIGHT
                            com.termex.app.data.prefs.ThemeMode.LIGHT -> com.termex.app.data.prefs.ThemeMode.DARK
                            com.termex.app.data.prefs.ThemeMode.DARK -> com.termex.app.data.prefs.ThemeMode.AUTO
                        }
                        viewModel.setThemeMode(next)
                    }
                )
                HorizontalDivider()
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.title_workplaces)) },
                    supportingContent = { Text(stringResource(R.string.workplaces_supporting)) },
                    modifier = Modifier.clickable { onNavigateToWorkplaces() }
                )
                HorizontalDivider()
            }

            item {
                Box {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.title_color_scheme)) },
                        supportingContent = { Text(terminalSettings.colorScheme) },
                        modifier = Modifier.clickable { showColorSchemeMenu = true }
                    )
                    DropdownMenu(
                        expanded = showColorSchemeMenu,
                        onDismissRequest = { showColorSchemeMenu = false }
                    ) {
                        com.termex.app.ui.theme.TerminalColorScheme.entries.forEach { scheme ->
                            DropdownMenuItem(
                                text = { Text(scheme.displayName) },
                                onClick = {
                                    viewModel.setColorScheme(scheme.displayName)
                                    showColorSchemeMenu = false
                                }
                            )
                        }
                    }
                }
                HorizontalDivider()
            }

            item {
                Box {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.title_font)) },
                        supportingContent = { Text(terminalSettings.fontFamily) },
                        modifier = Modifier.clickable { showFontMenu = true }
                    )
                    DropdownMenu(
                        expanded = showFontMenu,
                        onDismissRequest = { showFontMenu = false }
                    ) {
                        com.termex.app.ui.theme.TerminalFont.entries.forEach { font ->
                            DropdownMenuItem(
                                text = { Text(font.displayName) },
                                onClick = {
                                    viewModel.setFontFamily(font.displayName)
                                    showFontMenu = false
                                }
                            )
                        }
                    }
                }
                HorizontalDivider()
            }

            item {
                Box {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.title_font_size)) },
                        supportingContent = { Text("${terminalSettings.fontSize}pt") },
                        modifier = Modifier.clickable { showFontSizeMenu = true }
                    )
                    DropdownMenu(
                        expanded = showFontSizeMenu,
                        onDismissRequest = { showFontSizeMenu = false }
                    ) {
                        listOf(10, 12, 14, 16, 18, 20, 24).forEach { size ->
                            DropdownMenuItem(
                                text = { Text("${size}pt") },
                                onClick = {
                                    viewModel.setFontSize(size)
                                    showFontSizeMenu = false
                                }
                            )
                        }
                    }
                }
                HorizontalDivider()
            }

            item {
                Box {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.title_keep_alive_interval)) },
                        supportingContent = { Text(keepAliveInterval.label) },
                        modifier = Modifier.clickable { showKeepAliveMenu = true }
                    )
                    DropdownMenu(
                        expanded = showKeepAliveMenu,
                        onDismissRequest = { showKeepAliveMenu = false }
                    ) {
                        com.termex.app.data.prefs.KeepAliveInterval.entries.forEach { interval ->
                            DropdownMenuItem(
                                text = { Text(interval.label) },
                                onClick = {
                                    viewModel.setKeepAliveInterval(interval)
                                    showKeepAliveMenu = false
                                }
                            )
                        }
                    }
                }
                HorizontalDivider()
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.action_restore_purchases)) },
                    modifier = Modifier.clickable { viewModel.restorePurchases() }
                )
                HorizontalDivider()
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.action_reset_app)) },
                    supportingContent = { Text(stringResource(R.string.reset_app_supporting)) },
                    modifier = Modifier.clickable { showResetDialog = true }
                )
                HorizontalDivider()
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.title_version)) },
                    supportingContent = {
                        val versionName = BuildConfig.VERSION_NAME
                        val label = if (demoModeEnabled) {
                            stringResource(R.string.version_demo_format, versionName)
                        } else {
                            versionName
                        }
                        Text(label)
                    },
                    modifier = Modifier.clickable { viewModel.onVersionTap() }
                )
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.title_reset_app)) },
            text = { Text(stringResource(R.string.reset_app_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetApp()
                        showResetDialog = false
                    }
                ) {
                    Text(stringResource(R.string.action_reset))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
