package com.coffeepeek.admin

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppLinkHandlingTest {
    @Test
    fun acceptsEveryPublicContentLinkIncludingPosts() {
        val paths = listOf(
            "",
            "check-ins/d672cae3-610f-199d-c7ef-b671d3dab7f7",
            "coffee-shops/1801",
            "shops/1801",
            "roasters/roast",
            "coffees/ethiopia",
            "users/glasha",
        )
        for (path in paths) assertTrue(handleAppLink("https://coffeepeek.by/$path"), path)
    }

    @Test
    fun rejectsOtherDomainsAndWebPasswordRecovery() {
        assertFalse(handleAppLink("https://coffeepeek.by.evil.com/check-ins/d672cae3-610f-199d-c7ef-b671d3dab7f7"))
        assertFalse(handleAppLink("https://coffeepeek.by/reset-password?token=example"))
    }
}
