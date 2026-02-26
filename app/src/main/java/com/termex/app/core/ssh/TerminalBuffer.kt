package com.termex.app.core.ssh

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Terminal buffer with full CSI command support.
 * Handles cursor movement, erase, scroll regions, and mode setting.
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
    private val lines = ArrayList<MutableTerminalLine>(rows + scrollbackSize)
    
    // Cursor
    private var cursorX = 0
    private var cursorY = 0
    
    // Saved cursor
    private var savedCursorX = 0
    private var savedCursorY = 0
    
    // Scroll region (top and bottom, 0-indexed)
    private var scrollTop = 0
    private var scrollBottom = rows - 1
    
    // Origin mode: cursor relative to scroll region
    private var originMode = false
    
    // Scroll offset for viewing history
    private var scrollOffset = 0
    
    // The index of the first visible row in `lines` 
    // (lines before this are scrollback history)
    private var viewportStart: Int = 0
    
    private val _contentFlow = MutableStateFlow<List<TerminalLine>>(emptyList())
    val contentFlow: StateFlow<List<TerminalLine>> = _contentFlow.asStateFlow()
    
    private val _cursorPosition = MutableStateFlow(Pair(0, 0))
    val cursorPosition: StateFlow<Pair<Int, Int>> = _cursorPosition.asStateFlow()
    
    init {
        synchronized(lock) {
            repeat(rows) {
                lines.add(MutableTerminalLine().apply { ensureCapacity(cols) })
            }
            viewportStart = 0
            emitContentLocked()
        }
    }
    
    fun write(data: String) {
        synchronized(lock) {
            val segments = ansiParser.parse(data)

            for (segment in segments) {
                when (segment) {
                    is AnsiParser.ParsedSegment.Text -> writeText(segment)
                    is AnsiParser.ParsedSegment.CsiCommand -> processCsiCommand(segment)
                    is AnsiParser.ParsedSegment.OscCommand -> { /* title changes etc — ignore for now */ }
                }
            }

            _cursorPosition.value = Pair(cursorX, cursorY - viewportStart)
            emitContentLocked()
        }
    }
    
    private fun writeText(segment: AnsiParser.ParsedSegment.Text) {
        for (char in segment.text) {
            when (char) {
                '\n' -> {
                    lineFeedLocked()
                }
                '\r' -> {
                    cursorX = 0
                }
                '\b' -> {
                    if (cursorX > 0) cursorX--
                }
                '\t' -> {
                    val nextTab = ((cursorX / 8) + 1) * 8
                    cursorX = minOf(nextTab, cols - 1)
                }
                '\u0007' -> { /* BEL — ignore */ }
                else -> {
                    if (char.code >= 32) {
                        ensureLineExistsLocked(cursorY)
                        
                        if (cursorX >= cols) {
                            // Auto-wrap
                            cursorX = 0
                            lineFeedLocked()
                        }
                        
                        lines[cursorY].ensureCapacity(cols)
                        lines[cursorY].cells[cursorX] = TerminalCell(char, segment.style)
                        cursorX++
                    }
                }
            }
        }
    }
    
    private fun processCsiCommand(cmd: AnsiParser.ParsedSegment.CsiCommand) {
        val p = cmd.params
        val n = p.getOrElse(0) { 0 }
        
        when (cmd.command) {
            // Cursor movement
            'A' -> { // Cursor Up
                val amount = maxOf(1, n)
                cursorY = maxOf(viewportStart + scrollTop, cursorY - amount)
            }
            'B' -> { // Cursor Down
                val amount = maxOf(1, n)
                cursorY = minOf(viewportStart + scrollBottom, cursorY + amount)
            }
            'C' -> { // Cursor Forward
                val amount = maxOf(1, n)
                cursorX = minOf(cols - 1, cursorX + amount)
            }
            'D' -> { // Cursor Back
                val amount = maxOf(1, n)
                cursorX = maxOf(0, cursorX - amount)
            }
            'E' -> { // Cursor Next Line
                val amount = maxOf(1, n)
                cursorX = 0
                cursorY = minOf(viewportStart + scrollBottom, cursorY + amount)
            }
            'F' -> { // Cursor Previous Line
                val amount = maxOf(1, n)
                cursorX = 0
                cursorY = maxOf(viewportStart + scrollTop, cursorY - amount)
            }
            'G' -> { // Cursor Horizontal Absolute
                cursorX = (maxOf(1, n) - 1).coerceIn(0, cols - 1)
            }
            'H', 'f' -> { // Cursor Position
                val row = maxOf(1, p.getOrElse(0) { 1 })
                val col = maxOf(1, p.getOrElse(1) { 1 })
                cursorY = viewportStart + (row - 1).coerceIn(0, rows - 1)
                cursorX = (col - 1).coerceIn(0, cols - 1)
                ensureLineExistsLocked(cursorY)
            }
            'd' -> { // Vertical Position Absolute
                val row = maxOf(1, n)
                cursorY = viewportStart + (row - 1).coerceIn(0, rows - 1)
                ensureLineExistsLocked(cursorY)
            }
            
            // Erase
            'J' -> { // Erase in Display
                val mode = if (p.isEmpty()) 0 else n
                eraseInDisplay(mode)
            }
            'K' -> { // Erase in Line
                val mode = if (p.isEmpty()) 0 else n
                eraseInLine(mode)
            }
            
            // Line operations
            'L' -> { // Insert Lines
                val amount = maxOf(1, n)
                insertLines(amount)
            }
            'M' -> { // Delete Lines / Reverse Index
                if (cmd.params.isEmpty() && cmd.intermediate.isEmpty()) {
                    // ESC M — reverse index
                    reverseIndexLocked()
                } else {
                    val amount = maxOf(1, n)
                    deleteLines(amount)
                }
            }
            'P' -> { // Delete Characters
                val amount = maxOf(1, n)
                deleteChars(amount)
            }
            'X' -> { // Erase Characters
                val amount = maxOf(1, n)
                eraseChars(amount)
            }
            '@' -> { // Insert Characters
                val amount = maxOf(1, n)
                insertChars(amount)
            }
            
            // Scroll
            'S' -> { // Scroll Up
                val amount = maxOf(1, n)
                scrollUpRegion(amount)
            }
            'T' -> { // Scroll Down
                val amount = maxOf(1, n)
                scrollDownRegion(amount)
            }
            
            // Scroll region
            'r' -> { // Set Scrolling Region (DECSTBM)
                val top = (p.getOrElse(0) { 1 }) - 1
                val bottom = (p.getOrElse(1) { rows }) - 1
                scrollTop = top.coerceIn(0, rows - 1)
                scrollBottom = bottom.coerceIn(scrollTop, rows - 1)
                // Move cursor to home
                cursorX = 0
                cursorY = viewportStart + if (originMode) scrollTop else 0
            }
            
            // Cursor save/restore
            's' -> {
                savedCursorX = cursorX
                savedCursorY = cursorY
            }
            'u' -> {
                cursorX = savedCursorX
                cursorY = savedCursorY
            }
            
            // Mode set/reset
            'h' -> {
                if (cmd.intermediate == "?") {
                    for (mode in p) {
                        when (mode) {
                            6 -> originMode = true    // DECOM
                            25 -> { /* Show cursor — always shown */ }
                            1049 -> { /* Alternate screen buffer — simplified: just clear */ 
                                savedCursorX = cursorX
                                savedCursorY = cursorY
                                clearViewport()
                            }
                        }
                    }
                }
            }
            'l' -> {
                if (cmd.intermediate == "?") {
                    for (mode in p) {
                        when (mode) {
                            6 -> originMode = false
                            25 -> { /* Hide cursor */ }
                            1049 -> { /* Leave alternate buffer — restore */
                                cursorX = savedCursorX
                                cursorY = savedCursorY
                            }
                        }
                    }
                }
            }
            
            // Device status
            'n' -> { /* DSR — ignore, we'd need to send response back */ }
            't' -> { /* Window manipulation — ignore */ }
            
            // Full reset
            'c' -> {
                clearViewport()
                cursorX = 0
                cursorY = viewportStart
                scrollTop = 0
                scrollBottom = rows - 1
                originMode = false
            }
        }
    }
    
    private fun lineFeedLocked() {
        cursorY++
        ensureLineExistsLocked(cursorY)
        
        // Check if cursor went below scroll region
        val absBottom = viewportStart + scrollBottom
        if (cursorY > absBottom) {
            cursorY = absBottom
            scrollUpRegion(1)
        }
    }
    
    private fun reverseIndexLocked() {
        val absTop = viewportStart + scrollTop
        if (cursorY <= absTop) {
            scrollDownRegion(1)
        } else {
            cursorY--
        }
    }
    
    private fun scrollUpRegion(amount: Int) {
        val absTop = viewportStart + scrollTop
        val absBottom = viewportStart + scrollBottom
        
        for (i in 0 until amount) {
            if (absTop < lines.size && absTop <= absBottom) {
                // If scrolling the full screen and top is 0, move to scrollback
                if (scrollTop == 0) {
                    // Keep line as scrollback
                    viewportStart++
                    // Add new line at bottom
                    val insertIdx = minOf(absBottom + 1 + i, lines.size)
                    lines.add(insertIdx, MutableTerminalLine().apply { ensureCapacity(cols) })
                } else {
                    // Partial scroll region: remove top, insert at bottom
                    lines.removeAt(absTop)
                    val insertIdx = minOf(absBottom, lines.size)
                    lines.add(insertIdx, MutableTerminalLine().apply { ensureCapacity(cols) })
                }
            }
        }
        
        // Trim scrollback
        trimScrollback()
    }
    
    private fun scrollDownRegion(amount: Int) {
        val absTop = viewportStart + scrollTop
        val absBottom = viewportStart + scrollBottom
        
        for (i in 0 until amount) {
            if (absBottom < lines.size) {
                lines.removeAt(absBottom)
            }
            lines.add(absTop, MutableTerminalLine().apply { ensureCapacity(cols) })
        }
    }
    
    private fun eraseInDisplay(mode: Int) {
        when (mode) {
            0 -> { // Erase from cursor to end
                eraseInLine(0)
                for (y in (cursorY + 1)..(viewportStart + rows - 1)) {
                    ensureLineExistsLocked(y)
                    clearLine(y)
                }
            }
            1 -> { // Erase from start to cursor
                for (y in viewportStart until cursorY) {
                    ensureLineExistsLocked(y)
                    clearLine(y)
                }
                eraseInLine(1)
            }
            2, 3 -> { // Erase entire display
                clearViewport()
            }
        }
    }
    
    private fun eraseInLine(mode: Int) {
        ensureLineExistsLocked(cursorY)
        val line = lines[cursorY]
        line.ensureCapacity(cols)
        
        when (mode) {
            0 -> { // Erase from cursor to end of line
                for (x in cursorX until cols) {
                    if (x < line.cells.size) line.cells[x] = TerminalCell()
                }
            }
            1 -> { // Erase from start to cursor
                for (x in 0..cursorX) {
                    if (x < line.cells.size) line.cells[x] = TerminalCell()
                }
            }
            2 -> { // Erase entire line
                clearLine(cursorY)
            }
        }
    }
    
    private fun insertLines(amount: Int) {
        val absBottom = viewportStart + scrollBottom
        for (i in 0 until amount) {
            if (cursorY <= absBottom) {
                if (absBottom < lines.size) {
                    lines.removeAt(absBottom)
                }
                lines.add(cursorY, MutableTerminalLine().apply { ensureCapacity(cols) })
            }
        }
    }
    
    private fun deleteLines(amount: Int) {
        val absBottom = viewportStart + scrollBottom
        for (i in 0 until amount) {
            if (cursorY < lines.size && cursorY <= absBottom) {
                lines.removeAt(cursorY)
                val insertIdx = minOf(absBottom, lines.size)
                lines.add(insertIdx, MutableTerminalLine().apply { ensureCapacity(cols) })
            }
        }
    }
    
    private fun deleteChars(amount: Int) {
        ensureLineExistsLocked(cursorY)
        val line = lines[cursorY]
        line.ensureCapacity(cols)
        for (i in 0 until amount) {
            if (cursorX < line.cells.size) {
                line.cells.removeAt(cursorX)
                line.cells.add(TerminalCell())
            }
        }
    }
    
    private fun eraseChars(amount: Int) {
        ensureLineExistsLocked(cursorY)
        val line = lines[cursorY]
        line.ensureCapacity(cols)
        for (x in cursorX until minOf(cursorX + amount, cols)) {
            if (x < line.cells.size) line.cells[x] = TerminalCell()
        }
    }
    
    private fun insertChars(amount: Int) {
        ensureLineExistsLocked(cursorY)
        val line = lines[cursorY]
        line.ensureCapacity(cols)
        for (i in 0 until amount) {
            if (cursorX < line.cells.size) {
                line.cells.add(cursorX, TerminalCell())
                if (line.cells.size > cols) {
                    line.cells.removeAt(line.cells.size - 1)
                }
            }
        }
    }
    
    private fun clearLine(y: Int) {
        if (y < lines.size) {
            val line = lines[y]
            for (x in 0 until line.cells.size) {
                line.cells[x] = TerminalCell()
            }
        }
    }
    
    private fun clearViewport() {
        for (y in viewportStart until viewportStart + rows) {
            ensureLineExistsLocked(y)
            clearLine(y)
        }
        cursorX = 0
        cursorY = viewportStart
    }
    
    private fun trimScrollback() {
        val maxLines = rows + scrollbackSize
        if (lines.size > maxLines) {
            val toRemove = lines.size - maxLines
            // Use subList.clear() for O(1) instead of removeAt(0) loop
            lines.subList(0, toRemove).clear()
            viewportStart = maxOf(0, viewportStart - toRemove)
            cursorY = maxOf(viewportStart, cursorY - toRemove)
        }
    }
    
    private fun ensureLineExistsLocked(y: Int) {
        while (lines.size <= y) {
            lines.add(MutableTerminalLine().apply { ensureCapacity(cols) })
        }
    }
    
    private fun emitContentLocked() {
        val startIndex = maxOf(0, viewportStart + rows - rows - scrollOffset)
        val actualStart = maxOf(viewportStart - scrollOffset, 0)
        val endIndex = minOf(lines.size, actualStart + rows)
        val safeStart = maxOf(0, minOf(actualStart, lines.size))
        
        _contentFlow.value = if (safeStart < endIndex) {
            lines.subList(safeStart, endIndex).map { line ->
                TerminalLine(ArrayList(line.cells))
            }
        } else {
            emptyList()
        }
    }
    
    fun scrollUp(amount: Int = 1) {
        synchronized(lock) {
            val maxOffset = maxOf(0, viewportStart)
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
            viewportStart = 0
            scrollOffset = 0
            scrollTop = 0
            scrollBottom = rows - 1
            originMode = false
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
            scrollBottom = rows - 1
            scrollTop = 0

            lines.forEach { line ->
                if (line.cells.size > cols) {
                    line.cells.subList(cols, line.cells.size).clear()
                } else {
                    line.ensureCapacity(cols)
                }
            }

            trimScrollback()

            cursorX = cursorX.coerceIn(0, cols - 1)
            cursorY = cursorY.coerceIn(viewportStart, maxOf(viewportStart, lines.size - 1))
            val maxOffset = maxOf(0, viewportStart)
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
