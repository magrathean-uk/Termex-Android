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
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.termex.app.data.prefs.TerminalExtraKey
import com.termex.app.ui.theme.TerminalColorScheme

@Composable
fun TerminalExtraKeyBar(
    keys: List<TerminalExtraKey>,
    theme: TerminalColorScheme,
    ctrlActive: Boolean,
    altActive: Boolean,
    onCtrlToggle: () -> Unit,
    onAltToggle: () -> Unit,
    onKeyPress: (String) -> Unit,
    onHideKeyboard: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onHideKeyboard != null) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(onClick = onHideKeyboard)
                        .padding(start = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
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
                    when (key) {
                        TerminalExtraKey.CTRL -> ModifierKeyButton(
                            label = key.label,
                            active = ctrlActive,
                            theme = theme,
                            onClick = onCtrlToggle
                        )

                        TerminalExtraKey.ALT -> ModifierKeyButton(
                            label = key.label,
                            active = altActive,
                            theme = theme,
                            onClick = onAltToggle
                        )

                        else -> {
                            val icon = when (key) {
                                TerminalExtraKey.UP -> Icons.Filled.KeyboardArrowUp
                                TerminalExtraKey.DOWN -> Icons.Filled.KeyboardArrowDown
                                TerminalExtraKey.LEFT -> Icons.AutoMirrored.Filled.KeyboardArrowLeft
                                TerminalExtraKey.RIGHT -> Icons.AutoMirrored.Filled.KeyboardArrowRight
                                else -> null
                            }
                            if (icon != null) {
                                IconKeyButton(
                                    icon = icon,
                                    contentDescription = key.label,
                                    theme = theme,
                                    onClick = { onKeyPress(applyModifiers(key, ctrlActive, altActive)) }
                                )
                            } else {
                                TextKeyButton(
                                    label = key.label,
                                    theme = theme,
                                    onClick = { onKeyPress(applyModifiers(key, ctrlActive, altActive)) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun applyModifiers(
    key: TerminalExtraKey,
    ctrlActive: Boolean,
    altActive: Boolean
): String {
    var sequence = key.sequence
    if (ctrlActive && key.label.length == 1) {
        val char = key.label[0].uppercaseChar()
        if (char in 'A'..'Z') {
            sequence = (char.code - 64).toChar().toString()
        }
    }
    if (altActive && sequence.isNotEmpty()) {
        sequence = "\u001B$sequence"
    }
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
    return sequence
}

@Composable
private fun TextKeyButton(
    label: String,
    theme: TerminalColorScheme,
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    theme: TerminalColorScheme,
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
    theme: TerminalColorScheme,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        )
    }
}
