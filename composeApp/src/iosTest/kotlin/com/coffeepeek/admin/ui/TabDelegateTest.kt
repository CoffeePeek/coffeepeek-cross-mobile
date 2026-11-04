@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import platform.UIKit.UINavigationController
import platform.UIKit.UITabBarController
import platform.UIKit.UIViewController

/** Native delegate wiring only: no window, Compose content, account, or services. */
class TabDelegateTest {
    @Test
    fun activeRootTabIsReselectedOnEveryTap() {
        val navigation = UINavigationController(rootViewController = UIViewController())
        val tabs = UITabBarController().apply { setViewControllers(listOf(navigation), false) }
        val delegate = TabDelegate()
        val reselected = mutableListOf<UIViewController>()
        delegate.onTabReselected = reselected::add

        repeat(2) {
            assertTrue(delegate.tabBarController(tabs, shouldSelectViewController = navigation))
        }

        assertEquals(listOf<UIViewController>(navigation, navigation), reselected)
    }

    @Test
    fun switchingTabsDoesNotReselectEitherRoot() {
        val first = UINavigationController(rootViewController = UIViewController())
        val second = UINavigationController(rootViewController = UIViewController())
        val tabs = UITabBarController().apply { setViewControllers(listOf(first, second), false) }
        val delegate = TabDelegate()
        var reselections = 0
        delegate.onTabReselected = { reselections++ }

        assertTrue(delegate.tabBarController(tabs, shouldSelectViewController = second))
        tabs.selectedViewController = second
        delegate.tabBarController(tabs, didSelectViewController = second)

        assertEquals(0, reselections)
    }

    @Test
    fun activeTabWithAPushedScreenDoesNotScrollItsHiddenRoot() {
        val navigation = UINavigationController(rootViewController = UIViewController())
        navigation.setViewControllers(listOf(navigation.viewControllers.first(), UIViewController()), false)
        val tabs = UITabBarController().apply { setViewControllers(listOf(navigation), false) }
        val delegate = TabDelegate()
        var reselections = 0
        delegate.onTabReselected = { reselections++ }

        assertTrue(delegate.tabBarController(tabs, shouldSelectViewController = navigation))

        assertEquals(0, reselections)
    }
}
