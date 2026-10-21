package com.coffeepeek.core.presentation

import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class MviViewModelTest {
    private data class CounterState(val count: Int = 0)
    private data object Increment
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()

    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private class CounterViewModel : MviViewModel<CounterState, Increment, Nothing>(CounterState()) {
        override suspend fun handleActionInternal(action: Increment) {
            updateState { copy(count = count + 1) }
        }
    }

    private class EventViewModel : MviViewModel<CounterState, String, String>(CounterState()) {
        override suspend fun handleActionInternal(action: String) = sendEvent(action)
    }

    private sealed interface TestAction {
        data object Slow : TestAction
        data object Quick : TestAction
    }

    private class IndependentActionsViewModel(private val gate: CompletableDeferred<Unit>) :
        MviViewModel<CounterState, TestAction, String>(CounterState()) {
        override suspend fun handleActionInternal(action: TestAction) {
            when (action) {
                TestAction.Slow -> gate.await()
                TestAction.Quick -> sendEvent("quick")
            }
        }
    }

    @Test fun actionsUpdateTypedState() = runTest(dispatcher) {
        val viewModel = CounterViewModel().also { store.put("counter", it) }
        repeat(100) { viewModel.onAction(Increment) }
        runCurrent()

        assertEquals(100, viewModel.state.value.count)
    }

    @Test fun oneOffEventsKeepEmissionOrder() = runTest(dispatcher) {
        val viewModel = EventViewModel().also { store.put("events", it) }
        viewModel.onAction("first")
        viewModel.onAction("second")
        runCurrent()

        assertEquals("first", viewModel.events.first())
        assertEquals("second", viewModel.events.first())
    }

    @Test fun slowActionDoesNotBlockLaterAction() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val viewModel = IndependentActionsViewModel(gate).also { store.put("independent", it) }
        viewModel.onAction(TestAction.Slow)
        viewModel.onAction(TestAction.Quick)
        runCurrent()

        assertEquals("quick", viewModel.events.first())
    }
}
