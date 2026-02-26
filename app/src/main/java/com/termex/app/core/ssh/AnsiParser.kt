package com.termex.app.core.ssh

import androidx.compose.ui.graphics.Color

/**
 * ANSI escape sequence parser for terminal emulation.
 * Handles SGR (styling), CSI (cursor/erase/scroll), and OSC sequences.
 */
class AnsiParser {
    
    companion object {
        private const val ESC = '\u001B'
        
        // Standard 16 colors (indices 0-15)
        private val STANDARD_COLORS = listOf(
            Color(0xFF000000), // 0: Black
            Color(0xFFCD0000), // 1: Red
            Color(0xFF00CD00), // 2: Green
            Color(0xFFCDCD00), // 3: Yellow
            Color(0xFF0000EE), // 4: Blue
            Color(0xFFCD00CD), // 5: Magenta
            Color(0xFF00CDCD), // 6: Cyan
            Color(0xFFE5E5E5), // 7: White
            Color(0xFF7F7F7F), // 8: Bright Black (Gray)
            Color(0xFFFF0000), // 9: Bright Red
            Color(0xFF00FF00), // 10: Bright Green
            Color(0xFFFFFF00), // 11: Bright Yellow
            Color(0xFF5C5CFF), // 12: Bright Blue
            Color(0xFFFF00FF), // 13: Bright Magenta
            Color(0xFF00FFFF), // 14: Bright Cyan
            Color(0xFFFFFFFF), // 15: Bright White
        )
        
        val DEFAULT_FG = Color(0xFFE5E5E5)
        val DEFAULT_BG = Color(0xFF000000)
    }
    
    data class TextStyle(
        val fgColor: Color = DEFAULT_FG,
        val bgColor: Color = DEFAULT_BG,
        val bold: Boolean = false,
        val italic: Boolean = false,
        val underline: Boolean = false,
        val inverse: Boolean = false
    )
    
    /** Represents parsed output: either printable text or a terminal command. */
    sealed class ParsedSegment {
        data class Text(val text: String, val style: TextStyle) : ParsedSegment()
        data class CsiCommand(val command: Char, val params: List<Int>, val intermediate: String = "") : ParsedSegment()
        data class OscCommand(val params: String) : ParsedSegment()
    }
    
    private var currentStyle = TextStyle()
    private val textBuffer = StringBuilder()
    
    private enum class State { NORMAL, ESC, CSI, OSC, OSC_ESC }
    private var state = State.NORMAL
    private val escapeBuffer = StringBuilder()
    
    /**
     * Parse input and return a list of segments (text + commands).
     * The buffer processes CSI commands and returns them for the TerminalBuffer to handle.
     */
    fun parse(input: String): List<ParsedSegment> {
        val result = mutableListOf<ParsedSegment>()
        
        for (char in input) {
            when (state) {
                State.NORMAL -> {
                    when (char) {
                        ESC -> {
                            flushText(result)
                            state = State.ESC
                            escapeBuffer.clear()
                        }
                        else -> textBuffer.append(char)
                    }
                }
                State.ESC -> {
                    when (char) {
                        '[' -> {
                            state = State.CSI
                            escapeBuffer.clear()
                        }
                        ']' -> {
                            state = State.OSC
                            escapeBuffer.clear()
                        }
                        '(' , ')' -> {
                            // Character set designation — consume next char
                            state = State.NORMAL
                        }
                        '7' -> { /* Save cursor — emit as command */
                            result.add(ParsedSegment.CsiCommand('s', emptyList()))
                            state = State.NORMAL
                        }
                        '8' -> { /* Restore cursor */
                            result.add(ParsedSegment.CsiCommand('u', emptyList()))
                            state = State.NORMAL
                        }
                        'M' -> { /* Reverse index — scroll down */
                            result.add(ParsedSegment.CsiCommand('M', emptyList()))
                            state = State.NORMAL
                        }
                        'D' -> { /* Index — scroll up */
                            result.add(ParsedSegment.CsiCommand('S', listOf(1)))
                            state = State.NORMAL
                        }
                        'E' -> { /* Next line */
                            result.add(ParsedSegment.CsiCommand('E', emptyList()))
                            state = State.NORMAL
                        }
                        'c' -> { /* Full reset */
                            currentStyle = TextStyle()
                            result.add(ParsedSegment.CsiCommand('c', emptyList()))
                            state = State.NORMAL
                        }
                        else -> {
                            // Unknown escape — discard
                            state = State.NORMAL
                        }
                    }
                }
                State.CSI -> {
                    escapeBuffer.append(char)
                    // CSI sequence ends with a letter (0x40-0x7E)
                    if (char in '@'..'~') {
                        processCsiSequence(escapeBuffer.toString(), result)
                        state = State.NORMAL
                        escapeBuffer.clear()
                    }
                }
                State.OSC -> {
                    when {
                        char == '\u0007' -> { // BEL terminates OSC
                            result.add(ParsedSegment.OscCommand(escapeBuffer.toString()))
                            state = State.NORMAL
                            escapeBuffer.clear()
                        }
                        char == ESC -> state = State.OSC_ESC
                        else -> escapeBuffer.append(char)
                    }
                }
                State.OSC_ESC -> {
                    if (char == '\\') { // ST (String Terminator) = ESC \
                        result.add(ParsedSegment.OscCommand(escapeBuffer.toString()))
                    }
                    // Either way, done with OSC
                    state = State.NORMAL
                    escapeBuffer.clear()
                }
            }
        }
        
        flushText(result)
        return result
    }
    
