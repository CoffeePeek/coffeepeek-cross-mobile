package com.coffeepeek.admin.ui.component

import kotlin.test.Test
import kotlin.test.assertEquals

/** Pure callback policy, not a UI/UIKit test: the native bridge supplies the live button.enabled value. */
class PlatformMenuActionTest {
    @Test fun disabledOrReleasedTriggerNeverInvokesAnAction() {
        var calls = 0
        val items = listOf(PlatformMenuItem("edit", "Edit") { calls++ })
        performPlatformMenuAction(items, "edit", enabled = false)
        assertEquals(0, calls)
    }

    @Test fun aDisabledItemIsNotInvokedEvenFromAnEnabledTrigger() {
        var calls = 0
        val items = listOf(PlatformMenuItem("edit", "Edit", enabled = false) { calls++ })
        performPlatformMenuAction(items, "edit", enabled = true)
        assertEquals(0, calls)
    }

    @Test fun aRemovedItemDoesNotFallBackToAnotherAction() {
        var calls = 0
        val items = listOf(PlatformMenuItem("report", "Report") { calls++ })
        performPlatformMenuAction(items, "edit", enabled = true)
        assertEquals(0, calls)
    }

    @Test fun anEnabledActionRunsOnce() {
        var calls = 0
        val items = listOf(PlatformMenuItem("edit", "Edit") { calls++ })
        performPlatformMenuAction(items, "edit", enabled = true)
        assertEquals(1, calls)
    }

    @Test fun aRecomposedMenuUsesTheCurrentCallback() {
        val calls = mutableListOf<String>()
        val old = listOf(PlatformMenuItem("edit", "Edit") { calls += "old" })
        val current = listOf(PlatformMenuItem("edit", "Edit") { calls += "current" })
        performPlatformMenuAction(old, "edit", enabled = false)
        performPlatformMenuAction(current, "edit", enabled = true)
        assertEquals(listOf("current"), calls)
    }
}
