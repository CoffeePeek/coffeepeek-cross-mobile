package com.coffeepeek.admin.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NativeTabRoutesTest {
    @Test
    fun guestTabsIncludeProfileAndPreserveNativeTabIdentities() {
        val guest = mainTabScreens(isLoggedIn = false)
        assertEquals(
            listOf(Navigator.Screen.FeedTab, Navigator.Screen.CommunityTab, Navigator.Screen.ProfileTab, Navigator.Screen.SettingsTab),
            guest,
        )
        assertEquals(listOf(0, 1, 2, 3), guest.map(::nativeTabIndex))
        assertEquals(guest, mainTabScreens(isLoggedIn = true))
    }

    @Test
    fun guestProfileTabStaysSelectableThroughRootNavigation() {
        listOf(Navigator.Screen.ProfileGraph, Navigator.Screen.ProfileTab).forEach { route ->
            assertEquals(route, route.profileDestination(isLoggedIn = false))
        }
    }

    @Test
    fun businessTabsRequireAnAuthenticatedSession() {
        assertEquals(mainTabScreens(isLoggedIn = false), mainTabScreens(isLoggedIn = false, businessMode = true))
        assertEquals(listOf(Navigator.Screen.BusinessTab, Navigator.Screen.SettingsTab),
            mainTabScreens(isLoggedIn = true, businessMode = true))
    }

    @Test
    fun homeAndFallbackLinksSelectDiscoveryInsteadOfPushingAnotherMainScreen() {
        listOf("https://coffeepeek.by/", "https://coffeepeek.by/coffee-shops/invalid/extra").forEach { url ->
            assertEquals(0, nativeTabIndex(requireNotNull(appLinkScreen(url))))
        }
    }

    @Test
    fun detailLinksStayInTheNavigationStack() {
        listOf(
            "https://coffeepeek.by/coffee-shops/marks-prospekt-nezavisimosti-83",
            "https://coffeepeek.by/roasters/roast",
            "https://coffeepeek.by/users/glasha",
        ).forEach { url ->
            assertNull(nativeTabIndex(requireNotNull(appLinkScreen(url))))
        }
    }

    @Test
    fun existingGraphDestinationsAndTabDestinationsSelectTheSameNativeController() {
        val graphs = listOf(Navigator.Screen.FeedGraph, Navigator.Screen.CommunityGraph, Navigator.Screen.ProfileGraph, Navigator.Screen.SettingsGraph)
        val tabs = listOf(Navigator.Screen.FeedTab, Navigator.Screen.CommunityTab, Navigator.Screen.ProfileTab, Navigator.Screen.SettingsTab)
        assertEquals(4, tabs.map(::nativeTabIndex).filterNotNull().distinct().size)
        graphs.zip(tabs).forEach { (graph, tab) -> assertEquals(nativeTabIndex(graph), nativeTabIndex(tab)) }
    }
}
