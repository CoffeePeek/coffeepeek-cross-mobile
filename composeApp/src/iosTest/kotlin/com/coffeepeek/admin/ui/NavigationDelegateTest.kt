@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.ui

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import platform.UIKit.UIViewController

/** The former UIKit delegate now lives in Swift; these exercise its Kotlin callback boundary only. */
class NavigationDelegateTest {
    @AfterTest fun resetBridge() = NativeLocalBackProtection.configure({ _, _ -> }, { null })

    @Test fun nativeLocalStepsForwardProtectionWithoutOwningAUiContainer() {
        val controller = UIViewController()
        val values = mutableListOf<Boolean>()
        NativeLocalBackProtection.configure({ host, enabled ->
            assertEquals(controller, host)
            values += enabled
        }, { null })
        NativeLocalBackProtection.update(controller, true)
        NativeLocalBackProtection.update(controller, false)
        assertEquals(listOf(true, false), values)
    }

    @Test fun replacingTheSwiftHostStopsCallingThePreviousHost() {
        val controller = UIViewController()
        val values = mutableListOf<Boolean>()
        NativeLocalBackProtection.configure({ _, _ -> error("Retired host callback") }, { "old" })
        NativeLocalBackProtection.configure({ _, enabled -> values += enabled }, { "current" })
        NativeLocalBackProtection.update(controller, true)
        assertEquals(listOf(true), values)
        assertEquals("current", NativeLocalBackProtection.visit(controller))
    }

    @Test fun screensOutsideTheCurrentRouteDoNotAcquireAnAnalyticsVisit() {
        val hosted = UIViewController()
        val unrelated = UIViewController()
        NativeLocalBackProtection.configure({ _, _ -> }, { if (it == hosted) "visit" else null })
        assertEquals("visit", NativeLocalBackProtection.visit(hosted))
        assertNull(NativeLocalBackProtection.visit(unrelated))
        resetBridge()
        assertNull(NativeLocalBackProtection.visit(hosted))
    }
}
