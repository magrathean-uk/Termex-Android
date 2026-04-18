package com.termex.app.core.ssh

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PersistentTmuxSessionPlanTest {

    @Test
    fun `session name stays stable for a server`() {
        assertEquals(
            "termex-server-123",
            PersistentTmuxSessionPlan.sessionName("server-123")
        )
    }

    @Test
    fun `attach command creates or attaches tmux session`() {
        val command = PersistentTmuxSessionPlan.attachCommand("server-123")

        assertTrue(command.contains("tmux"))
        assertTrue(command.contains("new-session -A -s 'termex-server-123'"))
    }
}
