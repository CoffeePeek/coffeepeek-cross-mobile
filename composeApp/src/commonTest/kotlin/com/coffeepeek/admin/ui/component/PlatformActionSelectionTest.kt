package com.coffeepeek.admin.ui.component

import kotlin.test.Test
import kotlin.test.assertEquals

class PlatformActionSelectionTest {
    @Test
    fun resourceOpensAfterNativeDismissalAndCallerStateIsClosed() {
        val calls = mutableListOf<String>()
        var finishDismissal: (() -> Unit)? = null
        val selection = PlatformActionSelection(true) { calls += "dismiss" }
        selection.select(PlatformAction("Gallery") { calls += "gallery" }) {
            calls += "animation"
            finishDismissal = it
        }
        assertEquals(listOf("animation"), calls)
        finishDismissal!!.invoke()
        assertEquals(listOf("animation", "dismiss", "gallery"), calls)
    }

    @Test
    fun confirmationDoesNotInvokeCancellation() {
        val calls = mutableListOf<String>()
        val selection = PlatformActionSelection(false) { calls += "cancel" }
        selection.select(PlatformAction("Log out") { calls += "confirm" }) { it() }
        assertEquals(listOf("confirm"), calls)
    }

    @Test
    fun repeatedTapsWhileDismissingOpenOnlyOneResource() {
        var dismissals = 0
        var selections = 0
        var finishDismissal: (() -> Unit)? = null
        val selection = PlatformActionSelection(true) {}
        val action = PlatformAction("Camera") { selections++ }
        repeat(2) {
            selection.select(action) {
                dismissals++
                finishDismissal = it
            }
        }
        assertEquals(1, dismissals)
        finishDismissal!!.invoke()
        assertEquals(1, selections)
    }

    @Test
    fun disabledActionDoesNotDismissOrConsumeSelection() {
        var selections = 0
        val selection = PlatformActionSelection(true) {}
        selection.select(PlatformAction("Disabled", enabled = false) { error("disabled") }) { error("dismissed") }
        selection.select(PlatformAction("Gallery") { selections++ }) { it() }
        assertEquals(1, selections)
    }
}
