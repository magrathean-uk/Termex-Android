package com.termex.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class TerminalColorSchemeTest {

    @Test
    fun `theme set has ten schemes`() {
        assertEquals(10, TerminalColorScheme.entries.size)
    }

    @Test
    fun `legacy display names still resolve`() {
        assertEquals(TerminalColorScheme.WHITE_ON_BLACK, TerminalColorScheme.fromStoredValue("Default"))
        assertEquals(TerminalColorScheme.SOLARIZED_DARK, TerminalColorScheme.fromStoredValue("Solarized Dark"))
    }

    @Test
    fun `raw values resolve`() {
        assertEquals(TerminalColorScheme.OCEAN, TerminalColorScheme.fromStoredValue("ocean"))
    }
}
