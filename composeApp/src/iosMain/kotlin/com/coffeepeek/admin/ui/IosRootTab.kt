@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.ui

import androidx.compose.runtime.Composable
import platform.UIKit.UIViewController

/** Ordered to match the stable UIKit indexes in [nativeTabIndex]. */
internal enum class IosRootTab(
    val screen: Navigator.Screen,
    val symbol: String,
    private val analyticsPage: String,
) {
    Discovery(Navigator.Screen.FeedTab, "magnifyingglass", "discovery"),
    Community(Navigator.Screen.CommunityTab, "person.2", "feed"),
    Profile(Navigator.Screen.ProfileTab, "person.crop.circle", "profile"),
    Settings(Navigator.Screen.SettingsTab, "gearshape", "settings"),
    Business(Navigator.Screen.BusinessTab, "building.2", "business_ownership");

    fun controller(
        title: String,
        nativeContent: (Navigator.Screen) -> (() -> UIViewController)?,
        content: @Composable () -> Unit,
    ): NativeScreenController = NativeScreenController(
        title,
        route = screen,
        analyticsPage = analyticsPage,
        isTabRoot = true,
        nativeContent = nativeContent(screen),
        content = content,
    )
}
