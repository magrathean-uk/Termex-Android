package com.termex.app.core.ssh

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnsiParserTest {

    private val parser = AnsiParser()
    
    private fun textSegments(result: List<AnsiParser.ParsedSegment>): List<AnsiParser.ParsedSegment.Text> =
        result.filterIsInstance<AnsiParser.ParsedSegment.Text>()

    @Test
    fun `test plain text parsing`() {
        val input = "Hello World"
        val result = textSegments(parser.parse(input))
        
        assertEquals(1, result.size)
        assertEquals("Hello World", result[0].text)
        assertEquals(AnsiParser.DEFAULT_FG, result[0].style.fgColor)
    }

    @Test
    fun `test bold style`() {
        val input = "\u001B[1mBold Text\u001B[0m"
        val result = textSegments(parser.parse(input))
        
        assertEquals(1, result.size)
        assertEquals("Bold Text", result[0].text)
        assertEquals(true, result[0].style.bold)
    }

    @Test
    fun `test color parsing`() {
        val input = "\u001B[31mRed\u001B[32mGreen"
        val result = textSegments(parser.parse(input))
        
        assertEquals(2, result.size)
        
        assertEquals("Red", result[0].text)
        assertEquals(Color(0xFFCD0000), result[0].style.fgColor)
        
        assertEquals("Green", result[1].text)
        assertEquals(Color(0xFF00CD00), result[1].style.fgColor)
    }
    
    @Test
    fun `test cursor movement emits CSI command`() {
        val input = "\u001B[5A"
        val result = parser.parse(input)
        
        val csi = result.filterIsInstance<AnsiParser.ParsedSegment.CsiCommand>()
        assertEquals(1, csi.size)
        assertEquals('A', csi[0].command)
        assertEquals(listOf(5), csi[0].params)
    }
    
    @Test
    fun `test erase in display`() {
        val input = "\u001B[2J"
        val result = parser.parse(input)
        
        val csi = result.filterIsInstance<AnsiParser.ParsedSegment.CsiCommand>()
        assertEquals(1, csi.size)
        assertEquals('J', csi[0].command)
        assertEquals(listOf(2), csi[0].params)
    }
    
    @Test
    fun `test 24-bit true color`() {
        val input = "\u001B[38;2;255;128;0mOrange"
        val result = textSegments(parser.parse(input))
        
        assertEquals(1, result.size)
        assertEquals("Orange", result[0].text)
        assertEquals(Color(0xFFFF8000), result[0].style.fgColor)
    }
}
