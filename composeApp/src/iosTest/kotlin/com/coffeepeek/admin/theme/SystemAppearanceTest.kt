package com.coffeepeek.admin.theme

import platform.UIKit.UIUserInterfaceStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SystemAppearanceTest {
    @Test
    fun systemModeReleasesForcedWindowAndControllerAppearance() {
        assertEquals(UIUserInterfaceStyle.UIUserInterfaceStyleUnspecified, ThemeMode.SYSTEM.uiKitStyle)
        assertEquals(UIUserInterfaceStyle.UIUserInterfaceStyleDark, ThemeMode.DARK.uiKitStyle)
        assertEquals(UIUserInterfaceStyle.UIUserInterfaceStyleLight, ThemeMode.LIGHT.uiKitStyle)
    }

    @Test
    fun sceneAppearanceWinsOverThePreviouslyForcedComposeAppearance() {
        assertFalse(systemDarkTheme(UIUserInterfaceStyle.UIUserInterfaceStyleLight, fallback = true))
        assertTrue(systemDarkTheme(UIUserInterfaceStyle.UIUserInterfaceStyleDark, fallback = false))
        assertTrue(systemDarkTheme(UIUserInterfaceStyle.UIUserInterfaceStyleUnspecified, fallback = true))
        assertFalse(systemDarkTheme(null, fallback = false))
    }
}
