package com.termex.app.data.local

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import io.mockk.mockk
import io.mockk.verify

class TermexDatabaseSchemaTest {

    @Test
    fun `migration from 11 to 12 drops session table`() {
        val database = mockk<androidx.sqlite.db.SupportSQLiteDatabase>(relaxed = true)

        TermexDatabase.MIGRATION_11_12.migrate(database)

        verify(exactly = 1) {
            database.execSQL("DROP TABLE IF EXISTS session_states")
        }
    }

    @Test
    fun `version 12 schema does not expose session table`() {
        val schema = schemaText(12)

        assertTrue(schema.contains("\"version\": 12"))
        assertTrue(!schema.contains("\"tableName\": \"session_states\""))
    }

    @Test
    fun `version 10 schema includes certificate and persistent session columns`() {
        val schema = schemaText(10)

        assertTrue(schema.contains("\"version\": 10"))
        assertTrue(schema.contains("\"fieldPath\": \"certificatePath\""))
        assertTrue(schema.contains("\"fieldPath\": \"persistentSessionEnabled\""))
    }

    @Test
    fun `version 11 schema includes startup command column`() {
        val schema = schemaText(11)

        assertTrue(schema.contains("\"version\": 11"))
        assertTrue(schema.contains("\"fieldPath\": \"startupCommand\""))
    }

    private fun schemaText(version: Int): String {
        val candidates = listOf(
            File("app/schemas/com.termex.app.data.local.TermexDatabase/$version.json"),
            File("schemas/com.termex.app.data.local.TermexDatabase/$version.json")
        )
        val schemaFile = candidates.firstOrNull { it.exists() }
            ?: error("schema $version not found")
        return schemaFile.readText()
    }
}
