package com.coffeepeek.admin.ui

import kotlin.test.Test
import kotlin.test.assertEquals

/** Confirmed stack inputs for Swift tab-bar layout. UIKit/map gestures require manual device review. */
class NativeTabBarVisibilityTest {
    @Test fun planningAPushKeepsTheConfirmedRootUntilTheNativeTransitionFinishes() {
        val model = NativeRootNavigationModel(true)
        val root = model.actual
        val next = model.plan(Navigator.NavEvent.NavigateTo(Navigator.Screen.ShopDetail("shop")))
        assertEquals(2, next.stacks[0].size)
        assertEquals(root, model.actual)
        model.confirm(next)
        assertEquals(2, model.actual.stacks[0].size)
    }

    @Test fun cancelledNativePopRetainsTheDetailAndItsIdentity() {
        val model = detailModel()
        val shown = model.actual
        model.plan(Navigator.NavEvent.PopBack)
        model.confirm(shown)
        assertEquals(shown, model.actual)
    }

    @Test fun completedNativePopReturnsToTheOriginalRootIdentity() {
        val model = detailModel()
        val root = model.actual.stacks[0].first()
        model.confirm(model.plan(Navigator.NavEvent.PopBack))
        assertEquals(listOf(root), model.actual.stacks[0])
    }

    @Test fun aRetainedDiscoveryStackCannotReplaceAnotherSelectedTab() {
        val model = detailModel()
        val discovery = model.actual.stacks[0]
        model.confirm(model.plan(Navigator.NavEvent.NavigateTo(Navigator.Screen.ProfileTab)))
        assertEquals(2, model.actual.selected)
        assertEquals(discovery, model.actual.stacks[0])
        assertEquals(Navigator.Screen.ProfileTab, model.actual.stacks[2].single().screen)
    }

    @Test fun backAtTheRootCannotRemoveTheTabRoot() {
        val model = NativeRootNavigationModel(true)
        val initial = model.actual
        model.confirm(model.plan(Navigator.NavEvent.PopBack))
        assertEquals(initial, model.actual)
    }

    private fun detailModel() = NativeRootNavigationModel(true).apply {
        confirm(plan(Navigator.NavEvent.NavigateTo(Navigator.Screen.ShopDetail("shop"))))
    }
}
