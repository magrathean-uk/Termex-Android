package com.termex.app

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.termex.app.discovery.SharedServerDiscoveryItem
import com.termex.app.discovery.SharedServerImportTarget
import com.termex.app.discovery.SharedServersDiscoveryService
import com.termex.app.discovery.SharedServersDiscoveryState
import com.termex.app.di.DiscoveryModule
import com.termex.app.ui.AutomationTags
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltAndroidTest
@UninstallModules(DiscoveryModule::class)
@RunWith(AndroidJUnit4::class)
class SharedServersFlowTest {

    private val hiltRule = HiltAndroidRule(this)
    val composeRule = createAndroidComposeRule<MainActivity>()
    private val fakeSharedServersDiscoveryService = FakeSharedServersDiscoveryService()

    @BindValue
    @JvmField
    val sharedServersDiscoveryService: SharedServersDiscoveryService =
        fakeSharedServersDiscoveryService

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(hiltRule).around(composeRule)

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun sharedServersBrowserImportsResolvedHostAndPort() {
        dismissOnboarding()

        val item = SharedServerDiscoveryItem(
            id = "ssh:lab",
            serviceName = "Lab SSH"
        )
        fakeSharedServersDiscoveryService.setServices(listOf(item))
        fakeSharedServersDiscoveryService.setResolution(
            item.id,
            SharedServerImportTarget(
                host = "lab.example.com",
                port = 2222
            )
        )

        composeRule.onNodeWithTag(AutomationTags.MAIN_TAB_SERVERS).performClick()
        composeRule.onNodeWithTag(AutomationTags.SHARED_SERVERS_ENTRY).performClick()

        composeRule.waitUntil(10_000) { hasNodeWithTag(AutomationTags.SHARED_SERVERS_SCREEN) }
        composeRule.onNodeWithTag(AutomationTags.sharedServerItemTag(item.id)).performClick()

        composeRule.waitUntil(10_000) { hasNodeWithTag(AutomationTags.SERVER_FIELD_HOST) }
        composeRule.onNodeWithTag(AutomationTags.SERVER_FIELD_HOST).assertTextContains("lab.example.com")
        composeRule.onNodeWithTag(AutomationTags.SERVER_FIELD_PORT).assertTextContains("2222")
    }

    private fun dismissOnboarding() {
        if (hasNodeWithTag(AutomationTags.ONBOARDING_SKIP)) {
            composeRule.onNodeWithTag(AutomationTags.ONBOARDING_SKIP).performClick()
        }
        composeRule.waitUntil(10_000) { hasNodeWithTag(AutomationTags.ONBOARDING_PRIMARY) }
        composeRule.onNodeWithTag(AutomationTags.ONBOARDING_PRIMARY).performClick()
        composeRule.waitUntil(20_000) { hasNodeWithTag(AutomationTags.MAIN_TAB_SERVERS) }
    }

    private fun hasNodeWithTag(tag: String): Boolean {
        return composeRule.onAllNodes(hasTestTag(tag), useUnmergedTree = true)
            .fetchSemanticsNodes().isNotEmpty()
    }

    private class FakeSharedServersDiscoveryService : SharedServersDiscoveryService {
        private val _state = MutableStateFlow(SharedServersDiscoveryState())
        override val state: StateFlow<SharedServersDiscoveryState> = _state.asStateFlow()
        private val resolutions = mutableMapOf<String, SharedServerImportTarget>()

        override fun startDiscovery() {
            _state.value = _state.value.copy(isDiscovering = true)
        }

        override fun stopDiscovery() {
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
