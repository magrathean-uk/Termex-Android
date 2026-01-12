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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
    
    val items = listOf("Servers", "Keys", "Snippets", "Settings")
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
                    }
                )
            }
            composable(Route.Keys.route) { KeyListScreen() }
            composable(Route.Snippets.route) { SnippetListScreen() }
            composable(Route.Settings.route) { SettingsScreen() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerListScreen(
    onServerClick: (Server) -> Unit,
    viewModel: ServersViewModel = hiltViewModel()
) {
    val servers by viewModel.servers.collectAsState()
    val showDialog by viewModel.showAddEditDialog.collectAsState()
    val formState by viewModel.formState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Servers") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showAddDialog() }) {
                Icon(Icons.Default.Add, contentDescription = "Add Server")
            }
        }
    ) { padding ->
        if (servers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No servers. Tap + to add one.")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(servers, key = { it.id }) { server ->
                    var showMenu by remember { mutableStateOf(false) }
                    
                    ListItem(
                        headlineContent = { Text(server.displayName) },
                        supportingContent = { Text("${server.username}@${server.hostname}:${server.port}") },
                        modifier = Modifier.clickable { onServerClick(server) },
                        trailingContent = {
                            Box {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "More",
                                    modifier = Modifier.clickable { showMenu = true }
                                )
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Edit") },
                                        onClick = {
                                            showMenu = false
                                            viewModel.showEditDialog(server)
                                        },
                                        leadingIcon = { Icon(Icons.Default.Edit, null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete") },
                                        onClick = {
                                            showMenu = false
                                            viewModel.deleteServer(server)
                                        },
                                        leadingIcon = { Icon(Icons.Default.Delete, null) }
                                    )
                                }
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
    
    // Add/Edit Dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDialog() },
            title = { Text(if (formState.isEditing) "Edit Server" else "Add Server") },
            text = {
                Column {
                    OutlinedTextField(
                        value = formState.name,
                        onValueChange = { viewModel.updateFormName(it) },
                        label = { Text("Name (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = formState.hostname,
                        onValueChange = { viewModel.updateFormHostname(it) },
                        label = { Text("Hostname") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                    OutlinedTextField(
                        value = formState.port,
                        onValueChange = { viewModel.updateFormPort(it) },
                        label = { Text("Port") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                    OutlinedTextField(
                        value = formState.username,
                        onValueChange = { viewModel.updateFormUsername(it) },
                        label = { Text("Username") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.saveServer() },
                    enabled = formState.hostname.isNotBlank() && formState.username.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDialog() }) {
                    Text("Cancel")
                }
            }
        )
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
            TopAppBar(title = { Text("SSH Keys") })
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Key")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Generate New Key") },
                        onClick = {
                            showMenu = false
                            viewModel.showGenerateDialog()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Import Key") },
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
                Text("No SSH keys. Tap + to add one.")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(keys, key = { it.name }) { key ->
                    ListItem(
                        headlineContent = { Text(key.name) },
                        supportingContent = { Text(key.type) },
                        trailingContent = {
                            androidx.compose.material3.IconButton(
                                onClick = { viewModel.deleteKey(key) }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
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
            title = { Text("Generate RSA Key") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Key Name (e.g. id_rsa)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.generateKey(name) },
                    enabled = name.isNotBlank()
                ) {
                    Text("Generate")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideGenerateDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    if (showImport) {
        var name by remember { mutableStateOf("") }
        var privateKey by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { viewModel.hideImportDialog() },
            title = { Text("Import Private Key") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Key Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = privateKey,
                        onValueChange = { privateKey = it },
                        label = { Text("Private Key Content") },
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
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideImportDialog() }) {
                    Text("Cancel")
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
            TopAppBar(title = { Text("Snippets") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showAddDialog() }) {
                Icon(Icons.Default.Add, contentDescription = "Add Snippet")
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
                Text("No snippets. Tap + to add one.")
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
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
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
            title = { Text("Add Snippet") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = command,
                        onValueChange = { command = it },
                        label = { Text("Command") },
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
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissAddDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val terminalSettings by viewModel.terminalSettings.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                ListItem(
                    headlineContent = { Text("Theme") },
                    supportingContent = { Text(themeMode.label) },
                    modifier = Modifier.clickable {
                        // Cycle through themes
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
                    headlineContent = { Text("Font Size") },
                    supportingContent = { Text("${terminalSettings.fontSize}pt") }
                )
                HorizontalDivider()
            }
            
            item {
                ListItem(
                    headlineContent = { Text("Restore Purchases") },
                    modifier = Modifier.clickable { viewModel.restorePurchases() }
                )
                HorizontalDivider()
            }
            
            item {
                ListItem(
                    headlineContent = { Text("Version") },
                    supportingContent = { Text("1.0.0") }
                )
            }
        }
    }
}
