package com.coffeepeek.core.designsystem

import com.coffeepeek.core.designsystem.component.PullRefreshGestureState
import kotlin.test.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class PullRefreshGestureStateTest {
    private var now = Duration.ZERO
    private fun state() = PullRefreshGestureState(100f, 1.seconds) { now }

    @Test fun dampensAndCapsPull() {
        val state = state()
        assertEquals(100f, state.pull(100f, true, true))
        assertEquals(50f, state.offset)
        assertEquals(180f, state.pull(1000f, true, true))
        assertEquals(140f, state.offset)
        assertEquals(0f, state.pull(100f, true, true))
    }
    @Test fun retractConsumesOnlyExistingOffset() {
        val state = state()
        state.pull(100f, true, true)
        assertEquals(-30f, state.retract(-30f))
        assertEquals(35f, state.offset)
        assertEquals(-70f, state.retract(-500f))
        assertEquals(0f, state.offset)
    }
    @Test fun releaseBelowThresholdDoesNotRequest() {
        val state = state()
        state.pull(199f, true, true)
        assertFalse(state.release(true, true))
        assertEquals(0f, state.offset)
    }
    @Test fun thresholdTriggersOnceAndReleaseResets() {
        val state = state()
        state.pull(200f, true, true)
        assertTrue(state.release(true, true))
        assertEquals(0f, state.offset)
        assertFalse(state.release(true, true))
    }
    @Test fun cooldownSharedAndBoundaryAllowsNextRequest() {
        val state = state()
        assertTrue(state.request(true))
        state.pull(200f, true, true)
        assertFalse(state.release(true, true))
        now = 1.seconds
        state.pull(200f, true, true)
        assertTrue(state.release(true, true))
    }
    @Test fun disabledOrAwayFromTopClearsPull() {
        val state = state()
        state.pull(200f, true, true)
        assertEquals(0f, state.pull(10f, false, true))
        assertEquals(0f, state.offset)
        state.pull(200f, true, true)
        assertFalse(state.release(false, true))
        assertFalse(state.request(false))
    }
    @Test fun leavingTopBeforeReleaseDoesNotRequest() {
        val state = state()
        state.pull(200f, true, true)
        assertFalse(state.release(true, false))
    }
    @Test fun resetDoesNotEraseCooldown() {
        val state = state()
        assertTrue(state.request(true))
        state.reset()
        assertFalse(state.request(true))
    }
    @Test fun zeroCooldownDelegatesDeduplicationToCaller() {
        val state = PullRefreshGestureState(100f, Duration.ZERO) { now }
        assertTrue(state.request(true))
        assertTrue(state.request(true))
    }
    @Test fun invalidInputsCannotPoisonOffset() {
        val state = state()
        assertEquals(0f, state.pull(Float.NaN, true, true))
        assertEquals(0f, state.pull(Float.POSITIVE_INFINITY, true, true))
        assertEquals(0f, state.retract(Float.NEGATIVE_INFINITY))
        assertFailsWith<IllegalArgumentException> { PullRefreshGestureState(0f, 1.seconds) { now } }
        assertFailsWith<IllegalArgumentException> { PullRefreshGestureState(100f, (-1).seconds) { now } }
    }
}