    private fun flushText(result: MutableList<ParsedSegment>) {
        if (textBuffer.isNotEmpty()) {
            result.add(ParsedSegment.Text(textBuffer.toString(), currentStyle))
            textBuffer.clear()
        }
    }
    
    private fun processCsiSequence(sequence: String, result: MutableList<ParsedSegment>) {
        val command = sequence.last()
        val paramStr = sequence.dropLast(1)
        
        // Separate intermediate bytes (like '?' or '>')
        val intermediate = paramStr.takeWhile { it == '?' || it == '>' || it == '!' || it == '=' }
        val numericStr = paramStr.drop(intermediate.length)
        
        val params = if (numericStr.isEmpty()) {
            emptyList()
        } else {
            numericStr.split(";").map { it.toIntOrNull() ?: 0 }
        }
        
        when (command) {
            'm' -> processSgr(params)
            // All other CSI commands are emitted for TerminalBuffer to handle
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'f',
            'J', 'K', 'L', 'M', 'P', 'X', '@',
            'S', 'T',
            'd', 'e', 'r',
            's', 'u',
            'h', 'l',
            'n', 't' -> {
                result.add(ParsedSegment.CsiCommand(command, params, intermediate))
            }
            // Ignore unknown commands
        }
    }
    
    private fun processSgr(codes: List<Int>) {
        if (codes.isEmpty()) {
            currentStyle = TextStyle()
            return
        }
        
        var i = 0
        while (i < codes.size) {
            when (val code = codes[i]) {
                0 -> currentStyle = TextStyle()
                1 -> currentStyle = currentStyle.copy(bold = true)
                2 -> currentStyle = currentStyle.copy(bold = false) // Dim → treat as not bold
                3 -> currentStyle = currentStyle.copy(italic = true)
                4 -> currentStyle = currentStyle.copy(underline = true)
                7 -> currentStyle = currentStyle.copy(inverse = true)
                22 -> currentStyle = currentStyle.copy(bold = false)
                23 -> currentStyle = currentStyle.copy(italic = false)
                24 -> currentStyle = currentStyle.copy(underline = false)
                27 -> currentStyle = currentStyle.copy(inverse = false)
                
                in 30..37 -> currentStyle = currentStyle.copy(fgColor = STANDARD_COLORS[code - 30])
                39 -> currentStyle = currentStyle.copy(fgColor = DEFAULT_FG)
                in 40..47 -> currentStyle = currentStyle.copy(bgColor = STANDARD_COLORS[code - 40])
                49 -> currentStyle = currentStyle.copy(bgColor = DEFAULT_BG)
                in 90..97 -> currentStyle = currentStyle.copy(fgColor = STANDARD_COLORS[code - 90 + 8])
                in 100..107 -> currentStyle = currentStyle.copy(bgColor = STANDARD_COLORS[code - 100 + 8])
                
                // 256-color: 38;5;n (fg) or 48;5;n (bg)
                // 24-bit:    38;2;r;g;b (fg) or 48;2;r;g;b (bg)
                38 -> {
                    if (i + 1 < codes.size) {
                        when (codes[i + 1]) {
                            5 -> {
                                if (i + 2 < codes.size) {
                                    currentStyle = currentStyle.copy(fgColor = get256Color(codes[i + 2]))
                                    i += 2
                                }
                            }
                            2 -> {
                                if (i + 4 < codes.size) {
                                    val r = codes[i + 2].coerceIn(0, 255)
                                    val g = codes[i + 3].coerceIn(0, 255)
                                    val b = codes[i + 4].coerceIn(0, 255)
                                    currentStyle = currentStyle.copy(
                                        fgColor = Color(0xFF000000 or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong())
                                    )
                                    i += 4
                                }
                            }
                        }
                    }
                }
                48 -> {
                    if (i + 1 < codes.size) {
                        when (codes[i + 1]) {
                            5 -> {
                                if (i + 2 < codes.size) {
                                    currentStyle = currentStyle.copy(bgColor = get256Color(codes[i + 2]))
                                    i += 2
                                }
                            }
                            2 -> {
                                if (i + 4 < codes.size) {
                                    val r = codes[i + 2].coerceIn(0, 255)
                                    val g = codes[i + 3].coerceIn(0, 255)
                                    val b = codes[i + 4].coerceIn(0, 255)
                                    currentStyle = currentStyle.copy(
                                        bgColor = Color(0xFF000000 or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong())
                                    )
                                    i += 4
                                }
                            }
                        }
                    }
                }
            }
            i++
        }
    }
    
    private fun get256Color(index: Int): Color {
        return when {
            index < 16 -> STANDARD_COLORS[index]
            index < 232 -> {
                val n = index - 16
                val r = (n / 36) * 51
                val g = ((n / 6) % 6) * 51
                val b = (n % 6) * 51
                Color(0xFF000000 or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong())
            }
            else -> {
                val gray = (index - 232) * 10 + 8
                Color(0xFF000000 or (gray.toLong() shl 16) or (gray.toLong() shl 8) or gray.toLong())
            }
        }
    }
    
    fun reset() {
        currentStyle = TextStyle()
        textBuffer.clear()
        state = State.NORMAL
        escapeBuffer.clear()
    }
}
