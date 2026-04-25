package com.termex.app.ui.components

import com.termex.app.data.prefs.LinkHandlingMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalTranscriptToolsTest {

    @Test
    fun `stitch transcript appends new lines`() {
        val previous = listOf("one", "two", "three")
        val visible = listOf("two", "three", "four")

        assertEquals(listOf("one", "two", "three", "four"), stitchTranscript(previous, visible))
    }

    @Test
    fun `search transcript finds matches`() {
        val matches = searchTranscript(listOf("alpha", "bravo", "charlie"), "br")

        assertEquals(1, matches.size)
        assertEquals(1, matches.first().lineIndex)
    }

    @Test
    fun `search transcript trims query and ignores case`() {
        val matches = searchTranscript(listOf("Alpha", "bravo", "CHARLIE"), "  char  ")

        assertEquals(1, matches.size)
        assertEquals("CHARLIE", matches.first().lineText)
    }

    @Test
    fun `stitch transcript keeps all lines when overlap is missing`() {
        assertEquals(
            listOf("one", "two", "three"),
            stitchTranscript(listOf("one"), listOf("two", "three"))
        )
    }

    @Test
    fun `extract terminal links finds supported urls`() {
        val links = extractTerminalLinks("see https://example.com and ssh://host.example")

        assertEquals(2, links.size)
        assertTrue(links.contains("https://example.com"))
    }

    @Test
    fun `extract terminal links trims punctuation and deduplicates`() {
        val links = extractTerminalLinks("open https://example.com, then https://example.com.")

        assertEquals(listOf("https://example.com"), links)
    }

    @Test
    fun `link action respects handling mode`() {
        assertEquals(TerminalLinkAction.OPEN, linkActionFor("https://example.com", LinkHandlingMode.AUTOMATIC))
        assertEquals(TerminalLinkAction.CONFIRM, linkActionFor("https://example.com", LinkHandlingMode.ASK_FIRST))
        assertEquals(TerminalLinkAction.IGNORE, linkActionFor("not a link", LinkHandlingMode.AUTOMATIC))
    }
}
