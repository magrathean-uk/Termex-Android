package com.termex.app.core.ssh

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalBufferTest {

    @Test
    fun `resize keeps bottom content visible and expands viewport`() {
        val buffer = TerminalBuffer(cols = 10, rows = 3, scrollbackSize = 20)
        buffer.write("1\n2\n3\n4\n5")

        buffer.resize(newCols = 10, newRows = 5)

        assertEquals(listOf("1", "2", "3", "4", "5"), visible(buffer))
    }

    @Test
    fun `scrolling history moves cursor outside the visible viewport`() {
        val buffer = TerminalBuffer(cols = 10, rows = 3, scrollbackSize = 20)
        buffer.write("1\n2\n3\n4")

        buffer.scrollUp(1)

        assertEquals(listOf("1", "2", "3"), visible(buffer))
        assertTrue(buffer.cursorPosition.value.second > 2)
    }

    private fun visible(buffer: TerminalBuffer): List<String> {
        return buffer.getVisibleContent().map { it.trimEnd() }
    }
}
