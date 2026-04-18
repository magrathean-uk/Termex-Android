package com.termex.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.termex.app.data.prefs.LinkHandlingMode

data class TerminalTranscriptMatch(
    val lineIndex: Int,
    val lineText: String
)

fun stitchTranscript(previous: List<String>, visible: List<String>): List<String> {
    if (previous.isEmpty()) return visible.toList()
    val maxOverlap = minOf(previous.size, visible.size)
    for (overlap in maxOverlap downTo 1) {
        if (previous.takeLast(overlap) == visible.take(overlap)) {
            return previous + visible.drop(overlap)
        }
    }
    return previous + visible
}

fun searchTranscript(lines: List<String>, query: String): List<TerminalTranscriptMatch> {
    val needle = query.trim()
    if (needle.isEmpty()) return emptyList()
    return lines.mapIndexedNotNull { index, line ->
        if (line.contains(needle, ignoreCase = true)) {
            TerminalTranscriptMatch(index, line)
        } else {
            null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalTranscriptToolsSheet(
    transcriptLines: List<String>,
    linkHandlingMode: LinkHandlingMode,
    onJumpToLine: (Int) -> Unit,
    onOpenLink: (String) -> Unit,
    onExportTranscript: () -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val matches = remember(transcriptLines, query) {
        searchTranscript(transcriptLines, query)
    }
    val links = remember(transcriptLines) {
        extractTerminalLinks(transcriptLines.joinToString("\n"))
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Terminal tools",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                singleLine = true,
                label = { Text("Search transcript") }
            )

            ListItem(
                headlineContent = { Text("Export transcript") },
                supportingContent = { Text("Share the current session text.") },
                modifier = Modifier.clickable { onExportTranscript() }
            )
            HorizontalDivider()

            Text(
                text = "Matches",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            if (matches.isEmpty()) {
                Text(
                    text = if (query.isBlank()) "Type to search." else "No matches.",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                ) {
                    items(matches, key = { it.lineIndex }) { match ->
                        ListItem(
                            headlineContent = { Text("Line ${match.lineIndex + 1}") },
                            supportingContent = {
                                Text(
                                    text = match.lineText,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 3
                                )
                            },
                            modifier = Modifier.clickable { onJumpToLine(match.lineIndex) }
                        )
                        HorizontalDivider()
                    }
                }
            }

            Text(
                text = "Links",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            if (links.isEmpty()) {
                Text(
                    text = "No links found.",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else {
                links.forEach { link ->
                    ListItem(
                        headlineContent = { Text(link) },
                        supportingContent = { Text(linkHandlingMode.label) },
                        modifier = Modifier.clickable { onOpenLink(link) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
