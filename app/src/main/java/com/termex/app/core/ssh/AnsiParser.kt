package com.termex.app.core.ssh

import androidx.compose.ui.graphics.Color

/**
 * Simple ANSI escape sequence parser for terminal emulation.
 * Handles common SGR (Select Graphic Rendition) codes for styling.
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
    
    data class StyledText(
        val text: String,
        val style: TextStyle
    )
    
    private var currentStyle = TextStyle()
    private val buffer = StringBuilder()
    private var inEscapeSequence = false
    private val escapeBuffer = StringBuilder()
    
    fun parse(input: String): List<StyledText> {
        val result = mutableListOf<StyledText>()
        
        for (char in input) {
            when {
                char == ESC -> {
                    // Flush current buffer
                    if (buffer.isNotEmpty()) {
                        result.add(StyledText(buffer.toString(), currentStyle))
                        buffer.clear()
                    }
                    inEscapeSequence = true
                    escapeBuffer.clear()
                    escapeBuffer.append(char)
                }
                inEscapeSequence -> {
                    escapeBuffer.append(char)
                    // Check if sequence is complete
                    if (char.isLetter() || char == 'm') {
                        processEscapeSequence(escapeBuffer.toString())
                        inEscapeSequence = false
                        escapeBuffer.clear()
                    }
                }
                else -> {
                    buffer.append(char)
                }
            }
        }
        
        // Flush remaining buffer
        if (buffer.isNotEmpty()) {
            result.add(StyledText(buffer.toString(), currentStyle))
            buffer.clear()
        }
        
        return result
    }
    
    private fun processEscapeSequence(sequence: String) {
        // Handle CSI sequences: ESC [ ... m
        if (sequence.startsWith("\u001B[") && sequence.endsWith("m")) {
            val params = sequence.drop(2).dropLast(1)
            if (params.isEmpty()) {
                // ESC[m = reset
                currentStyle = TextStyle()
                return
            }
            
            val codes = params.split(";").mapNotNull { it.toIntOrNull() }
            var i = 0
            while (i < codes.size) {
                when (val code = codes[i]) {
                    0 -> currentStyle = TextStyle() // Reset
                    1 -> currentStyle = currentStyle.copy(bold = true)
                    3 -> currentStyle = currentStyle.copy(italic = true)
                    4 -> currentStyle = currentStyle.copy(underline = true)
                    7 -> currentStyle = currentStyle.copy(inverse = true)
                    22 -> currentStyle = currentStyle.copy(bold = false)
                    23 -> currentStyle = currentStyle.copy(italic = false)
                    24 -> currentStyle = currentStyle.copy(underline = false)
                    27 -> currentStyle = currentStyle.copy(inverse = false)
                    
                    // Standard foreground colors 30-37
                    in 30..37 -> currentStyle = currentStyle.copy(fgColor = STANDARD_COLORS[code - 30])
                    39 -> currentStyle = currentStyle.copy(fgColor = DEFAULT_FG) // Default FG
                    
                    // Standard background colors 40-47
                    in 40..47 -> currentStyle = currentStyle.copy(bgColor = STANDARD_COLORS[code - 40])
                    49 -> currentStyle = currentStyle.copy(bgColor = DEFAULT_BG) // Default BG
                    
                    // Bright foreground colors 90-97
                    in 90..97 -> currentStyle = currentStyle.copy(fgColor = STANDARD_COLORS[code - 90 + 8])
                    
                    // Bright background colors 100-107
                    in 100..107 -> currentStyle = currentStyle.copy(bgColor = STANDARD_COLORS[code - 100 + 8])
                    
                    // 256-color mode: 38;5;n or 48;5;n
                    38 -> {
                        if (i + 2 < codes.size && codes[i + 1] == 5) {
                            val colorIndex = codes[i + 2]
                            currentStyle = currentStyle.copy(fgColor = get256Color(colorIndex))
                            i += 2
                        }
                    }
                    48 -> {
                        if (i + 2 < codes.size && codes[i + 1] == 5) {
                            val colorIndex = codes[i + 2]
                            currentStyle = currentStyle.copy(bgColor = get256Color(colorIndex))
                            i += 2
                        }
                    }
                }
                i++
            }
        }
        // Ignore other escape sequences for now (cursor movement, etc.)
    }
    
    private fun get256Color(index: Int): Color {
        return when {
            index < 16 -> STANDARD_COLORS[index]
            index < 232 -> {
                // 6x6x6 color cube (indices 16-231)
                val n = index - 16
                val r = (n / 36) * 51
                val g = ((n / 6) % 6) * 51
                val b = (n % 6) * 51
                Color(0xFF000000 or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong())
            }
            else -> {
                // Grayscale (indices 232-255)
                val gray = (index - 232) * 10 + 8
                Color(0xFF000000 or (gray.toLong() shl 16) or (gray.toLong() shl 8) or gray.toLong())
            }
        }
    }
    
    fun reset() {
        currentStyle = TextStyle()
        buffer.clear()
        inEscapeSequence = false
        escapeBuffer.clear()
    }
}
