package com.termex.app.core.ssh

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Terminal buffer that manages terminal content with scrollback support.
 * Handles incoming data, cursor positioning, and provides content for rendering.
 */
class TerminalBuffer(
    private val cols: Int = 80,
    private val rows: Int = 24,
    private val scrollbackSize: Int = 1000
) {
    private val ansiParser = AnsiParser()
    
    // Each line is a list of styled characters
    data class TerminalCell(
        val char: Char = ' ',
        val style: AnsiParser.TextStyle = AnsiParser.TextStyle()
    )
    
    data class TerminalLine(
        val cells: MutableList<TerminalCell> = mutableListOf()
    ) {
        fun ensureCapacity(cols: Int) {
            while (cells.size < cols) {
                cells.add(TerminalCell())
            }
        }
    }
    
    // Main buffer (visible area + scrollback)
    private val lines = mutableListOf<TerminalLine>()
    
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
        // Initialize with empty lines
        repeat(rows) {
            lines.add(TerminalLine().apply { ensureCapacity(cols) })
        }
        emitContent()
    }
    
    fun write(data: String) {
        val styledTexts = ansiParser.parse(data)
        
        for (styledText in styledTexts) {
            for (char in styledText.text) {
                when (char) {
                    '\n' -> {
                        newLine()
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
                            ensureLineExists(cursorY)
                            lines[cursorY].ensureCapacity(cols)
                            
                            if (cursorX >= cols) {
                                // Wrap to next line
                                newLine()
                            }
                            
                            lines[cursorY].cells[cursorX] = TerminalCell(char, styledText.style)
                            cursorX++
                        }
                    }
                }
            }
        }
        
        _cursorPosition.value = Pair(cursorX, cursorY)
        emitContent()
    }
    
    private fun newLine() {
        cursorX = 0
        cursorY++
        
        if (cursorY >= lines.size) {
            // Add new line
            lines.add(TerminalLine().apply { ensureCapacity(cols) })
        }
        
        // Scroll if we exceed visible rows
        if (cursorY >= rows + scrollbackSize) {
            // Remove oldest line to prevent unbounded growth
            lines.removeAt(0)
            cursorY--
        }
    }
    
    private fun ensureLineExists(y: Int) {
        while (lines.size <= y) {
            lines.add(TerminalLine().apply { ensureCapacity(cols) })
        }
    }
    
    private fun emitContent() {
        // Return visible lines (last 'rows' lines adjusted by scroll offset)
        val startIndex = maxOf(0, lines.size - rows - scrollOffset)
        val endIndex = minOf(lines.size, startIndex + rows)
        _contentFlow.value = lines.subList(startIndex, endIndex).toList()
    }
    
    fun scrollUp(amount: Int = 1) {
        val maxOffset = maxOf(0, lines.size - rows)
        scrollOffset = minOf(scrollOffset + amount, maxOffset)
        emitContent()
    }
    
    fun scrollDown(amount: Int = 1) {
        scrollOffset = maxOf(0, scrollOffset - amount)
        emitContent()
    }
    
    fun scrollToBottom() {
        scrollOffset = 0
        emitContent()
    }
    
    fun clear() {
        lines.clear()
        repeat(rows) {
            lines.add(TerminalLine().apply { ensureCapacity(cols) })
        }
        cursorX = 0
        cursorY = 0
        scrollOffset = 0
        ansiParser.reset()
        _cursorPosition.value = Pair(0, 0)
        emitContent()
    }
    
    fun resize(newCols: Int, newRows: Int): TerminalBuffer {
        // Create new buffer with new dimensions
        return TerminalBuffer(newCols, newRows, scrollbackSize).also { newBuffer ->
            // Copy existing content
            for (line in lines) {
                val lineContent = StringBuilder()
                for (cell in line.cells) {
                    lineContent.append(cell.char)
                }
                newBuffer.write(lineContent.toString().trimEnd())
                newBuffer.write("\n")
            }
        }
    }
    
    fun getVisibleContent(): List<String> {
        return _contentFlow.value.map { line ->
            line.cells.map { it.char }.joinToString("")
        }
    }
}
