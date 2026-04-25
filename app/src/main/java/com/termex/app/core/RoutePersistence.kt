package com.termex.app.core

import kotlinx.serialization.Serializable

@Serializable
enum class RootRouteKind {
    NONE,
    SERVER,
    WORKPLACE,
    WORKSPACE
}

@Serializable
data class PersistedRootRoute(
    val kind: RootRouteKind = RootRouteKind.NONE,
    val id: String? = null,
    val serverIds: List<String> = emptyList(),
    val updatedAtMillis: Long = 0L
) {
    companion object {
        fun none() = PersistedRootRoute()

        fun server(serverId: String, updatedAtMillis: Long = System.currentTimeMillis()): PersistedRootRoute {
            val cleanedId = serverId.trim()
            return if (cleanedId.isBlank()) none() else PersistedRootRoute(
                kind = RootRouteKind.SERVER,
                id = cleanedId,
                serverIds = listOf(cleanedId),
                updatedAtMillis = updatedAtMillis
            )
        }

        fun workplace(workplaceId: String, updatedAtMillis: Long = System.currentTimeMillis()): PersistedRootRoute {
            val cleanedId = workplaceId.trim()
            return if (cleanedId.isBlank()) none() else PersistedRootRoute(
                kind = RootRouteKind.WORKPLACE,
                id = cleanedId,
                serverIds = emptyList(),
                updatedAtMillis = updatedAtMillis
            )
        }

        fun workspace(serverIds: List<String>, updatedAtMillis: Long = System.currentTimeMillis()): PersistedRootRoute {
            val cleanedIds = serverIds.map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
            return if (cleanedIds.isEmpty()) none() else PersistedRootRoute(
                kind = RootRouteKind.WORKSPACE,
                id = null,
                serverIds = cleanedIds,
                updatedAtMillis = updatedAtMillis
            )
        }
    }
}

sealed interface RootRouteDestination {
    data object None : RootRouteDestination

    data class Server(val serverId: String) : RootRouteDestination

    data class Workplace(val workplaceId: String) : RootRouteDestination

    data class Workspace(val serverIds: List<String>) : RootRouteDestination
}

data class RootRouteRestoreState(
    val destination: RootRouteDestination = RootRouteDestination.None,
    val persistedRoute: PersistedRootRoute = PersistedRootRoute.none()
)

fun resolveRootRoute(
    persistedRoute: PersistedRootRoute,
    validServerIds: Set<String>,
    validWorkplaceIds: Set<String>,
    eligibleResumeServerIds: Set<String>,
    resumeServerId: String?
): RootRouteRestoreState {
    fun resumeFallback(): RootRouteRestoreState {
        val serverId = resumeServerId?.trim()?.takeIf {
            it.isNotEmpty() && it in validServerIds && it in eligibleResumeServerIds
        }
            ?: return RootRouteRestoreState()
        return RootRouteRestoreState(
            destination = RootRouteDestination.Server(serverId),
            persistedRoute = PersistedRootRoute.server(serverId)
        )
    }

    return when (persistedRoute.kind) {
        RootRouteKind.SERVER -> {
            val serverId = persistedRoute.id?.trim()?.takeIf { it.isNotEmpty() && it in validServerIds }
                ?: return resumeFallback()
            RootRouteRestoreState(
                destination = RootRouteDestination.Server(serverId),
                persistedRoute = PersistedRootRoute.server(serverId)
            )
        }

        RootRouteKind.WORKPLACE -> {
            val workplaceId = persistedRoute.id?.trim()?.takeIf { it.isNotEmpty() && it in validWorkplaceIds }
                ?: return resumeFallback()
            RootRouteRestoreState(
                destination = RootRouteDestination.Workplace(workplaceId),
                persistedRoute = PersistedRootRoute.workplace(workplaceId)
            )
        }

        RootRouteKind.WORKSPACE -> {
            val normalizedIds = persistedRoute.serverIds
                .map { it.trim() }
                .filter { it.isNotEmpty() && it in validServerIds }
                .distinct()
            if (normalizedIds.isNotEmpty()) {
                RootRouteRestoreState(
                    destination = RootRouteDestination.Workspace(normalizedIds),
                    persistedRoute = PersistedRootRoute.workspace(normalizedIds)
                )
            } else {
                resumeFallback()
            }
        }

        RootRouteKind.NONE -> resumeFallback()
    }
}
