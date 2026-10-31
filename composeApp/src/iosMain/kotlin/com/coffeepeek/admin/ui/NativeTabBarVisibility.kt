package com.coffeepeek.admin.ui

import platform.UIKit.UINavigationController
import platform.UIKit.UITabBarController
import platform.UIKit.hidesBottomBarWhenPushed

/** Root composition owns tab visibility; a retained, hidden map cannot hide another tab. */
internal class NativeTabBarVisibility(
    private val tabs: UITabBarController,
    private val discoveryNavigation: UINavigationController,
) {
    private var mapExpanded = false

    fun onMapExpandedChange(expanded: Boolean) {
        mapExpanded = expanded
        sync()
    }

    fun sync() {
        val navigation = tabs.selectedViewController as? UINavigationController ?: return
        val top = navigation.topViewController
        val expandedMapVisible = mapExpanded && navigation === discoveryNavigation &&
            top === navigation.viewControllers.firstOrNull()
        val hidden = expandedMapVisible || top?.hidesBottomBarWhenPushed == true
        tabs.setTabBarHidden(hidden, animated = false)
        navigation.viewControllers.forEach { (it as? NativeScreenController)?.syncBottomClearance(hidden) }
    }
}
