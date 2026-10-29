@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.ui

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import platform.UIKit.UINavigationController
import platform.UIKit.UIViewController

/** Navigation wiring only: no window, Compose content, account, or services. */
class NavigationDelegateTest {
    @Test
    fun showingDetailRestoresDisabledSystemGestureAndItsDelegate() {
        val root = UIViewController()
        val detail = UIViewController()
        val navigation = UINavigationController(rootViewController = root)
        val delegate = NavigationDelegate().apply { this.navigation = navigation }
        navigation.setViewControllers(listOf(root, detail), false)
        navigation.loadViewIfNeeded()
        navigation.setNavigationBarHidden(true, false)
        val gesture = assertNotNull(navigation.interactivePopGestureRecognizer)

        repeat(2) {
            gesture.enabled = false
            gesture.delegate = null
            delegate.navigationController(navigation, didShowViewController = detail, animated = false)

            assertTrue(gesture.enabled)
            assertSame(delegate, gesture.delegate)
            assertTrue(delegate.gestureRecognizerShouldBegin(gesture))
        }
    }

    @Test
    fun returningToRootDisablesSystemGestureAndNotifiesStackObserver() {
        val root = UIViewController()
        val navigation = UINavigationController(rootViewController = root)
        val delegate = NavigationDelegate().apply { this.navigation = navigation }
        var stackChanged = false
        delegate.onStackChanged = { stackChanged = true }
        navigation.loadViewIfNeeded()
        val gesture = assertNotNull(navigation.interactivePopGestureRecognizer)
        gesture.enabled = true

        delegate.navigationController(navigation, didShowViewController = root, animated = false)

        assertFalse(gesture.enabled)
        assertFalse(delegate.gestureRecognizerShouldBegin(gesture))
        assertTrue(stackChanged)
    }
}
