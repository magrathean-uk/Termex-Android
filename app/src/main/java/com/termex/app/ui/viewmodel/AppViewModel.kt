package com.termex.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termex.app.core.billing.SubscriptionManager
import com.termex.app.core.billing.SubscriptionState
import com.termex.app.data.prefs.UserPreferencesRepository
import com.termex.app.domain.Server
import com.termex.app.domain.ServerRepository
import com.termex.app.domain.Snippet
import com.termex.app.domain.SnippetRepository
import com.termex.app.domain.Workplace
import com.termex.app.domain.WorkplaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val subscriptionManager: SubscriptionManager,
    private val serverRepository: ServerRepository,
    private val workplaceRepository: WorkplaceRepository,
    private val snippetRepository: SnippetRepository
) : ViewModel() {

    val hasCompletedOnboarding: StateFlow<Boolean> = userPreferencesRepository.hasCompletedOnboarding
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val demoModeEnabled: StateFlow<Boolean> = userPreferencesRepository.demoModeEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val subscriptionState: StateFlow<SubscriptionState> = subscriptionManager.subscriptionState
    
    init {
        // Check subscription status on launch
        subscriptionManager.querySubscriptionStatus()
    }
    
    fun completeOnboarding() {
        viewModelScope.launch {
            userPreferencesRepository.completeOnboarding()
        }
    }

    fun enableDemoMode() {
        viewModelScope.launch {
            userPreferencesRepository.setDemoModeEnabled(true)
            seedDemoDataIfNeeded()
        }
    }

    /** Seeds a demo server, workplace, and snippet the first time demo mode is activated. */
    private suspend fun seedDemoDataIfNeeded() {
        val existing = serverRepository.getServer(Server.DEMO_SERVER_ID)
        if (existing != null) return

        val demoServer = Server.createDemoServer()
        serverRepository.addServer(demoServer)

        val demoWorkplace = Workplace(name = "Demo Workspace")
        workplaceRepository.addWorkplace(demoWorkplace)

        val demoSnippet = Snippet(name = "Status", command = "status")
        snippetRepository.addSnippet(demoSnippet)
    }

    fun refreshSubscription() {
        subscriptionManager.querySubscriptionStatus()
    }
    
    // Expose subscription methods
    suspend fun getProductDetails() = subscriptionManager.getProductDetails()
    
    suspend fun launchSubscriptionFlow(activity: android.app.Activity, productDetails: com.android.billingclient.api.ProductDetails) {
        subscriptionManager.launchSubscriptionFlow(activity, productDetails)
    }
}
