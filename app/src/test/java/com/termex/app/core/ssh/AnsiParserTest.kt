package com.termex.app.core.ssh

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class AnsiParserTest {

    private val parser = AnsiParser()

    @Test
    fun `test plain text parsing`() {
        val input = "Hello World"
        val result = parser.parse(input)
        
        assertEquals(1, result.size)
        assertEquals("Hello World", result[0].text)
        assertEquals(AnsiParser.DEFAULT_FG, result[0].style.fgColor)
    }

    @Test
    fun `test bold style`() {
        val input = "\u001B[1mBold Text\u001B[0m"
        val result = parser.parse(input)
        
        // Expected: "Bold Text" with bold=true
        // The parser splits: [Bold Text, ""] (due to reset at end potentially creating empty flush or just changing style for next char)
        // With current impl:
        // ESC[1m -> buffer flushed (empty), currentStyle bold=true
        // "Bold Text" -> appended to buffer
        // ESC[0m -> buffer flushed ("Bold Text", bold=true), currentStyle reset
        
        assertEquals(1, result.size)
        assertEquals("Bold Text", result[0].text)
        assertEquals(true, result[0].style.bold)
    }

    @Test
    fun `test color parsing`() {
        val input = "\u001B[31mRed\u001B[32mGreen"
        val result = parser.parse(input)
        
        assertEquals(2, result.size)
        
        assertEquals("Red", result[0].text)
        // Color index 1 (Red) from STANDARD_COLORS
        // 0=Black, 1=Red
        assertEquals(Color(0xFFCD0000), result[0].style.fgColor)
        
        assertEquals("Green", result[1].text)
        // Color index 2 (Green)
        assertEquals(Color(0xFF00CD00), result[1].style.fgColor)
    }
}
