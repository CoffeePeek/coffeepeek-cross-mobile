package com.coffeepeek.admin.ui.screen.checkins

import android.Manifest
import androidx.test.platform.app.InstrumentationRegistry

/** MainActivity can request location before a test replaces its application content. */
internal fun grantCheckInFixtureStartupPermissions() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val packageName = instrumentation.targetContext.packageName
    instrumentation.uiAutomation.grantRuntimePermission(packageName, Manifest.permission.ACCESS_COARSE_LOCATION)
    instrumentation.uiAutomation.grantRuntimePermission(packageName, Manifest.permission.ACCESS_FINE_LOCATION)
}
