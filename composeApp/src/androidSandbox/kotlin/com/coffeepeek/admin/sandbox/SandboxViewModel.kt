package com.coffeepeek.admin.sandbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.coffeepeek.admin.sandbox.backend.SandboxCities
import com.coffeepeek.admin.sandbox.backend.SandboxScenario
import com.coffeepeek.admin.sandbox.backend.SandboxUpdatePolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class SandboxOwner : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() }
internal data class SandboxState(val owner: SandboxOwner = SandboxOwner(), val controls: Boolean = false,
    val scenario: SandboxScenario = SandboxScenario.Ready, val updatePolicy: SandboxUpdatePolicy = SandboxUpdatePolicy.UpToDate,
    val cities: SandboxCities = SandboxCities.Single)

/** Debug controls only. The actual application owns navigation and feature ViewModels. */
internal class SandboxViewModel : ViewModel() {
    private val mutableState = MutableStateFlow(SandboxState())
    val state = mutableState.asStateFlow()
    fun controls(show: Boolean) { mutableState.value = state.value.copy(controls = show) }
    fun refresh(scenario: SandboxScenario, updatePolicy: SandboxUpdatePolicy = state.value.updatePolicy, cities: SandboxCities = state.value.cities) {
        val previous = state.value.owner
        mutableState.value = SandboxState(scenario = scenario, updatePolicy = updatePolicy, cities = cities)
        previous.viewModelStore.clear()
    }
    override fun onCleared() { state.value.owner.viewModelStore.clear() }
}
