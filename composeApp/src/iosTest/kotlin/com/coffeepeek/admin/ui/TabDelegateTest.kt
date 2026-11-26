package com.coffeepeek.admin.ui

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** Kotlin tab/entry contracts; Swift owns UIKit delegates and lazy controller construction. */
@OptIn(ExperimentalCoroutinesApi::class)
class TabDelegateTest {
    @Test fun settingsRootRetainsTheSameEntryWhenSwitchingAwayAndBack() {
        val model = NativeRootNavigationModel(true)
        val settings = model.actual.stacks[3].single()
        model.confirm(model.plan(Navigator.NavEvent.NavigateTo(Navigator.Screen.SettingsTab)))
        model.confirm(model.plan(Navigator.NavEvent.NavigateTo(Navigator.Screen.FeedTab)))
        model.confirm(model.plan(Navigator.NavEvent.NavigateTo(Navigator.Screen.SettingsTab)))
        assertEquals(settings, model.actual.stacks[3].single())
        assertEquals(3, model.actual.selected)
    }

    @Test fun logicalRootOrderMatchesSharedTabIndexesIncludingTheBusinessRoot() {
        val model = NativeRootNavigationModel(true)
        model.actual.stacks.forEachIndexed { index, stack ->
            assertEquals(index, nativeTabIndex(stack.single().screen))
        }
        assertEquals(listOf(0, 1, 2, 3), model.visibleTabs)
        model.configure(true, true)
        assertEquals(listOf(4, 3), model.visibleTabs)
    }

    @Test fun eachSelectedSearchRootTapEmitsOneTransientReselection() = runTest {
        val gestures = MainTabReselections()
        var count = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { gestures.search.collect { count++ } }
        repeat(2) { gestures.onTabPressed(Navigator.Screen.FeedTab, true) }
        assertEquals(2, count)
    }

    @Test fun switchingTabsDoesNotEmitARootReselection() = runTest {
        val gestures = MainTabReselections()
        var count = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { gestures.search.collect { count++ } }
        gestures.onTabPressed(Navigator.Screen.FeedTab, false)
        gestures.onTabPressed(Navigator.Screen.CommunityTab, false)
        assertEquals(0, count)
    }

    @Test fun ordinaryTabSelectionRetainsItsPushedDetailForTheSwiftHost() {
        val model = NativeRootNavigationModel(true)
        model.confirm(model.plan(Navigator.NavEvent.NavigateTo(Navigator.Screen.ShopDetail("shop"))))
        val detail = model.actual.stacks[0].last()
        model.confirm(model.plan(Navigator.NavEvent.NavigateTo(Navigator.Screen.ProfileTab)))
        model.confirm(model.plan(Navigator.NavEvent.NavigateTo(Navigator.Screen.FeedTab)))
        assertEquals(detail, model.actual.stacks[0].last())
        assertEquals(2, model.actual.stacks[0].size)
    }
}
