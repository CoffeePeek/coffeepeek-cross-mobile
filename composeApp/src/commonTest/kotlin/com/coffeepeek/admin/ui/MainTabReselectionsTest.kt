package com.coffeepeek.admin.ui

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class MainTabReselectionsTest {
    @Test
    fun searchRetapIsIndependentFromOrdinaryTabChanges() = runTest {
        val reselections = MainTabReselections()
        var requests = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            reselections.search.collect { requests++ }
        }
        reselections.onTabPressed(Navigator.Screen.FeedTab, isSelected = false)
        reselections.onTabPressed(Navigator.Screen.CommunityTab, isSelected = true)
        assertEquals(0, requests)
        repeat(2) { reselections.onTabPressed(Navigator.Screen.FeedTab, isSelected = true) }
        assertEquals(2, requests)
    }

    @Test
    fun searchGesturesAreNotReplayedAfterCollectorReturns() = runTest {
        val reselections = MainTabReselections()
        reselections.onTabPressed(Navigator.Screen.FeedTab, isSelected = true)
        var requests = 0
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            reselections.search.collect { requests++ }
        }
        assertEquals(0, requests)
        reselections.onTabPressed(Navigator.Screen.FeedTab, isSelected = true)
        assertEquals(1, requests)
        collector.cancel()
        reselections.onTabPressed(Navigator.Screen.FeedTab, isSelected = true)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            reselections.search.collect { requests++ }
        }
        assertEquals(1, requests)
    }

    @Test
    fun recreatingTheTabHostStartsWithoutASearchGesture() = runTest {
        MainTabReselections().onTabPressed(Navigator.Screen.FeedTab, isSelected = true)
        val recreated = MainTabReselections()
        var requests = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            recreated.search.collect { requests++ }
        }
        assertEquals(0, requests)
    }
}
