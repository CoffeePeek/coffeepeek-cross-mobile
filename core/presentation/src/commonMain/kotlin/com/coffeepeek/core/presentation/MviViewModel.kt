package com.coffeepeek.core.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Typed state and one-off event mechanics for migrated feature ViewModels. */
abstract class MviViewModel<State : Any, Action : Any, Event : Any>(initialState: State) : ViewModel() {
    private val mutableState = MutableStateFlow(initialState)
    val state: StateFlow<State> = mutableState.asStateFlow()
    private val eventChannel = Channel<Event>(Channel.BUFFERED)
    val events: Flow<Event> = eventChannel.receiveAsFlow()

    fun onAction(action: Action) {
        viewModelScope.launch { handleActionInternal(action) }
    }

    protected abstract suspend fun handleActionInternal(action: Action)

    protected fun updateState(update: State.() -> State) {
        mutableState.update(update)
    }

    protected val currentState: State
        get() = state.value

    /** Call from an action handler or another lifecycle-owned coroutine. */
    protected suspend fun sendEvent(event: Event) {
        eventChannel.send(event)
    }
}
