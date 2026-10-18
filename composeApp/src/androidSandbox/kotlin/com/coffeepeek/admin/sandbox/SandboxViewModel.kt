package com.coffeepeek.admin.sandbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.coffeepeek.admin.sandbox.backend.SandboxScenario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class SandboxOwner : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() }
internal data class SandboxState(val owner: SandboxOwner = SandboxOwner(), val controls: Boolean = false,
    val scenario: SandboxScenario = SandboxScenario.Ready)

/** Debug controls only. The actual application owns navigation and feature ViewModels. */
internal class SandboxViewModel : ViewModel() {
    private val mutableState = MutableStateFlow(SandboxState())
    val state = mutableState.asStateFlow()
    fun controls(show: Boolean) { mutableState.value = state.value.copy(controls = show) }
    fun refresh(scenario: SandboxScenario) {
        val previous = state.value.owner
        mutableState.value = SandboxState(scenario = scenario)
        previous.viewModelStore.clear()
    }
    override fun onCleared() { state.value.owner.viewModelStore.clear() }
}
