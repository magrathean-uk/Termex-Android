package com.termex.app.data.prefs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TerminalExtraKeyBehaviorTest {

    @Test
    fun `preset key order stays stable`() {
        assertEquals(
            listOf("esc", "tab", "ctrl", "alt", "pipe", "slash", "dash", "tilde", "hash", "home", "end"),
            TerminalExtraKeyPreset.CODING.keyIds
        )
    }

    @Test
    fun `raw list drops invalid keys and falls back when empty`() {
        assertEquals(
            listOf(TerminalExtraKey.ESC, TerminalExtraKey.TAB),
            TerminalExtraKey.fromRawList(listOf("esc", "bogus", "tab"))
        )
        assertEquals(
            TerminalExtraKey.defaultKeys,
            TerminalExtraKey.fromRawList(listOf("bogus"))
        )
    }

    @Test
    fun `unknown raw key returns null`() {
        assertNull(TerminalExtraKey.fromRaw("bogus"))
    }
}
