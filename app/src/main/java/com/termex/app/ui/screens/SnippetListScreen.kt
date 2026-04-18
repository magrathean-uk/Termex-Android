package com.termex.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.termex.app.R
import com.termex.app.ui.viewmodel.SnippetsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnippetListScreen(
    viewModel: SnippetsViewModel = hiltViewModel()
) {
    val snippets by viewModel.snippets.collectAsState()
    val editorState by viewModel.editorState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.title_snippets)) })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                modifier = Modifier.testTag("snippets.add")
            ) {
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
                        headlineContent = { Text(snippet.name.ifBlank { stringResource(R.string.snippet_untitled) }) },
                        supportingContent = { 
                            Text(
                                text = snippet.command,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        trailingContent = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { viewModel.editSnippet(snippet) },
                                    modifier = Modifier.testTag("snippet.edit.${snippet.id}")
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = stringResource(R.string.action_edit)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.duplicateSnippet(snippet) },
                                    modifier = Modifier.testTag("snippet.duplicate.${snippet.id}")
                                ) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = stringResource(R.string.action_duplicate_snippet)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteSnippet(snippet) },
                                    modifier = Modifier.testTag("snippet.delete.${snippet.id}")
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.action_delete)
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
    
    val editor = editorState
    if (editor != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAddDialog() },
            title = {
                Text(
                    if (editor.id == null) {
                        stringResource(R.string.title_add_snippet)
                    } else {
                        stringResource(R.string.title_edit_snippet)
                    }
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = editor.name,
                        onValueChange = viewModel::updateEditorName,
                        label = { Text(stringResource(R.string.label_snippet_name_optional)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("snippetEditor.name")
                    )
                    OutlinedTextField(
                        value = editor.command,
                        onValueChange = viewModel::updateEditorCommand,
                        label = { Text(stringResource(R.string.label_command)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .testTag("snippetEditor.command"),
                        minLines = 3,
                        maxLines = 6
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.saveEditorSnippet() },
                    enabled = editor.command.isNotBlank(),
                    modifier = Modifier.testTag("snippetEditor.save")
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissAddDialog() },
                    modifier = Modifier.testTag("snippetEditor.cancel")
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
