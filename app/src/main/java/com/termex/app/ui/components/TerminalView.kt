package com.termex.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import com.termex.app.core.ssh.AnsiParser
import com.termex.app.core.ssh.TerminalBuffer
import kotlinx.coroutines.delay

@Composable
fun TerminalView(
    lines: List<TerminalBuffer.TerminalLine>,
    cursorPosition: Pair<Int, Int>,
    modifier: Modifier = Modifier,
    fontSize: Float = 14f,
    onTap: () -> Unit = {}
) {
    val density = LocalDensity.current
    var scale by remember { mutableFloatStateOf(1f) }
    var showCursor by remember { mutableStateOf(true) }
    
    // Cursor blink effect
    LaunchedEffect(cursorPosition) {
        while (true) {
            delay(530)
            showCursor = !showCursor
        }
    }
    
    val effectiveFontSize = fontSize * scale
    val charWidth = effectiveFontSize * 0.6f
    val lineHeight = effectiveFontSize * 1.2f
    
    val paint = remember(effectiveFontSize) {
        android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = with(density) { effectiveFontSize.sp.toPx() }
            typeface = android.graphics.Typeface.MONOSPACE
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 3f)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val scaledCharWidth = with(density) { charWidth.sp.toPx() }
            val scaledLineHeight = with(density) { lineHeight.sp.toPx() }
            
            lines.forEachIndexed { lineIndex, line ->
                var x = 0f
                val y = lineIndex * scaledLineHeight + scaledLineHeight
                
                line.cells.forEachIndexed { cellIndex, cell ->
                    // Draw background if not default
                    if (cell.style.bgColor != AnsiParser.DEFAULT_BG) {
                        drawRect(
                            color = if (cell.style.inverse) cell.style.fgColor else cell.style.bgColor,
                            topLeft = Offset(x, y - scaledLineHeight + 4),
                            size = androidx.compose.ui.geometry.Size(scaledCharWidth, scaledLineHeight)
                        )
                    }
                    
                    // Draw character
                    val textColor = if (cell.style.inverse) cell.style.bgColor else cell.style.fgColor
                    drawContext.canvas.nativeCanvas.drawText(
                        cell.char.toString(),
                        x,
                        y,
                        paint.apply {
                            color = textColor.toArgb()
                            isFakeBoldText = cell.style.bold
                            textSkewX = if (cell.style.italic) -0.25f else 0f
                        }
                    )
                    
                    // Draw underline
                    if (cell.style.underline) {
                        drawLine(
                            color = textColor,
                            start = Offset(x, y + 2),
                            end = Offset(x + scaledCharWidth, y + 2),
                            strokeWidth = 1f
                        )
                    }
                    
                    // Draw cursor
                    if (showCursor && lineIndex == cursorPosition.second && cellIndex == cursorPosition.first) {
                        drawRect(
                            color = Color(0xFFE5E5E5),
                            topLeft = Offset(x, y - scaledLineHeight + 4),
                            size = androidx.compose.ui.geometry.Size(scaledCharWidth, scaledLineHeight)
                        )
                        // Redraw character on cursor with inverted color
                        drawContext.canvas.nativeCanvas.drawText(
                            cell.char.toString(),
                            x,
                            y,
                            paint.apply { color = android.graphics.Color.BLACK }
                        )
                    }
                    
                    x += scaledCharWidth
                }
            }
        }
    }
}

private fun Color.toArgb(): Int {
    return android.graphics.Color.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt()
    )
}
