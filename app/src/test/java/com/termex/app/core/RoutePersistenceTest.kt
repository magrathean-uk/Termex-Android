package com.termex.app.core

import org.junit.Assert.assertEquals
import org.junit.Test

class RoutePersistenceTest {

    @Test
    fun `workspace route normalizes ids`() {
        val route = PersistedRootRoute.workspace(
            listOf(" server-b ", "", "server-a", "server-b", "server-c ")
        )

        assertEquals(RootRouteKind.WORKSPACE, route.kind)
        assertEquals(listOf("server-b", "server-a", "server-c"), route.serverIds)
    }

    @Test
    fun `resolve root route restores workspace and filters missing servers`() {
        val state = resolveRootRoute(
            persistedRoute = PersistedRootRoute.workspace(
                listOf("server-c", "server-a", "missing", "server-a")
            ),
            validServerIds = setOf("server-a", "server-b"),
            validWorkplaceIds = setOf("workplace-1"),
            eligibleResumeServerIds = setOf("server-b"),
            resumeServerId = "server-b"
        )

        assertEquals(
            RootRouteDestination.Workspace(listOf("server-a")),
            state.destination
        )
        assertEquals(
            PersistedRootRoute.workspace(listOf("server-a")),
            state.persistedRoute
        )
    }

    @Test
    fun `resolve root route falls back to tmux server when route is stale`() {
        val state = resolveRootRoute(
            persistedRoute = PersistedRootRoute.workplace("missing-workplace"),
            validServerIds = setOf("server-1"),
            validWorkplaceIds = setOf("workplace-1"),
            eligibleResumeServerIds = setOf("server-1"),
            resumeServerId = "server-1"
        )

        assertEquals(RootRouteDestination.Server("server-1"), state.destination)
        assertEquals(PersistedRootRoute.server("server-1").kind, state.persistedRoute.kind)
        assertEquals("server-1", state.persistedRoute.id)
    }

    @Test
    fun `resolve root route keeps valid saved route ahead of tmux resume`() {
        val state = resolveRootRoute(
            persistedRoute = PersistedRootRoute.workplace("workplace-1"),
            validServerIds = setOf("server-1"),
            validWorkplaceIds = setOf("workplace-1"),
            eligibleResumeServerIds = setOf("server-1"),
            resumeServerId = "server-1"
        )

        assertEquals(RootRouteDestination.Workplace("workplace-1"), state.destination)
        assertEquals(PersistedRootRoute.workplace("workplace-1").kind, state.persistedRoute.kind)
        assertEquals("workplace-1", state.persistedRoute.id)
    }

    @Test
    fun `resolve root route returns none when nothing is valid`() {
        val state = resolveRootRoute(
            persistedRoute = PersistedRootRoute.none(),
            validServerIds = emptySet(),
            validWorkplaceIds = emptySet(),
            eligibleResumeServerIds = emptySet(),
            resumeServerId = null
        )

        assertEquals(RootRouteDestination.None, state.destination)
        assertEquals(PersistedRootRoute.none(), state.persistedRoute)
    }
}
