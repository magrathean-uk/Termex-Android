package com.termex.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.termex.app.data.prefs.TerminalExtraKey
import com.termex.app.data.prefs.TerminalExtraKeyPreset
import com.termex.app.ui.AutomationTags
import com.termex.app.ui.viewmodel.SettingsViewModel
import kotlin.math.roundToInt

private val extraKeyRowHeight = 56.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExtraKeysSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val activeKeys by viewModel.terminalExtraKeys.collectAsState()
    val allKeys = remember { TerminalExtraKey.entries }
    val initialEnabledIds = remember(activeKeys) { activeKeys.map(TerminalExtraKey::raw).toSet() }
    val initialOrder = remember(activeKeys) {
        buildOrderedKeys(activeKeys, allKeys)
    }

    val enabledIds = remember { mutableStateListOf<String>() }
    val orderedKeys = remember { mutableStateListOf<TerminalExtraKey>() }
    var currentDragIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val rowHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) { extraKeyRowHeight.toPx() }

    LaunchedEffect(initialEnabledIds, initialOrder) {
        enabledIds.clear()
        enabledIds.addAll(initialEnabledIds)
        orderedKeys.clear()
        orderedKeys.addAll(initialOrder)
    }

    fun save() {
        viewModel.setTerminalExtraKeyIds(
            orderedKeys.filter { enabledIds.contains(it.raw) }.map(TerminalExtraKey::raw)
        )
    }

    fun applyPreset(preset: TerminalExtraKeyPreset) {
        enabledIds.clear()
        enabledIds.addAll(preset.keyIds)
        orderedKeys.clear()
        orderedKeys.addAll(buildOrderedKeys(preset.keys, allKeys))
        save()
    }

    fun reset() {
        enabledIds.clear()
        enabledIds.addAll(TerminalExtraKey.defaultKeys.map(TerminalExtraKey::raw))
        orderedKeys.clear()
        orderedKeys.addAll(buildOrderedKeys(TerminalExtraKey.defaultKeys, allKeys))
        save()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Extra Keys") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(AutomationTags.EXTRA_KEYS_SCREEN),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Accessory Bar Preview", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Keep your shell keys close. The terminal bar uses your saved order.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TerminalExtraKeyPreview(
                            keys = orderedKeys.filter { enabledIds.contains(it.raw) },
                            modifier = Modifier.testTag(AutomationTags.EXTRA_KEYS_PREVIEW)
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Presets", style = MaterialTheme.typography.titleMedium)
                        TerminalExtraKeyPreset.entries.forEach { preset ->
                            Button(
                                onClick = { applyPreset(preset) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(AutomationTags.extraKeyPresetTag(preset.raw))
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(preset.label)
                                    Text(
                                        preset.detail,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = "Keys",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        Text(
                            text = "Long press the grip to drag. Enabled keys show in the terminal bar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        orderedKeys.forEachIndexed { index, key ->
                            val isDragging = currentDragIndex == index
                            val offset = if (isDragging) dragOffsetY.roundToInt() else 0
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .zIndex(if (isDragging) 2f else 0f)
                                    .shadow(if (isDragging) 8.dp else 0.dp)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .testTag(AutomationTags.extraKeyRowTag(key.raw))
                                    .pointerInput(orderedKeys.map(TerminalExtraKey::raw), currentDragIndex) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                currentDragIndex = index
                                                dragOffsetY = 0f
                                            },
                                            onDragCancel = {
                                                currentDragIndex = -1
                                                dragOffsetY = 0f
                                            },
                                            onDragEnd = {
                                                currentDragIndex = -1
                                                dragOffsetY = 0f
                                                save()
                                            }
                                        ) { change, dragAmount ->
                                            change.consume()
                                            if (currentDragIndex != index) return@detectDragGesturesAfterLongPress
                                            dragOffsetY += dragAmount.y
                                            val direction = when {
                                                dragOffsetY > rowHeightPx && currentDragIndex < orderedKeys.lastIndex -> 1
                                                dragOffsetY < -rowHeightPx && currentDragIndex > 0 -> -1
                                                else -> 0
                                            }
                                            if (direction != 0) {
                                                val from = currentDragIndex
                                                val to = currentDragIndex + direction
                                                orderedKeys.add(to, orderedKeys.removeAt(from))
                                                currentDragIndex = to
                                                dragOffsetY -= rowHeightPx * direction
                                            }
                                        }
                                    }
                                    .padding(horizontal = 16.dp)
                                    .animateItem()
                                    .then(Modifier),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(vertical = 12.dp)
                                        .offset { IntOffset(0, offset) },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DragHandle,
                                        contentDescription = "Drag",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(end = 12.dp)
                                    )
                                    Text(
                                        text = key.label,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Switch(
                                        checked = enabledIds.contains(key.raw),
                                        onCheckedChange = { enabled ->
                                            if (enabled) {
                                                if (!enabledIds.contains(key.raw)) enabledIds.add(key.raw)
                                            } else {
                                                enabledIds.remove(key.raw)
                                            }
                                            save()
                                        },
                                        modifier = Modifier.testTag(AutomationTags.extraKeyToggleTag(key.raw))
                                    )
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                orderedKeys.add(index - 1, orderedKeys.removeAt(index))
                                                save()
                                            }
                                        },
                                        enabled = index > 0,
                                        modifier = Modifier.testTag(AutomationTags.extraKeyMoveUpTag(key.raw))
                                    ) {
                                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move up")
                                    }
                                    IconButton(
                                        onClick = {
                                            if (index < orderedKeys.lastIndex) {
                                                orderedKeys.add(index + 1, orderedKeys.removeAt(index))
                                                save()
                                            }
                                        },
                                        enabled = index < orderedKeys.lastIndex,
                                        modifier = Modifier.testTag(AutomationTags.extraKeyMoveDownTag(key.raw))
                                    ) {
                                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move down")
                                    }
                                }
                            }
                            if (index < orderedKeys.lastIndex) {
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }

            item {
                TextButton(
                    onClick = ::reset,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .testTag(AutomationTags.EXTRA_KEYS_RESET)
                ) {
                    Text("Reset to Defaults")
                }
            }
        }
    }
}

@Composable
private fun TerminalExtraKeyPreview(
    keys: List<TerminalExtraKey>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        keys.ifEmpty { TerminalExtraKey.defaultKeys }.forEach { key ->
            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = key.label, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

private fun buildOrderedKeys(
    enabledKeys: List<TerminalExtraKey>,
    allKeys: List<TerminalExtraKey>
): List<TerminalExtraKey> {
    val enabledIds = enabledKeys.map(TerminalExtraKey::raw).toSet()
    return enabledKeys + allKeys.filterNot { enabledIds.contains(it.raw) }
}
