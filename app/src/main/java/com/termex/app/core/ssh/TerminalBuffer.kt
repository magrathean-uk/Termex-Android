package com.termex.app.core.ssh

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Terminal buffer that manages terminal content with scrollback support.
 * Handles incoming data, cursor positioning, and provides content for rendering.
 */
class TerminalBuffer(
    cols: Int = 80,
    rows: Int = 24,
    private val scrollbackSize: Int = 1000
) {
    private val ansiParser = AnsiParser()
    private val lock = Any()

    private var cols: Int = cols
    private var rows: Int = rows
    
    // Each line is a list of styled characters
    data class TerminalCell(
        val char: Char = ' ',
        val style: AnsiParser.TextStyle = AnsiParser.TextStyle()
    )
    
    data class TerminalLine(
        val cells: List<TerminalCell> = emptyList()
    )

    private data class MutableTerminalLine(
        val cells: MutableList<TerminalCell> = mutableListOf()
    ) {
        fun ensureCapacity(cols: Int) {
            while (cells.size < cols) {
                cells.add(TerminalCell())
            }
        }
    }
    
    // Main buffer (visible area + scrollback)
    private val lines = mutableListOf<MutableTerminalLine>()
    
    // Cursor position
    private var cursorX = 0
    private var cursorY = 0
    
    // Scroll offset for viewing history
    private var scrollOffset = 0
    
    private val _contentFlow = MutableStateFlow<List<TerminalLine>>(emptyList())
    val contentFlow: StateFlow<List<TerminalLine>> = _contentFlow.asStateFlow()
    
    private val _cursorPosition = MutableStateFlow(Pair(0, 0))
    val cursorPosition: StateFlow<Pair<Int, Int>> = _cursorPosition.asStateFlow()
    
    init {
        synchronized(lock) {
            // Initialize with empty lines
            repeat(rows) {
                lines.add(MutableTerminalLine().apply { ensureCapacity(cols) })
            }
            emitContentLocked()
        }
    }
    
    fun write(data: String) {
        synchronized(lock) {
            val styledTexts = ansiParser.parse(data)

            for (styledText in styledTexts) {
                for (char in styledText.text) {
                    when (char) {
                        '\n' -> {
                            newLineLocked()
                        }
                        '\r' -> {
                            cursorX = 0
                        }
                        '\b' -> {
                            if (cursorX > 0) cursorX--
                        }
                        '\t' -> {
                            // Tab to next 8-column boundary
                            val nextTab = ((cursorX / 8) + 1) * 8
                            cursorX = minOf(nextTab, cols - 1)
                        }
                        else -> {
                            if (char.code >= 32) { // Printable character
                                ensureLineExistsLocked(cursorY)
                                lines[cursorY].ensureCapacity(cols)

                                if (cursorX >= cols) {
                                    // Wrap to next line
                                    newLineLocked()
                                }

                                lines[cursorY].cells[cursorX] = TerminalCell(char, styledText.style)
                                cursorX++
                            }
                        }
                    }
                }
            }

            _cursorPosition.value = Pair(cursorX, cursorY)
            emitContentLocked()
        }
    }
    
    private fun newLineLocked() {
        cursorX = 0
        cursorY++
        
        if (cursorY >= lines.size) {
            // Add new line
            lines.add(MutableTerminalLine().apply { ensureCapacity(cols) })
        }
        
        // Scroll if we exceed visible rows
        if (cursorY >= rows + scrollbackSize) {
            // Remove oldest line to prevent unbounded growth
            lines.removeAt(0)
            cursorY--
        }
    }
    
    private fun ensureLineExistsLocked(y: Int) {
        while (lines.size <= y) {
            lines.add(MutableTerminalLine().apply { ensureCapacity(cols) })
        }
    }
    
    private fun emitContentLocked() {
        // Return visible lines (last 'rows' lines adjusted by scroll offset)
        val startIndex = maxOf(0, lines.size - rows - scrollOffset)
        val endIndex = minOf(lines.size, startIndex + rows)
        _contentFlow.value = lines.subList(startIndex, endIndex).map { line ->
            TerminalLine(line.cells.toList())
        }
    }
    
    fun scrollUp(amount: Int = 1) {
        synchronized(lock) {
            val maxOffset = maxOf(0, lines.size - rows)
            scrollOffset = minOf(scrollOffset + amount, maxOffset)
            emitContentLocked()
        }
    }
    
    fun scrollDown(amount: Int = 1) {
        synchronized(lock) {
            scrollOffset = maxOf(0, scrollOffset - amount)
            emitContentLocked()
        }
    }
    
    fun scrollToBottom() {
        synchronized(lock) {
            scrollOffset = 0
            emitContentLocked()
        }
    }
    
    fun clear() {
        synchronized(lock) {
            lines.clear()
            repeat(rows) {
                lines.add(MutableTerminalLine().apply { ensureCapacity(cols) })
            }
            cursorX = 0
            cursorY = 0
            scrollOffset = 0
            ansiParser.reset()
            _cursorPosition.value = Pair(0, 0)
            emitContentLocked()
        }
    }
    
    fun resize(newCols: Int, newRows: Int) {
        if (newCols <= 0 || newRows <= 0) return
        synchronized(lock) {
            cols = newCols
            rows = newRows

            lines.forEach { line ->
                if (line.cells.size > cols) {
                    line.cells.subList(cols, line.cells.size).clear()
                } else {
                    line.ensureCapacity(cols)
                }
            }

            val maxLines = rows + scrollbackSize
            if (lines.size > maxLines) {
                val toRemove = lines.size - maxLines
                repeat(toRemove) {
                    lines.removeAt(0)
                }
                cursorY = maxOf(0, cursorY - toRemove)
            }

            cursorX = cursorX.coerceIn(0, cols - 1)
            cursorY = cursorY.coerceIn(0, maxOf(0, lines.size - 1))
            val maxOffset = maxOf(0, lines.size - rows)
            scrollOffset = scrollOffset.coerceIn(0, maxOffset)
            emitContentLocked()
        }
    }
    
    fun getVisibleContent(): List<String> {
        return _contentFlow.value.map { line ->
            line.cells.map { it.char }.joinToString("")
        }
    }
}
