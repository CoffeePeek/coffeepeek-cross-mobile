package com.coffeepeek.admin.sandbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.coffeepeek.admin.sandbox.backend.SandboxAuthPolicy
import com.coffeepeek.admin.sandbox.backend.SandboxCities
import com.coffeepeek.admin.sandbox.backend.SandboxContributions
import com.coffeepeek.admin.sandbox.backend.SandboxFeed
import com.coffeepeek.admin.sandbox.backend.SandboxScenario
import com.coffeepeek.admin.sandbox.backend.SandboxUpdatePolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class SandboxOwner : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() }
internal data class SandboxState(val owner: SandboxOwner = SandboxOwner(), val controls: Boolean = false,
    val scenario: SandboxScenario = SandboxScenario.Ready, val updatePolicy: SandboxUpdatePolicy = SandboxUpdatePolicy.UpToDate,
    val cities: SandboxCities = SandboxCities.Single, val authPolicy: SandboxAuthPolicy = SandboxAuthPolicy.Works,
    val feed: SandboxFeed = SandboxFeed.Single, val contributions: SandboxContributions = SandboxContributions.Seeded)

/** Debug controls only. The actual application owns navigation and feature ViewModels. */
internal class SandboxViewModel : ViewModel() {
    private val mutableState = MutableStateFlow(SandboxState())
    val state = mutableState.asStateFlow()
    /** Shows the fixtures chosen at launch without re-creating the app. */
    fun fixtures(updatePolicy: SandboxUpdatePolicy, cities: SandboxCities, authPolicy: SandboxAuthPolicy = state.value.authPolicy, feed: SandboxFeed = state.value.feed,
        contributions: SandboxContributions = state.value.contributions) {
        mutableState.value = state.value.copy(updatePolicy = updatePolicy, cities = cities, authPolicy = authPolicy, feed = feed, contributions = contributions)
    }
    fun controls(show: Boolean) { mutableState.value = state.value.copy(controls = show) }
    fun refresh(scenario: SandboxScenario, updatePolicy: SandboxUpdatePolicy = state.value.updatePolicy, cities: SandboxCities = state.value.cities,
        authPolicy: SandboxAuthPolicy = state.value.authPolicy, feed: SandboxFeed = state.value.feed,
        contributions: SandboxContributions = state.value.contributions) {
        val previous = state.value.owner
        mutableState.value = SandboxState(scenario = scenario, updatePolicy = updatePolicy, cities = cities, authPolicy = authPolicy, feed = feed, contributions = contributions)
        previous.viewModelStore.clear()
    }
    override fun onCleared() { state.value.owner.viewModelStore.clear() }
}
