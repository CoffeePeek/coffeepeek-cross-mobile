package com.coffeepeek.admin.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppLinksTest {
    @Test
    fun routesPublicProfilesWithoutTreatingNicknameAsAnId() {
        assertEquals(Navigator.Screen.UserProfile("glasha"), appLinkScreen("https://coffeepeek.by/users/glasha"))
        assertEquals(Navigator.Screen.UserProfile("old-slug"), appLinkScreen("https://coffeepeek.by/users/old-slug/?from=share#profile"))
    }

    @Test
    fun routesCoffeeShopsWithTheirFullSlug() {
        val slug = "marks-prospekt-nezavisimosti-83"
        assertEquals(Navigator.Screen.ShopDetail(slug), appLinkScreen("https://coffeepeek.by/coffee-shops/$slug"))
        assertEquals(Navigator.Screen.ShopDetail(slug), appLinkScreen("https://coffeepeek.by/coffee-shops/$slug/?from=share#photos"))
        assertEquals(
            Navigator.Screen.ShopDetail("1801-kofe-pr-nezavisimosti-95"),
            appLinkScreen("https://coffeepeek.by/coffee-shops/1801-kofe-pr-nezavisimosti-95"),
        )
    }

    @Test
    fun routesRoasters() {
        val roaster = Navigator.Screen.RoasterDetail("roast")
        assertEquals(roaster, appLinkScreen("https://coffeepeek.by/roasters/roast"))
        assertEquals(roaster, appLinkScreen("https://coffeepeek.by/roasters/roast/?from=share#coffee"))
    }

    @Test
    fun routesCoffeeAndLegacyShopLinks() {
        assertEquals(Navigator.Screen.CoffeeDetail("ethiopia"), appLinkScreen("https://coffeepeek.by/coffees/ethiopia"))
        assertEquals(Navigator.Screen.CoffeeDetail("ethiopia"), appLinkScreen("https://coffeepeek.by/coffees/ethiopia/?from=share#photos"))
        assertEquals(Navigator.Screen.ShopDetail("1801"), appLinkScreen("https://coffeepeek.by/shops/1801"))
    }

    @Test
    fun routesSharedPostsByTheirServerId() {
        val id = "6966d36f-f903-ccf8-8009-1a651d0c58ad"
        assertEquals(Navigator.Screen.CheckInDetail(id), appLinkScreen("https://coffeepeek.by/check-ins/$id"))
        assertEquals(Navigator.Screen.CheckInDetail(id), appLinkScreen("https://coffeepeek.by/check-ins/$id/?from=share#photos"))
        val sharedId = "d672cae3-610f-199d-c7ef-b671d3dab7f7"
        assertEquals(Navigator.Screen.CheckInDetail(sharedId), appLinkScreen("https://coffeepeek.by/check-ins/$sharedId"))
    }

    @Test
    fun aPostLinkWithoutAServerIdOrWithAnActionOpensTheMainScreen() {
        for (path in listOf("not-an-id", "6966d36f", "6966d36f-f903-ccf8-8009-1a651d0c58ad-extra", "6966d36f-f903-ccf8-8009-1a651d0c58ad/edit", "..")) {
            val link = "https://coffeepeek.by/check-ins/$path"
            assertEquals(Navigator.Screen.Main, appLinkScreen(link), link)
        }
    }

    @Test
    fun rejectsInvalidSlugsAndExtraPathSegments() {
        for (path in listOf("coffee-shops", "shops", "roasters", "coffees", "users")) {
            for (slug in listOf("", ".", "..", "%2e%2e", "%20", "a%2Fb", "a%5Cb", "a%00b", "a%0Ab", "slug/extra")) {
                val link = "https://coffeepeek.by/$path/$slug"
                assertEquals(Navigator.Screen.Main, appLinkScreen(link), link)
            }
        }
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
        assertEquals(
            Navigator.Screen.ShopDetail("marks-prospekt-nezavisimosti-83"),
            appLinkScreen("http://COFFEEPEEK.BY/coffee-shops/marks-prospekt-nezavisimosti-83"),
        )
        assertEquals(Navigator.Screen.Main, appLinkScreen("https://coffeepeek.by/"))
        assertNull(appLinkScreen("https://coffeepeek.by.evil.com/coffee-shops/marks-prospekt-nezavisimosti-83"))
        assertNull(appLinkScreen("https://coffeepeek.by@evil.com/roasters/roast"))
        assertNull(appLinkScreen("https://coffeepeek.by.evil.com/users/glasha"))
        assertNull(appLinkScreen("ftp://coffeepeek.by/users/glasha"))
    }
}
