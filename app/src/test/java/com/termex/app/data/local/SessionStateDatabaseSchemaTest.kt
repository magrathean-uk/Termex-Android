package com.termex.app.data.local

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SessionStateDatabaseSchemaTest {

    @Test
    fun `session store schema contains session table`() {
        val schema = schemaText(1)

        assertTrue(schema.contains("\"version\": 1"))
        assertTrue(schema.contains("\"tableName\": \"session_states\""))
    }

    private fun schemaText(version: Int): String {
        val candidates = listOf(
            File("app/schemas/com.termex.app.data.local.SessionStateDatabase/$version.json"),
            File("schemas/com.termex.app.data.local.SessionStateDatabase/$version.json")
        )
        val schemaFile = candidates.firstOrNull { it.exists() }
            ?: error("schema $version not found")
        return schemaFile.readText()
    }
}
