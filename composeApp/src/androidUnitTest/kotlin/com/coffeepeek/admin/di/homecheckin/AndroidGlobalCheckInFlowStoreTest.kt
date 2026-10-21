package com.coffeepeek.admin.di.homecheckin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.admin.di.shop.CheckInTestSessions
import com.coffeepeek.feature.homecheckin.api.HomeCheckInEditorEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlin.test.*

/** Non-UI lifetime contract: recreation retains work, final disposal and parent death clear it. */
@OptIn(ExperimentalCoroutinesApi::class)
class AndroidGlobalCheckInFlowStoreTest {
    private val dispatcher = StandardTestDispatcher()
    private val parent = ViewModelStore()
    private val owner = AndroidGlobalCheckInFlowStore().also { parent.put("flow-store", it) }

    @BeforeTest fun setup() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun cleanup() { parent.clear(); Dispatchers.resetMain() }

    private class Draft : ViewModel() {
        var cleared = false
        override fun onCleared() { cleared = true }
    }

    @Test fun recreationKeepsTheSameFlowAndDraftButFinalRemovalClearsBoth() {
        val flow = Draft().also { owner.viewModelStore.put("flow", it) }
        val editor = Draft().also { owner.viewModelStore.put("home-editor:0:null", it) }
        owner.dispose(changingConfigurations = true)
        assertSame(flow, owner.viewModelStore["flow"])
        assertSame(editor, owner.viewModelStore["home-editor:0:null"])
        assertFalse(flow.cleared || editor.cleared)
        owner.dispose(changingConfigurations = false)
        assertTrue(flow.cleared && editor.cleared)
        assertTrue(owner.viewModelStore.keys().isEmpty())
    }

    @Test fun parentDeathClearsRetainedWorkEvenAfterConfigurationDisposal() {
        val editor = Draft().also { owner.viewModelStore.put("editor", it) }
        owner.dispose(changingConfigurations = true)
        parent.clear()
        assertTrue(editor.cleared)
        assertTrue(owner.viewModelStore.keys().isEmpty())
    }

    @Test fun reopeningAfterFinalCloseStartsWithAnEmptyStore() {
        val previous = Draft().also { owner.viewModelStore.put("editor", it) }
        owner.dispose(changingConfigurations = false)
        val next = Draft().also { owner.viewModelStore.put("editor", it) }
        assertTrue(previous.cleared)
        assertFalse(next.cleared)
        assertSame(next, owner.viewModelStore["editor"])
        assertNotSame(previous, next)
    }

    @Test fun busyHomeFlowSurvivesRecreationAndDeliversSavedOnce() = runTest(dispatcher) {
        val flow = AndroidGlobalCheckInFlow(CheckInTestSessions()) { _, _ -> Entry() }
        owner.viewModelStore.put("flow", flow)
        flow.home()
        val home = assertIs<GlobalCheckInDestination.Home>(flow.state.value)
        flow.setDismissAllowed(home, false)
        owner.dispose(changingConfigurations = true)
        assertSame(home, flow.state.value)
        assertFalse(home.dismissAllowed.value)
        assertTrue(flow.canAccess())
        flow.cancel(home)
        assertSame(home, flow.state.value)
        flow.saved(home, "created")
        val closed = assertIs<GlobalCheckInDestination.Closed>(flow.state.value)
        assertEquals(GlobalCheckInOutcome.Saved, flow.claim(closed)?.outcome)
        assertNull(flow.claim(closed))
        owner.dispose(changingConfigurations = false); runCurrent()
        assertFalse(flow.canAccess())
    }

    private class Entry : HomeCheckInEditorEntry {
        @Composable override fun Content(preparationId: String?, sessionGeneration: Int, accessAllowed: Boolean,
            onClose: () -> Unit, onSaved: (String, Boolean) -> Unit, onDismissAllowedChange: (Boolean) -> Unit,
            modifier: Modifier) = error("Lifetime contract must not render UI")
    }
}
