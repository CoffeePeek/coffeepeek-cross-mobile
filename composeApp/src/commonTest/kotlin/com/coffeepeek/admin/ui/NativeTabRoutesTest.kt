package com.coffeepeek.admin.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NativeTabRoutesTest {
    @Test
    fun homeAndFallbackLinksSelectDiscoveryInsteadOfPushingAnotherMainScreen() {
        listOf("https://coffeepeek.by/", "https://coffeepeek.by/coffee-shops/invalid").forEach { url ->
            assertEquals(0, nativeTabIndex(requireNotNull(appLinkScreen(url))))
        }
    }

    @Test
    fun detailLinksStayInTheNavigationStack() {
        assertNull(nativeTabIndex(requireNotNull(appLinkScreen("https://coffeepeek.by/coffee-shops/26-name"))))
    }

    @Test
    fun legacyGraphDestinationsAndTabDestinationsSelectTheSameNativeController() {
        val graphs = listOf(Navigator.Screen.FeedGraph, Navigator.Screen.CoffeeGraph, Navigator.Screen.CommunityGraph, Navigator.Screen.ProfileGraph, Navigator.Screen.SettingsGraph)
        val tabs = listOf(Navigator.Screen.FeedTab, Navigator.Screen.CoffeeTab, Navigator.Screen.CommunityTab, Navigator.Screen.ProfileTab, Navigator.Screen.SettingsTab)
        assertEquals(5, tabs.map(::nativeTabIndex).filterNotNull().distinct().size)
        graphs.zip(tabs).forEach { (graph, tab) -> assertEquals(nativeTabIndex(graph), nativeTabIndex(tab)) }
    }
}
