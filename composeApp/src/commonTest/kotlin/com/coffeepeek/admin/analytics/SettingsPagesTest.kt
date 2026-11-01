package com.coffeepeek.admin.analytics

import kotlin.test.Test
import kotlin.test.assertEquals

/** Pins the screen-page names of the Settings routes (settings migration step 1): they come from the route class names, which must not change. */
class SettingsPagesTest {
    private fun page(route: String) = analyticsPage("com.coffeepeek.admin.ui.Navigator.Screen.$route")

    @Test fun settingsRoutesKeepTheirPages() {
        listOf("SettingsTab", "SettingsGraph", "CitySettings", "ThemeSettings", "LanguageSettings", "NotificationSettings")
            .forEach { assertEquals("settings", page(it), it) }
        assertEquals("privacy", page("UsagePolicy"))
    }

    @Test fun blockedAuthorsIsStillUnmapped() = assertEquals("other", page("BlockedAuthors"))
}
