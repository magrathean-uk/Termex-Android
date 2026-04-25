package com.termex.app.core.ssh

object PersistentTmuxSessionPlan {

    fun sessionName(serverId: String): String = "termex-$serverId"

    fun attachCommand(serverId: String): String {
        val sessionName = sessionName(serverId)
        return "tmux new-session -A -s '$sessionName'\n"
    }
}
