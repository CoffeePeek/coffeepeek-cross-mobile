package com.coffeepeek.admin.ui

internal fun mainTabScreens(isLoggedIn: Boolean, businessMode: Boolean = false): List<Navigator.Screen> = buildList {
    if (isLoggedIn && businessMode) {
        add(Navigator.Screen.BusinessTab)
        add(Navigator.Screen.SettingsTab)
        return@buildList
    }
    add(Navigator.Screen.FeedTab)
    add(Navigator.Screen.CommunityTab)
    add(Navigator.Screen.ProfileTab)
    add(Navigator.Screen.SettingsTab)
}

/** Stable tab identities used by the native shell, including every route a tab can start from. */
internal fun nativeTabIndex(screen: Navigator.Screen): Int? = when (screen) {
    Navigator.Screen.Main, Navigator.Screen.FeedTab, Navigator.Screen.FeedGraph -> 0
    Navigator.Screen.BusinessTab, Navigator.Screen.BusinessGraph -> 4
    Navigator.Screen.CommunityTab, Navigator.Screen.CommunityGraph -> 1
    Navigator.Screen.ProfileTab, Navigator.Screen.ProfileGraph -> 2
    Navigator.Screen.SettingsTab, Navigator.Screen.SettingsGraph -> 3
    else -> null
}
