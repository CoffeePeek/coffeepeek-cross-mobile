package com.coffeepeek.admin.sandbox

import androidx.lifecycle.ViewModel
import com.coffeepeek.admin.ui.component.PhotoViewerImages
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.coffeepeek.admin.sandbox.backend.SandboxScenario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class SandboxOwner : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() }
internal class SandboxDestination(val id: String?, val editing: Boolean) : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
}
internal data class SandboxState(
    val generation: Int = 0, val historyOwner: SandboxOwner = SandboxOwner(),
    val scenario: SandboxScenario = SandboxScenario.Ready, val dark: Boolean = true,
    val destination: SandboxDestination? = null, val dismissAllowed: Boolean = true,
    val photos: Pair<PhotoViewerImages, Int>? = null, val showScope: Boolean = false,
)

/** Sandbox navigation/scenario bookkeeping only; actual feature MVI owns drafts, validation and mutations. */
internal class SandboxViewModel : ViewModel() {
    val environment = SandboxEnvironment()
    private val mutableState = MutableStateFlow(SandboxState())
    val state = mutableState.asStateFlow()
    fun scenario(value: SandboxScenario) {
        if (state.value.destination != null) return
        environment.backend.scenario = value
        refresh(state.value.copy(scenario = value))
    }
    fun reset(populated: Boolean) {
        if (state.value.destination != null) return
        environment.backend.reset(populated)
        refresh(state.value.copy(scenario = SandboxScenario.Ready))
    }
    fun theme() { mutableState.value = state.value.copy(dark = !state.value.dark) }
    fun scope(show: Boolean) { mutableState.value = state.value.copy(showScope = show) }
    fun open(id: String? = null, editing: Boolean = id == null) {
        if (!state.value.dismissAllowed) return
        replace(SandboxDestination(id, editing))
    }
    fun dismiss(expected: SandboxDestination) {
        if (state.value.destination !== expected || !state.value.dismissAllowed) return
        if (expected.editing && expected.id != null) replace(SandboxDestination(expected.id, false)) else replace(null)
    }
    fun busy(expected: SandboxDestination, allowed: Boolean) {
        if (state.value.destination === expected) mutableState.value = state.value.copy(dismissAllowed = allowed)
    }
    fun saved(expected: SandboxDestination, id: String) {
        if (state.value.destination !== expected) return
        replace(SandboxDestination(id, false)); refresh(state.value)
    }
    fun deleted(expected: SandboxDestination) {
        if (state.value.destination !== expected) return
        replace(null); refresh(state.value)
    }
    fun photos(urls: List<String>, originals: List<String>, index: Int) {
        if (urls.size != originals.size || index !in originals.indices || originals.any(String::isBlank)) return
        mutableState.value = state.value.copy(photos = PhotoViewerImages(urls.toList(), originals.toList()) to index)
    }
    fun closePhotos() { mutableState.value = state.value.copy(photos = null) }
    private fun replace(destination: SandboxDestination?) {
        val previous = state.value.destination
        mutableState.value = state.value.copy(destination = destination, dismissAllowed = true)
        previous?.viewModelStore?.clear()
    }
    private fun refresh(value: SandboxState) {
        val old = value.historyOwner
        mutableState.value = value.copy(generation = value.generation + 1, historyOwner = SandboxOwner())
        old.viewModelStore.clear()
    }
    override fun onCleared() {
        state.value.destination?.viewModelStore?.clear(); state.value.historyOwner.viewModelStore.clear()
        environment.close(); super.onCleared()
    }
}
