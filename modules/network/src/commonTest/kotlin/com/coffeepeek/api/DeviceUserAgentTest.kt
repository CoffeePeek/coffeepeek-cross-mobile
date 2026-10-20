package com.coffeepeek.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeviceUserAgentTest {
    @Test fun pixelIncludesManufacturerModelAndAndroidRelease() {
        assertEquals("CoffeePeek (Google Pixel 9; Android 16)",
            formatDeviceUserAgent(androidDeviceModel("Google", "Pixel 9"), "Android 16"))
        assertEquals("Samsung Galaxy S24", androidDeviceModel("Samsung", "Samsung Galaxy S24"))
        assertEquals("samsung SM-S921B", androidDeviceModel("samsung", "SM-S921B"))
        assertEquals("Pixel 9", androidDeviceModel("", "Pixel 9"))
    }

    @Test fun iphoneHardwareIdentifiersResolveToProductNames() {
        val models = mapOf(
            "iPhone17,3" to "iPhone 16", "iPhone17,4" to "iPhone 16 Plus",
            "iPhone17,1" to "iPhone 16 Pro", "iPhone17,2" to "iPhone 16 Pro Max",
            "iPhone17,5" to "iPhone 16e", "iPhone16,1" to "iPhone 15 Pro",
            "iPhone13,1" to "iPhone 12 mini", "iPhone14,6" to "iPhone SE 3",
            "iPhone8,1" to "iPhone 6s", "iPhone18,4" to "iPhone Air",
            "iPhone19,7" to "iPhone 18 Pro Max",
        )
        for ((identifier, name) in models) assertEquals(name, appleDeviceModel(identifier))
        assertEquals("CoffeePeek (iPhone 16; iOS 26.0.1)",
            formatDeviceUserAgent(appleDeviceModel("iPhone17,3"), "iOS 26.0.1"))
    }

    @Test fun unknownHardwareKeepsItsIdentifierAndSimulatorsAreIdentified() {
        assertEquals("iPhone99,1", appleDeviceModel("iPhone99,1"))
        assertEquals("iPad99,1", appleDeviceModel("iPad99,1"))
        for (architecture in listOf("arm64", "x86_64", "i386")) {
            assertEquals("iOS Simulator", appleDeviceModel(architecture))
        }
    }

    @Test fun deviceMetadataFitsTheStoredSessionFieldAndCannotInjectHeaders() {
        val agent = formatDeviceUserAgent("x".repeat(300) + "\r\nInjected: value", "y".repeat(300))
        assertTrue(agent.length <= 255)
        assertTrue(agent.all { it in ' '..'~' })
        assertEquals("CoffeePeek (Pixel  9; Android 16)",
            formatDeviceUserAgent("Pixel\r\n9()\\", "Android 16"))
        assertEquals("CoffeePeek (Unknown; Unknown)", formatDeviceUserAgent("", ""))
    }
}
