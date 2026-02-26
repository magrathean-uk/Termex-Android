package com.termex.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.onSizeChanged
import com.termex.app.core.ssh.AnsiParser
import com.termex.app.core.ssh.TerminalBuffer
import kotlinx.coroutines.delay

@Composable
fun TerminalView(
    lines: List<TerminalBuffer.TerminalLine>,
    cursorPosition: Pair<Int, Int>,
    modifier: Modifier = Modifier,
    fontSize: Float = 14f,
    backgroundColor: Color = Color(0xFF000000),
    foregroundColor: Color = Color(0xFFE5E5E5),
    cursorColor: Color = Color(0xFFE5E5E5),
    onTap: () -> Unit = {},
    onScroll: (deltaLines: Int) -> Unit = {},
    onSizeChanged: (cols: Int, rows: Int, widthPx: Int, heightPx: Int) -> Unit = { _, _, _, _ -> }
) {
    val density = LocalDensity.current
    var scale by remember { mutableFloatStateOf(1f) }
    var showCursor by remember { mutableStateOf(true) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    
    // Cursor blink effect
    LaunchedEffect(Unit) {
        while (true) {
            delay(530)
            showCursor = !showCursor
        }
    }
    
    val effectiveFontSize = fontSize * scale
    val charWidthPx = remember(effectiveFontSize) {
        with(density) { (effectiveFontSize * 0.6f).sp.toPx() }
    }
    val lineHeightPx = remember(effectiveFontSize) {
        with(density) { (effectiveFontSize * 1.2f).sp.toPx() }
    }
    
    // Cache paint objects per style to avoid mutation during draw
    val basePaint = remember(effectiveFontSize) {
        android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = with(density) { effectiveFontSize.sp.toPx() }
            typeface = android.graphics.Typeface.MONOSPACE
        }
    }
    val normalPaint = remember(effectiveFontSize) {
        android.graphics.Paint(basePaint).apply { isFakeBoldText = false; textSkewX = 0f }
    }
    val boldPaint = remember(effectiveFontSize) {
        android.graphics.Paint(basePaint).apply { isFakeBoldText = true; textSkewX = 0f }
    }
    val italicPaint = remember(effectiveFontSize) {
        android.graphics.Paint(basePaint).apply { isFakeBoldText = false; textSkewX = -0.25f }
    }
    val boldItalicPaint = remember(effectiveFontSize) {
        android.graphics.Paint(basePaint).apply { isFakeBoldText = true; textSkewX = -0.25f }
    }

    LaunchedEffect(viewSize, effectiveFontSize) {
        if (viewSize.width > 0 && viewSize.height > 0) {
            val cols = (viewSize.width / charWidthPx).toInt().coerceAtLeast(1)
            val rows = (viewSize.height / lineHeightPx).toInt().coerceAtLeast(1)
            onSizeChanged(cols, rows, viewSize.width, viewSize.height)
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .onSizeChanged { size -> viewSize = size }
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
            .pointerInput(lineHeightPx) {
                var accumulatedDrag = 0f
                detectVerticalDragGestures { _, dragAmount ->
                    accumulatedDrag += dragAmount
                    val linesDragged = (accumulatedDrag / lineHeightPx).toInt()
                    if (linesDragged != 0) {
                        onScroll(-linesDragged) // negative = scroll up (drag down), positive = scroll down
                        accumulatedDrag -= linesDragged * lineHeightPx
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val scaledCharWidth = charWidthPx
            val scaledLineHeight = lineHeightPx
            val descent = scaledLineHeight * 0.2f // Proportional descent offset
            
            // Viewport culling: only draw visible lines
            val visibleRows = (size.height / scaledLineHeight).toInt() + 1
            val startLine = 0
            val endLine = minOf(lines.size, visibleRows)
            
            for (lineIndex in startLine until endLine) {
                val line = lines[lineIndex]
                var x = 0f
                val y = lineIndex * scaledLineHeight + scaledLineHeight
                
                // Skip lines entirely below viewport
                if (y - scaledLineHeight > size.height) break
                
                line.cells.forEachIndexed { cellIndex, cell ->
                    // Skip cells beyond viewport width
                    if (x > size.width) return@forEachIndexed
                    
                    // Draw background if not default
                    if (cell.style.bgColor != AnsiParser.DEFAULT_BG) {
                        drawRect(
                            color = if (cell.style.inverse) cell.style.fgColor else cell.style.bgColor,
                            topLeft = Offset(x, y - scaledLineHeight + descent),
                            size = androidx.compose.ui.geometry.Size(scaledCharWidth, scaledLineHeight)
                        )
                    }
                    
                    // Select cached paint by style
                    val paint = when {
                        cell.style.bold && cell.style.italic -> boldItalicPaint
                        cell.style.bold -> boldPaint
                        cell.style.italic -> italicPaint
                        else -> normalPaint
                    }
                    
                    // Draw character
                    val textColor = if (cell.style.inverse) cell.style.bgColor else cell.style.fgColor
                    paint.color = textColor.toArgb()
                    drawContext.canvas.nativeCanvas.drawText(
                        cell.char.toString(),
                        x,
                        y,
                        paint
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
                            color = cursorColor,
                            topLeft = Offset(x, y - scaledLineHeight + descent),
                            size = androidx.compose.ui.geometry.Size(scaledCharWidth, scaledLineHeight)
                        )
                        // Redraw character on cursor with inverted color
                        paint.color = android.graphics.Color.BLACK
                        drawContext.canvas.nativeCanvas.drawText(
                            cell.char.toString(),
                            x,
                            y,
                            paint
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
