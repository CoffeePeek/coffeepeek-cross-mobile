package com.coffeepeek.admin.map

import kotlin.test.Test
import kotlin.test.assertEquals

class ZoneColorTest {

    @Test
    fun usesBackendColorForBothThemes() {
        assertEquals("#A34D70", zoneColorForMap(" #a34d70 ", isDarkTheme = false))
        assertEquals("#A34D70", zoneColorForMap("#a34d70", isDarkTheme = true))
        assertEquals("#FF8800", zoneColorForMap("f80", isDarkTheme = false))
    }

    @Test
    fun fallsBackWhenColorIsMissingOrInvalid() {
        assertEquals("#B07A45", zoneColorForMap(null, isDarkTheme = false))
        assertEquals("#D2A26E", zoneColorForMap("not-a-color", isDarkTheme = true))
    }
}
