package com.termex.app.config

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupRulesTest {

    @Test
    fun `backup rules exclude v2 secrets prefs`() {
        assertFileContains("src/main/res/xml/backup_rules.xml", "termex_secrets_v2.xml")
        assertFileContains("src/main/res/xml/data_extraction_rules.xml", "termex_secrets_v2.xml")
    }

    private fun assertFileContains(path: String, expected: String) {
        val content = File(path).readText()
        assertTrue("$path should contain $expected", content.contains(expected))
    }
}
