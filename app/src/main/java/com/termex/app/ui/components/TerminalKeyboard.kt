package com.termex.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class TerminalKey(
    val label: String,
    val sequence: String,
    val icon: ImageVector? = null,
    val isModifier: Boolean = false
)

@Composable
fun TerminalKeyboard(
    modifier: Modifier = Modifier,
    ctrlActive: Boolean = false,
    altActive: Boolean = false,
    onCtrlToggle: () -> Unit = {},
    onAltToggle: () -> Unit = {},
    onKeyPress: (String) -> Unit = {},
    onHideKeyboard: (() -> Unit)? = null
) {
    val scrollState = rememberScrollState()
    
    val keys = listOf(
        TerminalKey("ESC", "\u001B"),
        TerminalKey("CTRL", "", isModifier = true),
        TerminalKey("ALT", "", isModifier = true),
        TerminalKey("TAB", "\t"),
        TerminalKey("↑", "\u001B[A", icon = Icons.Default.KeyboardArrowUp),
        TerminalKey("↓", "\u001B[B", icon = Icons.Default.KeyboardArrowDown),
        TerminalKey("←", "\u001B[D", icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft),
        TerminalKey("→", "\u001B[C", icon = Icons.AutoMirrored.Filled.KeyboardArrowRight),
        TerminalKey("HOME", "\u001B[H"),
        TerminalKey("END", "\u001B[F"),
        TerminalKey("PGUP", "\u001B[5~"),
        TerminalKey("PGDN", "\u001B[6~"),
        TerminalKey("INS", "\u001B[2~"),
        TerminalKey("DEL", "\u001B[3~"),
        TerminalKey("F1", "\u001BOP"),
        TerminalKey("F2", "\u001BOQ"),
        TerminalKey("F3", "\u001BOR"),
        TerminalKey("F4", "\u001BOS"),
        TerminalKey("F5", "\u001B[15~"),
        TerminalKey("F6", "\u001B[17~"),
        TerminalKey("F7", "\u001B[18~"),
        TerminalKey("F8", "\u001B[19~"),
        TerminalKey("F9", "\u001B[20~"),
        TerminalKey("F10", "\u001B[21~"),
        TerminalKey("F11", "\u001B[23~"),
        TerminalKey("F12", "\u001B[24~"),
        TerminalKey("-", "-"),
        TerminalKey("/", "/"),
        TerminalKey("|", "|"),
        TerminalKey("~", "~"),
    )
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Keyboard hide button — fixed on the left, outside scroll
            if (onHideKeyboard != null) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(onClick = onHideKeyboard)
                        .padding(start = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Hide keyboard",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
            keys.forEach { key ->
                when {
                    key.label == "CTRL" -> {
                        ModifierKeyButton(
                            label = key.label,
                            active = ctrlActive,
                            onClick = onCtrlToggle
                        )
                    }
                    key.label == "ALT" -> {
                        ModifierKeyButton(
                            label = key.label,
                            active = altActive,
                            onClick = onAltToggle
                        )
                    }
                    key.icon != null -> {
                        IconKeyButton(
                            icon = key.icon,
                            contentDescription = key.label,
                            onClick = {
                                var sequence = key.sequence
                                // For arrow keys with modifiers, use CSI modifier syntax
                                // ESC[1;5A = Ctrl+Up, ESC[1;3A = Alt+Up, ESC[1;7A = Ctrl+Alt+Up
                                if ((ctrlActive || altActive) && sequence.startsWith("\u001B[") && sequence.length == 3) {
                                    val modifier = when {
                                        ctrlActive && altActive -> 7
                                        ctrlActive -> 5
                                        altActive -> 3
                                        else -> 1
                                    }
                                    val cmd = sequence.last()
                                    sequence = "\u001B[1;${modifier}${cmd}"
                                }
                                onKeyPress(sequence)
                            }
                        )
                    }
                    else -> {
                        TextKeyButton(
                            label = key.label,
                            onClick = {
                                var sequence = key.sequence
                                if (ctrlActive && key.label.length == 1) {
                                    val char = key.label[0].uppercaseChar()
                                    if (char in 'A'..'Z') {
                                        sequence = (char.code - 64).toChar().toString()
                                    }
                                }
                                if (altActive) sequence = "\u001B" + sequence
                                onKeyPress(sequence)
                            }
                        )
                    }
                }
            }
            } // end scrollable keys Row
        } // end outer Row
    }
}

@Composable
private fun TextKeyButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun IconKeyButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ModifierKeyButton(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = if (active) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface
        )
    }
}
