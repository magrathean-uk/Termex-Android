package com.termex.app.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.termex.app.core.PersistedRootRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UserPreferencesRepositoryTest {

    @Test
    fun `resume marker persists and clears blank values`() = runTest {
        val dataStore = FakePreferencesDataStore()
        val repository = UserPreferencesRepository(dataStore)

        repository.setPersistentSessionResumeServerId("server-123")
        assertEquals(
            "server-123",
            repository.persistentSessionResumeServerIdFlow.first()
        )

        repository.setPersistentSessionResumeServerId("")
        assertNull(repository.persistentSessionResumeServerIdFlow.first())

        repository.setPersistentSessionResumeServerId("   ")
        assertNull(repository.persistentSessionResumeServerIdFlow.first())

        repository.setPersistedRootRoute(PersistedRootRoute.workspace(listOf("server-a", "server-b")))
        val persistedRoute = repository.persistedRootRouteFlow.first()
        assertEquals(PersistedRootRoute.workspace(listOf("server-a", "server-b")).kind, persistedRoute.kind)
        assertEquals(PersistedRootRoute.workspace(listOf("server-a", "server-b")).id, persistedRoute.id)
        assertEquals(PersistedRootRoute.workspace(listOf("server-a", "server-b")).serverIds, persistedRoute.serverIds)

        repository.setPersistedRootRoute(PersistedRootRoute.none())
        assertEquals(PersistedRootRoute.none(), repository.persistedRootRouteFlow.first())
    }

    @Test
    fun `link handling and extra key preset persist`() = runTest {
        val dataStore = FakePreferencesDataStore()
        val repository = UserPreferencesRepository(dataStore)

        repository.setLinkHandlingMode(LinkHandlingMode.ASK_FIRST)
        repository.setTerminalExtraKeyPreset(TerminalExtraKeyPreset.CODING)

        assertEquals(LinkHandlingMode.ASK_FIRST, repository.linkHandlingModeFlow.first())
        assertEquals(TerminalExtraKeyPreset.CODING, repository.terminalExtraKeyPresetFlow.first())
        assertEquals(TerminalExtraKeyPreset.CODING.keyIds, repository.terminalExtraKeyIdsFlow.first())
    }

    @Test
    fun `custom extra key ids persist with defaults for invalid values`() = runTest {
        val dataStore = FakePreferencesDataStore()
        val repository = UserPreferencesRepository(dataStore)

        repository.setTerminalExtraKeyIds(listOf("esc", "tab", "pipe"))
        assertEquals(listOf("esc", "tab", "pipe"), repository.terminalExtraKeyIdsFlow.first())

        repository.setTerminalExtraKeyIds(listOf("bogus"))
        assertEquals(
            TerminalExtraKey.defaultKeys.map(TerminalExtraKey::raw),
            repository.terminalExtraKeyIdsFlow.first()
        )
    }

    @Test
    fun `sync google account email persists and clears`() = runTest {
        val dataStore = FakePreferencesDataStore()
        val repository = UserPreferencesRepository(dataStore)

        repository.setSyncGoogleAccountEmail("user@example.com")
        assertEquals("user@example.com", repository.syncGoogleAccountEmailFlow.first())

        repository.setSyncGoogleAccountEmail(" ")
        assertNull(repository.syncGoogleAccountEmailFlow.first())
    }

    @Test
    fun `terminal settings keep stored color scheme raw value`() = runTest {
        val dataStore = FakePreferencesDataStore()
        val repository = UserPreferencesRepository(dataStore)

        repository.setTerminalSettings(
            TerminalSettings(
                fontSize = 16,
                fontFamily = "Menlo",
                colorScheme = "solarizedDark"
            )
        )

        assertEquals("solarizedDark", repository.terminalSettingsFlow.first().colorScheme)
    }
}

private class FakePreferencesDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())

    override val data = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}
