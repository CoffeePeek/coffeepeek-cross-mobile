package com.coffeepeek.admin.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NativeTabRoutesTest {
    @Test
    fun guestTabsExcludeProfileButPreserveSettingsIdentity() {
        assertEquals(
            listOf(Navigator.Screen.FeedTab, Navigator.Screen.CoffeeTab, Navigator.Screen.CommunityTab, Navigator.Screen.SettingsTab),
            mainTabScreens(isLoggedIn = false),
        )
        assertEquals(4, nativeTabIndex(mainTabScreens(isLoggedIn = false).last()))
    }

    @Test
    fun authenticatedTabsRestoreProfileWithoutChangingOtherTabIdentities() {
        val authenticated = mainTabScreens(isLoggedIn = true)
        assertEquals(5, authenticated.size)
        assertEquals(Navigator.Screen.ProfileTab, authenticated[3])
        assertEquals(mainTabScreens(isLoggedIn = false), authenticated.filterNot { it == Navigator.Screen.ProfileTab })
    }

    @Test
    fun homeAndFallbackLinksSelectDiscoveryInsteadOfPushingAnotherMainScreen() {
        listOf("https://coffeepeek.by/", "https://coffeepeek.by/coffee-shops/invalid").forEach { url ->
            assertEquals(0, nativeTabIndex(requireNotNull(appLinkScreen(url))))
        }
    }

    @Test
    fun detailAndPasswordLinksStayInTheNavigationStack() {
        listOf("https://coffeepeek.by/coffee-shops/26-name", "https://coffeepeek.by/reset-password?token=test").forEach { url ->
            assertNull(nativeTabIndex(requireNotNull(appLinkScreen(url))))
        }
    }

    @Test
    fun legacyGraphDestinationsAndTabDestinationsSelectTheSameNativeController() {
        val graphs = listOf(Navigator.Screen.FeedGraph, Navigator.Screen.CoffeeGraph, Navigator.Screen.CommunityGraph, Navigator.Screen.ProfileGraph, Navigator.Screen.SettingsGraph)
        val tabs = listOf(Navigator.Screen.FeedTab, Navigator.Screen.CoffeeTab, Navigator.Screen.CommunityTab, Navigator.Screen.ProfileTab, Navigator.Screen.SettingsTab)
        assertEquals(5, tabs.map(::nativeTabIndex).filterNotNull().distinct().size)
        graphs.zip(tabs).forEach { (graph, tab) -> assertEquals(nativeTabIndex(graph), nativeTabIndex(tab)) }
    }
}
