package com.termex.app.ui.viewmodel

import app.cash.turbine.test
import com.termex.app.discovery.SharedServerDiscoveryItem
import com.termex.app.discovery.SharedServerImportTarget
import com.termex.app.discovery.SharedServersDiscoveryService
import com.termex.app.discovery.SharedServersDiscoveryState
import com.termex.app.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SharedServersViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun resolvesSelectedServerIntoImportTarget() = runTest {
        val service = FakeSharedServersDiscoveryService()
        val item = SharedServerDiscoveryItem(
            id = "ssh:lab",
            serviceName = "Lab SSH"
        )
        val target = SharedServerImportTarget(
            host = "lab.example.com",
            port = 2222
        )
        service.setServices(listOf(item))
        service.setResolution(item.id, target)

        val viewModel = SharedServersViewModel(service)

        assertTrue(viewModel.state.value.isDiscovering)
        assertEquals(listOf(item), viewModel.state.value.services)

        viewModel.events.test {
            viewModel.onServiceSelected(item)
            advanceUntilIdle()
            assertEquals(
                SharedServersEvent.ImportServer(target),
                awaitItem()
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun refresh_restarts_discovery() = runTest {
        val service = FakeSharedServersDiscoveryService()
        val viewModel = SharedServersViewModel(service)

        viewModel.refresh()

        assertEquals(2, service.startCalls)
        assertEquals(1, service.stopCalls)
    }

    @Test
    fun unresolved_service_emits_no_import_event() = runTest {
        val service = FakeSharedServersDiscoveryService()
        val item = SharedServerDiscoveryItem(
            id = "ssh:missing",
            serviceName = "Missing"
        )
        service.setServices(listOf(item))

        val viewModel = SharedServersViewModel(service)

        viewModel.events.test {
            viewModel.onServiceSelected(item)
            advanceUntilIdle()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    private class FakeSharedServersDiscoveryService : SharedServersDiscoveryService {
        private val _state = MutableStateFlow(SharedServersDiscoveryState())
        override val state: StateFlow<SharedServersDiscoveryState> = _state.asStateFlow()
        private val resolutions = mutableMapOf<String, SharedServerImportTarget>()
        var startCalls = 0
        var stopCalls = 0

        override fun startDiscovery() {
            startCalls += 1
            _state.value = _state.value.copy(isDiscovering = true)
        }

        override fun stopDiscovery() {
            stopCalls += 1
            _state.value = _state.value.copy(isDiscovering = false)
        }

        override suspend fun resolve(serviceId: String): SharedServerImportTarget? {
            return resolutions[serviceId]
        }

        fun setServices(services: List<SharedServerDiscoveryItem>) {
            _state.value = _state.value.copy(services = services)
        }

        fun setResolution(serviceId: String, target: SharedServerImportTarget) {
            resolutions[serviceId] = target
        }
    }
}
