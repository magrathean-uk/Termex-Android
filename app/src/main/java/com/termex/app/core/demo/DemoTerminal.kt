package com.termex.app.core.demo

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DemoTerminal {

    private val _output = MutableStateFlow("")
    val output: StateFlow<String> = _output.asStateFlow()

    private val cannedResponses = mapOf(
        "ls" to "Documents  Downloads  Pictures  Videos  projects\n",
        "ls -la" to """total 32
drwxr-xr-x  6 demo demo 4096 Jan 15 10:00 .
drwxr-xr-x  3 root root 4096 Jan  1 00:00 ..
-rw-r--r--  1 demo demo  220 Jan  1 00:00 .bash_logout
-rw-r--r--  1 demo demo 3526 Jan  1 00:00 .bashrc
drwxr-xr-x  2 demo demo 4096 Jan 15 10:00 Documents
drwxr-xr-x  2 demo demo 4096 Jan 15 10:00 Downloads
drwxr-xr-x  2 demo demo 4096 Jan 15 10:00 Pictures
drwxr-xr-x  2 demo demo 4096 Jan 15 10:00 projects
""",
        "pwd" to "/home/demo\n",
        "whoami" to "demo\n",
        "uname -a" to "Linux demo-server 5.15.0-generic #1 SMP x86_64 GNU/Linux\n",
        "date" to "Mon Jan 15 10:00:00 UTC 2024\n",
        "uptime" to " 10:00:00 up 42 days,  3:14,  1 user,  load average: 0.08, 0.12, 0.10\n",
        "free -h" to """              total        used        free      shared  buff/cache   available
Mem:           7.8G        1.2G        5.1G        128M        1.5G        6.2G
Swap:          2.0G          0B        2.0G
""",
        "df -h" to """Filesystem      Size  Used Avail Use% Mounted on
/dev/sda1        50G   12G   35G  26% /
tmpfs           3.9G     0  3.9G   0% /dev/shm
""",
        "cat /etc/os-release" to """NAME="Ubuntu"
VERSION="22.04 LTS (Jammy Jellyfish)"
ID=ubuntu
ID_LIKE=debian
PRETTY_NAME="Ubuntu 22.04 LTS"
""",
        "help" to """Demo Mode Commands:
  ls, ls -la    - List files
  pwd           - Print working directory
  whoami        - Print current user
  uname -a      - System information
  date          - Current date/time
  uptime        - System uptime
  free -h       - Memory usage
  df -h         - Disk usage
  help          - Show this help
  exit          - Exit demo mode
"""
    )

    private var commandBuffer = StringBuilder()

    init {
        showPrompt()
    }

    suspend fun processInput(input: String) {
        for (char in input) {
            when (char) {
                '\r', '\n' -> {
                    _output.value += "\n"
                    processCommand(commandBuffer.toString().trim())
                    commandBuffer.clear()
                    showPrompt()
                }
                '\u007F' -> { // Backspace
                    if (commandBuffer.isNotEmpty()) {
                        commandBuffer.deleteCharAt(commandBuffer.length - 1)
                        _output.value = _output.value.dropLast(1)
                    }
                }
                else -> {
                    commandBuffer.append(char)
                    _output.value += char
                }
            }
        }
    }

    private suspend fun processCommand(command: String) {
        if (command.isEmpty()) return

        delay(100) // Simulate network latency

        val response = cannedResponses[command]
            ?: if (command == "exit") {
                "Goodbye!\n"
            } else {
                "bash: $command: command not found\n"
            }

        _output.value += response
    }

    private fun showPrompt() {
        _output.value += "demo@demo-server:~$ "
    }

    fun getWelcomeMessage(): String {
        return """
Welcome to Termex Demo Mode!

This is a simulated SSH terminal for demonstration purposes.
No actual network connection is made.

Type 'help' for available commands.

""".trimIndent() + "\n"
    }

    fun reset() {
        _output.value = ""
        commandBuffer.clear()
        _output.value = getWelcomeMessage()
        showPrompt()
    }
}
