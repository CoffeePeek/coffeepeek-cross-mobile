package com.coffeepeek.admin.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppLinksTest {
    @Test
    fun routesOnlyCoffeePeekLinks() {
        val shop = Navigator.Screen.ShopDetail("26")
        assertEquals(shop, appLinkScreen("https://coffeepeek.by/coffee-shops/26-ulyanovskaya-ulitsa-30"))
        assertEquals(shop, appLinkScreen("https://coffeepeek.by/coffee-shops/26/?from=share#photos"))
        assertEquals(Navigator.Screen.Main, appLinkScreen("https://coffeepeek.by/"))
        assertEquals(Navigator.Screen.Main, appLinkScreen("https://coffeepeek.by/coffee-shops/invalid"))
        assertEquals(Navigator.Screen.Main, appLinkScreen("https://coffeepeek.by/coffee-shops/26-slug/extra"))
        assertNull(appLinkScreen("https://coffeepeek.by.evil.com/coffee-shops/26-slug"))
        assertNull(appLinkScreen("https://coffeepeek.by@evil.com/coffee-shops/26-slug"))
        assertNull(appLinkScreen("ftp://coffeepeek.by/coffee-shops/26-slug"))
    }
}
