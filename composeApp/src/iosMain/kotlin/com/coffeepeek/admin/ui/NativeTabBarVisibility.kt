package com.coffeepeek.admin.ui

import platform.UIKit.UINavigationController
import platform.UIKit.UITabBarController
import platform.UIKit.hidesBottomBarWhenPushed

/** Root composition owns tab visibility; a retained, hidden map cannot hide another tab. */
internal class NativeTabBarVisibility(
    tabs: UITabBarController,
    discoveryNavigation: UINavigationController,
) {
    private var tabs: UITabBarController? = tabs
    private var discoveryNavigation: UINavigationController? = discoveryNavigation
    private var mapExpanded = false

    fun onMapExpandedChange(expanded: Boolean) {
        mapExpanded = expanded
        sync()
    }

    fun sync() {
        val tabs = tabs ?: return
        (discoveryNavigation?.viewControllers?.firstOrNull() as? NativeScreenController)?.syncExpandedMap(mapExpanded)
        val navigation = tabs.selectedViewController as? UINavigationController ?: return
        val top = navigation.topViewController
        // UIKit getters may return different Kotlin wrappers for the same native controller.
        val expandedMapVisible = mapExpanded && navigation == discoveryNavigation &&
            top == navigation.viewControllers.firstOrNull()
        val hidden = expandedMapVisible || top?.hidesBottomBarWhenPushed == true
        tabs.setTabBarHidden(hidden, animated = false)
        navigation.viewControllers.forEach { (it as? NativeScreenController)?.syncBottomClearance(hidden) }
    }

    fun dispose() {
        // Root content captures this callback; break its reference back to the UIKit tree.
        tabs = null
        discoveryNavigation = null
    }
}
