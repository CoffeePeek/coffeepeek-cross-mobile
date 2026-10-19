package com.coffeepeek.admin.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppLinksTest {
    @Test
    fun routesPublicProfilesWithoutTreatingNicknameAsAnId() {
        assertEquals(Navigator.Screen.UserProfile("glasha"), appLinkScreen("https://coffeepeek.by/users/glasha"))
        assertEquals(Navigator.Screen.UserProfile("old-slug"), appLinkScreen("https://coffeepeek.by/users/old-slug/?from=share"))
        assertEquals(Navigator.Screen.Main, appLinkScreen("https://coffeepeek.by/users/glasha/extra"))
        assertEquals(Navigator.Screen.Main, appLinkScreen("https://coffeepeek.by/users/a%2Fb"))
        assertNull(appLinkScreen("https://coffeepeek.by.evil.com/users/glasha"))
    }

    @Test
    fun leavesPasswordResetLinksOnTheWebsite() {
        assertNull(appLinkScreen("https://coffeepeek.by/reset-password?token=a%2Bb%2Fc%3D%3D"))
        assertNull(appLinkScreen("https://coffeepeek.by/reset-password?token="))
        assertNull(appLinkScreen("https://coffeepeek.by/reset-password"))
        assertNull(appLinkScreen("https://coffeepeek.by/reset-password/?token=secret"))
        assertNull(appLinkScreen("https://coffeepeek.by.evil.com/reset-password?token=secret"))
        assertNull(appLinkScreen("http://coffeepeek.by/reset-password?token=secret"))
    }

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
