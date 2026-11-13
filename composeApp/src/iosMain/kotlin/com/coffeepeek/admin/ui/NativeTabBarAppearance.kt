@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.ui

import com.coffeepeek.core.designsystem.component.coffeePeekSystemMaterial
import kotlinx.cinterop.useContents
import platform.Foundation.NSProcessInfo
import platform.UIKit.UINavigationController
import platform.UIKit.UITabBar
import platform.UIKit.UITabBarAppearance
import platform.UIKit.UITabBarController

internal fun UITabBarController.installRootTabs(controllers: List<UINavigationController>, selectedTab: Int) {
    setViewControllers(controllers, false)
    tabBar.applyMaterialBeforeLiquidGlass()
    selectedIndex = selectedTab.toULong()
    customizableViewControllers = null
}

internal fun UITabBar.applyMaterialBeforeLiquidGlass() {
    if (NSProcessInfo.processInfo.operatingSystemVersion.useContents { majorVersion } >= 26) return

    val appearance = UITabBarAppearance().apply {
        configureWithDefaultBackground()
        backgroundEffect = coffeePeekSystemMaterial()
    }
    standardAppearance = appearance
    scrollEdgeAppearance = appearance
}
