@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.ui

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import platform.UIKit.UINavigationController
import platform.UIKit.UITabBarController
import platform.UIKit.UIViewController

/** Native state contracts only: no window, Compose, account or application services. */
class NativeTabBarVisibilityTest {
    private val discovery = UINavigationController(rootViewController = UIViewController())
    private val profile = UINavigationController(rootViewController = UIViewController())
    private val tabs = UITabBarController().apply { setViewControllers(listOf(discovery, profile), false) }
    private val visibility = NativeTabBarVisibility(tabs, discovery)

    @Test
    fun expandingAndClosingMapHidesAndRestoresTabs() {
        visibility.onMapExpandedChange(true)
        assertTrue(tabs.tabBarHidden)
        visibility.onMapExpandedChange(false)
        assertFalse(tabs.tabBarHidden)
    }

    @Test
    fun retainedMapCannotHideAnotherTab() {
        visibility.onMapExpandedChange(true)
        tabs.selectedViewController = profile
        visibility.sync()
        assertFalse(tabs.tabBarHidden)
        tabs.selectedViewController = discovery
        visibility.sync()
        assertTrue(tabs.tabBarHidden)
    }

    @Test
    fun mapCallbackCannotShowBarOverPushedDetail() {
        discovery.pushViewController(UIViewController().apply { hidesBottomBarWhenPushed = true }, false)
        visibility.onMapExpandedChange(false)
        assertTrue(tabs.tabBarHidden)
        discovery.popToRootViewControllerAnimated(false)
        visibility.sync()
        assertFalse(tabs.tabBarHidden)
    }

    @Test
    fun returningFromDetailKeepsExpandedMapFullScreen() {
        visibility.onMapExpandedChange(true)
        discovery.pushViewController(UIViewController().apply { hidesBottomBarWhenPushed = true }, false)
        visibility.sync()
        assertTrue(tabs.tabBarHidden)
        discovery.popToRootViewControllerAnimated(false)
        visibility.sync()
        assertTrue(tabs.tabBarHidden)
    }
}
